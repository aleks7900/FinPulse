<#
.SYNOPSIS
    Generates a secure release keystore and key.properties file for Fin Pulse Google Play publication.
#>

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$keystorePath = Join-Path $projectRoot "release.keystore"
$keyPropsPath = Join-Path $projectRoot "key.properties"

Write-Host "=== Fin Pulse Release Keystore Generator ===" -ForegroundColor Green

if (Test-Path $keystorePath) {
    Write-Warning "Keystore already exists at: $keystorePath"
    $overwrite = Read-Host "Do you want to overwrite it? (y/N)"
    if ($overwrite -ne "y" -and $overwrite -ne "Y") {
        Write-Host "Aborted. Existing keystore preserved."
        exit 0
    }
}

$storePass = Read-Host -Prompt "Enter Keystore Password (min 6 characters)"
if ($storePass.Length -lt 6) {
    Write-Error "Password must be at least 6 characters."
    exit 1
}

$keyAlias = "finpulse_release_key"
$dname = "CN=Fin Pulse, OU=Finance, O=FinPulse, L=San Francisco, ST=California, C=US"

# Resolve keytool.exe path
$keytoolCmd = "keytool"
$cmd = Get-Command "keytool" -ErrorAction SilentlyContinue
if ($cmd) {
    $keytoolCmd = $cmd.Source
} elseif ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\keytool.exe")) {
    $keytoolCmd = "$env:JAVA_HOME\bin\keytool.exe"
} else {
    $candidates = @(
        Get-ChildItem -Path "$env:USERPROFILE\.jdks" -Filter "keytool.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
        Get-ChildItem -Path "C:\Program Files\Java", "C:\Program Files\Android" -Filter "keytool.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
    )
    if ($candidates.Count -gt 0) {
        $keytoolCmd = $candidates[0]
    } else {
        Write-Error "Could not locate keytool.exe. Please ensure a JDK is installed."
        exit 1
    }
}
Write-Host "Using keytool from: $keytoolCmd" -ForegroundColor DarkGray

& $keytoolCmd -genkeypair `
    -v `
    -keystore $keystorePath `
    -alias $keyAlias `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -storepass $storePass `
    -keypass $storePass `
    -dname $dname

if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to generate keystore using keytool."
    exit 1
}

Write-Host "`nCreating key.properties..." -ForegroundColor Cyan
$propsContent = @"
storeFile=release.keystore
storePassword=$storePass
keyAlias=$keyAlias
keyPassword=$storePass
"@

Set-Content -Path $keyPropsPath -Value $propsContent -Encoding Ascii

Write-Host "`n[SUCCESS] Release keystore generated at: $keystorePath" -ForegroundColor Green
Write-Host "[SUCCESS] key.properties created at: $keyPropsPath" -ForegroundColor Green
Write-Host "`nYou can now build the release Android App Bundle for Google Play:" -ForegroundColor Yellow
Write-Host "  .\gradlew bundleRelease" -ForegroundColor White

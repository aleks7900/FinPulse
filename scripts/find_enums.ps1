$ktFiles = Get-ChildItem -Path "app/src/main/java" -Filter "*.kt" -Recurse

foreach ($f in $ktFiles) {
    $content = Get-Content $f.FullName -Raw
    $matches = [regex]::Matches($content, 'enum\s+class\s+([A-Za-z0-9_]+)\s*\(([^)]+)\)')
    foreach ($m in $matches) {
        Write-Host "Enum in $($f.Name): $($m.Groups[1].Value) with params: $($m.Groups[2].Value)"
    }
}

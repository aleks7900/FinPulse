$ktFiles = Get-ChildItem -Path "app/src/main/java" -Filter "*.kt" -Recurse

$hardcodedText = @()
$hardcodedTitle = @()

foreach ($file in $ktFiles) {
    $lines = Get-Content $file.FullName
    for ($i = 0; $i -lt $lines.Length; $i++) {
        $line = $lines[$i]
        $lineNum = $i + 1
        
        # Check for Text("...") where string doesn't start with variable and has letters
        if ($line -match 'Text\(\s*"([A-Za-z][^"]+)"' -and $line -notmatch 'Text\(\s*"\$') {
            $hardcodedText += [PSCustomObject]@{
                File = $file.FullName.Replace((Get-Location).Path + "\", "")
                Line = $lineNum
                Match = $Matches[1]
                Snippet = $line.Trim()
            }
        }
        
        # Check for title = "..." or label = "..."
        if ($line -match '(title|label|placeholder|description)\s*=\s*"([A-Za-z][^"]+)"' -and $line -notmatch '\$') {
            $hardcodedTitle += [PSCustomObject]@{
                File = $file.FullName.Replace((Get-Location).Path + "\", "")
                Line = $lineNum
                Match = $Matches[2]
                Snippet = $line.Trim()
            }
        }
    }
}

Write-Host "Found $($hardcodedText.Count) hardcoded Text(...) strings:"
$hardcodedText | Group-Object File | ForEach-Object {
    Write-Host "`nFile: $($_.Name)" -ForegroundColor Yellow
    $_.Group | ForEach-Object {
        Write-Host "  L$($_.Line): $($_.Match)"
    }
}

Write-Host "`n=========================================="
Write-Host "Found $($hardcodedTitle.Count) hardcoded title/label/placeholder strings:"
$hardcodedTitle | Group-Object File | ForEach-Object {
    Write-Host "`nFile: $($_.Name)" -ForegroundColor Cyan
    $_.Group | ForEach-Object {
        Write-Host "  L$($_.Line): $($_.Match)"
    }
}

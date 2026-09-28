Get-ChildItem -Path 'app/src/main/java/com/finpulse/app/presentation' -Filter '*.kt' -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $resMatches = ([regex]::Matches($content, 'stringResource\(')).Count
    $textMatches = ([regex]::Matches($content, 'Text\(\s*"[A-Za-z]')).Count
    [PSCustomObject]@{
        File = $_.Name
        StringResourceCount = $resMatches
        HardcodedTextCount = $textMatches
    }
} | Sort-Object HardcodedTextCount -Descending | Format-Table -AutoSize

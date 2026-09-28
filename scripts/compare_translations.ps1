$enXml = [xml](Get-Content 'app/src/main/res/values/strings.xml' -Encoding UTF8)
$ruXml = [xml](Get-Content 'app/src/main/res/values-ru/strings.xml' -Encoding UTF8)

$enMap = @{}
foreach ($node in $enXml.resources.string) {
    $enMap[$node.name] = $node.InnerText
}

$ruMap = @{}
foreach ($node in $ruXml.resources.string) {
    $ruMap[$node.name] = $node.InnerText
}

Write-Host "Total EN keys: $($enMap.Count)"
Write-Host "Total RU keys: $($ruMap.Count)"

$missingInRu = @()
foreach ($k in $enMap.Keys) {
    if (-not $ruMap.ContainsKey($k)) {
        $missingInRu += $k
    }
}
Write-Host "`nMissing in RU ($($missingInRu.Count)):"
$missingInRu | ForEach-Object { Write-Host "  - $_ = $($enMap[$_])" }

$identical = @()
$whitelist = @(
    'app_name', 'lang_en', 'lang_es', 'lang_pt_br', 'lang_de', 'lang_fr', 'lang_it', 
    'lang_pl', 'lang_tr', 'lang_ja', 'lang_ko', 'lang_zh_cn', 'currency_usd', 'currency_eur', 
    'currency_gbp', 'currency_jpy', 'currency_cny', 'currency_inr', 'currency_cad', 
    'currency_aud', 'currency_chf', 'currency_brl', 'currency_rub', 'currency_try', 'currency_krw', 'currency_pln'
)

foreach ($k in $enMap.Keys) {
    if ($ruMap.ContainsKey($k) -and ($enMap[$k] -eq $ruMap[$k]) -and ($enMap[$k].Length -gt 1)) {
        if (-not ($whitelist -contains $k)) {
            $identical += [PSCustomObject]@{ Key=$k; Val=$enMap[$k] }
        }
    }
}
Write-Host "`nIdentical EN/RU non-whitelisted ($($identical.Count)):"
$identical | ForEach-Object { Write-Host "  - $($_.Key) = $($_.Val)" }

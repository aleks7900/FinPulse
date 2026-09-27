Add-Type -AssemblyName System.Drawing

$projectRoot = "c:\Users\aleks\.gemini\antigravity-ide\scratch\FinPulse"
$outDir = Join-Path $projectRoot "distribution\play_store\graphics"
if (!(Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

$iconSrcPath = Join-Path $projectRoot "FinPulse-icon-512.png"
$iconBmp = [System.Drawing.Bitmap]::FromFile($iconSrcPath)

# -------------------------------------------------------------
# 1. Feature Graphic (1024 x 500 px, no transparency required by Google Play)
# -------------------------------------------------------------
$fgBmp = New-Object System.Drawing.Bitmap(1024, 500, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
$g = [System.Drawing.Graphics]::FromImage($fgBmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

# Background: Rich deep emerald gradient
$bgRect = New-Object System.Drawing.Rectangle(0, 0, 1024, 500)
$bgBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    $bgRect,
    [System.Drawing.Color]::FromArgb(10, 28, 20),
    [System.Drawing.Color]::FromArgb(4, 18, 12),
    45.0
)
$g.FillRectangle($bgBrush, $bgRect)
$bgBrush.Dispose()

# Soft glowing ambient circles
$glowBrush1 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(35, 16, 185, 105))
$g.FillEllipse($glowBrush1, 600, -100, 500, 500)
$glowBrush1.Dispose()

$glowBrush2 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(25, 33, 150, 243))
$g.FillEllipse($glowBrush2, 750, 150, 400, 400)
$glowBrush2.Dispose()

# Draw FinPulse App Icon on left
$iconSize = 130
$iconRect = New-Object System.Drawing.Rectangle(70, 75, $iconSize, $iconSize)

# Subtle icon glow
$iconGlow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(50, 25, 198, 116))
$g.FillEllipse($iconGlow, 55, 60, 160, 160)
$iconGlow.Dispose()

$g.DrawImage($iconBmp, $iconRect)

# Typography - Brand Name
$titleFont = New-Object System.Drawing.Font("Segoe UI", 36, [System.Drawing.FontStyle]::Bold)
$titleBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 255))
$g.DrawString("Fin Pulse", $titleFont, $titleBrush, 225, 80)
$titleFont.Dispose()
$titleBrush.Dispose()

# Tagline
$subFont = New-Object System.Drawing.Font("Segoe UI", 16, [System.Drawing.FontStyle]::Bold)
$subBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(25, 198, 116))
$g.DrawString("EXPENSE TRACKER & BUDGET PLANNER", $subFont, $subBrush, 228, 142)
$subFont.Dispose()
$subBrush.Dispose()

# Value Proposition Lines
$bodyFont = New-Object System.Drawing.Font("Segoe UI", 14, [System.Drawing.FontStyle]::Regular)
$bodyBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(200, 225, 215))
$g.DrawString("• Offline-First Engine with Local Room Database", $bodyFont, $bodyBrush, 75, 250)
$g.DrawString("• Instant Google Account Cloud Synchronization", $bodyFont, $bodyBrush, 75, 290)
$g.DrawString("• Smart Analytics, Budgets & Biometric App Lock", $bodyFont, $bodyBrush, 75, 330)
$bodyFont.Dispose()
$bodyBrush.Dispose()

# Badges (Pills)
function Draw-Pill($graphics, $text, $x, $y, $w, $h, $bgColor, $textColor) {
    $rect = New-Object System.Drawing.Rectangle($x, $y, $w, $h)
    $radius = [int]($h / 2)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddArc($x, $y, $radius * 2, $radius * 2, 180, 90)
    $path.AddArc($x + $w - $radius * 2, $y, $radius * 2, $radius * 2, 270, 90)
    $path.AddArc($x + $w - $radius * 2, $y + $h - $radius * 2, $radius * 2, $radius * 2, 0, 90)
    $path.AddArc($x, $y + $h - $radius * 2, $radius * 2, $radius * 2, 90, 90)
    $path.CloseFigure()
    
    $brush = New-Object System.Drawing.SolidBrush($bgColor)
    $graphics.FillPath($brush, $path)
    $brush.Dispose()
    
    $f = New-Object System.Drawing.Font("Segoe UI", 10, [System.Drawing.FontStyle]::Bold)
    $tb = New-Object System.Drawing.SolidBrush($textColor)
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $graphics.DrawString($text, $f, $tb, (New-Object System.Drawing.RectangleF($x, $y, $w, $h)), $sf)
    $f.Dispose()
    $tb.Dispose()
    $sf.Dispose()
    $path.Dispose()
}

Draw-Pill $g "100% OFFLINE-FIRST" 75 390 160 34 ([System.Drawing.Color]::FromArgb(40, 25, 198, 116)) ([System.Drawing.Color]::FromArgb(40, 255, 160))
Draw-Pill $g "BIOMETRIC LOCK" 245 390 150 34 ([System.Drawing.Color]::FromArgb(40, 33, 150, 243)) ([System.Drawing.Color]::FromArgb(100, 200, 255))
Draw-Pill $g "GOOGLE CLOUD SYNC" 405 390 175 34 ([System.Drawing.Color]::FromArgb(50, 255, 193, 7)) ([System.Drawing.Color]::FromArgb(255, 230, 100))

# Right Side Card Mockup: Floating Financial Metrics Card
$cardRect = New-Object System.Drawing.Rectangle(640, 75, 330, 350)
$cardPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$cRadius = 24
$cardPath.AddArc(640, 75, $cRadius * 2, $cRadius * 2, 180, 90)
$cardPath.AddArc(640 + 330 - $cRadius * 2, 75, $cRadius * 2, $cRadius * 2, 270, 90)
$cardPath.AddArc(640 + 330 - $cRadius * 2, 75 + 350 - $cRadius * 2, $cRadius * 2, $cRadius * 2, 0, 90)
$cardPath.AddArc(640, 75 + 350 - $cRadius * 2, $cRadius * 2, $cRadius * 2, 90, 90)
$cardPath.CloseFigure()

# Card background & border
$cardBg = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(16, 40, 30))
$g.FillPath($cardBg, $cardPath)
$cardBg.Dispose()

$cardBorder = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(60, 25, 198, 116), 1.5)
$g.DrawPath($cardBorder, $cardPath)
$cardBorder.Dispose()

# Inside Card Content
$cardHeadFont = New-Object System.Drawing.Font("Segoe UI", 12, [System.Drawing.FontStyle]::Bold)
$cardHeadBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(160, 200, 180))
$g.DrawString("TOTAL NET WORTH", $cardHeadFont, $cardHeadBrush, 665, 100)
$cardHeadFont.Dispose()
$cardHeadBrush.Dispose()

$cardBalanceFont = New-Object System.Drawing.Font("Segoe UI", 26, [System.Drawing.FontStyle]::Bold)
$cardBalanceBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 255))
$g.DrawString("$42,850.00", $cardBalanceFont, $cardBalanceBrush, 665, 125)
$cardBalanceFont.Dispose()
$cardBalanceBrush.Dispose()

# Change indicator
$badgeFont = New-Object System.Drawing.Font("Segoe UI", 11, [System.Drawing.FontStyle]::Bold)
$badgeBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(25, 198, 116))
$g.DrawString("+18.4% this month", $badgeFont, $badgeBrush, 665, 175)
$badgeFont.Dispose()
$badgeBrush.Dispose()

# Decorative mini bar chart
$bars = @(
    @{ X = 670; H = 55;  Color = [System.Drawing.Color]::FromArgb(60, 25, 198, 116) },
    @{ X = 715; H = 80;  Color = [System.Drawing.Color]::FromArgb(100, 25, 198, 116) },
    @{ X = 760; H = 65;  Color = [System.Drawing.Color]::FromArgb(140, 25, 198, 116) },
    @{ X = 805; H = 110; Color = [System.Drawing.Color]::FromArgb(190, 25, 198, 116) },
    @{ X = 850; H = 145; Color = [System.Drawing.Color]::FromArgb(255, 25, 198, 116) },
    @{ X = 895; H = 175; Color = [System.Drawing.Color]::FromArgb(255, 76, 175, 80) }
)

foreach ($b in $bars) {
    $y = 390 - $b.H
    $bBrush = New-Object System.Drawing.SolidBrush($b.Color)
    $g.FillRectangle($bBrush, $b.X, $y, 32, $b.H)
    $bBrush.Dispose()
}

# Curve trendline over bars
$curvePen = New-Object System.Drawing.Pen([System.Drawing.Color]::White, 3)
$points = @(
    (New-Object System.Drawing.Point(686, 330)),
    (New-Object System.Drawing.Point(731, 305)),
    (New-Object System.Drawing.Point(776, 320)),
    (New-Object System.Drawing.Point(821, 275)),
    (New-Object System.Drawing.Point(866, 240)),
    (New-Object System.Drawing.Point(911, 210))
)
$g.DrawCurve($curvePen, $points)
$curvePen.Dispose()

$cardPath.Dispose()
$g.Dispose()

$featureGraphicPath = Join-Path $outDir "feature_graphic_1024x500.png"
$fgBmp.Save($featureGraphicPath, [System.Drawing.Imaging.ImageFormat]::Png)
$fgBmp.Dispose()
$iconBmp.Dispose()

Write-Host "[SUCCESS] Generated Feature Graphic at: $featureGraphicPath (1024x500)" -ForegroundColor Green

# Copy icon_512x512 to graphics dir as well
Copy-Item $iconSrcPath (Join-Path $outDir "icon_512x512.png") -Force
Write-Host "[SUCCESS] Copied 512x512 Icon to: $outDir\icon_512x512.png" -ForegroundColor Green

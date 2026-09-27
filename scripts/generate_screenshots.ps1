Add-Type -AssemblyName System.Drawing

$projectRoot = "c:\Users\aleks\.gemini\antigravity-ide\scratch\FinPulse"
$screenshotDir = Join-Path $projectRoot "distribution\play_store\screenshots\phone"
if (!(Test-Path $screenshotDir)) { New-Item -ItemType Directory -Path $screenshotDir -Force | Out-Null }

$iconPath = Join-Path $projectRoot "FinPulse-icon-512.png"
$iconBmp = [System.Drawing.Bitmap]::FromFile($iconPath)

function Create-Screenshot {
    param(
        [string]$Title,
        [string]$Subtitle,
        [string]$Badge,
        [string]$OutFileName,
        [scriptblock]$RenderContent
    )

    $bmp = New-Object System.Drawing.Bitmap(1080, 1920, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

    # Background gradient
    $bgRect = New-Object System.Drawing.Rectangle(0, 0, 1080, 1920)
    $bgBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $bgRect,
        [System.Drawing.Color]::FromArgb(12, 30, 22),
        [System.Drawing.Color]::FromArgb(5, 14, 10),
        90.0
    )
    $g.FillRectangle($bgBrush, $bgRect)
    $bgBrush.Dispose()

    # Ambient glows
    $g1 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(40, 25, 198, 116))
    $g.FillEllipse($g1, 200, -100, 680, 680)
    $g1.Dispose()

    # Badge Pill
    $bFont = New-Object System.Drawing.Font("Segoe UI", 16, [System.Drawing.FontStyle]::Bold)
    $bSize = $g.MeasureString($Badge, $bFont)
    $pillW = [int]($bSize.Width + 40)
    $pillH = 48
    $pillX = [int]((1080 - $pillW) / 2)
    $pillY = 90
    
    $pillRadius = 24
    $pillPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $pillPath.AddArc($pillX, $pillY, $pillRadius * 2, $pillRadius * 2, 180, 90)
    $pillPath.AddArc($pillX + $pillW - $pillRadius * 2, $pillY, $pillRadius * 2, $pillRadius * 2, 270, 90)
    $pillPath.AddArc($pillX + $pillW - $pillRadius * 2, $pillY + $pillH - $pillRadius * 2, $pillRadius * 2, $pillRadius * 2, 0, 90)
    $pillPath.AddArc($pillX, $pillY + $pillH - $pillRadius * 2, $pillRadius * 2, $pillRadius * 2, 90, 90)
    $pillPath.CloseFigure()
    
    $pillBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(60, 25, 198, 116))
    $g.FillPath($pillBrush, $pillPath)
    $pillBrush.Dispose()
    
    $bTextBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(25, 198, 116))
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($Badge, $bFont, $bTextBrush, (New-Object System.Drawing.RectangleF($pillX, $pillY, $pillW, $pillH)), $sf)
    $bFont.Dispose()
    $bTextBrush.Dispose()
    $pillPath.Dispose()

    # Title
    $titleFont = New-Object System.Drawing.Font("Segoe UI", 42, [System.Drawing.FontStyle]::Bold)
    $titleBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $titleRect = New-Object System.Drawing.RectangleF(40, 160, 1000, 75)
    $sfTitle = New-Object System.Drawing.StringFormat
    $sfTitle.Alignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($Title, $titleFont, $titleBrush, $titleRect, $sfTitle)
    $titleFont.Dispose()
    $titleBrush.Dispose()

    # Subtitle
    $subFont = New-Object System.Drawing.Font("Segoe UI", 20, [System.Drawing.FontStyle]::Regular)
    $subBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(160, 200, 180))
    $subRect = New-Object System.Drawing.RectangleF(60, 245, 960, 60)
    $sfSub = New-Object System.Drawing.StringFormat
    $sfSub.Alignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($Subtitle, $subFont, $subBrush, $subRect, $sfSub)
    $subFont.Dispose()
    $subBrush.Dispose()

    # Phone Mockup Frame
    $phoneX = 100
    $phoneY = 340
    $phoneW = 880
    $phoneH = 1580
    $phoneRadius = 60

    # Phone drop shadow
    $shadowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(80, 0, 0, 0))
    $g.FillEllipse($shadowBrush, $phoneX - 30, $phoneY + 40, $phoneW + 60, $phoneH)
    $shadowBrush.Dispose()

    # Outer border
    $outerBorderBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(40, 65, 50))
    $phoneOuterPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $phoneOuterPath.AddArc($phoneX - 8, $phoneY - 8, ($phoneRadius + 8) * 2, ($phoneRadius + 8) * 2, 180, 90)
    $phoneOuterPath.AddArc($phoneX + $phoneW - $phoneRadius - 8, $phoneY - 8, ($phoneRadius + 8) * 2, ($phoneRadius + 8) * 2, 270, 90)
    $phoneOuterPath.AddArc($phoneX + $phoneW - $phoneRadius - 8, $phoneY + $phoneH - $phoneRadius - 8, ($phoneRadius + 8) * 2, ($phoneRadius + 8) * 2, 0, 90)
    $phoneOuterPath.AddArc($phoneX - 8, $phoneY + $phoneH - $phoneRadius - 8, ($phoneRadius + 8) * 2, ($phoneRadius + 8) * 2, 90, 90)
    $phoneOuterPath.CloseFigure()
    $g.FillPath($outerBorderBrush, $phoneOuterPath)
    $outerBorderBrush.Dispose()
    $phoneOuterPath.Dispose()

    # Inner Phone Display
    $phonePath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $phonePath.AddArc($phoneX, $phoneY, $phoneRadius * 2, $phoneRadius * 2, 180, 90)
    $phonePath.AddArc($phoneX + $phoneW - $phoneRadius * 2, $phoneY, $phoneRadius * 2, $phoneRadius * 2, 270, 90)
    $phonePath.AddArc($phoneX + $phoneW - $phoneRadius * 2, $phoneY + $phoneH - $phoneRadius * 2, $phoneRadius * 2, $phoneRadius * 2, 0, 90)
    $phonePath.AddArc($phoneX, $phoneY + $phoneH - $phoneRadius * 2, $phoneRadius * 2, $phoneRadius * 2, 90, 90)
    $phonePath.CloseFigure()

    $screenBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(15, 23, 19))
    $g.FillPath($screenBrush, $phonePath)
    $screenBrush.Dispose()

    # Phone Top Bar (App Title & Status)
    $topBarB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(18, 30, 24))
    $g.FillRectangle($topBarB, $phoneX, $phoneY, $phoneW, 110)
    $topBarB.Dispose()

    # Logo in top bar
    $g.DrawImage($iconBmp, $phoneX + 35, $phoneY + 30, 50, 50)
    $appF = New-Object System.Drawing.Font("Segoe UI", 20, [System.Drawing.FontStyle]::Bold)
    $appB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $g.DrawString("Fin Pulse", $appF, $appB, $phoneX + 95, $phoneY + 40)
    $appF.Dispose()
    $appB.Dispose()

    # Phone Status Bar / Notch
    $notchBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(8, 10, 9))
    $g.FillEllipse($notchBrush, 515, 410, 50, 50)
    $notchBrush.Dispose()

    # Render Screen Content inside the phone
    & $RenderContent $g $phoneX $phoneY $phoneW $phoneH

    $phonePath.Dispose()
    $g.Dispose()

    $destPath = Join-Path $screenshotDir $OutFileName
    $bmp.Save($destPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "[SUCCESS] Generated Screenshot: $OutFileName (1080x1920)" -ForegroundColor Green
}

# 1. Dashboard Screenshot
Create-Screenshot `
    -Title "Master Your Finances" `
    -Subtitle "Real-time net worth, cash flow & account tracking" `
    -Badge "FINANCIAL DASHBOARD" `
    -OutFileName "01_dashboard_and_cashflow.png" `
    -RenderContent {
        param($g, $px, $py, $pw, $ph)
        
        # Net worth card
        $cardX = $px + 40
        $cardY = $py + 130
        $cardW = $pw - 80
        $cardH = 260
        
        $cBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(25, 45, 35))
        $g.FillRectangle($cBrush, $cardX, $cardY, $cardW, $cardH)
        $cBrush.Dispose()
        
        $f1 = New-Object System.Drawing.Font("Segoe UI", 16, [System.Drawing.FontStyle]::Bold)
        $b1 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(160, 200, 180))
        $g.DrawString("TOTAL NET WORTH", $f1, $b1, $cardX + 30, $cardY + 30)
        
        $f2 = New-Object System.Drawing.Font("Segoe UI", 48, [System.Drawing.FontStyle]::Bold)
        $b2 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
        $g.DrawString("$48,720.50", $f2, $b2, $cardX + 25, $cardY + 70)
        
        $f3 = New-Object System.Drawing.Font("Segoe UI", 18, [System.Drawing.FontStyle]::Bold)
        $b3 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(25, 198, 116))
        $g.DrawString("+14.2% (+$6,040) this month", $f3, $b3, $cardX + 30, $cardY + 165)
        
        # Account Cards
        $accs = @(
            @{ Name="Primary Checking"; Bal="$24,350.00"; Type="Chase Bank"; Col=[System.Drawing.Color]::FromArgb(33, 150, 243) },
            @{ Name="High Yield Savings"; Bal="$18,500.00"; Type="Marcus HYSA (4.5%)"; Col=[System.Drawing.Color]::FromArgb(25, 198, 116) },
            @{ Name="Investment Portfolio"; Bal="$5,870.50"; Type="Vanguard ETF"; Col=[System.Drawing.Color]::FromArgb(156, 39, 176) }
        )
        
        $ay = $cardY + 300
        foreach ($a in $accs) {
            $ab = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(24, 32, 28))
            $g.FillRectangle($ab, $cardX, $ay, $cardW, 140)
            $ab.Dispose()
            
            $dotB = New-Object System.Drawing.SolidBrush($a.Col)
            $g.FillEllipse($dotB, $cardX + 30, $ay + 45, 50, 50)
            $dotB.Dispose()
            
            $anF = New-Object System.Drawing.Font("Segoe UI", 20, [System.Drawing.FontStyle]::Bold)
            $anB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
            $g.DrawString($a.Name, $anF, $anB, $cardX + 100, $ay + 35)
            
            $atF = New-Object System.Drawing.Font("Segoe UI", 15, [System.Drawing.FontStyle]::Regular)
            $atB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(150, 180, 165))
            $g.DrawString($a.Type, $atF, $atB, $cardX + 100, $ay + 75)
            
            $balF = New-Object System.Drawing.Font("Segoe UI", 22, [System.Drawing.FontStyle]::Bold)
            $balB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
            $sfR = New-Object System.Drawing.StringFormat
            $sfR.Alignment = [System.Drawing.StringAlignment]::Far
            $rectBal = New-Object System.Drawing.RectangleF -ArgumentList @([float]($cardX + 400), [float]($ay + 45), [float]($cardW - 430), [float]60.0)
            $g.DrawString($a.Bal, $balF, $balB, $rectBal, $sfR)
            
            $ay += 170
        }
    }

# 2. Analytics Screenshot
Create-Screenshot `
    -Title "Deep Spending Analytics" `
    -Subtitle "Interactive charts, merchant trends & category breakdowns" `
    -Badge "SMART ANALYTICS" `
    -OutFileName "02_transactions_and_analytics.png" `
    -RenderContent {
        param($g, $px, $py, $pw, $ph)
        
        $cardX = $px + 40
        $cardY = $py + 130
        $cardW = $pw - 80
        
        # Monthly Spend Chart Box
        $cb = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(24, 38, 30))
        $g.FillRectangle($cb, $cardX, $cardY, $cardW, 460)
        $cb.Dispose()
        
        $f1 = New-Object System.Drawing.Font("Segoe UI", 18, [System.Drawing.FontStyle]::Bold)
        $b1 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
        $g.DrawString("EXPENSE BREAKDOWN", $f1, $b1, $cardX + 30, $cardY + 30)
        
        # Draw sample bar chart
        $chartBars = @(
            @{ Label="Housing"; Val=1800; H=260; Col=[System.Drawing.Color]::FromArgb(25, 198, 116) },
            @{ Label="Food"; Val=650; H=140; Col=[System.Drawing.Color]::FromArgb(33, 150, 243) },
            @{ Label="Travel"; Val=420; H=95; Col=[System.Drawing.Color]::FromArgb(255, 193, 7) },
            @{ Label="Bills"; Val=310; H=75; Col=[System.Drawing.Color]::FromArgb(156, 39, 176) },
            @{ Label="Leisure"; Val=240; H=60; Col=[System.Drawing.Color]::FromArgb(244, 67, 54) }
        )
        
        $bx = $cardX + 40
        foreach ($cbItem in $chartBars) {
            $by = $cardY + 390 - $cbItem.H
            $barBrush = New-Object System.Drawing.SolidBrush($cbItem.Col)
            $g.FillRectangle($barBrush, $bx, $by, 100, $cbItem.H)
            $barBrush.Dispose()
            
            $lf = New-Object System.Drawing.Font("Segoe UI", 14, [System.Drawing.FontStyle]::Bold)
            $lb = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
            $g.DrawString($cbItem.Label, $lf, $lb, $bx, $cardY + 410)
            
            $bx += 140
        }

        # Recent Transactions Header
        $rtH = New-Object System.Drawing.Font("Segoe UI", 18, [System.Drawing.FontStyle]::Bold)
        $rtB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
        $g.DrawString("RECENT TRANSACTIONS", $rtH, $rtB, $cardX + 10, $cardY + 500)

        # Recent items
        $txItems = @(
            @{ Merchant="Whole Foods Market"; Cat="Groceries"; Amt="-$84.20"; Date="Today" },
            @{ Merchant="Apple Store"; Cat="Electronics"; Amt="-$199.00"; Date="Yesterday" },
            @{ Merchant="Salary Direct Deposit"; Cat="Income"; Amt="+$3,500.00"; Date="Sep 25" }
        )
        $ty = $cardY + 550
        foreach ($item in $txItems) {
            $ib = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(24, 32, 28))
            $g.FillRectangle($ib, $cardX, $ty, $cardW, 110)
            $ib.Dispose()

            $mF = New-Object System.Drawing.Font("Segoe UI", 18, [System.Drawing.FontStyle]::Bold)
            $mB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
            $g.DrawString($item.Merchant, $mF, $mB, $cardX + 25, $ty + 20)

            $cF = New-Object System.Drawing.Font("Segoe UI", 14, [System.Drawing.FontStyle]::Regular)
            $cB2 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(150, 180, 165))
            $g.DrawString($item.Cat + " * " + $item.Date, $cF, $cB2, $cardX + 25, $ty + 60)

            $aCol = if ($item.Amt.StartsWith("+")) { [System.Drawing.Color]::FromArgb(25, 198, 116) } else { [System.Drawing.Color]::White }
            $aB = New-Object System.Drawing.SolidBrush($aCol)
            $aF = New-Object System.Drawing.Font("Segoe UI", 20, [System.Drawing.FontStyle]::Bold)
            $sfR2 = New-Object System.Drawing.StringFormat
            $sfR2.Alignment = [System.Drawing.StringAlignment]::Far
            $rectAmt = New-Object System.Drawing.RectangleF -ArgumentList @([float]($cardX + 400), [float]($ty + 30), [float]($cardW - 430), [float]50.0)
            $g.DrawString($item.Amt, $aF, $aB, $rectAmt, $sfR2)

            $ty += 135
        }
    }

# 3. Google Cloud Sync Screenshot
Create-Screenshot `
    -Title "Seamless Multi-Device Sync" `
    -Subtitle "Sign in with Google to synchronize across all Android devices" `
    -Badge "GOOGLE ACCOUNT SYNC" `
    -OutFileName "03_google_cloud_sync.png" `
    -RenderContent {
        param($g, $px, $py, $pw, $ph)
        
        $cardX = $px + 40
        $cardY = $py + 130
        $cardW = $pw - 80
        
        # Account Card
        $cb = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(22, 36, 28))
        $g.FillRectangle($cb, $cardX, $cardY, $cardW, 600)
        $cb.Dispose()
        
        # Google User Card Header
        $avatarB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(25, 198, 116))
        $g.FillEllipse($avatarB, $cardX + 40, $cardY + 40, 100, 100)
        $avatarB.Dispose()
        
        $uFont = New-Object System.Drawing.Font("Segoe UI", 26, [System.Drawing.FontStyle]::Bold)
        $uBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
        $g.DrawString("FinPulse User", $uFont, $uBrush, $cardX + 165, $cardY + 45)
        
        $eFont = New-Object System.Drawing.Font("Segoe UI", 16, [System.Drawing.FontStyle]::Regular)
        $eBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(160, 200, 180))
        $g.DrawString("alex@finpulse.app", $eFont, $eBrush, $cardX + 165, $cardY + 95)
        
        # Sync status badge
        $badgeB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(35, 25, 198, 116))
        $g.FillRectangle($badgeB, $cardX + 40, $cardY + 180, $cardW - 80, 90)
        $badgeB.Dispose()
        
        $stFont = New-Object System.Drawing.Font("Segoe UI", 20, [System.Drawing.FontStyle]::Bold)
        $stBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(25, 198, 116))
        $g.DrawString("[SYNC ACTIVE] Synchronized with Cloud", $stFont, $stBrush, $cardX + 60, $cardY + 205)
        
        # Bullets
        $bf = New-Object System.Drawing.Font("Segoe UI", 16, [System.Drawing.FontStyle]::Regular)
        $bb = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(210, 235, 220))
        $g.DrawString("* Automatic background synchronization", $bf, $bb, $cardX + 40, $cardY + 310)
        $g.DrawString("* Last-Write-Wins deterministic conflict resolution", $bf, $bb, $cardX + 40, $cardY + 360)
        $g.DrawString("* End-to-end data isolation by Google UID", $bf, $bb, $cardX + 40, $cardY + 410)
        $g.DrawString("* Instant restore on new Android devices", $bf, $bb, $cardX + 40, $cardY + 460)
    }

# 4. Offline & Biometric Screenshot
Create-Screenshot `
    -Title "100% Offline-First & Private" `
    -Subtitle "Use fully without internet. Protect finances with Biometric Lock" `
    -Badge "PRIVACY AND SECURITY" `
    -OutFileName "04_offline_and_biometric_lock.png" `
    -RenderContent {
        param($g, $px, $py, $pw, $ph)
        
        $cardX = $px + 40
        $cardY = $py + 130
        $cardW = $pw - 80
        
        $cb = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(20, 32, 26))
        $g.FillRectangle($cb, $cardX, $cardY, $cardW, 650)
        $cb.Dispose()
        
        # Lock Icon Graphic
        $lCircle = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(35, 25, 198, 116))
        $g.FillEllipse($lCircle, [int]($cardX + ($cardW - 140)/2), $cardY + 50, 140, 140)
        $lCircle.Dispose()
        
        $titleF = New-Object System.Drawing.Font("Segoe UI", 28, [System.Drawing.FontStyle]::Bold)
        $titleB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
        $sfC = New-Object System.Drawing.StringFormat
        $sfC.Alignment = [System.Drawing.StringAlignment]::Center
        $rectTitle = New-Object System.Drawing.RectangleF -ArgumentList @([float]$cardX, [float]($cardY + 220), [float]$cardW, [float]60.0)
        $g.DrawString("Biometric Lock Active", $titleF, $titleB, $rectTitle, $sfC)
        
        $descF = New-Object System.Drawing.Font("Segoe UI", 16, [System.Drawing.FontStyle]::Regular)
        $descB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(160, 200, 180))
        $rectDesc = New-Object System.Drawing.RectangleF -ArgumentList @([float]($cardX + 40), [float]($cardY + 280), [float]($cardW - 80), [float]50.0)
        $g.DrawString("Unlock securely with Fingerprint or App PIN", $descF, $descB, $rectDesc, $sfC)
        
        # Feature rows
        $featF = New-Object System.Drawing.Font("Segoe UI", 16, [System.Drawing.FontStyle]::Bold)
        $featB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
        $g.DrawString("[SEC] Hardware-backed Keystore Encryption", $featF, $featB, $cardX + 50, $cardY + 370)
        $g.DrawString("[PRIV] Privacy Mode (Mask Balances on Demand)", $featF, $featB, $cardX + 50, $cardY + 430)
        $g.DrawString("[WALL] FLAG_SECURE Screenshot Protection", $featF, $featB, $cardX + 50, $cardY + 490)
        $g.DrawString("[OFFL] Local Room DB Works 100% Offline", $featF, $featB, $cardX + 50, $cardY + 550)
    }

# 5. Budgets & Goals Screenshot
Create-Screenshot `
    -Title "Budgets & Savings Goals" `
    -Subtitle "Plan spending limits, forecast recurring bills & achieve goals" `
    -Badge "BUDGETS AND GOALS" `
    -OutFileName "05_budgets_and_financial_goals.png" `
    -RenderContent {
        param($g, $px, $py, $pw, $ph)
        
        $cardX = $px + 40
        $cardY = $py + 120
        $cardW = $pw - 80
        
        # Budget Item 1
        $b1B = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(24, 36, 28))
        $g.FillRectangle($b1B, $cardX, $cardY, $cardW, 210)
        $b1B.Dispose()
        
        $bf = New-Object System.Drawing.Font("Segoe UI", 22, [System.Drawing.FontStyle]::Bold)
        $bw = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
        $g.DrawString("Groceries & Dining", $bf, $bw, $cardX + 30, $cardY + 25)
        
        $valF = New-Object System.Drawing.Font("Segoe UI", 16, [System.Drawing.FontStyle]::Regular)
        $valB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(160, 200, 180))
        $g.DrawString("$620 spent of $800 limit (77%)", $valF, $valB, $cardX + 30, $cardY + 68)
        
        # Progress Bar 1
        $pBg = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(40, 60, 50))
        $g.FillRectangle($pBg, $cardX + 30, $cardY + 115, $cardW - 60, 24)
        $pBg.Dispose()
        
        $pFill = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(25, 198, 116))
        $g.FillRectangle($pFill, $cardX + 30, $cardY + 115, [int](($cardW - 60) * 0.77), 24)
        $pFill.Dispose()
        
        # Goal Item 2
        $g2Y = $cardY + 250
        $g2B = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(24, 36, 28))
        $g.FillRectangle($g2B, $cardX, $g2Y, $cardW, 210)
        $g2B.Dispose()
        
        $g.DrawString("Emergency Fund Goal", $bf, $bw, $cardX + 30, $g2Y + 25)
        $g.DrawString("$8,500 saved of $10,000 target (85%)", $valF, $valB, $cardX + 30, $g2Y + 68)
        
        $pBg2 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(40, 60, 50))
        $g.FillRectangle($pBg2, $cardX + 30, $g2Y + 115, $cardW - 60, 24)
        $pBg2.Dispose()
        
        $pFill2 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(33, 150, 243))
        $g.FillRectangle($pFill2, $cardX + 30, $g2Y + 115, [int](($cardW - 60) * 0.85), 24)
        $pFill2.Dispose()

        # Recurring subscription item
        $g3Y = $cardY + 500
        $g3B = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(24, 36, 28))
        $g.FillRectangle($g3B, $cardX, $g3Y, $cardW, 210)
        $g3B.Dispose()

        $g.DrawString("Upcoming Subscriptions", $bf, $bw, $cardX + 30, $g3Y + 25)
        $g.DrawString("Netflix, Spotify & Gym due in 4 days ($44.97)", $valF, $valB, $cardX + 30, $g3Y + 68)

        $pillRecB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(40, 255, 193, 7))
        $g.FillRectangle($pillRecB, $cardX + 30, $g3Y + 115, 260, 45)
        $pillRecB.Dispose()

        $recF = New-Object System.Drawing.Font("Segoe UI", 15, [System.Drawing.FontStyle]::Bold)
        $recB = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 193, 7))
        $g.DrawString("Auto-Reminder Active", $recF, $recB, $cardX + 45, $g3Y + 125)
    }

$iconBmp.Dispose()
Write-Host "`nAll 5 Google Play phone screenshots generated successfully!" -ForegroundColor Green

package md.alexlab.finpulse.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * Full Material 3 type scale (all 15 roles) plus the Expressive "emphasized" set.
 *
 * Money is everywhere in FinPulse, so display/headline/title/label roles use tabular figures
 * ("tnum") and lining numerals ("lnum") so amounts align in lists and don't jitter while
 * animating. Line heights are trimmed so large numbers sit tightly in hero cards.
 */

private val Sans = FontFamily.Default
private const val NUMERIC = "tnum, lnum"

private val TrimmedLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both
)

private fun style(
    size: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight,
    tracking: TextUnit = 0.sp,
    numeric: Boolean = false,
    trim: Boolean = false
) = TextStyle(
    fontFamily = Sans,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = tracking,
    fontFeatureSettings = if (numeric) NUMERIC else null,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = if (trim) TrimmedLineHeight else null
)

val FinPulseTypography = Typography(
    displayLarge = style(42.sp, 48.sp, FontWeight.SemiBold, (-0.03).em, numeric = true, trim = true),
    displayMedium = style(36.sp, 42.sp, FontWeight.SemiBold, (-0.025).em, numeric = true, trim = true),
    displaySmall = style(30.sp, 36.sp, FontWeight.SemiBold, (-0.02).em, numeric = true),

    headlineLarge = style(28.sp, 34.sp, FontWeight.SemiBold, (-0.015).em, numeric = true),
    headlineMedium = style(24.sp, 30.sp, FontWeight.SemiBold, (-0.01).em, numeric = true),
    headlineSmall = style(20.sp, 26.sp, FontWeight.SemiBold, (-0.005).em, numeric = true),

    titleLarge = style(20.sp, 26.sp, FontWeight.SemiBold, numeric = true),
    titleMedium = style(16.sp, 22.sp, FontWeight.SemiBold, 0.005.em, numeric = true),
    titleSmall = style(14.sp, 20.sp, FontWeight.SemiBold, 0.005.em, numeric = true),

    bodyLarge = style(16.sp, 24.sp, FontWeight.Normal, 0.005.em),
    bodyMedium = style(14.sp, 20.sp, FontWeight.Normal, 0.01.em),
    bodySmall = style(12.sp, 16.sp, FontWeight.Normal, 0.02.em),

    labelLarge = style(14.sp, 20.sp, FontWeight.Medium, 0.01.em, numeric = true),
    labelMedium = style(12.sp, 16.sp, FontWeight.Medium, 0.03.em, numeric = true),
    labelSmall = style(11.sp, 16.sp, FontWeight.Medium, 0.04.em, numeric = true),

    // Material 3 Expressive emphasized roles: heavier weights for hero moments and actions.
    displayLargeEmphasized = style(42.sp, 48.sp, FontWeight.Bold, (-0.03).em, numeric = true, trim = true),
    displayMediumEmphasized = style(36.sp, 42.sp, FontWeight.Bold, (-0.025).em, numeric = true, trim = true),
    displaySmallEmphasized = style(30.sp, 36.sp, FontWeight.Bold, (-0.02).em, numeric = true),
    headlineLargeEmphasized = style(28.sp, 34.sp, FontWeight.Bold, (-0.015).em, numeric = true),
    headlineMediumEmphasized = style(24.sp, 30.sp, FontWeight.Bold, (-0.01).em, numeric = true),
    headlineSmallEmphasized = style(20.sp, 26.sp, FontWeight.Bold, (-0.005).em, numeric = true),
    titleLargeEmphasized = style(20.sp, 26.sp, FontWeight.Bold, numeric = true),
    titleMediumEmphasized = style(16.sp, 22.sp, FontWeight.Bold, 0.005.em, numeric = true),
    titleSmallEmphasized = style(14.sp, 20.sp, FontWeight.Bold, 0.005.em, numeric = true),
    bodyLargeEmphasized = style(16.sp, 24.sp, FontWeight.Medium, 0.005.em),
    bodyMediumEmphasized = style(14.sp, 20.sp, FontWeight.Medium, 0.01.em),
    bodySmallEmphasized = style(12.sp, 16.sp, FontWeight.Medium, 0.02.em),
    labelLargeEmphasized = style(14.sp, 20.sp, FontWeight.SemiBold, 0.01.em, numeric = true),
    labelMediumEmphasized = style(12.sp, 16.sp, FontWeight.SemiBold, 0.03.em, numeric = true),
    labelSmallEmphasized = style(11.sp, 16.sp, FontWeight.SemiBold, 0.04.em, numeric = true)
)

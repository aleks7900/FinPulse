package md.alexlab.finpulse.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * FinPulse color system (2026 refresh).
 *
 * Two layers:
 *  1. A complete Material 3 color scheme (every role, including the surfaceContainer family)
 *     for light and dark, seeded from the FinPulse emerald. On Android 12+ the user can opt
 *     into wallpaper-based dynamic color instead.
 *  2. Semantic finance colors ([FinPulseColors]): income, expense, transfer, etc. Each has a
 *     light and dark value tuned for WCAG AA text contrast on its surfaces.
 *
 * The legacy token names (EmeraldPrimary, CrimsonExpense, ...) are kept as theme-aware
 * getters so existing screens pick up the new palette, dark/light contrast tuning and
 * dynamic color without rewriting every call site.
 */

// ---------------------------------------------------------------------------------------------
// Brand reference values (raw). Prefer MaterialTheme.colorScheme / MaterialTheme.finColors.
// ---------------------------------------------------------------------------------------------
object FinPulseBrand {
    val Emerald = Color(0xFF10B981)
    val EmeraldBright = Color(0xFF34D399)
    val EmeraldDeep = Color(0xFF047857)
    val LauncherGreen = Color(0xFF19C674)
}

// ---------------------------------------------------------------------------------------------
// Material 3 scheme: light
// ---------------------------------------------------------------------------------------------
internal object LightTokens {
    val primary = Color(0xFF006C4E)
    val onPrimary = Color(0xFFFFFFFF)
    val primaryContainer = Color(0xFF8BF5C8)
    val onPrimaryContainer = Color(0xFF00513A)
    val inversePrimary = Color(0xFF5FDBA8)

    val secondary = Color(0xFF4A635A)
    val onSecondary = Color(0xFFFFFFFF)
    val secondaryContainer = Color(0xFFCCE8DC)
    val onSecondaryContainer = Color(0xFF334B42)

    val tertiary = Color(0xFF5A52B0)
    val onTertiary = Color(0xFFFFFFFF)
    val tertiaryContainer = Color(0xFFE4DFFF)
    val onTertiaryContainer = Color(0xFF423A97)

    val error = Color(0xFFBA1A1A)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFFFDAD6)
    val onErrorContainer = Color(0xFF93000A)

    val background = Color(0xFFF8F9FC)
    val onBackground = Color(0xFF181C22)
    val surface = Color(0xFFF8F9FC)
    val onSurface = Color(0xFF181C22)
    val surfaceVariant = Color(0xFFDDE3EA)
    val onSurfaceVariant = Color(0xFF41484F)
    val surfaceTint = primary
    val inverseSurface = Color(0xFF2D3137)
    val inverseOnSurface = Color(0xFFEFF1F6)
    val outline = Color(0xFF71787F)
    val outlineVariant = Color(0xFFC1C7CE)
    val scrim = Color(0xFF000000)

    val surfaceDim = Color(0xFFD8DAE0)
    val surfaceBright = Color(0xFFF8F9FC)
    val surfaceContainerLowest = Color(0xFFFFFFFF)
    val surfaceContainerLow = Color(0xFFF2F3F8)
    val surfaceContainer = Color(0xFFECEEF3)
    val surfaceContainerHigh = Color(0xFFE6E8ED)
    val surfaceContainerHighest = Color(0xFFE0E2E8)
}

// ---------------------------------------------------------------------------------------------
// Material 3 scheme: dark ("obsidian")
// ---------------------------------------------------------------------------------------------
internal object DarkTokens {
    val primary = Color(0xFF5FDBA8)
    val onPrimary = Color(0xFF003827)
    val primaryContainer = Color(0xFF00513A)
    val onPrimaryContainer = Color(0xFF8BF5C8)
    val inversePrimary = Color(0xFF006C4E)

    val secondary = Color(0xFFB0CCC0)
    val onSecondary = Color(0xFF1C352C)
    val secondaryContainer = Color(0xFF334B42)
    val onSecondaryContainer = Color(0xFFCCE8DC)

    val tertiary = Color(0xFFC6BFFF)
    val onTertiary = Color(0xFF2B2280)
    val tertiaryContainer = Color(0xFF423A97)
    val onTertiaryContainer = Color(0xFFE4DFFF)

    val error = Color(0xFFFFB4AB)
    val onError = Color(0xFF690005)
    val errorContainer = Color(0xFF93000A)
    val onErrorContainer = Color(0xFFFFDAD6)

    val background = Color(0xFF0B0F16)
    val onBackground = Color(0xFFE1E3EA)
    val surface = Color(0xFF0B0F16)
    val onSurface = Color(0xFFE1E3EA)
    val surfaceVariant = Color(0xFF41484F)
    val onSurfaceVariant = Color(0xFFA9B1BC)
    val surfaceTint = primary
    val inverseSurface = Color(0xFFE1E3EA)
    val inverseOnSurface = Color(0xFF2D3137)
    val outline = Color(0xFF8B929B)
    val outlineVariant = Color(0xFF333A45)
    val scrim = Color(0xFF000000)

    val surfaceDim = Color(0xFF0B0F16)
    val surfaceBright = Color(0xFF31363F)
    val surfaceContainerLowest = Color(0xFF06090E)
    val surfaceContainerLow = Color(0xFF12161F)
    val surfaceContainer = Color(0xFF171C26)
    val surfaceContainerHigh = Color(0xFF212733)
    val surfaceContainerHighest = Color(0xFF2B323F)
}

// ---------------------------------------------------------------------------------------------
// Semantic finance colors
// ---------------------------------------------------------------------------------------------
@Immutable
data class FinPulseColors(
    val isDark: Boolean,
    val income: Color,
    val expense: Color,
    val transfer: Color,
    val investment: Color,
    val debt: Color,
    val warning: Color,
    val savings: Color,
    val info: Color,
    /** Brighter / deeper brand tones for gradients and hero accents. */
    val brandBright: Color,
    val brandDeep: Color,
    /** Glow tint used behind hero numbers. */
    val heroGlow: Color
)

internal val LightFinColors = FinPulseColors(
    isDark = false,
    income = Color(0xFF007A52),
    expense = Color(0xFFC0263F),
    transfer = Color(0xFF0B67A0),
    investment = Color(0xFF6B3FD4),
    debt = Color(0xFFB8470F),
    warning = Color(0xFF9A5B00),
    savings = Color(0xFF0D7369),
    info = Color(0xFF0E6F86),
    brandBright = Color(0xFF10B981),
    brandDeep = Color(0xFF00513A),
    heroGlow = Color(0xFF10B981)
)

internal val DarkFinColors = FinPulseColors(
    isDark = true,
    income = Color(0xFF4ADE9E),
    expense = Color(0xFFFF7A8E),
    transfer = Color(0xFF5CC3F7),
    investment = Color(0xFFB39DFF),
    debt = Color(0xFFFF9D5C),
    warning = Color(0xFFFFC34D),
    savings = Color(0xFF3DDAC6),
    info = Color(0xFF4FD8EE),
    brandBright = Color(0xFF34D399),
    brandDeep = Color(0xFF047857),
    heroGlow = Color(0xFF34D399)
)

val LocalFinPulseColors = staticCompositionLocalOf { DarkFinColors }

/** Semantic finance palette for the current theme. */
val MaterialTheme.finColors: FinPulseColors
    @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current

/** True when the active FinPulse theme is dark (replaces comparing background hex values). */
val isFinPulseDark: Boolean
    @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current.isDark

// ---------------------------------------------------------------------------------------------
// Legacy token names, now theme-aware. Call sites keep compiling; values follow the theme.
// ---------------------------------------------------------------------------------------------
val EmeraldPrimary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val EmeraldLight: Color @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current.brandBright
val EmeraldDark: Color @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current.brandDeep

/** Content color for anything drawn on top of [EmeraldPrimary]. */
val OnEmerald: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onPrimary

val SapphireAccent: Color @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current.transfer
val PurpleAccent: Color @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current.investment
val AmberWarning: Color @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current.warning
val CrimsonExpense: Color @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current.expense
val TealSavings: Color @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current.savings
val CyanAccent: Color @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current.info

val IncomeGreen: Color @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current.income
val ExpenseRed: Color @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current.expense
val TransferBlue: Color @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current.transfer
val InvestmentPurple: Color @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current.investment
val DebtOrange: Color @Composable @ReadOnlyComposable get() = LocalFinPulseColors.current.debt

// Raw palette constants kept for non-Compose consumers (widgets, XML, notifications).
val DarkBackground = DarkTokens.background
val DarkSurface = DarkTokens.surface
val DarkSurfaceVariant = DarkTokens.surfaceContainerHigh
val DarkSurfaceElevated = DarkTokens.surfaceContainerHighest
val DarkOutline = DarkTokens.outline
val DarkOutlineVariant = DarkTokens.outlineVariant
val DarkOnBackground = DarkTokens.onBackground
val DarkOnSurface = DarkTokens.onSurface
val DarkOnSurfaceMuted = DarkTokens.onSurfaceVariant

val LightBackground = LightTokens.background
val LightSurface = LightTokens.surfaceContainerLowest
val LightSurfaceVariant = LightTokens.surfaceContainer
val LightSurfaceElevated = LightTokens.surfaceContainerHigh
val LightOutline = LightTokens.outline
val LightOutlineVariant = LightTokens.outlineVariant
val LightOnBackground = LightTokens.onBackground
val LightOnSurface = LightTokens.onSurface
val LightOnSurfaceMuted = LightTokens.onSurfaceVariant

// Curated gradients (static; used by a few hero surfaces).
object FinPulseGradients {
    val emeraldPrimary = listOf(Color(0xFF10B981), Color(0xFF047857))
    val emeraldGlow = listOf(Color(0xFF34D399), Color(0xFF047857))
    val sapphire = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
    val purple = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
    val expense = listOf(Color(0xFFF43F5E), Color(0xFFBE123C))
    val amber = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
    val darkCard = listOf(DarkTokens.surfaceContainerHigh, DarkTokens.surfaceContainerLow)
    val darkCardElevated = listOf(DarkTokens.surfaceContainerHighest, DarkTokens.surfaceContainer)
    val lightCard = listOf(LightTokens.surfaceContainerLowest, LightTokens.surfaceContainerLow)
}

// Shimmer tokens
val ShimmerDarkBase = DarkTokens.surfaceContainerHigh
val ShimmerDarkHighlight = DarkTokens.surfaceContainerHighest
val ShimmerLightBase = LightTokens.surfaceContainerHigh
val ShimmerLightHighlight = LightTokens.surfaceContainerLowest

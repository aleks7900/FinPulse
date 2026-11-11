package md.alexlab.finpulse.presentation.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import md.alexlab.finpulse.R

sealed class BottomNavItem(
    val screen: Screen,
    val titleRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Dashboard : BottomNavItem(
        Screen.Dashboard, R.string.nav_dashboard,
        Icons.Outlined.SpaceDashboard, Icons.Filled.SpaceDashboard
    )
    data object Transactions : BottomNavItem(
        Screen.Transactions, R.string.nav_transactions,
        Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong
    )
    data object Budgets : BottomNavItem(
        Screen.Budgets, R.string.nav_budgets,
        Icons.Outlined.PieChart, Icons.Filled.PieChart
    )
    data object Analytics : BottomNavItem(
        Screen.Analytics, R.string.nav_analytics,
        Icons.Outlined.Insights, Icons.Filled.Insights
    )
    data object More : BottomNavItem(
        Screen.More, R.string.nav_more,
        Icons.Outlined.Widgets, Icons.Filled.Widgets
    )

    companion object {
        val items = listOf(Dashboard, Transactions, Budgets, Analytics, More)
    }
}

/**
 * Material 3 Expressive short navigation bar.
 *
 * - Pill indicator and icon/label motion come from the theme's expressive motion scheme.
 * - Outlined icons swap to filled when selected.
 * - Labels auto-shrink (down to 9sp) so long translations (de, ru, pl...) never truncate, and
 *   font scale is clamped so five tabs stay usable at large accessibility sizes.
 */
@Composable
fun FinPulseBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDensity = LocalDensity.current
    val navBarDensity = remember(currentDensity) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.85f, 1.15f)
        )
    }

    CompositionLocalProvider(LocalDensity provides navBarDensity) {
        ShortNavigationBar(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            BottomNavItem.items.forEach { item ->
                val selected = currentRoute == item.screen.route
                val title = stringResource(item.titleRes)

                ShortNavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.screen) },
                    icon = {
                        Crossfade(targetState = selected, label = "navIcon") { isSelected ->
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.icon,
                                contentDescription = null
                            )
                        }
                    },
                    label = {
                        Text(
                            text = title,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            style = if (selected) {
                                MaterialTheme.typography.labelMediumEmphasized
                            } else {
                                MaterialTheme.typography.labelMedium
                            },
                            autoSize = TextAutoSize.StepBased(
                                minFontSize = 9.sp,
                                maxFontSize = 12.sp,
                                stepSize = 0.5.sp
                            )
                        )
                    },
                    colors = ShortNavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedIndicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

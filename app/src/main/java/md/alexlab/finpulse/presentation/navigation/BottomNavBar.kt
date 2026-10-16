package md.alexlab.finpulse.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.designsystem.FinPulseMotion
import md.alexlab.finpulse.core.designsystem.FinPulseRadius
import md.alexlab.finpulse.core.designsystem.bounceClick

sealed class BottomNavItem(
    val screen: Screen,
    val titleRes: Int,
    val icon: ImageVector
) {
    data object Dashboard : BottomNavItem(Screen.Dashboard, R.string.nav_dashboard, Icons.Default.Dashboard)
    data object Transactions : BottomNavItem(Screen.Transactions, R.string.nav_transactions, Icons.AutoMirrored.Filled.ReceiptLong)
    data object Budgets : BottomNavItem(Screen.Budgets, R.string.nav_budgets, Icons.Default.PieChart)
    data object Analytics : BottomNavItem(Screen.Analytics, R.string.nav_analytics, Icons.Default.BarChart)
    data object More : BottomNavItem(Screen.More, R.string.nav_more, Icons.Default.Widgets)

    companion object {
        val items = listOf(Dashboard, Transactions, Budgets, Analytics, More)
    }
}

@Composable
fun FinPulseBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val isDark = MaterialTheme.colorScheme.background.value == md.alexlab.finpulse.core.designsystem.DarkBackground.value ||
            MaterialTheme.colorScheme.surface.value == md.alexlab.finpulse.core.designsystem.DarkSurface.value

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(barShape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.02f))
                    } else {
                        listOf(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f), Color.Transparent)
                    }
                ),
                shape = barShape
            ),
        shape = barShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem.items.forEach { item ->
                val selected = currentRoute == item.screen.route
                val itemTitle = stringResource(item.titleRes)

                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1.0f,
                    animationSpec = FinPulseMotion.springBouncy,
                    label = "navIconScale"
                )

                val pillWidthAnim by animateFloatAsState(
                    targetValue = if (selected) 54f else 40f,
                    animationSpec = FinPulseMotion.springStandard,
                    label = "pillWidth"
                )

                val iconOffsetY by animateFloatAsState(
                    targetValue = if (selected) -2f else 0f,
                    animationSpec = FinPulseMotion.springStandard,
                    label = "iconOffsetY"
                )

                val iconColor by animateColorAsState(
                    targetValue = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "navIconColor"
                )

                val pillBgColor by animateColorAsState(
                    targetValue = if (selected) EmeraldPrimary.copy(alpha = 0.16f) else Color.Transparent,
                    animationSpec = tween(220),
                    label = "navPillColor"
                )

                val dotScale by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = FinPulseMotion.springBouncy,
                    label = "dotScale"
                )

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(FinPulseRadius.lg))
                        .bounceClick(scaleDown = 0.90f) { onNavigate(item.screen) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(height = 30.dp, width = pillWidthAnim.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(pillBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = itemTitle,
                            tint = iconColor,
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                    translationY = iconOffsetY.dp.toPx()
                                }
                                .size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = itemTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = iconColor
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Active dot indicator
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = dotScale
                                scaleY = dotScale
                                alpha = dotScale
                            }
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary)
                    )
                }
            }
        }
    }
}

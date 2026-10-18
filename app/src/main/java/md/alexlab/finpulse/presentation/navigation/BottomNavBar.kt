package md.alexlab.finpulse.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.designsystem.FinPulseMotion
import md.alexlab.finpulse.core.designsystem.FinPulseRadius

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

    // Clamp font scale for bottom bar to prevent fixed 5-tab layout breakdown on large accessibility scaling
    val currentDensity = LocalDensity.current
    val navBarDensity = remember(currentDensity) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.85f, 1.15f)
        )
    }

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
        CompositionLocalProvider(LocalDensity provides navBarDensity) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem.items.forEach { item ->
                    val selected = currentRoute == item.screen.route
                    val itemTitle = stringResource(item.titleRes)

                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val pressScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else 1.0f,
                        animationSpec = if (isPressed) FinPulseMotion.springTactile else FinPulseMotion.springBouncy,
                        label = "navItemPressScale"
                    )

                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.10f else 1.0f,
                        animationSpec = FinPulseMotion.springBouncy,
                        label = "navIconScale"
                    )

                    val pillScaleX by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.7f,
                        animationSpec = FinPulseMotion.springStandard,
                        label = "navPillScale"
                    )

                    val pillAlpha by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = tween(200),
                        label = "navPillAlpha"
                    )

                    val iconColor by animateColorAsState(
                        targetValue = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "navIconColor"
                    )

                    val dotScale by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = FinPulseMotion.springBouncy,
                        label = "dotScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(FinPulseRadius.lg))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onNavigate(item.screen)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = pressScale
                                    scaleY = pressScale
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Fixed size pill container ensures no layout recalculations during selection animations
                            Box(
                                modifier = Modifier
                                    .size(width = 50.dp, height = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = pillScaleX
                                            alpha = pillAlpha
                                        }
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(EmeraldPrimary.copy(alpha = 0.16f))
                                )

                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = itemTitle,
                                    tint = iconColor,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                        .size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            AutoResizeNavText(
                                text = itemTitle,
                                isSelected = selected,
                                color = iconColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Fixed size dot slot avoids vertical layout shifts
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .graphicsLayer {
                                        scaleX = dotScale
                                        scaleY = dotScale
                                        alpha = dotScale
                                    }
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoResizeNavText(
    text: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val availableWidthPx = with(density) { (maxWidth - 4.dp).coerceAtLeast(0.dp).toPx() }
        val targetWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        val baseStyle = MaterialTheme.typography.labelSmall.copy(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeight = 13.sp,
            textAlign = TextAlign.Center
        )

        // Measure against SemiBold (widest state) so font size never abruptly changes when selected
        val bestFontSize = remember(text, availableWidthPx) {
            if (availableWidthPx <= 0f) return@remember 10.sp
            val candidateSizes = listOf(10.sp, 9.5.sp, 9.sp, 8.5.sp, 8.sp)
            val measureStyle = baseStyle.copy(fontWeight = FontWeight.SemiBold)
            candidateSizes.firstOrNull { size ->
                val layoutResult = textMeasurer.measure(
                    text = text,
                    style = measureStyle.copy(fontSize = size),
                    maxLines = 1,
                    softWrap = false
                )
                layoutResult.size.width <= availableWidthPx
            } ?: 8.sp
        }

        Text(
            text = text,
            style = baseStyle.copy(
                fontSize = bestFontSize,
                fontWeight = targetWeight,
                color = color
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

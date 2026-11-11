package md.alexlab.finpulse.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.AnimatedAmountText
import md.alexlab.finpulse.core.designsystem.LocalMotionConfig
import md.alexlab.finpulse.core.designsystem.bounceClick
import md.alexlab.finpulse.core.designsystem.finColors
import md.alexlab.finpulse.core.model.Money

/**
 * Dashboard hero: net worth with available balance and this month's cash flow.
 *
 * Expressive styling: an extra-large rounded primaryContainer surface with a slow ambient
 * glow, emphasized display numerals that auto-shrink for long amounts, and tonal cash-flow
 * tiles. Works with dynamic color because every tone comes from the theme.
 */
@Composable
fun BalanceCard(
    totalBalance: Money,
    availableBalance: Money,
    monthlyIncome: Money,
    monthlyExpenses: Money,
    hideBalances: Boolean,
    onToggleHideBalances: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotionConfig.current
    val heroShape = MaterialTheme.shapes.extraLargeIncreased

    val animateGlow = !motion.isReducedMotion && motion.enableContinuousEffects
    val shift by if (animateGlow) {
        rememberInfiniteTransition(label = "heroGlow").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "heroGlowShift"
        )
    } else {
        remember { mutableFloatStateOf(0.5f) }
    }

    val glow = scheme.primary
    val glowSecondary = scheme.tertiary
    val hiddenLabel = stringResource(R.string.dashboard_net_worth)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = heroShape,
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer
    ) {
        Box(
            modifier = Modifier
                .clip(heroShape)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(glow.copy(alpha = 0.28f), Color.Transparent),
                            center = Offset(size.width * (0.85f - shift * 0.15f), size.height * 0.05f),
                            radius = size.maxDimension * (0.75f + shift * 0.1f)
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(glowSecondary.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(size.width * (0.05f + shift * 0.1f), size.height),
                            radius = size.maxDimension * 0.6f
                        )
                    )
                }
                .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_net_worth),
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconToggleButton(
                        checked = hideBalances,
                        onCheckedChange = { onToggleHideBalances() },
                        shapes = IconButtonDefaults.toggleableShapes(),
                        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                            containerColor = scheme.onPrimaryContainer.copy(alpha = 0.08f),
                            contentColor = scheme.onPrimaryContainer,
                            checkedContainerColor = scheme.onPrimaryContainer,
                            checkedContentColor = scheme.primaryContainer
                        )
                    ) {
                        Crossfade(targetState = hideBalances, label = "eye") { hidden ->
                            Icon(
                                imageVector = if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (hidden) "Show balances" else "Hide balances",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                AnimatedAmountText(
                    amountText = if (hideBalances) "••••••" else totalBalance.formatted(),
                    style = MaterialTheme.typography.displayLargeEmphasized,
                    fontWeight = MaterialTheme.typography.displayLargeEmphasized.fontWeight
                        ?: androidx.compose.ui.text.font.FontWeight.Bold,
                    color = scheme.onPrimaryContainer,
                    autoSize = TextAutoSize.StepBased(minFontSize = 26.sp, maxFontSize = 44.sp, stepSize = 1.sp),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .semantics { if (hideBalances) contentDescription = hiddenLabel }
                )

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.account_available_balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.width(6.dp))
                    AnimatedAmountText(
                        amountText = if (hideBalances) "••••" else availableBalance.formatted(),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = scheme.onPrimaryContainer
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CashFlowPill(
                        label = stringResource(R.string.tx_type_income),
                        amount = monthlyIncome,
                        isIncome = true,
                        hideBalances = hideBalances,
                        modifier = Modifier.weight(1f)
                    )
                    CashFlowPill(
                        label = stringResource(R.string.tx_type_expense),
                        amount = monthlyExpenses,
                        isIncome = false,
                        hideBalances = hideBalances,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Tonal cash-flow tile used inside the hero card. */
@Composable
fun CashFlowPill(
    label: String,
    amount: Money,
    isIncome: Boolean,
    hideBalances: Boolean,
    modifier: Modifier = Modifier
) {
    val fin = MaterialTheme.finColors
    val accent = if (isIncome) fin.income else fin.expense
    val icon = if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
    val prefix = if (isIncome) "+" else "−"

    Surface(
        modifier = modifier.bounceClick(scaleDown = 0.97f),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = if (fin.isDark) 0.35f else 0.7f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                AnimatedAmountText(
                    amountText = if (hideBalances) "••••" else "$prefix${amount.formatted()}",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp, stepSize = 0.5.sp)
                )
            }
        }
    }
}

package md.alexlab.finpulse.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.CrimsonExpense
import md.alexlab.finpulse.core.designsystem.EmeraldDark
import md.alexlab.finpulse.core.designsystem.EmeraldLight
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.designsystem.FinPulseMotion
import md.alexlab.finpulse.core.designsystem.FinPulseRadius
import md.alexlab.finpulse.core.designsystem.AnimatedAmountText
import md.alexlab.finpulse.core.designsystem.bounceClick
import md.alexlab.finpulse.core.model.Money

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
    val cardShape = RoundedCornerShape(FinPulseRadius.xxl)
    val isDark = MaterialTheme.colorScheme.background.value == md.alexlab.finpulse.core.designsystem.DarkBackground.value ||
            MaterialTheme.colorScheme.surface.value == md.alexlab.finpulse.core.designsystem.DarkSurface.value

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                ),
                shape = cardShape
            ),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = if (isDark) 0.16f else 0.08f),
                            Color.Transparent
                        ),
                        radius = 750f
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                // Top Header Row: Label & Eye Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dashboard_net_worth).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val rotationAnim by animateFloatAsState(
                        targetValue = if (hideBalances) 180f else 0f,
                        animationSpec = FinPulseMotion.springGentle,
                        label = "eyeRotation"
                    )

                    Surface(
                        modifier = Modifier
                            .size(34.dp)
                            .bounceClick(scaleDown = 0.90f, onClick = onToggleHideBalances),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Box(
                            modifier = Modifier.rotate(rotationAnim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hideBalances) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (hideBalances) "Show balances" else "Hide balances",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hero Total Balance with Animated Rolling Numbers
                AnimatedAmountText(
                    amountText = if (hideBalances) "••••••••" else totalBalance.formatted(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Available Balance subtitle
                Text(
                    text = "${stringResource(R.string.account_available_balance)} • " +
                            if (hideBalances) "••••••" else availableBalance.formatted(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Income & Expenses Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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

@Composable
fun CashFlowPill(
    label: String,
    amount: Money,
    isIncome: Boolean,
    hideBalances: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isIncome) EmeraldPrimary else CrimsonExpense
    val icon = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
    val prefix = if (isIncome) "+" else "-"

    Surface(
        modifier = modifier
            .border(
                width = 0.75.dp,
                color = accentColor.copy(alpha = 0.20f),
                shape = RoundedCornerShape(FinPulseRadius.lg)
            ),
        shape = RoundedCornerShape(FinPulseRadius.lg),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (hideBalances) "••••••" else "$prefix${amount.formatted()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

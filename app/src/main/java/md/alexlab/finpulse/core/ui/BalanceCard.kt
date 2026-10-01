package md.alexlab.finpulse.core.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import md.alexlab.finpulse.core.designsystem.CrimsonExpense
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        radius = 600f
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(md.alexlab.finpulse.R.string.dashboard_net_worth).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${androidx.compose.ui.res.stringResource(md.alexlab.finpulse.R.string.account_available_balance)}: " + if (hideBalances) "••••••" else availableBalance.formatted(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    IconButton(
                        onClick = onToggleHideBalances,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (hideBalances) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (hideBalances) "Show balances" else "Hide balances",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedContent(
                    targetState = hideBalances,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "BalanceAnimation"
                ) { hidden ->
                    Text(
                        text = if (hidden) "••••••••" else totalBalance.formatted(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Income & Expenses Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Income Pill
                    CashFlowPill(
                        label = androidx.compose.ui.res.stringResource(md.alexlab.finpulse.R.string.tx_type_income),
                        amount = monthlyIncome,
                        isIncome = true,
                        hideBalances = hideBalances,
                        modifier = Modifier.weight(1f)
                    )

                    // Expense Pill
                    CashFlowPill(
                        label = androidx.compose.ui.res.stringResource(md.alexlab.finpulse.R.string.tx_type_expense),
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

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
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
                    text = if (hideBalances) "••••••" else amount.formatted(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

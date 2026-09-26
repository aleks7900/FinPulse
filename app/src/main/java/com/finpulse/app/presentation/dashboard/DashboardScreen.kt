package com.finpulse.app.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finpulse.app.core.designsystem.AmberWarning
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.DebtOrange
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.InvestmentPurple
import com.finpulse.app.core.designsystem.TealSavings
import com.finpulse.app.core.model.Money
import com.finpulse.app.core.model.TimePeriod
import com.finpulse.app.core.ui.BalanceCard
import com.finpulse.app.core.ui.BudgetProgressBar
import com.finpulse.app.core.ui.TransactionItem
import com.finpulse.app.domain.model.FinancialInsight
import com.finpulse.app.domain.model.InsightType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onPeriodSelected: (TimePeriod) -> Unit,
    onToggleHideBalances: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToReviewQueue: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryMap = uiState.categories.associateBy { it.id }
    val accountMap = uiState.accounts.associateBy { it.id }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Executive Top Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "F",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "FinPulse",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            actions = {
                IconButton(onClick = onNavigateToSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Time Range Selector
            item {
                TimeRangeSelector(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = onPeriodSelected
                )
            }

            // Review Queue Alert Banner
            if (uiState.unreviewedCount > 0) {
                item {
                    ReviewQueueBannerCard(
                        unreviewedCount = uiState.unreviewedCount,
                        onClick = onNavigateToReviewQueue
                    )
                }
            }

            // 2. Executive Balance & Cash Flow Card
            item {
                BalanceCard(
                    totalBalance = uiState.summary.totalBalance,
                    availableBalance = uiState.summary.availableBalance,
                    monthlyIncome = uiState.summary.income,
                    monthlyExpenses = uiState.summary.expenses,
                    hideBalances = uiState.hideBalances,
                    onToggleHideBalances = onToggleHideBalances
                )
            }

            // 3. Quick Action Bar
            item {
                QuickActionBar(
                    onAddTransaction = onNavigateToAddTransaction,
                    onViewAccounts = onNavigateToAccounts,
                    onViewBudgets = onNavigateToBudgets
                )
            }

            // 4. Financial Health Metrics Strip
            item {
                FinancialMetricsStrip(
                    savings = uiState.summary.totalSavings,
                    investments = uiState.summary.totalInvestments,
                    debt = uiState.summary.totalDebt,
                    savingsRate = uiState.summary.savingsRatePercentage,
                    hideBalances = uiState.hideBalances
                )
            }

            // 5. Smart Insights Feed Card (if available)
            if (uiState.insights.isNotEmpty()) {
                item {
                    val topInsight = uiState.insights.first()
                    InsightBanner(
                        insight = topInsight,
                        onClick = onNavigateToInsights
                    )
                }
            }

            // 6. Active Budgets Preview
            if (uiState.budgetStatuses.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Budget Performance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldPrimary,
                            modifier = Modifier.clickable(onClick = onNavigateToBudgets)
                        )
                    }
                }

                items(uiState.budgetStatuses.take(2)) { status ->
                    BudgetProgressCard(
                        status = status,
                        hideBalances = uiState.hideBalances
                    )
                }
            }

            // 7. Recent Transactions Header & List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ledger",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary,
                        modifier = Modifier.clickable(onClick = onNavigateToTransactions)
                    )
                }
            }

            if (uiState.recentTransactions.isEmpty()) {
                item {
                    EmptyTransactionsPrompt(onAddTransaction = onNavigateToAddTransaction)
                }
            } else {
                items(uiState.recentTransactions) { tx ->
                    val cat = categoryMap[tx.categoryId]
                    val acc = accountMap[tx.sourceAccountId]?.name ?: "Account"
                    TransactionItem(
                        transaction = tx,
                        category = cat,
                        accountName = acc,
                        hideBalances = uiState.hideBalances,
                        onClick = onNavigateToTransactions
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun TimeRangeSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimePeriod.values().forEach { period ->
            val isSelected = period == selectedPeriod
            Surface(
                modifier = Modifier.clickable { onPeriodSelected(period) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = period.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionBar(
    onAddTransaction: () -> Unit,
    onViewAccounts: () -> Unit,
    onViewBudgets: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionButton(
            label = "Add Entry",
            icon = Icons.Default.Add,
            accentColor = EmeraldPrimary,
            onClick = onAddTransaction,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = "Accounts",
            icon = Icons.Default.SwapHoriz,
            accentColor = MaterialTheme.colorScheme.secondary,
            onClick = onViewAccounts,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = "Budgets",
            icon = Icons.Default.TrendingUp,
            accentColor = InvestmentPurple,
            onClick = onViewBudgets,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FinancialMetricsStrip(
    savings: Money,
    investments: Money,
    debt: Money,
    savingsRate: Double,
    hideBalances: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricTile(
            title = "Savings",
            value = if (hideBalances) "••••" else savings.formattedCompact(),
            color = TealSavings,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = "Investments",
            value = if (hideBalances) "••••" else investments.formattedCompact(),
            color = InvestmentPurple,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = "Debt",
            value = if (hideBalances) "••••" else debt.formattedCompact(),
            color = DebtOrange,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = "Savings Rate",
            value = "${"%.0f".format(savingsRate)}%",
            color = EmeraldPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun InsightBanner(
    insight: FinancialInsight,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (borderColor, icon) = when (insight.type) {
        InsightType.DANGER -> CrimsonExpense to Icons.Default.Warning
        InsightType.WARNING -> AmberWarning to Icons.Default.Warning
        InsightType.SUCCESS -> EmeraldPrimary to Icons.Default.TrendingUp
        InsightType.INFO -> MaterialTheme.colorScheme.secondary to Icons.Default.Info
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(borderColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun BudgetProgressCard(
    status: com.finpulse.app.domain.model.BudgetStatus,
    hideBalances: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (hideBalances) "•••• / ••••" else "${status.spentAmount.formatted()} / ${status.budget.limitAmount.formatted()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            BudgetProgressBar(percentage = status.percentageConsumed)

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(status.percentageConsumed * 100).toInt()}% consumed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (hideBalances) "•••• left" else "${status.remainingAmount.formatted()} left",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (status.isExceeded) CrimsonExpense else EmeraldPrimary
                )
            }
        }
    }
}

@Composable
fun EmptyTransactionsPrompt(
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No Transactions Recorded",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add your first income or expense to generate live cash flow analytics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAddTransaction,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Record Transaction", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ReviewQueueBannerCard(
    unreviewedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = AmberWarning.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AmberWarning.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Review Queue",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = AmberWarning
                    ) {
                        Text(
                            text = unreviewedCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$unreviewedCount transactions need category confirmation or rule learning",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Review",
                tint = AmberWarning,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

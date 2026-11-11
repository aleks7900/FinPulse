package md.alexlab.finpulse.presentation.dashboard

import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.key
import androidx.compose.material3.toShape
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import md.alexlab.finpulse.core.designsystem.OnEmerald
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.AmberWarning
import md.alexlab.finpulse.core.designsystem.CrimsonExpense
import md.alexlab.finpulse.core.designsystem.DebtOrange
import md.alexlab.finpulse.core.designsystem.EmeraldDark
import md.alexlab.finpulse.core.designsystem.EmeraldLight
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.designsystem.FinPulseGradients
import md.alexlab.finpulse.core.designsystem.FinPulseRadius
import md.alexlab.finpulse.core.designsystem.FinPulseMotion
import md.alexlab.finpulse.core.designsystem.AnimatedAmountText
import md.alexlab.finpulse.core.designsystem.staggeredEntrance
import md.alexlab.finpulse.core.designsystem.InvestmentPurple
import md.alexlab.finpulse.core.designsystem.SapphireAccent
import md.alexlab.finpulse.core.designsystem.TealSavings
import md.alexlab.finpulse.core.designsystem.bounceClick
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.core.model.TimePeriod
import md.alexlab.finpulse.core.ui.BalanceCard
import md.alexlab.finpulse.core.ui.BudgetProgressBar
import md.alexlab.finpulse.core.ui.FinPulseCard
import md.alexlab.finpulse.core.ui.FinPulseEmptyState
import md.alexlab.finpulse.core.ui.TransactionItem
import md.alexlab.finpulse.core.ui.getCategoryDisplayName
import md.alexlab.finpulse.core.ui.getLocalizedName
import md.alexlab.finpulse.domain.model.FinancialInsight
import md.alexlab.finpulse.domain.model.getLocalizedTitle
import md.alexlab.finpulse.domain.model.getLocalizedDescription
import md.alexlab.finpulse.domain.model.InsightType

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
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToDigest: () -> Unit = {},
    syncAction: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryMap = uiState.categories.associateBy { it.id }
    val accountMap = uiState.accounts.associateBy { it.id }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        // Executive Top Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(MaterialShapes.Cookie9Sided.toShape())
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "F",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "FinPulse",
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            actions = {
                FilledTonalIconButton(
                    onClick = onNavigateToSearch,
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.action_search)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                syncAction()
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            scrollBehavior = scrollBehavior
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
                    onPeriodSelected = onPeriodSelected,
                    modifier = Modifier.staggeredEntrance(index = 0)
                )
            }

            // Review Queue Alert Banner
            if (uiState.unreviewedCount > 0) {
                item {
                    ReviewQueueBannerCard(
                        unreviewedCount = uiState.unreviewedCount,
                        onClick = onNavigateToReviewQueue,
                        modifier = Modifier.staggeredEntrance(index = 1)
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
                    onToggleHideBalances = onToggleHideBalances,
                    modifier = Modifier.staggeredEntrance(index = 2)
                )
            }

            // 3. Quick Action Bar
            item {
                QuickActionBar(
                    onAddTransaction = onNavigateToAddTransaction,
                    onViewAccounts = onNavigateToAccounts,
                    onViewBudgets = onNavigateToBudgets,
                    modifier = Modifier.staggeredEntrance(index = 3)
                )
            }

            // Financial Calendar & Upcoming Cash Flow Banner
            item {
                FinPulseCard(
                    shape = RoundedCornerShape(FinPulseRadius.lg),
                    onClick = onNavigateToCalendar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(index = 4)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialShapes.Pill.toShape(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.calendar_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.more_calendar_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 4. Financial Health Metrics Strip
            item {
                FinancialMetricsStrip(
                    savings = uiState.summary.totalSavings,
                    investments = uiState.summary.totalInvestments,
                    debt = uiState.summary.totalDebt,
                    savingsRate = uiState.summary.savingsRatePercentage,
                    hideBalances = uiState.hideBalances,
                    modifier = Modifier.staggeredEntrance(index = 5)
                )
            }

            // Financial Digest Glance Banner
            item {
                FinancialDigestBannerCard(
                    onClick = onNavigateToDigest,
                    modifier = Modifier.staggeredEntrance(index = 6)
                )
            }

            // 5. Smart Insights Feed Card (if available)
            if (uiState.insights.isNotEmpty()) {
                item {
                    val topInsight = uiState.insights.first()
                    InsightBanner(
                        insight = topInsight,
                        onClick = onNavigateToInsights,
                        modifier = Modifier.staggeredEntrance(index = 7)
                    )
                }
            }

            // 6. Active Budgets Preview
            if (uiState.budgetStatuses.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(index = 8),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.budgets_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = onNavigateToBudgets) {
                            Text(
                                text = stringResource(R.string.action_view_all),
                                style = MaterialTheme.typography.labelLargeEmphasized
                            )
                        }
                    }
                }

                items(uiState.budgetStatuses.take(2), key = { it.budget.id }) { status ->
                    BudgetProgressCard(
                        status = status,
                        hideBalances = uiState.hideBalances,
                        modifier = Modifier.animateItem()
                    )
                }
            }

            // 7. Recent Transactions Header & List
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(index = 9),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_recent_transactions),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onNavigateToTransactions) {
                        Text(
                            text = stringResource(R.string.action_view_all),
                            style = MaterialTheme.typography.labelLargeEmphasized
                        )
                    }
                }
            }

            if (uiState.recentTransactions.isEmpty()) {
                item {
                    FinPulseEmptyState(
                        icon = Icons.Default.ReceiptLong,
                        title = stringResource(R.string.dashboard_no_transactions),
                        description = stringResource(R.string.tx_empty_desc),
                        actionLabel = stringResource(R.string.shortcut_quick_add_short),
                        onActionClick = onNavigateToAddTransaction
                    )
                }
            } else {
                item(key = "recent_transactions_group") {
                    val fallbackAccount = stringResource(R.string.tx_account)
                    val lastIndex = uiState.recentTransactions.lastIndex
                    Column(
                        modifier = Modifier.animateItem(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        uiState.recentTransactions.forEachIndexed { index, tx ->
                            key(tx.id) {
                                Surface(
                                    shape = segmentedShape(index, lastIndex),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    TransactionItem(
                                        transaction = tx,
                                        category = categoryMap[tx.categoryId],
                                        accountName = accountMap[tx.sourceAccountId]?.name ?: fallbackAccount,
                                        hideBalances = uiState.hideBalances,
                                        onClick = onNavigateToTransactions,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(84.dp))
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
            val scaleAnim by animateFloatAsState(
                targetValue = if (isSelected) 1.04f else 1.0f,
                animationSpec = FinPulseMotion.springBouncy,
                label = "periodScale"
            )
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                animationSpec = tween(220),
                label = "periodBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) OnEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(220),
                label = "periodText"
            )

            Surface(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scaleAnim
                        scaleY = scaleAnim
                    }
                    .clip(RoundedCornerShape(FinPulseRadius.full))
                    .bounceClick(scaleDown = 0.94f) { onPeriodSelected(period) },
                shape = RoundedCornerShape(FinPulseRadius.full),
                color = bgColor,
                contentColor = textColor
            ) {
                Text(
                    text = period.getLocalizedName(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
            label = stringResource(R.string.action_add),
            icon = Icons.Default.Add,
            accentColor = EmeraldPrimary,
            onClick = onAddTransaction,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = stringResource(R.string.dashboard_accounts_overview),
            icon = Icons.Default.SwapHoriz,
            accentColor = SapphireAccent,
            onClick = onViewAccounts,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = stringResource(R.string.nav_budgets),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
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
        modifier = modifier.bounceClick(scaleDown = 0.94f, onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
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
            title = stringResource(R.string.goals_total_saved),
            value = if (hideBalances) "••••" else savings.formattedCompact(),
            color = TealSavings,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = stringResource(R.string.investments_title),
            value = if (hideBalances) "••••" else investments.formattedCompact(),
            color = InvestmentPurple,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = stringResource(R.string.debt_title),
            value = if (hideBalances) "••••" else debt.formattedCompact(),
            color = DebtOrange,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = stringResource(R.string.analytics_savings_rate, "%.0f".format(savingsRate)),
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
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedAmountText(
                amountText = value,
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
        InsightType.SUCCESS -> EmeraldPrimary to Icons.AutoMirrored.Filled.TrendingUp
        InsightType.INFO -> MaterialTheme.colorScheme.secondary to Icons.Default.Info
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = androidx.compose.ui.graphics.lerp(
            MaterialTheme.colorScheme.surfaceContainerLow,
            borderColor,
            0.10f
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(borderColor.copy(alpha = 0.16f)),
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
                    text = insight.getLocalizedTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = insight.getLocalizedDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun BudgetProgressCard(
    status: md.alexlab.finpulse.domain.model.BudgetStatus,
    hideBalances: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getCategoryDisplayName(status.budget.categoryId, status.categoryName),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (hideBalances) "•••• / ••••" else "${status.spentAmount.formatted()} / ${status.budget.limitAmount.formatted()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            BudgetProgressBar(percentage = status.percentageConsumed)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(status.percentageConsumed * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (hideBalances) "••••" else if (status.isExceeded) stringResource(R.string.budget_exceeded) else "${status.remainingAmount.formatted()} (${stringResource(R.string.budget_remaining)})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (status.isExceeded) CrimsonExpense else EmeraldPrimary
                )
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
            .border(1.dp, AmberWarning.copy(alpha = 0.4f), RoundedCornerShape(FinPulseRadius.lg))
            .bounceClick(scaleDown = 0.98f, onClick = onClick),
        shape = RoundedCornerShape(FinPulseRadius.lg),
        color = AmberWarning.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AmberWarning.copy(alpha = 0.22f)),
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
                        text = stringResource(R.string.review_inbox_title),
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
                            color = OnEmerald,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val pendingText = if (unreviewedCount == 1) {
                    stringResource(R.string.dashboard_review_queue_pending_one)
                } else {
                    stringResource(R.string.dashboard_review_queue_pending_many, unreviewedCount)
                }
                Text(
                    text = "$pendingText • ${stringResource(R.string.dashboard_review_queue_clean_up)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.review_inbox_open),
                tint = AmberWarning,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun FinancialDigestBannerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                SapphireAccent.copy(alpha = 0.35f),
                RoundedCornerShape(FinPulseRadius.lg)
            )
            .bounceClick(scaleDown = 0.98f, onClick = onClick),
        shape = RoundedCornerShape(FinPulseRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SapphireAccent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = SapphireAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.digest_screen_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SapphireAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = stringResource(R.string.digest_badge_daily_weekly),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SapphireAccent
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.digest_card_summary_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Shape for item [index] in a segmented (grouped) list ending at [lastIndex]. */
private fun segmentedShape(index: Int, lastIndex: Int): RoundedCornerShape {
    val outer = 24.dp
    val inner = 6.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == lastIndex) outer else inner,
        bottomEnd = if (index == lastIndex) outer else inner
    )
}

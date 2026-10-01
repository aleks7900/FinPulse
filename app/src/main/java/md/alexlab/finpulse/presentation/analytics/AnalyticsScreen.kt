package md.alexlab.finpulse.presentation.analytics

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.CrimsonExpense
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.model.TimePeriod
import md.alexlab.finpulse.core.ui.CategoryIconBadge
import md.alexlab.finpulse.core.ui.DonutChart
import md.alexlab.finpulse.core.ui.DonutSegment
import md.alexlab.finpulse.core.ui.getDisplayName
import md.alexlab.finpulse.presentation.dashboard.TimeRangeSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    uiState: AnalyticsUiState,
    onPeriodSelected: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.analytics_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Selector
            item {
                TimeRangeSelector(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = onPeriodSelected
                )
            }

            // Cash Flow Summary Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.dashboard_monthly_net).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.hideBalances) "••••••" else uiState.netCashFlow.formatted(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.netCashFlow.isNegative) CrimsonExpense else EmeraldPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(stringResource(R.string.total_income), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (uiState.hideBalances) "••••" else uiState.totalIncome.formatted(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = EmeraldPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.total_expenses), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (uiState.hideBalances) "••••" else uiState.totalExpenses.formatted(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = CrimsonExpense)
                            }
                        }
                    }
                }
            }

            // Category Donut Chart
            if (uiState.categoryBreakdown.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.analytics_spending_by_category).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val segments = uiState.categoryBreakdown.map {
                                DonutSegment(
                                    label = it.category.getDisplayName(),
                                    value = it.totalAmount.amountMinor.toDouble(),
                                    color = Color(it.category.colorHex)
                                )
                            }

                            DonutChart(
                                segments = segments,
                                centerTitle = stringResource(R.string.budget_total_spent),
                                centerValue = if (uiState.hideBalances) "••••" else uiState.totalExpenses.formatted(),
                                modifier = Modifier.size(200.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Category breakdown rows
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                uiState.categoryBreakdown.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(item.category.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = item.category.getDisplayName(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${(item.percentageOfTotal * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (uiState.hideBalances) "••••" else item.totalAmount.formatted(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Key Statistics Tiles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = stringResource(R.string.analytics_avg_daily),
                        value = if (uiState.hideBalances) "••••" else uiState.averageDailyExpense.formatted(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = stringResource(R.string.analytics_largest_expense),
                        value = if (uiState.hideBalances) "••••" else uiState.largestExpense.formatted(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = stringResource(R.string.analytics_savings_rate_title),
                        value = "${"%.1f".format(uiState.savingsRate)}%",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Top Merchants
            if (uiState.topMerchants.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.analytics_top_merchants),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(uiState.topMerchants) { merchant ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = merchant.merchant,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = pluralStringResource(R.plurals.plural_transactions, merchant.transactionCount, merchant.transactionCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (uiState.hideBalances) "••••" else merchant.totalAmount.formatted(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

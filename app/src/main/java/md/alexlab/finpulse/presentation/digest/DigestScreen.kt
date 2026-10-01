package md.alexlab.finpulse.presentation.digest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.AmberWarning
import md.alexlab.finpulse.core.designsystem.DebtOrange
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.designsystem.InvestmentPurple
import md.alexlab.finpulse.core.designsystem.SapphireAccent
import md.alexlab.finpulse.core.designsystem.TealSavings
import md.alexlab.finpulse.domain.model.BudgetDigestItem
import md.alexlab.finpulse.domain.model.DigestFrequency
import md.alexlab.finpulse.domain.model.DigestInsight
import md.alexlab.finpulse.domain.model.DigestPeriod
import md.alexlab.finpulse.domain.model.DigestPriority
import md.alexlab.finpulse.domain.model.FinancialDigest
import md.alexlab.finpulse.domain.model.GoalProgressDigestItem
import md.alexlab.finpulse.domain.model.TopExpenseItem
import md.alexlab.finpulse.domain.model.UpcomingBillDigestItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigestScreen(
    viewModel: DigestViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.testNotificationMessage) {
        uiState.testNotificationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearTestNotificationMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.digest_screen_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.selectedPeriod == DigestPeriod.DAILY) {
                                stringResource(R.string.digest_period_daily_desc)
                            } else {
                                stringResource(R.string.digest_period_weekly_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSettings(true) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.digest_settings_title)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading || uiState.digest == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else {
            val digest = uiState.digest!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Period Selector (Daily vs Weekly)
                item {
                    DigestPeriodSelector(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = { viewModel.selectPeriod(it) }
                    )
                }

                // 2. Habit-forming Pulse Hero Banner
                item {
                    DigestPulseHeroCard(digest = digest)
                }

                // 3. AI / Narrative Summary Card
                item {
                    DigestNarrativeCard(digest = digest)
                }

                // 4. Priority Alerts (Critical & High)
                val priorityInsights = digest.insights.filter {
                    it.priority == DigestPriority.CRITICAL || it.priority == DigestPriority.HIGH
                }
                if (priorityInsights.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.digest_section_priority_alerts),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(priorityInsights, key = { it.id }) { insight ->
                        DigestPriorityInsightCard(
                            insight = insight,
                            onActionClick = { route ->
                                onNavigateToRoute(route)
                            }
                        )
                    }
                }

                // 5. Largest Expenses Section
                if (digest.topExpenses.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.digest_section_top_expenses),
                            actionLabel = stringResource(R.string.action_view_all),
                            onActionClick = { onNavigateToRoute("transactions") }
                        )
                    }
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                digest.topExpenses.forEachIndexed { index, expense ->
                                    TopExpenseRow(expense = expense)
                                    if (index < digest.topExpenses.size - 1) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Budget Status & Pacing
                if (digest.budgetItems.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.digest_section_budgets),
                            actionLabel = stringResource(R.string.nav_budgets),
                            onActionClick = { onNavigateToRoute("budgets") }
                        )
                    }
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                digest.budgetItems.forEachIndexed { index, budget ->
                                    BudgetDigestRow(budget = budget)
                                    if (index < digest.budgetItems.size - 1) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. Upcoming Obligations
                if (digest.upcomingBills.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.digest_section_upcoming_bills),
                            actionLabel = stringResource(R.string.cal_tab_agenda),
                            onActionClick = { onNavigateToRoute("calendar") }
                        )
                    }
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                digest.upcomingBills.forEachIndexed { index, bill ->
                                    UpcomingBillRow(bill = bill)
                                    if (index < digest.upcomingBills.size - 1) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }

                // 8. Review Inbox Queue
                if (digest.unreviewedCount > 0) {
                    item {
                        ReviewQueueGlanceCard(
                            count = digest.unreviewedCount,
                            onCleanUpClick = { onNavigateToRoute("review_inbox") }
                        )
                    }
                }

                // 9. Savings Goals Progress
                if (digest.goalItems.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.digest_section_savings_goals),
                            actionLabel = stringResource(R.string.cal_filter_goals),
                            onActionClick = { onNavigateToRoute("goals") }
                        )
                    }
                    items(digest.goalItems, key = { it.id }) { goal ->
                        GoalDigestCard(goal = goal)
                    }
                }

                // Footer spacer
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Settings Bottom Sheet
        if (uiState.isSettingsOpen) {
            DigestSettingsBottomSheet(
                frequency = uiState.frequency,
                privacyEnabled = uiState.privacyEnabled,
                deliveryHour = uiState.deliveryHour,
                onFrequencyChange = { viewModel.setFrequency(it) },
                onPrivacyChange = { viewModel.setPrivacyEnabled(it) },
                onDeliveryHourChange = { viewModel.setDeliveryHour(it) },
                onSendTestNotification = { viewModel.sendTestNotification(context) },
                onDismiss = { viewModel.toggleSettings(false) }
            )
        }
    }
}

@Composable
private fun DigestPeriodSelector(
    selectedPeriod: DigestPeriod,
    onPeriodSelected: (DigestPeriod) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isDaily = selectedPeriod == DigestPeriod.DAILY
            val dailyBg by animateColorAsState(
                targetValue = if (isDaily) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                label = "dailyBg"
            )
            val dailyTextColor by animateColorAsState(
                targetValue = if (isDaily) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "dailyText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(dailyBg)
                    .clickable { onPeriodSelected(DigestPeriod.DAILY) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.digest_period_daily),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isDaily) FontWeight.Bold else FontWeight.Medium,
                    color = dailyTextColor
                )
            }

            val isWeekly = selectedPeriod == DigestPeriod.WEEKLY
            val weeklyBg by animateColorAsState(
                targetValue = if (isWeekly) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                label = "weeklyBg"
            )
            val weeklyTextColor by animateColorAsState(
                targetValue = if (isWeekly) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "weeklyText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(weeklyBg)
                    .clickable { onPeriodSelected(DigestPeriod.WEEKLY) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.digest_period_weekly),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isWeekly) FontWeight.Bold else FontWeight.Medium,
                    color = weeklyTextColor
                )
            }
        }
    }
}

@Composable
private fun DigestPulseHeroCard(digest: FinancialDigest) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                        )
                        Text(
                            text = if (digest.period == DigestPeriod.DAILY) {
                                stringResource(R.string.digest_pulse_badge_daily)
                            } else {
                                stringResource(R.string.digest_pulse_badge_weekly)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Spend Trend Badge
                    if (digest.previousPeriodSpend.amountMinor > 0L) {
                        val isDrop = digest.spendChangePercentage < 0.0
                        val badgeBg = if (isDrop) EmeraldPrimary.copy(alpha = 0.15f) else DebtOrange.copy(alpha = 0.15f)
                        val badgeColor = if (isDrop) EmeraldPrimary else DebtOrange
                        val icon = if (isDrop) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = badgeColor
                                )
                                Text(
                                    text = if (isDrop) {
                                        "${"%.0f".format(-digest.spendChangePercentage)}% " + stringResource(R.string.digest_trend_down)
                                    } else {
                                        "+${"%.0f".format(digest.spendChangePercentage)}% " + stringResource(R.string.digest_trend_up)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }
                        }
                    }
                }

                // Total Spend Figure
                Column {
                    Text(
                        text = stringResource(R.string.digest_total_spent_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = digest.totalSpend.formatted(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DigestStatPill(
                        label = stringResource(R.string.digest_stat_transactions),
                        value = digest.transactionCount.toString()
                    )
                    DigestStatPill(
                        label = stringResource(R.string.digest_stat_bills_due),
                        value = digest.upcomingBills.size.toString()
                    )
                    DigestStatPill(
                        label = stringResource(R.string.digest_stat_review_items),
                        value = digest.unreviewedCount.toString(),
                        highlight = digest.unreviewedCount > 0
                    )
                }
            }
        }
    }
}

@Composable
private fun DigestStatPill(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlight) AmberWarning else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DigestNarrativeCard(digest: FinancialDigest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = SapphireAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = digest.headline,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = digest.narrativeSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
        }
    }
}

@Composable
private fun DigestPriorityInsightCard(
    insight: DigestInsight,
    onActionClick: (String) -> Unit
) {
    val isCritical = insight.priority == DigestPriority.CRITICAL
    val accentColor = if (isCritical) MaterialTheme.colorScheme.error else AmberWarning
    val icon = if (isCritical) Icons.Default.Error else Icons.Default.Warning

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.08f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = insight.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (insight.actionRoute != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { onActionClick(insight.actionRoute) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.digest_action_review),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(onClick = onActionClick) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun TopExpenseRow(expense: TopExpenseItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${expense.categoryName} • ${expense.percentageOfPeriodSpend.toInt()}% of period",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = expense.amount.formatted(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BudgetDigestRow(budget: BudgetDigestItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = budget.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (budget.isExceeded) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)) {
                        Text(
                            text = stringResource(R.string.budget_exceeded),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else if (budget.isWarning) {
                    Surface(shape = RoundedCornerShape(8.dp), color = AmberWarning.copy(alpha = 0.15f)) {
                        Text(
                            text = "${budget.percentageConsumed.toInt()}%",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning
                        )
                    }
                }
            }

            Text(
                text = "${budget.spentAmount.formatted()} / ${budget.limitAmount.formatted()}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val progress = (budget.percentageConsumed / 100.0).toFloat().coerceIn(0f, 1f)
        val progressColor = if (budget.isExceeded) MaterialTheme.colorScheme.error else if (budget.isWarning) AmberWarning else EmeraldPrimary

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun UpcomingBillRow(bill: UpcomingBillDigestItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (bill.isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (bill.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val dueText = when {
                    bill.isOverdue -> stringResource(R.string.calendar_status_overdue)
                    bill.daysUntilDue == 0L -> stringResource(R.string.cal_reminder_due_day)
                    bill.daysUntilDue == 1L -> stringResource(R.string.cal_reminder_1d_before)
                    else -> "${bill.daysUntilDue} days"
                }
                Text(
                    text = dueText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bill.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = bill.amount.formatted(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReviewQueueGlanceCard(
    count: Int,
    onCleanUpClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AmberWarning.copy(alpha = 0.08f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AmberWarning.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.dashboard_review_queue_clean_up),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (count == 1) {
                            stringResource(R.string.dashboard_review_queue_pending_one)
                        } else {
                            stringResource(R.string.dashboard_review_queue_pending_many, count)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onCleanUpClick,
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.review_inbox_open),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun GoalDigestCard(goal: GoalProgressDigestItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = TealSavings,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${goal.progressPercentage.toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TealSavings
                )
            }

            LinearProgressIndicator(
                progress = { (goal.progressPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = TealSavings,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = goal.currentAmount.formatted(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = goal.targetAmount.formatted(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DigestSettingsBottomSheet(
    frequency: DigestFrequency,
    privacyEnabled: Boolean,
    deliveryHour: Int,
    onFrequencyChange: (DigestFrequency) -> Unit,
    onPrivacyChange: (Boolean) -> Unit,
    onDeliveryHourChange: (Int) -> Unit,
    onSendTestNotification: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.digest_settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_done), fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider()

            // Frequency Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.digest_settings_frequency_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        DigestFrequency.DAILY to R.string.digest_period_daily,
                        DigestFrequency.WEEKLY to R.string.digest_period_weekly,
                        DigestFrequency.OFF to R.string.digest_frequency_off
                    ).forEach { (freq, labelRes) ->
                        val isSelected = frequency == freq
                        OutlinedButton(
                            onClick = { onFrequencyChange(freq) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                            )
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Privacy Mode Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.digest_settings_privacy_mode),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.digest_settings_privacy_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = privacyEnabled,
                    onCheckedChange = onPrivacyChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldPrimary,
                        checkedTrackColor = EmeraldPrimary.copy(alpha = 0.3f)
                    )
                )
            }

            // Delivery Time Picker (Hour)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.digest_settings_delivery_time),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        9 to "09:00",
                        13 to "13:00",
                        20 to "20:00",
                        22 to "22:00"
                    ).forEach { (hour, label) ->
                        val isSelected = deliveryHour == hour
                        OutlinedButton(
                            onClick = { onDeliveryHourChange(hour) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                            )
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Test Notification Button
            Button(
                onClick = onSendTestNotification,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.digest_action_test_notification),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

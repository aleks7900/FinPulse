package com.finpulse.app.presentation.recurring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finpulse.app.core.designsystem.AmberWarning
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.PurpleAccent
import com.finpulse.app.core.designsystem.TransferBlue
import com.finpulse.app.core.model.Money
import com.finpulse.app.core.ui.CategoryIconBadge
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CustomIntervalUnit
import com.finpulse.app.domain.model.OccurrenceStatus
import com.finpulse.app.domain.model.PaymentFrequency
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.finpulse.app.R
import com.finpulse.app.core.ui.DateFormatterUtils
import com.finpulse.app.core.ui.getDisplayName
import com.finpulse.app.core.ui.getLocalizedName
import com.finpulse.app.domain.model.RecurringOccurrence
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    uiState: RecurringUiState,
    onNavigateBack: () -> Unit,
    onTabSelected: (RecurringTab) -> Unit,
    onWindowDaysChanged: (Int) -> Unit,
    onShowAddEditDialog: (Boolean, RecurringTransaction?) -> Unit,
    onShowEditOccurrenceDialog: (Boolean, RecurringOccurrence?) -> Unit,
    onShowConfirmPayDialog: (Boolean, RecurringOccurrence?) -> Unit,
    onSaveRecurring: (
        id: String?,
        title: String,
        amountMinor: Long,
        type: TransactionType,
        accountId: String,
        destinationAccountId: String?,
        categoryId: String,
        frequency: PaymentFrequency,
        customIntervalValue: Int,
        customIntervalUnit: CustomIntervalUnit,
        nextDueDate: Long,
        isSubscription: Boolean,
        isVariableAmount: Boolean,
        reminderDaysBefore: Int,
        notes: String?
    ) -> Unit,
    onMarkPaid: (occurrence: RecurringOccurrence, actualAmountMinor: Long?, paidDate: Long, accountId: String?) -> Unit,
    onSkipOccurrence: (RecurringOccurrence) -> Unit,
    onEditOccurrence: (occurrence: RecurringOccurrence, newAmountMinor: Long, newDueDate: Long, notes: String?) -> Unit,
    onToggleActive: (RecurringTransaction) -> Unit,
    onCancelRule: (String) -> Unit,
    onDeleteRecurring: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryMap = uiState.categories.associateBy { it.id }
    val accountMap = uiState.accounts.associateBy { it.id }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.recurring_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onShowAddEditDialog(true, null) },
                containerColor = EmeraldPrimary,
                contentColor = Color.Black
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.recurring_add_title))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row: [Upcoming] [All Rules] [Subscriptions]
            SecondaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = EmeraldPrimary
            ) {
                RecurringTab.values().forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                text = tab.getLocalizedName(),
                                fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.selectedTab == tab) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (uiState.selectedTab) {
                // 1. UPCOMING TAB
                RecurringTab.UPCOMING -> {
                    UpcomingSection(
                        occurrences = uiState.upcomingOccurrences,
                        windowDays = uiState.upcomingWindowDays,
                        categoryMap = categoryMap,
                        accountMap = accountMap,
                        hideBalances = uiState.hideBalances,
                        onWindowDaysChanged = onWindowDaysChanged,
                        onPayClick = { onShowConfirmPayDialog(true, it) },
                        onSkipClick = onSkipOccurrence,
                        onEditClick = { onShowEditOccurrenceDialog(true, it) }
                    )
                }

                // 2. ALL RULES TAB
                RecurringTab.RULES -> {
                    AllRulesSection(
                        rules = uiState.recurringList,
                        totalMonthlyCost = uiState.totalMonthlyCost,
                        totalAnnualCost = uiState.totalAnnualCost,
                        categoryMap = categoryMap,
                        accountMap = accountMap,
                        hideBalances = uiState.hideBalances,
                        onRuleClick = { onShowAddEditDialog(true, it) },
                        onToggle = onToggleActive
                    )
                }

                // 3. SUBSCRIPTIONS TAB
                RecurringTab.SUBSCRIPTIONS -> {
                    SubscriptionsSection(
                        subscriptions = uiState.subscriptionsList,
                        monthlySub = uiState.totalMonthlySubscriptions,
                        annualSub = uiState.totalAnnualSubscriptions,
                        activeCount = uiState.activeSubscriptionsCount,
                        categoryMap = categoryMap,
                        accountMap = accountMap,
                        hideBalances = uiState.hideBalances,
                        onRuleClick = { onShowAddEditDialog(true, it) },
                        onToggle = onToggleActive
                    )
                }
            }
        }

        // Add / Edit Rule Dialog
        if (uiState.isAddEditDialogVisible) {
            AddEditRecurringRuleDialog(
                editingRecurring = uiState.editingRecurring,
                accounts = uiState.accounts,
                categories = uiState.categories,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { onShowAddEditDialog(false, null) },
                onSave = onSaveRecurring,
                onCancelRule = { uiState.editingRecurring?.let { onCancelRule(it.id) } },
                onDelete = { uiState.editingRecurring?.let { onDeleteRecurring(it.id) } }
            )
        }

        // Edit Individual Occurrence Dialog
        if (uiState.isEditOccurrenceDialogVisible && uiState.editingOccurrence != null) {
            EditOccurrenceDialog(
                occurrence = uiState.editingOccurrence,
                onDismiss = { onShowEditOccurrenceDialog(false, null) },
                onSave = onEditOccurrence
            )
        }

        // Confirm Payment Dialog
        if (uiState.isConfirmPayDialogVisible && uiState.payingOccurrence != null) {
            ConfirmPaymentDialog(
                occurrence = uiState.payingOccurrence,
                accounts = uiState.accounts,
                onDismiss = { onShowConfirmPayDialog(false, null) },
                onConfirm = onMarkPaid
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 1. UPCOMING SECTION
// ---------------------------------------------------------------------------
@Composable
fun UpcomingSection(
    occurrences: List<RecurringOccurrence>,
    windowDays: Int,
    categoryMap: Map<String, Category>,
    accountMap: Map<String, Account>,
    hideBalances: Boolean,
    onWindowDaysChanged: (Int) -> Unit,
    onPayClick: (RecurringOccurrence) -> Unit,
    onSkipClick: (RecurringOccurrence) -> Unit,
    onEditClick: (RecurringOccurrence) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Window Toggle: [Next 7 Days] [Next 30 Days]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(7, 30).forEach { days ->
                val isSelected = windowDays == days
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onWindowDaysChanged(days) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = if (days == 7) stringResource(R.string.recurring_window_7d) else stringResource(R.string.recurring_window_30d),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (occurrences.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.recurring_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.recurring_empty_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(occurrences, key = { it.id }) { occ ->
                    val cat = categoryMap[occ.categoryId]
                    val acc = accountMap[occ.accountId]
                    val destAcc = occ.destinationAccountId?.let { accountMap[it] }

                    UpcomingOccurrenceCard(
                        occurrence = occ,
                        category = cat,
                        account = acc,
                        destinationAccount = destAcc,
                        hideBalances = hideBalances,
                        onPayClick = { onPayClick(occ) },
                        onSkipClick = { onSkipClick(occ) },
                        onEditClick = { onEditClick(occ) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun UpcomingOccurrenceCard(
    occurrence: RecurringOccurrence,
    category: Category?,
    account: Account?,
    destinationAccount: Account?,
    hideBalances: Boolean,
    onPayClick: () -> Unit,
    onSkipClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = ZoneId.systemDefault()
    val dueLocalDate = Instant.ofEpochMilli(occurrence.dueDate).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    val daysDiff = ChronoUnit.DAYS.between(today, dueLocalDate)

    val formattedDate = DateFormatterUtils.formatDate(occurrence.dueDate)
    val dueText = when {
        daysDiff < 0 -> "${occurrence.status.getLocalizedName()} ($formattedDate)"
        daysDiff == 0L -> "${stringResource(R.string.date_today)} • $formattedDate"
        else -> "${occurrence.status.getLocalizedName()} • $formattedDate"
    }

    val statusColor = when (occurrence.status) {
        OccurrenceStatus.OVERDUE -> CrimsonExpense
        OccurrenceStatus.PAID -> EmeraldPrimary
        OccurrenceStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
        OccurrenceStatus.GENERATED -> AmberWarning
        OccurrenceStatus.EXPECTED -> if (daysDiff <= 3) AmberWarning else EmeraldPrimary
    }

    val borderStroke = if (occurrence.status == OccurrenceStatus.OVERDUE) {
        BorderStroke(1.5.dp, CrimsonExpense.copy(alpha = 0.8f))
    } else null

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = borderStroke,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Category icon, Title, Due text, Status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconBadge(
                    iconName = category?.icon ?: "receipt",
                    colorHex = category?.colorHex ?: 0xFF9C27B0,
                    size = 38.dp,
                    iconSize = 20.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = occurrence.ruleTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (occurrence.isVariableAmount) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Est.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val accDisplay = if (occurrence.type == TransactionType.TRANSFER && destinationAccount != null) {
                        "${account?.name ?: "Account"} → ${destinationAccount.name}"
                    } else {
                        account?.name ?: "Account"
                    }
                    Text(
                        text = "$dueText • $accDisplay",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (occurrence.status == OccurrenceStatus.OVERDUE) CrimsonExpense else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Amount & Status Badge
                Column(horizontalAlignment = Alignment.End) {
                    val amountColor = when (occurrence.type) {
                        TransactionType.INCOME -> EmeraldPrimary
                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
                        TransactionType.TRANSFER -> TransferBlue
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    val prefix = when (occurrence.type) {
                        TransactionType.INCOME -> "+"
                        TransactionType.EXPENSE -> "-"
                        TransactionType.TRANSFER -> "⇄ "
                        else -> ""
                    }

                    Text(
                        text = if (hideBalances) "••••••" else "$prefix${occurrence.amount.formatted()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = occurrence.status.getLocalizedName(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Bottom Action Row (Pay / Skip / Edit) if not settled
            if (occurrence.status == OccurrenceStatus.EXPECTED || occurrence.status == OccurrenceStatus.OVERDUE || occurrence.status == OccurrenceStatus.GENERATED) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onEditClick) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_edit), style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    TextButton(onClick = onSkipClick) {
                        Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_skip), style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = onPayClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.recurring_mark_paid), color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. ALL RULES SECTION
// ---------------------------------------------------------------------------
@Composable
fun AllRulesSection(
    rules: List<RecurringTransaction>,
    totalMonthlyCost: Money,
    totalAnnualCost: Money,
    categoryMap: Map<String, Category>,
    accountMap: Map<String, Account>,
    hideBalances: Boolean,
    onRuleClick: (RecurringTransaction) -> Unit,
    onToggle: (RecurringTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "TOTAL RECURRING COMMITMENTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = if (hideBalances) "••••••" else totalMonthlyCost.formatted(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("Monthly commitment", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (hideBalances) "••••••" else totalAnnualCost.formatted(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent
                            )
                            Text("Annualized", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (rules.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recurring rules configured yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(rules, key = { it.id }) { rule ->
                val cat = categoryMap[rule.categoryId]
                val acc = accountMap[rule.accountId]
                RuleItemCard(
                    rule = rule,
                    category = cat,
                    account = acc,
                    hideBalances = hideBalances,
                    onClick = { onRuleClick(rule) },
                    onToggle = { onToggle(rule) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun RuleItemCard(
    rule: RecurringTransaction,
    category: Category?,
    account: Account?,
    hideBalances: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nextDateFormatted = DateFormatterUtils.formatDate(rule.nextDueDate)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                iconName = category?.icon ?: "receipt",
                colorHex = category?.colorHex ?: 0xFF9C27B0,
                size = 38.dp,
                iconSize = 20.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rule.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!rule.isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(6.dp), color = AmberWarning.copy(alpha = 0.2f)) {
                            Text(
                                text = stringResource(R.string.recurring_pause),
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberWarning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${rule.frequency.getLocalizedName()} • $nextDateFormatted • ${account?.name ?: "Account"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (hideBalances) "••••••" else rule.amount.formatted(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Switch(
                    checked = rule.isActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. SUBSCRIPTIONS SECTION
// ---------------------------------------------------------------------------
@Composable
fun SubscriptionsSection(
    subscriptions: List<RecurringTransaction>,
    monthlySub: Money,
    annualSub: Money,
    activeCount: Int,
    categoryMap: Map<String, Category>,
    accountMap: Map<String, Account>,
    hideBalances: Boolean,
    onRuleClick: (RecurringTransaction) -> Unit,
    onToggle: (RecurringTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE SUBSCRIPTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(shape = CircleShape, color = PurpleAccent.copy(alpha = 0.2f)) {
                            Text(
                                text = "$activeCount active",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = PurpleAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = if (hideBalances) "••••••" else monthlySub.formatted(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("Monthly cost", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (hideBalances) "••••••" else annualSub.formatted(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent
                            )
                            Text("Annualized spend", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (subscriptions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active subscriptions tracked.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(subscriptions, key = { it.id }) { rule ->
                val cat = categoryMap[rule.categoryId]
                val acc = accountMap[rule.accountId]
                RuleItemCard(
                    rule = rule,
                    category = cat,
                    account = acc,
                    hideBalances = hideBalances,
                    onClick = { onRuleClick(rule) },
                    onToggle = { onToggle(rule) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// 4. ADD / EDIT RECURRING RULE DIALOG
// ---------------------------------------------------------------------------
@Composable
fun AddEditRecurringRuleDialog(
    editingRecurring: RecurringTransaction?,
    accounts: List<Account>,
    categories: List<Category>,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        title: String,
        amountMinor: Long,
        type: TransactionType,
        accountId: String,
        destinationAccountId: String?,
        categoryId: String,
        frequency: PaymentFrequency,
        customIntervalValue: Int,
        customIntervalUnit: CustomIntervalUnit,
        nextDueDate: Long,
        isSubscription: Boolean,
        isVariableAmount: Boolean,
        reminderDaysBefore: Int,
        notes: String?
    ) -> Unit,
    onCancelRule: () -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(editingRecurring?.title ?: "") }
    var amountText by remember { mutableStateOf(editingRecurring?.amount?.amountBigDecimal?.toPlainString() ?: "") }
    var type by remember { mutableStateOf(editingRecurring?.type ?: TransactionType.EXPENSE) }
    var accountId by remember { mutableStateOf(editingRecurring?.accountId ?: accounts.firstOrNull()?.id ?: "") }
    var destinationAccountId by remember { mutableStateOf(editingRecurring?.destinationAccountId ?: accounts.getOrNull(1)?.id) }
    var categoryId by remember { mutableStateOf(editingRecurring?.categoryId ?: categories.firstOrNull()?.id ?: "") }
    var frequency by remember { mutableStateOf(editingRecurring?.frequency ?: PaymentFrequency.MONTHLY) }
    var isSubscription by remember { mutableStateOf(editingRecurring?.isSubscription ?: false) }
    var isVariableAmount by remember { mutableStateOf(editingRecurring?.isVariableAmount ?: false) }
    var reminderDays by remember { mutableStateOf(editingRecurring?.reminderDaysBefore ?: 1) }
    var notes by remember { mutableStateOf(editingRecurring?.notes ?: "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingRecurring != null) stringResource(R.string.action_edit) else stringResource(R.string.recurring_add_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type Switcher: [Expense] [Income] [Transfer]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER).forEach { t ->
                        val isSel = type == t
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { type = t },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface
                        ) {
                            Text(
                                text = t.getLocalizedName(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; isError = false },
                    label = { Text(stringResource(R.string.recurring_bill_name_hint)) },
                    singleLine = true,
                    isError = isError && title.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; isError = false },
                    label = { Text("${stringResource(R.string.tx_amount)} ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = isError && (amountText.toDoubleOrNull() == null || amountText.toDouble() <= 0.0),
                    modifier = Modifier.fillMaxWidth()
                )

                // Variable Amount Checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isVariableAmount,
                        onCheckedChange = { isVariableAmount = it }
                    )
                    Text(stringResource(R.string.recurring_variable_amount), style = MaterialTheme.typography.bodySmall)
                }

                // Frequency Switcher
                Text(stringResource(R.string.recurring_frequency), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val freqScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(freqScroll),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentFrequency.values().forEach { f ->
                        val isSel = frequency == f
                        Surface(
                            modifier = Modifier.clickable { frequency = f },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface
                        ) {
                            Text(
                                text = f.getLocalizedName(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Mark as subscription
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isSubscription,
                        onCheckedChange = { isSubscription = it }
                    )
                    Text(stringResource(R.string.recurring_is_subscription), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountText.toDoubleOrNull()
                    if (title.isBlank() || amountVal == null || amountVal <= 0.0) {
                        isError = true
                        return@Button
                    }
                    val amountMinor = (amountVal * 100).toLong()
                    val nextDue = editingRecurring?.nextDueDate ?: System.currentTimeMillis()

                    onSave(
                        editingRecurring?.id,
                        title,
                        amountMinor,
                        type,
                        accountId,
                        if (type == TransactionType.TRANSFER) destinationAccountId else null,
                        categoryId,
                        frequency,
                        1,
                        CustomIntervalUnit.MONTHS,
                        nextDue,
                        isSubscription,
                        isVariableAmount,
                        reminderDays,
                        notes.takeIf { it.isNotBlank() }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.action_save), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (editingRecurring != null) {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = CrimsonExpense)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    )
}

// ---------------------------------------------------------------------------
// 5. CONFIRM PAYMENT DIALOG
// ---------------------------------------------------------------------------
@Composable
fun ConfirmPaymentDialog(
    occurrence: RecurringOccurrence,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (occurrence: RecurringOccurrence, actualAmountMinor: Long?, paidDate: Long, accountId: String?) -> Unit
) {
    var amountInput by remember { mutableStateOf(occurrence.amount.amountBigDecimal.toPlainString()) }
    var selectedAccountId by remember { mutableStateOf(occurrence.accountId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Confirm Payment: ${occurrence.ruleTitle}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Confirm payment and record transaction in ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Actual Paid Amount (${occurrence.amount.currencyCode})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountInput.toDoubleOrNull()
                    val actualMinor = amountVal?.let { (it * 100).toLong() }
                    onConfirm(occurrence, actualMinor, System.currentTimeMillis(), selectedAccountId)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.debt_record_payment), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

// ---------------------------------------------------------------------------
// 6. EDIT OCCURRENCE DIALOG
// ---------------------------------------------------------------------------
@Composable
fun EditOccurrenceDialog(
    occurrence: RecurringOccurrence,
    onDismiss: () -> Unit,
    onSave: (occurrence: RecurringOccurrence, newAmountMinor: Long, newDueDate: Long, notes: String?) -> Unit
) {
    var amountInput by remember { mutableStateOf(occurrence.amount.amountBigDecimal.toPlainString()) }
    var notes by remember { mutableStateOf(occurrence.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Edit Occurrence: ${occurrence.ruleTitle}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Changes apply only to this upcoming cycle without modifying the recurring rule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Expected Amount (${occurrence.amount.currencyCode})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note for this occurrence") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountInput.toDoubleOrNull() ?: occurrence.amount.amountBigDecimal.toDouble()
                    val minor = (amountVal * 100).toLong()
                    onSave(occurrence, minor, occurrence.dueDate, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.quick_add_update), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

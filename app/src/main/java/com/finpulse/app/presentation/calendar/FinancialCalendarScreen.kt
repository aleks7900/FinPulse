package com.finpulse.app.presentation.calendar

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finpulse.app.R
import com.finpulse.app.core.designsystem.AmberWarning
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.DebtOrange
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.ExpenseRed
import com.finpulse.app.core.designsystem.IncomeGreen
import com.finpulse.app.core.designsystem.InvestmentPurple
import com.finpulse.app.core.designsystem.PurpleAccent
import com.finpulse.app.core.designsystem.SapphireAccent
import com.finpulse.app.core.designsystem.TealSavings
import com.finpulse.app.core.designsystem.TransferBlue
import com.finpulse.app.core.model.Money
import com.finpulse.app.core.ui.CategoryIconBadge
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.CalendarEvent
import com.finpulse.app.domain.model.CalendarEventStatus
import com.finpulse.app.domain.model.CalendarEventType
import com.finpulse.app.domain.model.CashFlowPeriodSummary
import com.finpulse.app.domain.model.RecurringOccurrence
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialCalendarScreen(
    uiState: CalendarUiState,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onPeriodPresetChange: (PeriodPreset) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onFilterChange: (CalendarFilterType) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onDismissEventDetails: () -> Unit,
    onShowMarkPaid: (CalendarEvent) -> Unit,
    onDismissMarkPaid: () -> Unit,
    onConfirmMarkPaid: (CalendarEvent, Long?, Long, String?) -> Unit,
    onSkipEvent: (CalendarEvent) -> Unit,
    onShowEditOccurrence: (CalendarEvent) -> Unit,
    onDismissEditOccurrence: () -> Unit,
    onSaveEditedOccurrence: (Long, Long, String?) -> Unit,
    onSetReminder: (CalendarEvent, Int, Boolean) -> Unit,
    onClearReminderMessage: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenRecurringScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.reminderMessage) {
        uiState.reminderMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearReminderMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FinancialCalendarTopBar(
                viewMode = uiState.viewMode,
                currentMonth = uiState.currentMonth,
                onViewModeChange = onViewModeChange,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onTodayClick = onTodayClick,
                onNavigateBack = onNavigateBack
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
            // Period Presets & Type Filters
            item {
                PeriodAndFilterSection(
                    selectedPreset = uiState.periodPreset,
                    selectedFilter = uiState.selectedTypeFilter,
                    onPresetSelected = onPeriodPresetChange,
                    onFilterSelected = onFilterChange
                )
            }

            // Summary Metrics & Deterministic Cash Flow Banner
            uiState.summary?.let { summary ->
                item {
                    CashFlowSummaryCard(
                        summary = summary,
                        hideBalances = uiState.hideBalances
                    )
                }

                // Balance Timeline Chart
                item {
                    ProjectedBalanceCard(summary = summary)
                }
            }

            // Main Content: Month View vs Agenda View
            if (uiState.viewMode == CalendarViewMode.MONTH) {
                item {
                    CalendarMonthGrid(
                        yearMonth = uiState.currentMonth,
                        selectedDate = uiState.selectedDate,
                        events = uiState.summary?.allEvents ?: emptyList(),
                        onDateSelected = onDateSelect
                    )
                }

                // Selected Day Events Section
                item {
                    SelectedDayEventsSection(
                        selectedDate = uiState.selectedDate,
                        events = uiState.selectedDateEvents,
                        onEventClick = onEventClick
                    )
                }
            } else {
                // Agenda View
                item {
                    Text(
                        text = "UPCOMING CASH FLOW AGENDA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val events = uiState.filteredEvents
                if (events.isEmpty()) {
                    item {
                        EmptyEventsState(message = "No obligations found for the selected filter.")
                    }
                } else {
                    val grouped = events.groupBy {
                        Instant.ofEpochMilli(it.dateMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }

                    grouped.forEach { (date, dateEvents) ->
                        item(key = "header_$date") {
                            AgendaDateHeader(date = date)
                        }

                        items(dateEvents, key = { it.id }) { event ->
                            CalendarEventItem(
                                event = event,
                                onClick = { onEventClick(event) }
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

    // Event Detail Bottom Sheet
    if (uiState.isEventDetailSheetVisible && uiState.selectedEventForDetails != null) {
        EventDetailBottomSheet(
            event = uiState.selectedEventForDetails,
            onDismiss = onDismissEventDetails,
            onMarkPaid = { onShowMarkPaid(uiState.selectedEventForDetails) },
            onSkip = { onSkipEvent(uiState.selectedEventForDetails) },
            onEditOccurrence = { onShowEditOccurrence(uiState.selectedEventForDetails) },
            onOpenRule = onOpenRecurringScreen,
            onSetReminder = { days, instant ->
                onSetReminder(uiState.selectedEventForDetails, days, instant)
            }
        )
    }

    // Mark Paid Dialog
    if (uiState.isConfirmPayDialogVisible && uiState.payingEvent != null) {
        ConfirmPayDialog(
            event = uiState.payingEvent,
            accounts = uiState.accounts,
            onConfirm = { actualAmount, paidDate, accountId ->
                onConfirmMarkPaid(uiState.payingEvent, actualAmount, paidDate, accountId)
            },
            onDismiss = onDismissMarkPaid
        )
    }

    // Edit Occurrence Dialog
    if (uiState.isEditOccurrenceDialogVisible && uiState.editingOccurrence != null) {
        EditOccurrenceDialog(
            occurrence = uiState.editingOccurrence,
            onSave = onSaveEditedOccurrence,
            onDismiss = onDismissEditOccurrence
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinancialCalendarTopBar(
    viewMode: CalendarViewMode,
    currentMonth: YearMonth,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Financial Calendar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (viewMode == CalendarViewMode.MONTH) {
                    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
                    Text(
                        text = currentMonth.format(monthFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            // View Mode Toggle (Month / Agenda)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    ViewModeChip(
                        selected = viewMode == CalendarViewMode.MONTH,
                        label = "Month",
                        icon = Icons.Default.CalendarMonth,
                        onClick = { onViewModeChange(CalendarViewMode.MONTH) }
                    )
                    ViewModeChip(
                        selected = viewMode == CalendarViewMode.AGENDA,
                        label = "Agenda",
                        icon = Icons.Default.DateRange,
                        onClick = { onViewModeChange(CalendarViewMode.AGENDA) }
                    )
                }
            }

            if (viewMode == CalendarViewMode.MONTH) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                }
                IconButton(onClick = onTodayClick) {
                    Icon(imageVector = Icons.Default.Today, contentDescription = "Today", tint = EmeraldPrimary)
                }
                IconButton(onClick = onNextMonth) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun ViewModeChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) EmeraldPrimary else Color.Transparent,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodAndFilterSection(
    selectedPreset: PeriodPreset,
    selectedFilter: CalendarFilterType,
    onPresetSelected: (PeriodPreset) -> Unit,
    onFilterSelected: (CalendarFilterType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Quick Window Presets (7d, 30d, Month, 90d)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeriodPreset.entries.forEach { preset ->
                val isSelected = preset == selectedPreset
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable { onPresetSelected(preset) }
                ) {
                    Text(
                        text = preset.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Event Type Filters (All, Income, Bills, Loans, Transfers, Goals)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalendarFilterType.entries.forEach { filter ->
                val isSelected = filter == selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(text = filter.displayName, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                        selectedLabelColor = EmeraldPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun CashFlowSummaryCard(
    summary: CashFlowPeriodSummary,
    hideBalances: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Deterministic Indicator Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SapphireAccent.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SapphireAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Deterministic Projection: strictly based on scheduled events.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SapphireAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Numbers: Inflow vs Outflow vs Net
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Expected Inflow
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(IncomeGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Expected Inflow",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hideBalances) "••••" else summary.totalInflow.formatted(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                    Text(
                        text = "${summary.incomeCount} incoming events",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Expected Outflow
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Expected Outflow",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hideBalances) "••••" else summary.totalOutflow.formatted(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                    val totalObligations = summary.billCount + summary.subscriptionCount + summary.loanPaymentCount + summary.goalContributionCount
                    Text(
                        text = "$totalObligations scheduled bills/dues",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Net Cash Flow Pill & Status Summary
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (summary.netCashFlow.amountMinor >= 0L) {
                    EmeraldPrimary.copy(alpha = 0.12f)
                } else {
                    CrimsonExpense.copy(alpha = 0.12f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (summary.netCashFlow.amountMinor >= 0L) {
                                Icons.AutoMirrored.Filled.TrendingUp
                            } else {
                                Icons.AutoMirrored.Filled.TrendingDown
                            },
                            contentDescription = null,
                            tint = if (summary.netCashFlow.amountMinor >= 0L) EmeraldPrimary else CrimsonExpense,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Net Cash Flow",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = if (hideBalances) "••••" else {
                            val prefix = if (summary.netCashFlow.amountMinor > 0L) "+" else ""
                            "$prefix${summary.netCashFlow.formatted()}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.netCashFlow.amountMinor >= 0L) EmeraldPrimary else CrimsonExpense
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectedBalanceCard(summary: CashFlowPeriodSummary) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROJECTED BALANCE TIMELINE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (summary.lowestProjectedBalance.amountMinor < 0L) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CrimsonExpense.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = CrimsonExpense,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Deficit Risk",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonExpense
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ProjectedBalanceTimelineChart(
                dailyProjections = summary.dailyProjections,
                startingBalance = summary.startingBalance,
                projectedEndingBalance = summary.projectedEndingBalance,
                lowestBalance = summary.lowestProjectedBalance
            )
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Day of Week Headers
            val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                daysOfWeek.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Days Matrix
            val firstDayOfMonth = yearMonth.atDay(1)
            val firstDayOfWeekIndex = (firstDayOfMonth.dayOfWeek.value - 1) // 0 for Monday, 6 for Sunday
            val daysInMonth = yearMonth.lengthOfMonth()
            val totalCells = ((firstDayOfWeekIndex + daysInMonth + 6) / 7) * 7

            val eventsByDate = remember(events) {
                events.groupBy {
                    Instant.ofEpochMilli(it.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                }
            }

            val today = LocalDate.now()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (weekIndex in 0 until (totalCells / 7)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (dayIndex in 0 until 7) {
                            val cellIndex = weekIndex * 7 + dayIndex
                            val dayOfMonth = cellIndex - firstDayOfWeekIndex + 1

                            if (dayOfMonth in 1..daysInMonth) {
                                val date = yearMonth.atDay(dayOfMonth)
                                val isSelected = date == selectedDate
                                val isToday = date == today
                                val dayEvents = eventsByDate[date] ?: emptyList()

                                CalendarDayCell(
                                    day = dayOfMonth,
                                    isSelected = isSelected,
                                    isToday = isToday,
                                    events = dayEvents,
                                    onClick = { onDateSelected(date) }
                                )
                            } else {
                                // Empty spacer for leading/trailing padding days
                                Spacer(modifier = Modifier.size(40.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    events: List<CalendarEvent>,
    onClick: () -> Unit
) {
    val hasIncome = events.any { it.isInflow }
    val hasBills = events.any { it.type == CalendarEventType.BILL || it.type == CalendarEventType.SUBSCRIPTION }
    val hasLoans = events.any { it.type == CalendarEventType.LOAN_PAYMENT }
    val hasGoals = events.any { it.type == CalendarEventType.GOAL_CONTRIBUTION }
    val hasOverdue = events.any { it.status == CalendarEventStatus.OVERDUE }
    val allCompleted = events.isNotEmpty() && events.all { it.isCompleted }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            isSelected -> EmeraldPrimary
            isToday -> EmeraldPrimary.copy(alpha = 0.15f)
            else -> Color.Transparent
        },
        border = if (isToday && !isSelected) BorderStroke(1.5.dp, EmeraldPrimary) else null,
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.Black
                    isToday -> EmeraldPrimary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // Event Dots
            if (events.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (allCompleted) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.Black else IncomeGreen)
                        )
                    } else {
                        if (hasIncome) {
                            Box(
                                modifier = Modifier
                                    .size(3.5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.Black else IncomeGreen)
                            )
                            Spacer(modifier = Modifier.width(1.5.dp))
                        }
                        if (hasBills) {
                            Box(
                                modifier = Modifier
                                    .size(3.5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.Black else ExpenseRed)
                            )
                            Spacer(modifier = Modifier.width(1.5.dp))
                        }
                        if (hasLoans) {
                            Box(
                                modifier = Modifier
                                    .size(3.5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.Black else DebtOrange)
                            )
                        }
                        if (hasGoals) {
                            Box(
                                modifier = Modifier
                                    .size(3.5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.Black else TealSavings)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayEventsSection(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()) }
    val isToday = selectedDate == LocalDate.now()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedDate.format(formatter)}${if (isToday) " (Today)" else ""}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${events.size} items",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (events.isEmpty()) {
            EmptyEventsState(message = "No obligations or scheduled events on this day.")
        } else {
            events.forEach { event ->
                CalendarEventItem(
                    event = event,
                    onClick = { onEventClick(event) }
                )
            }
        }
    }
}

@Composable
private fun AgendaDateHeader(date: LocalDate) {
    val today = LocalDate.now()
    val daysUntil = ChronoUnit.DAYS.between(today, date)

    val relativeText = when {
        daysUntil == 0L -> "Today"
        daysUntil == 1L -> "Tomorrow"
        daysUntil == -1L -> "Yesterday"
        daysUntil > 1L -> "In $daysUntil days"
        else -> "Past due (${-daysUntil}d ago)"
    }

    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date.format(formatter),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when {
                daysUntil == 0L -> EmeraldPrimary.copy(alpha = 0.15f)
                daysUntil < 0L -> CrimsonExpense.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Text(
                text = relativeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    daysUntil == 0L -> EmeraldPrimary
                    daysUntil < 0L -> CrimsonExpense
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun CalendarEventItem(
    event: CalendarEvent,
    onClick: () -> Unit
) {
    val isCompleted = event.isCompleted
    val isOverdue = event.status == CalendarEventStatus.OVERDUE
    val isSkipped = event.status == CalendarEventStatus.SKIPPED

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = when {
            isOverdue -> BorderStroke(1.dp, AmberWarning)
            isCompleted -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            else -> null
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            EventIconBadge(event = event)

            Spacer(modifier = Modifier.width(12.dp))

            // Title, Category, Account
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isSkipped) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (event.hasReminder) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Reminder active",
                            tint = AmberWarning,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    event.sourceAccountName?.let { accName ->
                        Text(
                            text = " • $accName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Amount & Status Badge
            Column(horizontalAlignment = Alignment.End) {
                val amountText = if (event.isInflow) {
                    "+${event.amount.formatted()}"
                } else {
                    "-${event.amount.formatted()}"
                }

                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        event.isInflow -> IncomeGreen
                        else -> ExpenseRed
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Status Pill
                StatusPill(status = event.status)
            }
        }
    }
}

@Composable
private fun EventIconBadge(event: CalendarEvent) {
    val (bgColor, iconVector) = when (event.type) {
        CalendarEventType.INCOME -> IncomeGreen to Icons.AutoMirrored.Filled.TrendingUp
        CalendarEventType.BILL -> ExpenseRed to Icons.Default.CreditCard
        CalendarEventType.SUBSCRIPTION -> PurpleAccent to Icons.Default.Schedule
        CalendarEventType.LOAN_PAYMENT -> DebtOrange to Icons.Default.AccountBalance
        CalendarEventType.RECURRING_TRANSFER -> TransferBlue to Icons.Default.SwapHoriz
        CalendarEventType.GOAL_CONTRIBUTION -> TealSavings to Icons.Default.Savings
        CalendarEventType.TRANSACTION -> SapphireAccent to Icons.Default.Category
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor.copy(alpha = 0.15f),
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = bgColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StatusPill(status: CalendarEventStatus) {
    val (pillColor, textColor, text) = when (status) {
        CalendarEventStatus.COMPLETED -> Triple(
            EmeraldPrimary.copy(alpha = 0.18f),
            EmeraldPrimary,
            "Paid ✓"
        )
        CalendarEventStatus.EXPECTED -> Triple(
            SapphireAccent.copy(alpha = 0.14f),
            SapphireAccent,
            "Upcoming"
        )
        CalendarEventStatus.OVERDUE -> Triple(
            CrimsonExpense.copy(alpha = 0.18f),
            CrimsonExpense,
            "Overdue !"
        )
        CalendarEventStatus.SKIPPED -> Triple(
            Color.Gray.copy(alpha = 0.15f),
            Color.Gray,
            "Skipped"
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = pillColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyEventsState(message: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------------------
// Bottom Sheet & Dialogs
// -------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailBottomSheet(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onMarkPaid: () -> Unit,
    onSkip: () -> Unit,
    onEditOccurrence: () -> Unit,
    onOpenRule: () -> Unit,
    onSetReminder: (daysBefore: Int, instantNotification: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault()) }
    val dateStr = Instant.ofEpochMilli(event.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

    var reminderDays by remember { mutableIntStateOf(event.reminderDaysBefore) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = event.amount.formatted(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (event.isInflow) IncomeGreen else ExpenseRed
                )
            }

            // Status and Type Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(status = event.status)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SapphireAccent.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = event.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SapphireAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Details List
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    event.categoryName?.let { cat ->
                        DetailRow(label = "Category", value = cat)
                    }
                    event.sourceAccountName?.let { acc ->
                        DetailRow(label = "Account", value = acc)
                    }
                    event.destinationAccountName?.let { dest ->
                        DetailRow(label = "Destination Account", value = dest)
                    }
                    event.notes?.let { n ->
                        DetailRow(label = "Notes", value = n)
                    }
                }
            }

            // Reminders Section
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reminder Alerts",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        TextButton(
                            onClick = {
                                onSetReminder(reminderDays, true)
                                Toast.makeText(context, "Test notification dispatched!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Test Alert", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Reminder Days Selector Chips
                    val reminderOptions = listOf(
                        0 to "Due Day",
                        1 to "1 Day Before",
                        2 to "2 Days Before",
                        3 to "3 Days Before",
                        7 to "1 Week Before"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        reminderOptions.forEach { (days, label) ->
                            val isSelected = reminderDays == days
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    reminderDays = days
                                    onSetReminder(days, false)
                                },
                                label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberWarning.copy(alpha = 0.2f),
                                    selectedLabelColor = AmberWarning
                                )
                            )
                        }
                    }
                }
            }

            // Action Buttons
            if (event.underlyingRecurringRuleId != null && !event.isCompleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onMarkPaid,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Paid", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.SkipNext, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Skip")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onEditOccurrence,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Amount/Date")
                    }

                    OutlinedButton(
                        onClick = onOpenRule,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Open Rule")
                    }
                }
            } else if (event.underlyingRecurringRuleId != null && event.isCompleted) {
                OutlinedButton(
                    onClick = onOpenRule,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Recurring Rule")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ConfirmPayDialog(
    event: CalendarEvent,
    accounts: List<Account>,
    onConfirm: (actualAmount: Long?, paidDate: Long, accountId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var amountInput by remember { mutableStateOf(event.amount.amountBigDecimal.toPlainString()) }
    var selectedAccountId by remember { mutableStateOf(event.sourceAccountId ?: accounts.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Confirm Payment", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Mark '${event.title}' as paid. An actual transaction will be recorded.",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount Paid (${event.amount.currencyCode})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountInput.toDoubleOrNull()
                    val parsedMinor = amountVal?.let { (it * 100).toLong() } ?: event.amount.amountMinor
                    onConfirm(parsedMinor, System.currentTimeMillis(), selectedAccountId)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Confirm Paid", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditOccurrenceDialog(
    occurrence: RecurringOccurrence,
    onSave: (newAmountMinor: Long, newDueDate: Long, notes: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var amountInput by remember { mutableStateOf(occurrence.amount.amountBigDecimal.toPlainString()) }
    var notesInput by remember { mutableStateOf(occurrence.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Edit Occurrence", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Override the amount or notes for this specific occurrence without altering the master recurring rule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount (${occurrence.amount.currencyCode})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountInput.toDoubleOrNull()
                    val parsedMinor = amountVal?.let { (it * 100).toLong() } ?: occurrence.amount.amountMinor
                    onSave(parsedMinor, occurrence.dueDate, notesInput.ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save Changes", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

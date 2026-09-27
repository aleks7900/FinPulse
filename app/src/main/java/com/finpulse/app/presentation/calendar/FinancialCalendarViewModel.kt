package com.finpulse.app.presentation.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.util.ObligationReminderHelper
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.CalendarEvent
import com.finpulse.app.domain.model.CalendarEventType
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CashFlowPeriodSummary
import com.finpulse.app.domain.model.RecurringOccurrence
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.RecurringRepository
import com.finpulse.app.domain.usecase.calendar.GetFinancialCalendarUseCase
import com.finpulse.app.domain.usecase.recurring.EditOccurrenceUseCase
import com.finpulse.app.domain.usecase.recurring.ManageRecurringRuleUseCase
import com.finpulse.app.domain.usecase.recurring.MarkOccurrencePaidUseCase
import com.finpulse.app.domain.usecase.recurring.SkipOccurrenceUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class CalendarViewMode {
    MONTH,
    AGENDA
}

enum class PeriodPreset(val displayName: String, val days: Int?) {
    NEXT_7_DAYS("7 Days", 7),
    NEXT_30_DAYS("30 Days", 30),
    SELECTED_MONTH("Month", null),
    NEXT_90_DAYS("90 Days", 90)
}

enum class CalendarFilterType(val displayName: String) {
    ALL("All"),
    INCOME("Income"),
    BILLS("Bills & Subs"),
    LOANS("Loans"),
    TRANSFERS("Transfers"),
    GOALS("Goals")
}

data class CalendarUiState(
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val periodPreset: PeriodPreset = PeriodPreset.SELECTED_MONTH,
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTypeFilter: CalendarFilterType = CalendarFilterType.ALL,
    val summary: CashFlowPeriodSummary? = null,
    val filteredEvents: List<CalendarEvent> = emptyList(),
    val selectedDateEvents: List<CalendarEvent> = emptyList(),
    val selectedEventForDetails: CalendarEvent? = null,
    val isEventDetailSheetVisible: Boolean = false,
    val isEditOccurrenceDialogVisible: Boolean = false,
    val editingOccurrence: RecurringOccurrence? = null,
    val isConfirmPayDialogVisible: Boolean = false,
    val payingEvent: CalendarEvent? = null,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val baseCurrency: String = "USD",
    val hideBalances: Boolean = false,
    val reminderMessage: String? = null,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialCalendarViewModel(
    private val getFinancialCalendarUseCase: GetFinancialCalendarUseCase,
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val markOccurrencePaidUseCase: MarkOccurrencePaidUseCase,
    private val skipOccurrenceUseCase: SkipOccurrenceUseCase,
    private val editOccurrenceUseCase: EditOccurrenceUseCase,
    private val manageRecurringRuleUseCase: ManageRecurringRuleUseCase
) : ViewModel() {

    private val _viewMode = MutableStateFlow(CalendarViewMode.MONTH)
    private val _periodPreset = MutableStateFlow(PeriodPreset.SELECTED_MONTH)
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _selectedTypeFilter = MutableStateFlow(CalendarFilterType.ALL)

    private val _selectedEventForDetails = MutableStateFlow<CalendarEvent?>(null)
    private val _isEventDetailSheetVisible = MutableStateFlow(false)

    private val _isEditOccurrenceDialogVisible = MutableStateFlow(false)
    private val _editingOccurrence = MutableStateFlow<RecurringOccurrence?>(null)

    private val _isConfirmPayDialogVisible = MutableStateFlow(false)
    private val _payingEvent = MutableStateFlow<CalendarEvent?>(null)

    private val _reminderMessage = MutableStateFlow<String?>(null)

    // Calculate effective start and end dates based on preset / selected month
    private val _dateRangeFlow = combine(_periodPreset, _currentMonth) { preset, month ->
        val today = LocalDate.now()
        when (preset) {
            PeriodPreset.NEXT_7_DAYS -> today to today.plusDays(7)
            PeriodPreset.NEXT_30_DAYS -> today to today.plusDays(30)
            PeriodPreset.NEXT_90_DAYS -> today to today.plusDays(90)
            PeriodPreset.SELECTED_MONTH -> {
                val start = month.atDay(1)
                val end = month.atEndOfMonth()
                start to end
            }
        }
    }

    private val _summaryFlow = _dateRangeFlow.flatMapLatest { (start, end) ->
        getFinancialCalendarUseCase(
            startDate = start,
            endDate = end,
            nowMillis = System.currentTimeMillis()
        )
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        combine(
            _viewMode,
            _periodPreset,
            _currentMonth,
            _selectedDate,
            _selectedTypeFilter
        ) { mode, preset, month, date, filter ->
            ViewStateParams(mode, preset, month, date, filter)
        },
        combine(
            _summaryFlow,
            accountRepository.getActiveAccountsFlow(),
            categoryRepository.getAllCategoriesFlow(),
            userPreferencesDataStore.userPreferencesFlow
        ) { summary, accounts, categories, prefs ->
            DataParams(summary, accounts, categories, prefs)
        },
        combine(
            combine(
                _selectedEventForDetails,
                _isEventDetailSheetVisible,
                _isEditOccurrenceDialogVisible
            ) { eventDetails, isSheetVisible, isEditOcc ->
                Triple(eventDetails, isSheetVisible, isEditOcc)
            },
            combine(
                _editingOccurrence,
                _isConfirmPayDialogVisible,
                _payingEvent,
                _reminderMessage
            ) { editOcc, isConfirmPay, payEvt, remMsg ->
                Quad(editOcc, isConfirmPay, payEvt, remMsg)
            }
        ) { (eventDetails, isSheetVisible, isEditOcc), quad ->
            DialogParams(
                eventDetails = eventDetails,
                isSheetVisible = isSheetVisible,
                isEditOcc = isEditOcc,
                editOcc = quad.editOcc,
                isConfirmPay = quad.isConfirmPay,
                payEvt = quad.payEvt,
                remMsg = quad.remMsg
            )
        }
    ) { vp, dp, dia ->
        val allEvents = dp.summary.allEvents

        // Filter events by selected type
        val filtered = allEvents.filter { event ->
            when (vp.filter) {
                CalendarFilterType.ALL -> true
                CalendarFilterType.INCOME -> event.type == CalendarEventType.INCOME
                CalendarFilterType.BILLS -> event.type in setOf(CalendarEventType.BILL, CalendarEventType.SUBSCRIPTION)
                CalendarFilterType.LOANS -> event.type == CalendarEventType.LOAN_PAYMENT
                CalendarFilterType.TRANSFERS -> event.type == CalendarEventType.RECURRING_TRANSFER
                CalendarFilterType.GOALS -> event.type == CalendarEventType.GOAL_CONTRIBUTION
            }
        }

        // Events for the selected single day
        val selectedDayEvents = allEvents.filter { event ->
            val eventDate = Instant.ofEpochMilli(event.dateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            eventDate == vp.date
        }

        CalendarUiState(
            viewMode = vp.mode,
            periodPreset = vp.preset,
            currentMonth = vp.month,
            selectedDate = vp.date,
            selectedTypeFilter = vp.filter,
            summary = dp.summary,
            filteredEvents = filtered,
            selectedDateEvents = selectedDayEvents,
            selectedEventForDetails = dia.eventDetails,
            isEventDetailSheetVisible = dia.isSheetVisible,
            isEditOccurrenceDialogVisible = dia.isEditOcc,
            editingOccurrence = dia.editOcc,
            isConfirmPayDialogVisible = dia.isConfirmPay,
            payingEvent = dia.payEvt,
            accounts = dp.accounts,
            categories = dp.categories,
            baseCurrency = dp.prefs.baseCurrencyCode,
            hideBalances = dp.prefs.hideBalances,
            reminderMessage = dia.remMsg,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(isLoading = true)
    )

    fun setViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
    }

    fun setPeriodPreset(preset: PeriodPreset) {
        _periodPreset.value = preset
        if (preset == PeriodPreset.SELECTED_MONTH) {
            _currentMonth.value = YearMonth.now()
        }
    }

    fun selectPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
        _periodPreset.value = PeriodPreset.SELECTED_MONTH
    }

    fun selectNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
        _periodPreset.value = PeriodPreset.SELECTED_MONTH
    }

    fun selectToday() {
        val today = LocalDate.now()
        _currentMonth.value = YearMonth.from(today)
        _selectedDate.value = today
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setTypeFilter(filter: CalendarFilterType) {
        _selectedTypeFilter.value = filter
    }

    fun onEventClicked(event: CalendarEvent) {
        _selectedEventForDetails.value = event
        _isEventDetailSheetVisible.value = true
    }

    fun dismissEventDetails() {
        _isEventDetailSheetVisible.value = false
        _selectedEventForDetails.value = null
    }

    fun showMarkPaidDialog(event: CalendarEvent) {
        _payingEvent.value = event
        _isConfirmPayDialogVisible.value = true
    }

    fun dismissMarkPaidDialog() {
        _isConfirmPayDialogVisible.value = false
        _payingEvent.value = null
    }

    fun confirmMarkPaid(
        event: CalendarEvent,
        actualAmountMinor: Long?,
        paidDate: Long = System.currentTimeMillis(),
        accountId: String? = null
    ) {
        viewModelScope.launch {
            val ruleId = event.underlyingRecurringRuleId
            if (ruleId != null) {
                markOccurrencePaidUseCase(
                    ruleId = ruleId,
                    occurrenceDueDate = event.dateMillis,
                    actualAmountMinor = actualAmountMinor ?: event.amount.amountMinor,
                    paidDate = paidDate,
                    accountId = accountId ?: event.sourceAccountId ?: ""
                )
            }
            _isConfirmPayDialogVisible.value = false
            _payingEvent.value = null
            _isEventDetailSheetVisible.value = false
        }
    }

    fun skipEvent(event: CalendarEvent) {
        viewModelScope.launch {
            val ruleId = event.underlyingRecurringRuleId
            if (ruleId != null) {
                skipOccurrenceUseCase(
                    ruleId = ruleId,
                    occurrenceDueDate = event.dateMillis
                )
            }
            _isEventDetailSheetVisible.value = false
        }
    }

    fun showEditOccurrenceDialog(event: CalendarEvent) {
        viewModelScope.launch {
            val occId = event.underlyingOccurrenceId
            val existing = occId?.let { recurringRepository.getOccurrenceById(it) }
            val occ = existing ?: RecurringOccurrence(
                id = occId ?: "${event.underlyingRecurringRuleId}_${event.dateMillis}",
                ruleId = event.underlyingRecurringRuleId ?: "",
                ruleTitle = event.title,
                amount = event.amount,
                type = com.finpulse.app.domain.model.TransactionType.EXPENSE,
                accountId = event.sourceAccountId ?: "",
                categoryId = event.categoryId ?: "",
                dueDate = event.dateMillis,
                status = com.finpulse.app.domain.model.OccurrenceStatus.EXPECTED
            )
            _editingOccurrence.value = occ
            _isEditOccurrenceDialogVisible.value = true
        }
    }

    fun dismissEditOccurrenceDialog() {
        _isEditOccurrenceDialogVisible.value = false
        _editingOccurrence.value = null
    }

    fun saveEditedOccurrence(
        newAmountMinor: Long,
        newDueDate: Long,
        notes: String?
    ) {
        viewModelScope.launch {
            val occ = _editingOccurrence.value ?: return@launch
            editOccurrenceUseCase(
                ruleId = occ.ruleId,
                originalDueDate = occ.dueDate,
                newDueDate = newDueDate,
                newAmountMinor = newAmountMinor,
                notes = notes
            )
            _isEditOccurrenceDialogVisible.value = false
            _editingOccurrence.value = null
            _isEventDetailSheetVisible.value = false
        }
    }

    fun setEventReminder(
        context: Context,
        event: CalendarEvent,
        reminderDaysBefore: Int,
        sendInstantNotification: Boolean = false
    ) {
        viewModelScope.launch {
            val ruleId = event.underlyingRecurringRuleId
            if (ruleId != null) {
                val rule = recurringRepository.getRecurringById(ruleId)
                if (rule != null) {
                    val updated = rule.copy(reminderDaysBefore = reminderDaysBefore)
                    recurringRepository.saveRecurring(updated)
                }
            }

            if (sendInstantNotification) {
                ObligationReminderHelper.sendEventReminderNotification(context, event)
            }

            _reminderMessage.value = if (reminderDaysBefore > 0) {
                "Reminder set for $reminderDaysBefore days before due date"
            } else {
                "Reminder set for due date"
            }
        }
    }

    fun clearReminderMessage() {
        _reminderMessage.value = null
    }
}

private data class ViewStateParams(
    val mode: CalendarViewMode,
    val preset: PeriodPreset,
    val month: YearMonth,
    val date: LocalDate,
    val filter: CalendarFilterType
)

private data class DataParams(
    val summary: CashFlowPeriodSummary,
    val accounts: List<Account>,
    val categories: List<Category>,
    val prefs: com.finpulse.app.core.datastore.UserPreferences
)

private data class DialogParams(
    val eventDetails: CalendarEvent?,
    val isSheetVisible: Boolean,
    val isEditOcc: Boolean,
    val editOcc: RecurringOccurrence?,
    val isConfirmPay: Boolean,
    val payEvt: CalendarEvent?,
    val remMsg: String?
)

private data class Quad(
    val editOcc: RecurringOccurrence?,
    val isConfirmPay: Boolean,
    val payEvt: CalendarEvent?,
    val remMsg: String?
)

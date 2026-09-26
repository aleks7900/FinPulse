package com.finpulse.app.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CustomIntervalUnit
import com.finpulse.app.domain.model.PaymentFrequency
import com.finpulse.app.domain.model.RecurringOccurrence
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.RecurringRepository
import com.finpulse.app.domain.usecase.recurring.EditOccurrenceUseCase
import com.finpulse.app.domain.usecase.recurring.GetUpcomingOccurrencesUseCase
import com.finpulse.app.domain.usecase.recurring.ManageRecurringRuleUseCase
import com.finpulse.app.domain.usecase.recurring.MarkOccurrencePaidUseCase
import com.finpulse.app.domain.usecase.recurring.SaveRecurringRuleResult
import com.finpulse.app.domain.usecase.recurring.SkipOccurrenceUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RecurringTab(val label: String) {
    UPCOMING("Upcoming"),
    RULES("All Rules"),
    SUBSCRIPTIONS("Subscriptions")
}

data class RecurringUiState(
    val selectedTab: RecurringTab = RecurringTab.UPCOMING,
    val upcomingWindowDays: Int = 30,
    val upcomingOccurrences: List<RecurringOccurrence> = emptyList(),
    val recurringList: List<RecurringTransaction> = emptyList(),
    val subscriptionsList: List<RecurringTransaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val totalMonthlyCost: Money = Money.zero(),
    val totalAnnualCost: Money = Money.zero(),
    val totalMonthlySubscriptions: Money = Money.zero(),
    val totalAnnualSubscriptions: Money = Money.zero(),
    val activeSubscriptionsCount: Int = 0,
    val hideBalances: Boolean = false,
    val baseCurrency: String = "USD",
    val isAddEditDialogVisible: Boolean = false,
    val editingRecurring: RecurringTransaction? = null,
    val isEditOccurrenceDialogVisible: Boolean = false,
    val editingOccurrence: RecurringOccurrence? = null,
    val isConfirmPayDialogVisible: Boolean = false,
    val payingOccurrence: RecurringOccurrence? = null,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringViewModel(
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val getUpcomingOccurrencesUseCase: GetUpcomingOccurrencesUseCase,
    private val markOccurrencePaidUseCase: MarkOccurrencePaidUseCase,
    private val skipOccurrenceUseCase: SkipOccurrenceUseCase,
    private val editOccurrenceUseCase: EditOccurrenceUseCase,
    private val manageRecurringRuleUseCase: ManageRecurringRuleUseCase
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(RecurringTab.UPCOMING)
    private val _upcomingWindowDays = MutableStateFlow(30)
    private val _isAddEditDialogVisible = MutableStateFlow(false)
    private val _editingRecurring = MutableStateFlow<RecurringTransaction?>(null)
    private val _isEditOccurrenceDialogVisible = MutableStateFlow(false)
    private val _editingOccurrence = MutableStateFlow<RecurringOccurrence?>(null)
    private val _isConfirmPayDialogVisible = MutableStateFlow(false)
    private val _payingOccurrence = MutableStateFlow<RecurringOccurrence?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _upcomingOccurrencesFlow = _upcomingWindowDays.flatMapLatest { days ->
        getUpcomingOccurrencesUseCase(windowDays = days)
    }

    val uiState: StateFlow<RecurringUiState> = combine(
        combine(
            _selectedTab,
            _upcomingWindowDays,
            _upcomingOccurrencesFlow,
            recurringRepository.getAllRecurringFlow(),
            accountRepository.getActiveAccountsFlow()
        ) { tab, windowDays, occurrences, rules, accounts ->
            TabAndRules(tab, windowDays, occurrences, rules, accounts)
        },
        combine(
            categoryRepository.getAllCategoriesFlow(),
            userPreferencesDataStore.userPreferencesFlow,
            _isAddEditDialogVisible,
            _editingRecurring
        ) { categories, prefs, isAddVisible, editingR ->
            CategoryAndDialog(categories, prefs, isAddVisible, editingR)
        },
        combine(
            _isEditOccurrenceDialogVisible,
            _editingOccurrence,
            _isConfirmPayDialogVisible,
            _payingOccurrence,
            _errorMessage
        ) { isEditOcc, editOcc, isConfirmPay, payOcc, err ->
            OccurrenceDialogs(isEditOcc, editOcc, isConfirmPay, payOcc, err)
        }
    ) { tr, cd, od ->
        val currency = cd.prefs.baseCurrencyCode
        val activeRules = tr.rules.filter { it.isActive && !it.isCancelled }
        val subscriptions = activeRules.filter { it.isSubscription }

        val monthlyCostMinor = activeRules.sumOf { it.calculateMonthlyCost().amountMinor }
        val annualCostMinor = activeRules.sumOf { it.calculateAnnualCost().amountMinor }

        val monthlySubMinor = subscriptions.sumOf { it.calculateMonthlyCost().amountMinor }
        val annualSubMinor = subscriptions.sumOf { it.calculateAnnualCost().amountMinor }

        RecurringUiState(
            selectedTab = tr.tab,
            upcomingWindowDays = tr.windowDays,
            upcomingOccurrences = tr.occurrences,
            recurringList = tr.rules,
            subscriptionsList = subscriptions,
            accounts = tr.accounts,
            categories = cd.categories,
            totalMonthlyCost = Money(monthlyCostMinor, currency),
            totalAnnualCost = Money(annualCostMinor, currency),
            totalMonthlySubscriptions = Money(monthlySubMinor, currency),
            totalAnnualSubscriptions = Money(annualSubMinor, currency),
            activeSubscriptionsCount = subscriptions.size,
            hideBalances = cd.prefs.hideBalances,
            baseCurrency = currency,
            isAddEditDialogVisible = cd.isAddVisible,
            editingRecurring = cd.editingR,
            isEditOccurrenceDialogVisible = od.isEditOcc,
            editingOccurrence = od.editOcc,
            isConfirmPayDialogVisible = od.isConfirmPay,
            payingOccurrence = od.payOcc,
            errorMessage = od.err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringUiState()
    )

    fun onTabSelected(tab: RecurringTab) {
        _selectedTab.value = tab
    }

    fun onWindowDaysChanged(days: Int) {
        _upcomingWindowDays.value = days
    }

    fun showAddEditDialog(show: Boolean, recurring: RecurringTransaction? = null) {
        _editingRecurring.value = recurring
        _isAddEditDialogVisible.value = show
        _errorMessage.value = null
    }

    fun showEditOccurrenceDialog(show: Boolean, occurrence: RecurringOccurrence? = null) {
        _editingOccurrence.value = occurrence
        _isEditOccurrenceDialogVisible.value = show
        _errorMessage.value = null
    }

    fun showConfirmPayDialog(show: Boolean, occurrence: RecurringOccurrence? = null) {
        _payingOccurrence.value = occurrence
        _isConfirmPayDialogVisible.value = show
        _errorMessage.value = null
    }

    fun saveRecurring(
        id: String?,
        title: String,
        amountMinor: Long,
        type: TransactionType = TransactionType.EXPENSE,
        accountId: String,
        destinationAccountId: String? = null,
        categoryId: String,
        frequency: PaymentFrequency,
        customIntervalValue: Int = 1,
        customIntervalUnit: CustomIntervalUnit = CustomIntervalUnit.MONTHS,
        nextDueDate: Long,
        isSubscription: Boolean,
        isVariableAmount: Boolean = false,
        reminderDaysBefore: Int = 1,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val result = manageRecurringRuleUseCase.saveRule(
                id = id,
                title = title,
                amountMinor = amountMinor,
                currencyCode = uiState.value.baseCurrency,
                type = type,
                sourceAccountId = accountId,
                destinationAccountId = destinationAccountId,
                categoryId = categoryId,
                frequency = frequency,
                customIntervalValue = customIntervalValue,
                customIntervalUnit = customIntervalUnit,
                nextDueDate = nextDueDate,
                isSubscription = isSubscription,
                isVariableAmount = isVariableAmount,
                reminderDaysBefore = reminderDaysBefore,
                notes = notes
            )
            when (result) {
                is SaveRecurringRuleResult.Success -> {
                    _isAddEditDialogVisible.value = false
                    _editingRecurring.value = null
                    _errorMessage.value = null
                }
                is SaveRecurringRuleResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun markOccurrencePaid(
        occurrence: RecurringOccurrence,
        actualAmountMinor: Long? = null,
        paidDate: Long = System.currentTimeMillis(),
        accountId: String? = null
    ) {
        viewModelScope.launch {
            markOccurrencePaidUseCase(
                ruleId = occurrence.ruleId,
                occurrenceDueDate = occurrence.dueDate,
                actualAmountMinor = actualAmountMinor ?: occurrence.amount.amountMinor,
                paidDate = paidDate,
                accountId = accountId ?: occurrence.accountId
            )
            _isConfirmPayDialogVisible.value = false
            _payingOccurrence.value = null
        }
    }

    fun skipOccurrence(occurrence: RecurringOccurrence) {
        viewModelScope.launch {
            skipOccurrenceUseCase(
                ruleId = occurrence.ruleId,
                occurrenceDueDate = occurrence.dueDate
            )
        }
    }

    fun editOccurrence(
        occurrence: RecurringOccurrence,
        newAmountMinor: Long,
        newDueDate: Long,
        notes: String?
    ) {
        viewModelScope.launch {
            editOccurrenceUseCase(
                ruleId = occurrence.ruleId,
                originalDueDate = occurrence.dueDate,
                newDueDate = newDueDate,
                newAmountMinor = newAmountMinor,
                notes = notes
            )
            _isEditOccurrenceDialogVisible.value = false
            _editingOccurrence.value = null
        }
    }

    fun toggleActive(recurring: RecurringTransaction) {
        viewModelScope.launch {
            manageRecurringRuleUseCase.setRuleActive(recurring.id, !recurring.isActive)
        }
    }

    fun cancelRule(id: String) {
        viewModelScope.launch {
            manageRecurringRuleUseCase.cancelRule(id)
            _isAddEditDialogVisible.value = false
            _editingRecurring.value = null
        }
    }

    fun deleteRecurring(id: String) {
        viewModelScope.launch {
            manageRecurringRuleUseCase.deleteRule(id)
            _isAddEditDialogVisible.value = false
            _editingRecurring.value = null
        }
    }
}

private data class TabAndRules(
    val tab: RecurringTab,
    val windowDays: Int,
    val occurrences: List<RecurringOccurrence>,
    val rules: List<RecurringTransaction>,
    val accounts: List<Account>
)

private data class CategoryAndDialog(
    val categories: List<Category>,
    val prefs: com.finpulse.app.core.datastore.UserPreferences,
    val isAddVisible: Boolean,
    val editingR: RecurringTransaction?
)

private data class OccurrenceDialogs(
    val isEditOcc: Boolean,
    val editOcc: RecurringOccurrence?,
    val isConfirmPay: Boolean,
    val payOcc: RecurringOccurrence?,
    val err: String?
)

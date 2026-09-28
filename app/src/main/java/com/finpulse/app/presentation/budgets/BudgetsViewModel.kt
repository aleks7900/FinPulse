package com.finpulse.app.presentation.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.model.Money
import com.finpulse.app.core.model.TimePeriod
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.BudgetAlertLevel
import com.finpulse.app.domain.model.BudgetPeriod
import com.finpulse.app.domain.model.BudgetStatus
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.FinancialGoal
import com.finpulse.app.domain.model.RecurringOccurrence
import com.finpulse.app.domain.model.SafeToSpendBreakdown
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.BudgetRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.GoalRepository
import com.finpulse.app.domain.repository.RecurringRepository
import com.finpulse.app.domain.repository.TransactionRepository
import com.finpulse.app.domain.usecase.EvaluateBudgetStatusUseCase
import com.finpulse.app.domain.usecase.budget.CalculateSafeToSpendUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

enum class BudgetFilter(val label: String) {
    ALL("All Budgets"),
    WARNING("Near Limit"),
    EXCEEDED("Overspent")
}

data class BudgetsUiState(
    val overallBudgetStatus: BudgetStatus? = null,
    val categoryBudgetStatuses: List<BudgetStatus> = emptyList(),
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val categories: List<Category> = emptyList(),
    val totalBudgeted: Money = Money.zero(),
    val totalSpent: Money = Money.zero(),
    val hideBalances: Boolean = false,
    val baseCurrency: String = "USD",
    val isAddEditDialogVisible: Boolean = false,
    val editingBudget: Budget? = null,
    val safeToSpendBreakdown: SafeToSpendBreakdown? = null,
    val isAssumptionsDialogVisible: Boolean = false,
    val selectedFilter: BudgetFilter = BudgetFilter.ALL
)

class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val recurringRepository: RecurringRepository,
    private val goalRepository: GoalRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val evaluateBudgetStatusUseCase: EvaluateBudgetStatusUseCase = EvaluateBudgetStatusUseCase(),
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase = CalculateSafeToSpendUseCase()
) : ViewModel() {

    private val _isAddEditDialogVisible = MutableStateFlow(false)
    private val _editingBudget = MutableStateFlow<Budget?>(null)
    private val _isAssumptionsDialogVisible = MutableStateFlow(false)
    private val _selectedFilter = MutableStateFlow(BudgetFilter.ALL)

    // Current month date range
    private val currentMonthRange = TimePeriod.MONTH.toDateRange()

    private val baseDataFlow = combine(
        budgetRepository.getAllActiveBudgetsFlow(),
        categoryRepository.getAllCategoriesFlow(),
        transactionRepository.getAllTransactionsFlow(),
        accountRepository.getActiveAccountsFlow()
    ) { budgets, categories, transactions, accounts ->
        Quadruple(budgets, categories, transactions, accounts)
    }

    private val obligationsFlow = combine(
        recurringRepository.getOccurrencesInRangeFlow(currentMonthRange.first, currentMonthRange.second),
        goalRepository.getAllGoalsFlow(),
        userPreferencesDataStore.userPreferencesFlow
    ) { occurrences, goals, userPrefs ->
        Triple(occurrences, goals, userPrefs)
    }

    private val uiControlFlow = combine(
        _isAddEditDialogVisible,
        _editingBudget,
        _isAssumptionsDialogVisible,
        _selectedFilter
    ) { isAddVisible, editingB, showAssumptions, filter ->
        FilterState(isAddVisible, editingB, showAssumptions, filter)
    }

    val uiState: StateFlow<BudgetsUiState> = combine(
        baseDataFlow,
        obligationsFlow,
        uiControlFlow
    ) { (budgets, categories, transactions, accounts), (occurrences, goals, userPrefs), control ->
        val (startMillis, endMillis) = currentMonthRange
        val periodTransactions = transactions.filter { it.timestamp in startMillis..endMillis }

        val allStatuses = evaluateBudgetStatusUseCase(
            budgets = budgets,
            categories = categories,
            transactions = periodTransactions,
            currentDate = LocalDate.now()
        )

        val overallStatus = allStatuses.firstOrNull { it.isOverall }
        val categoryStatuses = allStatuses.filter { !it.isOverall }

        val safeToSpend = calculateSafeToSpendUseCase(
            accounts = accounts,
            upcomingOccurrences = occurrences,
            goals = goals,
            overallBudgetStatus = overallStatus,
            baseCurrency = userPrefs.baseCurrencyCode,
            currentDate = LocalDate.now(),
            periodEndMillis = endMillis
        )

        val filteredCategoryStatuses = when (control.filter) {
            BudgetFilter.ALL -> categoryStatuses
            BudgetFilter.WARNING -> categoryStatuses.filter { it.isWarning || it.alertLevel != BudgetAlertLevel.NORMAL }
            BudgetFilter.EXCEEDED -> categoryStatuses.filter { it.isExceeded }
        }

        val baseCode = userPrefs.baseCurrencyCode

        val totalBudgeted = if (overallStatus != null) {
            val limit = overallStatus.budget.effectiveLimit
            if (limit.currencyCode.equals(baseCode, ignoreCase = true)) {
                limit
            } else {
                val rate = com.finpulse.app.data.repository.ExchangeRateProviderImpl.computeFallbackRate(limit.currencyCode, baseCode)
                val targetMajor = limit.amountBigDecimal.multiply(java.math.BigDecimal.valueOf(rate))
                Money.fromMajor(targetMajor, baseCode)
            }
        } else {
            var sumMinor = 0L
            for (status in categoryStatuses) {
                val limit = status.budget.effectiveLimit
                if (limit.currencyCode.equals(baseCode, ignoreCase = true)) {
                    sumMinor += limit.amountMinor
                } else {
                    val rate = com.finpulse.app.data.repository.ExchangeRateProviderImpl.computeFallbackRate(limit.currencyCode, baseCode)
                    val targetMajor = limit.amountBigDecimal.multiply(java.math.BigDecimal.valueOf(rate))
                    sumMinor += com.finpulse.app.core.model.CurrencyConfig.toMinor(targetMajor, baseCode)
                }
            }
            Money(sumMinor, baseCode)
        }

        val totalSpent = if (overallStatus != null) {
            val spent = overallStatus.spentAmount
            if (spent.currencyCode.equals(baseCode, ignoreCase = true)) {
                spent
            } else {
                val rate = com.finpulse.app.data.repository.ExchangeRateProviderImpl.computeFallbackRate(spent.currencyCode, baseCode)
                val targetMajor = spent.amountBigDecimal.multiply(java.math.BigDecimal.valueOf(rate))
                Money.fromMajor(targetMajor, baseCode)
            }
        } else {
            var sumMinor = 0L
            for (status in categoryStatuses) {
                val spent = status.spentAmount
                if (spent.currencyCode.equals(baseCode, ignoreCase = true)) {
                    sumMinor += spent.amountMinor
                } else {
                    val rate = com.finpulse.app.data.repository.ExchangeRateProviderImpl.computeFallbackRate(spent.currencyCode, baseCode)
                    val targetMajor = spent.amountBigDecimal.multiply(java.math.BigDecimal.valueOf(rate))
                    sumMinor += com.finpulse.app.core.model.CurrencyConfig.toMinor(targetMajor, baseCode)
                }
            }
            Money(sumMinor, baseCode)
        }

        BudgetsUiState(
            overallBudgetStatus = overallStatus,
            categoryBudgetStatuses = filteredCategoryStatuses,
            budgetStatuses = allStatuses,
            categories = categories,
            totalBudgeted = totalBudgeted,
            totalSpent = totalSpent,
            hideBalances = userPrefs.hideBalances,
            baseCurrency = userPrefs.baseCurrencyCode,
            isAddEditDialogVisible = control.isAddVisible,
            editingBudget = control.editingBudget,
            safeToSpendBreakdown = safeToSpend,
            isAssumptionsDialogVisible = control.showAssumptions,
            selectedFilter = control.filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetsUiState()
    )

    fun showAddEditDialog(show: Boolean, budget: Budget? = null) {
        _editingBudget.value = budget
        _isAddEditDialogVisible.value = show
    }

    fun toggleAssumptionsDialog(show: Boolean) {
        _isAssumptionsDialogVisible.value = show
    }

    fun setFilter(filter: BudgetFilter) {
        _selectedFilter.value = filter
    }

    fun saveBudget(
        id: String?,
        categoryId: String,
        name: String,
        limitAmountMinor: Long,
        currencyCode: String? = null,
        period: BudgetPeriod = BudgetPeriod.MONTHLY,
        isOverall: Boolean = false,
        isRolloverEnabled: Boolean = false,
        rolloverAmountMinor: Long = 0L,
        alertThresholdPercent: Int = 85
    ) {
        viewModelScope.launch {
            val (startMillis, endMillis) = when (period) {
                BudgetPeriod.WEEKLY -> TimePeriod.WEEK.toDateRange()
                BudgetPeriod.MONTHLY -> TimePeriod.MONTH.toDateRange()
                BudgetPeriod.CUSTOM -> TimePeriod.MONTH.toDateRange()
            }

            val resolvedCurrency = currencyCode?.uppercase()
                ?: _editingBudget.value?.limitAmount?.currencyCode
                ?: uiState.value.baseCurrency

            val b = Budget(
                id = id ?: UUID.randomUUID().toString(),
                categoryId = if (isOverall) "overall" else categoryId,
                name = name.ifBlank { if (isOverall) "Overall Monthly Budget" else "Budget" },
                limitAmount = Money(limitAmountMinor, resolvedCurrency),
                periodType = period,
                startDate = startMillis,
                endDate = endMillis,
                isOverall = isOverall,
                isRolloverEnabled = isRolloverEnabled,
                rolloverAmountMinor = rolloverAmountMinor,
                alertThresholdPercent = alertThresholdPercent
            )
            budgetRepository.saveBudget(b)
            _isAddEditDialogVisible.value = false
            _editingBudget.value = null
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(id)
            _isAddEditDialogVisible.value = false
            _editingBudget.value = null
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
private data class FilterState(
    val isAddVisible: Boolean,
    val editingBudget: Budget?,
    val showAssumptions: Boolean,
    val filter: BudgetFilter
)

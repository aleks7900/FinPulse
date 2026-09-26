package com.finpulse.app.presentation.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.model.Money
import com.finpulse.app.core.model.TimePeriod
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.BudgetPeriod
import com.finpulse.app.domain.model.BudgetStatus
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.repository.BudgetRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.TransactionRepository
import com.finpulse.app.domain.usecase.EvaluateBudgetStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class BudgetsUiState(
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val categories: List<Category> = emptyList(),
    val totalBudgeted: Money = Money.zero(),
    val totalSpent: Money = Money.zero(),
    val hideBalances: Boolean = false,
    val baseCurrency: String = "USD",
    val isAddEditDialogVisible: Boolean = false,
    val editingBudget: Budget? = null
)

class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val evaluateBudgetStatusUseCase: EvaluateBudgetStatusUseCase = EvaluateBudgetStatusUseCase()
) : ViewModel() {

    private val _isAddEditDialogVisible = MutableStateFlow(false)
    private val _editingBudget = MutableStateFlow<Budget?>(null)

    val uiState: StateFlow<BudgetsUiState> = combine(
        budgetRepository.getAllActiveBudgetsFlow(),
        categoryRepository.getAllCategoriesFlow(),
        transactionRepository.getAllTransactionsFlow(),
        userPreferencesDataStore.userPreferencesFlow,
        _isAddEditDialogVisible,
        _editingBudget
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val budgets = params[0] as List<Budget>
        @Suppress("UNCHECKED_CAST")
        val categories = params[1] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val transactions = params[2] as List<Transaction>
        val userPrefs = params[3] as com.finpulse.app.core.datastore.UserPreferences
        val isAddVisible = params[4] as Boolean
        val editingB = params[5] as Budget?

        val (startMillis, endMillis) = TimePeriod.MONTH.toDateRange()
        val periodTransactions = transactions.filter { it.timestamp in startMillis..endMillis }

        val statuses = evaluateBudgetStatusUseCase(
            budgets = budgets,
            categories = categories,
            transactions = periodTransactions,
            currentDate = LocalDate.now()
        )

        val totalBudgetedMinor = budgets.sumOf { it.limitAmount.amountMinor }
        val totalSpentMinor = statuses.sumOf { it.spentAmount.amountMinor }

        BudgetsUiState(
            budgetStatuses = statuses,
            categories = categories,
            totalBudgeted = Money(totalBudgetedMinor, userPrefs.baseCurrencyCode),
            totalSpent = Money(totalSpentMinor, userPrefs.baseCurrencyCode),
            hideBalances = userPrefs.hideBalances,
            baseCurrency = userPrefs.baseCurrencyCode,
            isAddEditDialogVisible = isAddVisible,
            editingBudget = editingB
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

    fun saveBudget(
        id: String?,
        categoryId: String,
        name: String,
        limitAmountMinor: Long,
        period: BudgetPeriod
    ) {
        viewModelScope.launch {
            val (startMillis, endMillis) = when (period) {
                BudgetPeriod.WEEKLY -> TimePeriod.WEEK.toDateRange()
                BudgetPeriod.MONTHLY -> TimePeriod.MONTH.toDateRange()
                BudgetPeriod.CUSTOM -> TimePeriod.MONTH.toDateRange()
            }

            val b = Budget(
                id = id ?: UUID.randomUUID().toString(),
                categoryId = categoryId,
                name = name,
                limitAmount = Money(limitAmountMinor, uiState.value.baseCurrency),
                periodType = period,
                startDate = startMillis,
                endDate = endMillis
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

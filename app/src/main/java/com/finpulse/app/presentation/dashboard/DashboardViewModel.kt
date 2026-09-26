package com.finpulse.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.model.Money
import com.finpulse.app.core.model.TimePeriod
import com.finpulse.app.domain.engine.InsightsEngine
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.BudgetStatus
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.FinancialInsight
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.BudgetRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.DebtRepository
import com.finpulse.app.domain.repository.GoalRepository
import com.finpulse.app.domain.repository.InvestmentRepository
import com.finpulse.app.domain.repository.RecurringRepository
import com.finpulse.app.domain.repository.TransactionRepository
import com.finpulse.app.domain.usecase.DashboardSummary
import com.finpulse.app.domain.usecase.EvaluateBudgetStatusUseCase
import com.finpulse.app.domain.usecase.GetDashboardSummaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val summary: DashboardSummary = DashboardSummary(
        totalBalance = Money.zero(),
        availableBalance = Money.zero(),
        income = Money.zero(),
        expenses = Money.zero(),
        netCashFlow = Money.zero(),
        totalSavings = Money.zero(),
        totalInvestments = Money.zero(),
        totalDebt = Money.zero(),
        savingsRatePercentage = 0.0
    ),
    val accounts: List<Account> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val categories: List<Category> = emptyList(),
    val insights: List<FinancialInsight> = emptyList(),
    val selectedPeriod: TimePeriod = TimePeriod.MONTH,
    val hideBalances: Boolean = false,
    val baseCurrency: String = "USD",
    val unreviewedCount: Int = 0,
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringRepository,
    private val goalRepository: GoalRepository,
    private val investmentRepository: InvestmentRepository,
    private val debtRepository: DebtRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase = GetDashboardSummaryUseCase(),
    private val evaluateBudgetStatusUseCase: EvaluateBudgetStatusUseCase = EvaluateBudgetStatusUseCase(),
    private val insightsEngine: InsightsEngine = InsightsEngine()
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(TimePeriod.MONTH)

    val uiState: StateFlow<DashboardUiState> = combine(
        accountRepository.getAllAccountsFlow(),
        transactionRepository.getAllTransactionsFlow(),
        budgetRepository.getAllActiveBudgetsFlow(),
        categoryRepository.getAllCategoriesFlow(),
        recurringRepository.getActiveSubscriptionsFlow(),
        goalRepository.getAllGoalsFlow(),
        investmentRepository.getAllAssetsFlow(),
        debtRepository.getAllDebtsFlow(),
        userPreferencesDataStore.userPreferencesFlow,
        _selectedPeriod,
        transactionRepository.getUnreviewedCountFlow()
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val accounts = params[0] as List<Account>
        @Suppress("UNCHECKED_CAST")
        val transactions = params[1] as List<Transaction>
        @Suppress("UNCHECKED_CAST")
        val budgets = params[2] as List<Budget>
        @Suppress("UNCHECKED_CAST")
        val categories = params[3] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val subscriptions = params[4] as List<com.finpulse.app.domain.model.RecurringTransaction>
        @Suppress("UNCHECKED_CAST")
        val goals = params[5] as List<com.finpulse.app.domain.model.FinancialGoal>
        @Suppress("UNCHECKED_CAST")
        val investments = params[6] as List<com.finpulse.app.domain.model.InvestmentAsset>
        @Suppress("UNCHECKED_CAST")
        val debts = params[7] as List<com.finpulse.app.domain.model.Debt>
        val userPrefs = params[8] as com.finpulse.app.core.datastore.UserPreferences
        val period = params[9] as TimePeriod
        val unreviewedCount = params[10] as Int

        val (startMillis, endMillis) = period.toDateRange()
        val periodTransactions = transactions.filter { it.timestamp in startMillis..endMillis }

        val summary = getDashboardSummaryUseCase(
            accounts = accounts,
            transactions = periodTransactions,
            investments = investments,
            debts = debts,
            goals = goals,
            baseCurrency = userPrefs.baseCurrencyCode
        )

        val budgetStatuses = evaluateBudgetStatusUseCase(
            budgets = budgets,
            categories = categories,
            transactions = periodTransactions,
            currentDate = LocalDate.now()
        )

        val insights = insightsEngine.generateInsights(
            summary = summary,
            budgetStatuses = budgetStatuses,
            transactions = periodTransactions,
            categories = categories,
            subscriptions = subscriptions
        )

        val recent = transactions.take(6)

        DashboardUiState(
            summary = summary,
            accounts = accounts.filter { !it.isArchived },
            recentTransactions = recent,
            budgetStatuses = budgetStatuses,
            categories = categories,
            insights = insights,
            selectedPeriod = period,
            hideBalances = userPrefs.hideBalances,
            baseCurrency = userPrefs.baseCurrencyCode,
            unreviewedCount = unreviewedCount,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun onPeriodSelected(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    fun toggleHideBalances() {
        val current = uiState.value.hideBalances
        viewModelScope.launch {
            userPreferencesDataStore.setHideBalances(!current)
        }
    }
}

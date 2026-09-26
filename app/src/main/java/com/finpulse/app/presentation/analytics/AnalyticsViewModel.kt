package com.finpulse.app.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.model.Money
import com.finpulse.app.core.model.TimePeriod
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class CategorySpendItem(
    val category: Category,
    val totalAmount: Money,
    val percentageOfTotal: Double
)

data class MerchantSpendItem(
    val merchant: String,
    val totalAmount: Money,
    val transactionCount: Int
)

data class AnalyticsUiState(
    val selectedPeriod: TimePeriod = TimePeriod.MONTH,
    val totalIncome: Money = Money.zero(),
    val totalExpenses: Money = Money.zero(),
    val netCashFlow: Money = Money.zero(),
    val averageDailyExpense: Money = Money.zero(),
    val largestExpense: Money = Money.zero(),
    val savingsRate: Double = 0.0,
    val categoryBreakdown: List<CategorySpendItem> = emptyList(),
    val topMerchants: List<MerchantSpendItem> = emptyList(),
    val momExpenseVariancePercent: Double = 0.0,
    val hideBalances: Boolean = false,
    val baseCurrency: String = "USD"
)

class AnalyticsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(TimePeriod.MONTH)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        transactionRepository.getAllTransactionsFlow(),
        categoryRepository.getAllCategoriesFlow(),
        userPreferencesDataStore.userPreferencesFlow,
        _selectedPeriod
    ) { transactions, categories, userPrefs, period ->
        val currency = userPrefs.baseCurrencyCode
        val (startMillis, endMillis) = period.toDateRange()
        val periodTx = transactions.filter { it.timestamp in startMillis..endMillis }

        var incomeMinor = 0L
        var expenseMinor = 0L
        var maxExpenseMinor = 0L

        for (tx in periodTx) {
            when (tx.type) {
                TransactionType.INCOME, TransactionType.REFUND -> incomeMinor += tx.amount.amountMinor
                TransactionType.EXPENSE -> {
                    if (!tx.isExcludedFromBudget) {
                        expenseMinor += tx.amount.amountMinor
                        if (tx.amount.amountMinor > maxExpenseMinor) {
                            maxExpenseMinor = tx.amount.amountMinor
                        }
                    }
                }
                TransactionType.TRANSFER -> {}
            }
        }

        val netMinor = incomeMinor - expenseMinor
        val savingsRate = if (incomeMinor > 0) {
            ((incomeMinor - expenseMinor).coerceAtLeast(0L).toDouble() / incomeMinor.toDouble()) * 100.0
        } else 0.0

        val daysInPeriod = when (period) {
            TimePeriod.WEEK -> 7
            TimePeriod.MONTH -> LocalDate.now().dayOfMonth.coerceAtLeast(1)
            TimePeriod.THREE_MONTHS -> 90
            TimePeriod.SIX_MONTHS -> 180
            TimePeriod.YEAR -> 365
            TimePeriod.ALL -> 365
        }
        val avgDailyMinor = if (daysInPeriod > 0) expenseMinor / daysInPeriod else 0L

        // Category breakdown
        val catMap = categories.associateBy { it.id }
        val expenseTx = periodTx.filter { it.type == TransactionType.EXPENSE && !it.isExcludedFromBudget }
        val categoryBreakdown = expenseTx
            .groupBy { it.categoryId }
            .mapNotNull { (catId, txList) ->
                val cat = catMap[catId] ?: return@mapNotNull null
                val catTotal = txList.sumOf { it.amount.amountMinor }
                val pct = if (expenseMinor > 0) catTotal.toDouble() / expenseMinor.toDouble() else 0.0
                CategorySpendItem(
                    category = cat,
                    totalAmount = Money(catTotal, currency),
                    percentageOfTotal = pct
                )
            }
            .sortedByDescending { it.totalAmount.amountMinor }

        // Top merchants
        val topMerchants = expenseTx
            .filter { !it.merchant.isNullOrBlank() }
            .groupBy { it.merchant!! }
            .map { (merchant, txList) ->
                val merchantTotal = txList.sumOf { it.amount.amountMinor }
                MerchantSpendItem(
                    merchant = merchant,
                    totalAmount = Money(merchantTotal, currency),
                    transactionCount = txList.size
                )
            }
            .sortedByDescending { it.totalAmount.amountMinor }
            .take(5)

        AnalyticsUiState(
            selectedPeriod = period,
            totalIncome = Money(incomeMinor, currency),
            totalExpenses = Money(expenseMinor, currency),
            netCashFlow = Money(netMinor, currency),
            averageDailyExpense = Money(avgDailyMinor, currency),
            largestExpense = Money(maxExpenseMinor, currency),
            savingsRate = savingsRate,
            categoryBreakdown = categoryBreakdown,
            topMerchants = topMerchants,
            hideBalances = userPrefs.hideBalances,
            baseCurrency = currency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun onPeriodSelected(period: TimePeriod) {
        _selectedPeriod.value = period
    }
}

package md.alexlab.finpulse.presentation.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.engine.DebtPayoffEngine
import md.alexlab.finpulse.domain.engine.DebtPayoffPlan
import md.alexlab.finpulse.domain.engine.DebtStrategy
import md.alexlab.finpulse.domain.model.Debt
import md.alexlab.finpulse.domain.model.DebtType
import md.alexlab.finpulse.domain.repository.DebtRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class DebtUiState(
    val debts: List<Debt> = emptyList(),
    val payoffPlan: DebtPayoffPlan = DebtPayoffPlan(
        strategy = DebtStrategy.SNOWBALL,
        orderedDebts = emptyList(),
        totalDebt = Money.zero(),
        totalMonthlyMinimum = Money.zero(),
        estimatedMonthsToFree = 0
    ),
    val selectedStrategy: DebtStrategy = DebtStrategy.SNOWBALL,
    val hideBalances: Boolean = false,
    val baseCurrency: String = "USD",
    val isAddEditDialogVisible: Boolean = false,
    val isPaymentDialogVisible: Boolean = false,
    val selectedDebt: Debt? = null
)

class DebtViewModel(
    private val debtRepository: DebtRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val payoffEngine: DebtPayoffEngine = DebtPayoffEngine()
) : ViewModel() {

    private val _strategy = MutableStateFlow(DebtStrategy.SNOWBALL)
    private val _isAddEditDialogVisible = MutableStateFlow(false)
    private val _isPaymentDialogVisible = MutableStateFlow(false)
    private val _selectedDebt = MutableStateFlow<Debt?>(null)

    val uiState: StateFlow<DebtUiState> = combine(
        debtRepository.getAllDebtsFlow(),
        userPreferencesDataStore.userPreferencesFlow,
        _strategy,
        _isAddEditDialogVisible,
        _isPaymentDialogVisible,
        _selectedDebt
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val debts = params[0] as List<Debt>
        val userPrefs = params[1] as md.alexlab.finpulse.core.datastore.UserPreferences
        val strategy = params[2] as DebtStrategy
        val isAddVisible = params[3] as Boolean
        val isPayVisible = params[4] as Boolean
        val selDebt = params[5] as Debt?

        val plan = payoffEngine.generatePlan(debts, strategy)

        DebtUiState(
            debts = debts,
            payoffPlan = plan,
            selectedStrategy = strategy,
            hideBalances = userPrefs.hideBalances,
            baseCurrency = userPrefs.baseCurrencyCode,
            isAddEditDialogVisible = isAddVisible,
            isPaymentDialogVisible = isPayVisible,
            selectedDebt = selDebt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtUiState()
    )

    fun onStrategyChange(strategy: DebtStrategy) {
        _strategy.value = strategy
    }

    fun showAddEditDialog(show: Boolean, debt: Debt? = null) {
        _selectedDebt.value = debt
        _isAddEditDialogVisible.value = show
    }

    fun showPaymentDialog(show: Boolean, debt: Debt? = null) {
        _selectedDebt.value = debt
        _isPaymentDialogVisible.value = show
    }

    fun saveDebt(
        id: String?,
        name: String,
        type: DebtType,
        totalPrincipalMinor: Long,
        remainingMinor: Long,
        ratePercent: Double,
        minimumPaymentMinor: Long
    ) {
        viewModelScope.launch {
            val d = Debt(
                id = id ?: UUID.randomUUID().toString(),
                name = name,
                type = type,
                totalPrincipal = Money(totalPrincipalMinor, uiState.value.baseCurrency),
                remainingBalance = Money(remainingMinor, uiState.value.baseCurrency),
                interestRatePercent = ratePercent,
                minimumPayment = Money(minimumPaymentMinor, uiState.value.baseCurrency),
                nextPaymentDate = System.currentTimeMillis() + (30L * 86_400_000L)
            )
            debtRepository.saveDebt(d)
            _isAddEditDialogVisible.value = false
            _selectedDebt.value = null
        }
    }

    fun makePayment(debtId: String, amountMinor: Long) {
        viewModelScope.launch {
            debtRepository.makePayment(debtId, Money(amountMinor, uiState.value.baseCurrency))
            _isPaymentDialogVisible.value = false
            _selectedDebt.value = null
        }
    }

    fun deleteDebt(id: String) {
        viewModelScope.launch {
            debtRepository.deleteDebt(id)
            _isAddEditDialogVisible.value = false
            _selectedDebt.value = null
        }
    }
}

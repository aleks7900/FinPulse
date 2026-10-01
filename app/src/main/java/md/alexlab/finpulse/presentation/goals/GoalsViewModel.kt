package md.alexlab.finpulse.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.FinancialGoal
import md.alexlab.finpulse.domain.repository.GoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class GoalsUiState(
    val goals: List<FinancialGoal> = emptyList(),
    val totalSaved: Money = Money.zero(),
    val totalTarget: Money = Money.zero(),
    val hideBalances: Boolean = false,
    val baseCurrency: String = "USD",
    val isAddEditDialogVisible: Boolean = false,
    val isContributeDialogVisible: Boolean = false,
    val selectedGoal: FinancialGoal? = null
)

class GoalsViewModel(
    private val goalRepository: GoalRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _isAddEditDialogVisible = MutableStateFlow(false)
    private val _isContributeDialogVisible = MutableStateFlow(false)
    private val _selectedGoal = MutableStateFlow<FinancialGoal?>(null)

    val uiState: StateFlow<GoalsUiState> = combine(
        goalRepository.getAllGoalsFlow(),
        userPreferencesDataStore.userPreferencesFlow,
        _isAddEditDialogVisible,
        _isContributeDialogVisible,
        _selectedGoal
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val goals = params[0] as List<FinancialGoal>
        val userPrefs = params[1] as md.alexlab.finpulse.core.datastore.UserPreferences
        val isAddVisible = params[2] as Boolean
        val isContributeVisible = params[3] as Boolean
        val selGoal = params[4] as FinancialGoal?

        val currency = userPrefs.baseCurrencyCode
        val totalSavedMinor = goals.sumOf { it.currentAmount.amountMinor }
        val totalTargetMinor = goals.sumOf { it.targetAmount.amountMinor }

        GoalsUiState(
            goals = goals,
            totalSaved = Money(totalSavedMinor, currency),
            totalTarget = Money(totalTargetMinor, currency),
            hideBalances = userPrefs.hideBalances,
            baseCurrency = currency,
            isAddEditDialogVisible = isAddVisible,
            isContributeDialogVisible = isContributeVisible,
            selectedGoal = selGoal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsUiState()
    )

    fun showAddEditDialog(show: Boolean, goal: FinancialGoal? = null) {
        _selectedGoal.value = goal
        _isAddEditDialogVisible.value = show
    }

    fun showContributeDialog(show: Boolean, goal: FinancialGoal? = null) {
        _selectedGoal.value = goal
        _isContributeDialogVisible.value = show
    }

    fun saveGoal(
        id: String?,
        title: String,
        targetMinor: Long,
        currentMinor: Long,
        targetDate: Long
    ) {
        viewModelScope.launch {
            val g = FinancialGoal(
                id = id ?: UUID.randomUUID().toString(),
                title = title,
                targetAmount = Money(targetMinor, uiState.value.baseCurrency),
                currentAmount = Money(currentMinor, uiState.value.baseCurrency),
                targetDate = targetDate,
                isCompleted = currentMinor >= targetMinor
            )
            goalRepository.saveGoal(g)
            _isAddEditDialogVisible.value = false
            _selectedGoal.value = null
        }
    }

    fun contributeToGoal(goalId: String, amountMinor: Long) {
        viewModelScope.launch {
            goalRepository.contributeToGoal(goalId, Money(amountMinor, uiState.value.baseCurrency))
            _isContributeDialogVisible.value = false
            _selectedGoal.value = null
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            goalRepository.deleteGoal(id)
            _isAddEditDialogVisible.value = false
            _selectedGoal.value = null
        }
    }
}

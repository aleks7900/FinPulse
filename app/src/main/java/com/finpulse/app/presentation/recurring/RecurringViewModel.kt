package com.finpulse.app.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.PaymentFrequency
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class RecurringUiState(
    val recurringList: List<RecurringTransaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val totalMonthlyCost: Money = Money.zero(),
    val totalAnnualCost: Money = Money.zero(),
    val hideBalances: Boolean = false,
    val baseCurrency: String = "USD",
    val isAddEditDialogVisible: Boolean = false,
    val editingRecurring: RecurringTransaction? = null
)

class RecurringViewModel(
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _isAddEditDialogVisible = MutableStateFlow(false)
    private val _editingRecurring = MutableStateFlow<RecurringTransaction?>(null)

    val uiState: StateFlow<RecurringUiState> = combine(
        recurringRepository.getAllRecurringFlow(),
        accountRepository.getActiveAccountsFlow(),
        categoryRepository.getAllCategoriesFlow(),
        userPreferencesDataStore.userPreferencesFlow,
        _isAddEditDialogVisible,
        _editingRecurring
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val list = params[0] as List<RecurringTransaction>
        @Suppress("UNCHECKED_CAST")
        val accounts = params[1] as List<Account>
        @Suppress("UNCHECKED_CAST")
        val categories = params[2] as List<Category>
        val userPrefs = params[3] as com.finpulse.app.core.datastore.UserPreferences
        val isAddVisible = params[4] as Boolean
        val editingR = params[5] as RecurringTransaction?

        val active = list.filter { it.isActive }
        val monthlyMinor = active.sumOf { it.calculateMonthlyCost().amountMinor }
        val currency = userPrefs.baseCurrencyCode

        RecurringUiState(
            recurringList = list,
            accounts = accounts,
            categories = categories,
            totalMonthlyCost = Money(monthlyMinor, currency),
            totalAnnualCost = Money(monthlyMinor * 12L, currency),
            hideBalances = userPrefs.hideBalances,
            baseCurrency = currency,
            isAddEditDialogVisible = isAddVisible,
            editingRecurring = editingR
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringUiState()
    )

    fun showAddEditDialog(show: Boolean, recurring: RecurringTransaction? = null) {
        _editingRecurring.value = recurring
        _isAddEditDialogVisible.value = show
    }

    fun saveRecurring(
        id: String?,
        title: String,
        amountMinor: Long,
        accountId: String,
        categoryId: String,
        frequency: PaymentFrequency,
        nextDueDate: Long,
        isSubscription: Boolean
    ) {
        viewModelScope.launch {
            val r = RecurringTransaction(
                id = id ?: UUID.randomUUID().toString(),
                title = title,
                amount = Money(amountMinor, uiState.value.baseCurrency),
                accountId = accountId,
                categoryId = categoryId,
                frequency = frequency,
                nextDueDate = nextDueDate,
                isSubscription = isSubscription
            )
            recurringRepository.saveRecurring(r)
            _isAddEditDialogVisible.value = false
            _editingRecurring.value = null
        }
    }

    fun toggleActive(recurring: RecurringTransaction) {
        viewModelScope.launch {
            recurringRepository.saveRecurring(recurring.copy(isActive = !recurring.isActive))
        }
    }

    fun deleteRecurring(id: String) {
        viewModelScope.launch {
            recurringRepository.deleteRecurring(id)
            _isAddEditDialogVisible.value = false
            _editingRecurring.value = null
        }
    }
}

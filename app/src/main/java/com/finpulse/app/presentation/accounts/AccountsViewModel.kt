package com.finpulse.app.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val totalBalance: Money = Money.zero(),
    val hideBalances: Boolean = false,
    val baseCurrency: String = "USD",
    val isAddEditDialogVisible: Boolean = false,
    val isTransferDialogVisible: Boolean = false,
    val editingAccount: Account? = null
)

class AccountsViewModel(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _isAddEditDialogVisible = MutableStateFlow(false)
    private val _isTransferDialogVisible = MutableStateFlow(false)
    private val _editingAccount = MutableStateFlow<Account?>(null)

    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.getAllAccountsFlow(),
        userPreferencesDataStore.userPreferencesFlow,
        _isAddEditDialogVisible,
        _isTransferDialogVisible,
        _editingAccount
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val accounts = params[0] as List<Account>
        val userPrefs = params[1] as com.finpulse.app.core.datastore.UserPreferences
        val isAddVisible = params[2] as Boolean
        val isTransferVisible = params[3] as Boolean
        val editingAcc = params[4] as Account?

        val active = accounts.filter { !it.isArchived }
        val totalMinor = active.sumOf { it.balance.amountMinor }
        val total = Money(totalMinor, userPrefs.baseCurrencyCode)

        AccountsUiState(
            accounts = accounts,
            totalBalance = total,
            hideBalances = userPrefs.hideBalances,
            baseCurrency = userPrefs.baseCurrencyCode,
            isAddEditDialogVisible = isAddVisible,
            isTransferDialogVisible = isTransferVisible,
            editingAccount = editingAcc
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsUiState()
    )

    fun showAddEditDialog(show: Boolean, account: Account? = null) {
        _editingAccount.value = account
        _isAddEditDialogVisible.value = show
    }

    fun showTransferDialog(show: Boolean) {
        _isTransferDialogVisible.value = show
    }

    fun saveAccount(
        id: String?,
        name: String,
        type: AccountType,
        balanceMinor: Long,
        institution: String?,
        colorHex: Long
    ) {
        viewModelScope.launch {
            val currency = uiState.value.baseCurrency
            val acc = Account(
                id = id ?: UUID.randomUUID().toString(),
                name = name,
                type = type,
                balance = Money(balanceMinor, currency),
                availableBalance = Money(balanceMinor, currency),
                institution = institution?.takeIf { it.isNotBlank() },
                colorHex = colorHex
            )
            accountRepository.saveAccount(acc)
            _isAddEditDialogVisible.value = false
            _editingAccount.value = null
        }
    }

    fun transferFunds(
        sourceAccountId: String,
        destinationAccountId: String,
        amountMinor: Long,
        note: String
    ) {
        viewModelScope.launch {
            val currency = uiState.value.baseCurrency
            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                amount = Money(amountMinor, currency),
                type = TransactionType.TRANSFER,
                sourceAccountId = sourceAccountId,
                destinationAccountId = destinationAccountId,
                categoryId = "cat_transfer",
                description = note.ifBlank { "Account Transfer" }
            )
            transactionRepository.createTransaction(tx)
            _isTransferDialogVisible.value = false
        }
    }

    fun archiveAccount(id: String, archive: Boolean) {
        viewModelScope.launch {
            accountRepository.setArchived(id, archive)
        }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch {
            accountRepository.deleteAccount(id)
            _isAddEditDialogVisible.value = false
            _editingAccount.value = null
        }
    }
}

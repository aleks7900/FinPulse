package md.alexlab.finpulse.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import md.alexlab.finpulse.core.datastore.UserPreferences
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.engine.CurrencyConverter
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.ExchangeRateProvider
import md.alexlab.finpulse.domain.repository.TransactionRepository
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
    val editingAccount: Account? = null,
    val editingAccountTransactionCount: Int = 0
)

class AccountsViewModel(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val currencyConverter: CurrencyConverter,
    private val exchangeRateProvider: ExchangeRateProvider
) : ViewModel() {

    private val _isAddEditDialogVisible = MutableStateFlow(false)
    private val _isTransferDialogVisible = MutableStateFlow(false)
    private val _editingAccount = MutableStateFlow<Account?>(null)
    private val _editingAccountTxCount = MutableStateFlow(0)

    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.getAllAccountsFlow(),
        userPreferencesDataStore.userPreferencesFlow,
        exchangeRateProvider.getAllRatesFlow(),
        _isAddEditDialogVisible,
        _isTransferDialogVisible,
        _editingAccount,
        _editingAccountTxCount
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val accounts = params[0] as List<Account>
        val userPrefs = params[1] as UserPreferences
        // params[2] is List<ExchangeRate>, triggers re-computation on rate updates
        val isAddVisible = params[3] as Boolean
        val isTransferVisible = params[4] as Boolean
        val editingAcc = params[5] as Account?
        val txCount = params[6] as Int

        val active = accounts.filter { !it.isArchived }
        val total = currencyConverter.sumIn(active.map { it.balance }, userPrefs.baseCurrencyCode)

        AccountsUiState(
            accounts = accounts,
            totalBalance = total,
            hideBalances = userPrefs.hideBalances,
            baseCurrency = userPrefs.baseCurrencyCode,
            isAddEditDialogVisible = isAddVisible,
            isTransferDialogVisible = isTransferVisible,
            editingAccount = editingAcc,
            editingAccountTransactionCount = txCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsUiState()
    )

    fun showAddEditDialog(show: Boolean, account: Account? = null) {
        _editingAccount.value = account
        _isAddEditDialogVisible.value = show
        if (show && account != null) {
            viewModelScope.launch {
                val count = transactionRepository.getTransactionCountForAccount(account.id)
                _editingAccountTxCount.value = count
            }
        } else {
            _editingAccountTxCount.value = 0
        }
    }

    fun showTransferDialog(show: Boolean) {
        _isTransferDialogVisible.value = show
    }

    fun saveAccount(
        id: String?,
        name: String,
        type: AccountType,
        balanceMinor: Long,
        currencyCode: String? = null,
        institution: String?,
        colorHex: Long
    ) {
        viewModelScope.launch {
            val existing = if (id != null) accountRepository.getAccountById(id) else null
            val txCount = if (id != null) transactionRepository.getTransactionCountForAccount(id) else 0

            // If account contains transactions, NEVER silently reinterpret or mutate historical currency
            val resolvedCurrency = if (existing != null && txCount > 0) {
                existing.balance.currencyCode
            } else {
                currencyCode?.uppercase() ?: existing?.balance?.currencyCode ?: uiState.value.baseCurrency
            }

            val acc = Account(
                id = id ?: UUID.randomUUID().toString(),
                name = name,
                type = type,
                balance = Money(balanceMinor, resolvedCurrency),
                availableBalance = Money(balanceMinor, resolvedCurrency),
                institution = institution?.takeIf { it.isNotBlank() },
                colorHex = colorHex
            )
            accountRepository.saveAccount(acc)
            _isAddEditDialogVisible.value = false
            _editingAccount.value = null
            _editingAccountTxCount.value = 0
        }
    }

    fun transferFunds(
        sourceAccountId: String,
        destinationAccountId: String,
        amountMinor: Long,
        destinationAmountMinor: Long? = null,
        exchangeRate: Double? = null,
        note: String = ""
    ) {
        viewModelScope.launch {
            val accounts = uiState.value.accounts
            val sourceAcc = accounts.find { it.id == sourceAccountId }
                ?: accountRepository.getAccountById(sourceAccountId)
            val destAcc = accounts.find { it.id == destinationAccountId }
                ?: accountRepository.getAccountById(destinationAccountId)

            val sourceCurrency = sourceAcc?.balance?.currencyCode ?: uiState.value.baseCurrency
            val destCurrency = destAcc?.balance?.currencyCode ?: sourceCurrency

            val sourceMoney = Money(amountMinor, sourceCurrency)
            val destMoney: Money = if (destinationAmountMinor != null && destinationAmountMinor > 0L) {
                Money(destinationAmountMinor, destCurrency)
            } else if (sourceCurrency.equals(destCurrency, ignoreCase = true)) {
                sourceMoney
            } else {
                currencyConverter.convert(sourceMoney, destCurrency, customRate = exchangeRate)
            }

            val effectiveRate = exchangeRate ?: if (!sourceCurrency.equals(destCurrency, ignoreCase = true)) {
                currencyConverter.computeTransferRate(sourceMoney, destMoney)
            } else null

            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                amount = sourceMoney,
                destinationAmount = destMoney,
                exchangeRate = effectiveRate,
                exchangeRateDate = if (effectiveRate != null) System.currentTimeMillis() else null,
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

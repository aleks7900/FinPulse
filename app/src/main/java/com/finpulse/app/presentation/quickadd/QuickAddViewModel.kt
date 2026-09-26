package com.finpulse.app.presentation.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.usecase.transaction.CreateTransactionResult
import com.finpulse.app.domain.usecase.transaction.CreateTransactionUseCase
import com.finpulse.app.domain.usecase.transaction.GetQuickAddSuggestionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class QuickAddDateChoice(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    CUSTOM("Custom")
}

data class QuickAddUiState(
    val amountInput: String = "0",
    val amountMinor: Long = 0L,
    val currencyCode: String = "USD",
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val selectedAccountId: String? = null,
    val selectedDestinationAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val merchant: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val dateChoice: QuickAddDateChoice = QuickAddDateChoice.TODAY,
    val selectedTimestamp: Long = System.currentTimeMillis(),
    val frequentCategories: List<Category> = emptyList(),
    val frequentMerchants: List<String> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val lastSavedTransaction: Transaction? = null,
    val isEditing: Boolean = false,
    val editingTransactionId: String? = null
)

class QuickAddViewModel(
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val suggestionsUseCase: GetQuickAddSuggestionsUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _amountInput = MutableStateFlow("0")
    private val _selectedType = MutableStateFlow(TransactionType.EXPENSE)
    private val _selectedAccountId = MutableStateFlow<String?>(null)
    private val _selectedDestinationAccountId = MutableStateFlow<String?>(null)
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _merchant = MutableStateFlow("")
    private val _description = MutableStateFlow("")
    private val _tags = MutableStateFlow<List<String>>(emptyList())
    private val _dateChoice = MutableStateFlow(QuickAddDateChoice.TODAY)
    private val _selectedTimestamp = MutableStateFlow(System.currentTimeMillis())
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isSaving = MutableStateFlow(false)
    private val _lastSavedTransaction = MutableStateFlow<Transaction?>(null)
    private val _editingTransactionId = MutableStateFlow<String?>(null)

    init {
        // Preload defaults
        viewModelScope.launch {
            suggestionsUseCase.getSuggestionsFlow(_selectedType.value).collect { suggestions ->
                if (_selectedAccountId.value == null) {
                    _selectedAccountId.value = suggestions.defaultAccountId
                }
                if (_selectedCategoryId.value == null) {
                    _selectedCategoryId.value = suggestions.defaultCategoryId
                }
            }
        }
    }

    val uiState: StateFlow<QuickAddUiState> = combine(
        combine(
            _amountInput,
            _selectedType,
            _selectedAccountId,
            _selectedDestinationAccountId,
            _selectedCategoryId
        ) { amount, type, sourceId, destId, catId ->
            AmountAndEntities(amount, type, sourceId, destId, catId)
        },
        combine(
            _merchant,
            _description,
            _tags,
            _dateChoice,
            _selectedTimestamp
        ) { merch, desc, tags, dateCh, time ->
            DetailsAndDate(merch, desc, tags, dateCh, time)
        },
        combine(
            accountRepository.getActiveAccountsFlow(),
            categoryRepository.getAllCategoriesFlow(),
            userPreferencesDataStore.userPreferencesFlow
        ) { accounts, categories, prefs ->
            AccountsAndCategories(accounts, categories, prefs)
        },
        suggestionsUseCase.getSuggestionsFlow(_selectedType.value),
        combine(_errorMessage, _isSaving, _lastSavedTransaction, _editingTransactionId) { err, saving, lastTx, editId ->
            StatusAndFeedback(err, saving, lastTx, editId)
        }
    ) { ae, dd, ac, suggestions, status ->
        val amountMinor = parseAmountToMinor(ae.amountInput)
        val selectedAccount = ac.accounts.find { it.id == ae.sourceId }
        val currency = selectedAccount?.balance?.currencyCode ?: ac.userPrefs.baseCurrencyCode

        val catType = when (ae.type) {
            TransactionType.INCOME, TransactionType.REFUND -> CategoryType.INCOME
            else -> CategoryType.EXPENSE
        }
        val relevantCategories = ac.categories.filter { it.type == catType }

        QuickAddUiState(
            amountInput = ae.amountInput,
            amountMinor = amountMinor,
            currencyCode = currency,
            selectedType = ae.type,
            selectedAccountId = ae.sourceId ?: suggestions.defaultAccountId ?: ac.accounts.firstOrNull()?.id,
            selectedDestinationAccountId = ae.destId ?: ac.accounts.getOrNull(1)?.id,
            selectedCategoryId = ae.catId ?: suggestions.defaultCategoryId ?: relevantCategories.firstOrNull()?.id,
            merchant = dd.merchant,
            description = dd.description,
            tags = dd.tags,
            dateChoice = dd.dateChoice,
            selectedTimestamp = dd.timestamp,
            frequentCategories = suggestions.frequentCategories,
            frequentMerchants = suggestions.frequentMerchants,
            accounts = ac.accounts,
            categories = relevantCategories,
            errorMessage = status.errorMessage,
            isSaving = status.isSaving,
            lastSavedTransaction = status.lastSavedTransaction,
            isEditing = status.editingTransactionId != null,
            editingTransactionId = status.editingTransactionId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuickAddUiState()
    )

    fun onNumpadDigit(digit: Char) {
        val current = _amountInput.value
        _errorMessage.value = null

        val updated = when (digit) {
            '⌫' -> {
                if (current.length <= 1) "0" else current.dropLast(1)
            }
            'C' -> "0"
            '.' -> {
                if (current.contains('.')) current else "$current."
            }
            else -> {
                if (current == "0") {
                    digit.toString()
                } else {
                    // Check if already has 2 decimals
                    val dotIdx = current.indexOf('.')
                    if (dotIdx != -1 && current.length - dotIdx > 2) {
                        current
                    } else {
                        current + digit
                    }
                }
            }
        }
        _amountInput.value = updated
    }

    fun onQuickAmountAdd(majorAmount: Int) {
        val currentMinor = parseAmountToMinor(_amountInput.value)
        val newMinor = currentMinor + (majorAmount * 100L)
        val bigDecimal = Money(newMinor).amountBigDecimal
        _amountInput.value = bigDecimal.toPlainString()
        _errorMessage.value = null
    }

    fun onTypeSelected(type: TransactionType) {
        _selectedType.value = type
        _errorMessage.value = null
        // Reset category selection so it defaults to top category for that type
        _selectedCategoryId.value = null
    }

    fun onAccountSelected(accountId: String) {
        _selectedAccountId.value = accountId
        _errorMessage.value = null
    }

    fun onDestinationAccountSelected(accountId: String) {
        _selectedDestinationAccountId.value = accountId
        _errorMessage.value = null
    }

    fun onCategorySelected(categoryId: String) {
        _selectedCategoryId.value = categoryId
        _errorMessage.value = null
    }

    fun onMerchantSelected(merchant: String) {
        _merchant.value = merchant
        _errorMessage.value = null
        // Predict historical category for this merchant
        viewModelScope.launch {
            val suggestedCategory = suggestionsUseCase.getSuggestedCategoryForMerchant(merchant)
            if (suggestedCategory != null) {
                _selectedCategoryId.value = suggestedCategory
            }
        }
    }

    fun onMerchantTextChange(merchant: String) {
        _merchant.value = merchant
        _errorMessage.value = null
    }

    fun onDescriptionTextChange(description: String) {
        _description.value = description
    }

    fun onDateChoiceSelected(choice: QuickAddDateChoice) {
        _dateChoice.value = choice
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)
        when (choice) {
            QuickAddDateChoice.TODAY -> {
                _selectedTimestamp.value = System.currentTimeMillis()
            }
            QuickAddDateChoice.YESTERDAY -> {
                val yesterday = now.minusDays(1)
                _selectedTimestamp.value = yesterday.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
            }
            QuickAddDateChoice.CUSTOM -> {
                // Keep current or prompt picker
            }
        }
    }

    fun onCustomDateEpoch(epochMillis: Long) {
        _dateChoice.value = QuickAddDateChoice.CUSTOM
        _selectedTimestamp.value = epochMillis
    }

    fun loadForEdit(transaction: Transaction) {
        _editingTransactionId.value = transaction.id
        _amountInput.value = transaction.amount.amountBigDecimal.toPlainString()
        _selectedType.value = transaction.type
        _selectedAccountId.value = transaction.sourceAccountId
        _selectedDestinationAccountId.value = transaction.destinationAccountId
        _selectedCategoryId.value = transaction.categoryId
        _merchant.value = transaction.merchant ?: ""
        _description.value = transaction.description
        _tags.value = transaction.tags
        _selectedTimestamp.value = transaction.timestamp
        _dateChoice.value = QuickAddDateChoice.CUSTOM
        _errorMessage.value = null
    }

    fun reset() {
        _editingTransactionId.value = null
        _amountInput.value = "0"
        _merchant.value = ""
        _description.value = ""
        _tags.value = emptyList()
        _errorMessage.value = null
        _dateChoice.value = QuickAddDateChoice.TODAY
        _selectedTimestamp.value = System.currentTimeMillis()
    }

    fun save(andAddAnother: Boolean, onSaved: (Transaction) -> Unit) {
        val state = uiState.value
        if (state.amountMinor <= 0L) {
            _errorMessage.value = "Enter an amount greater than zero"
            return
        }
        val sourceAccId = state.selectedAccountId
        if (sourceAccId.isNullOrBlank()) {
            _errorMessage.value = "Select an account"
            return
        }
        val catId = state.selectedCategoryId
        if (catId.isNullOrBlank()) {
            _errorMessage.value = "Select a category"
            return
        }

        _isSaving.value = true
        _errorMessage.value = null

        val editId = _editingTransactionId.value

        viewModelScope.launch {
            val result = createTransactionUseCase(
                id = editId,
                amountMinor = state.amountMinor,
                currencyCode = state.currencyCode,
                type = state.selectedType,
                sourceAccountId = sourceAccId,
                destinationAccountId = state.selectedDestinationAccountId,
                categoryId = catId,
                merchant = state.merchant,
                description = state.description,
                tags = state.tags,
                timestamp = state.selectedTimestamp
            )

            _isSaving.value = false

            when (result) {
                is CreateTransactionResult.Success -> {
                    val savedTx = result.transaction
                    _lastSavedTransaction.value = savedTx
                    _editingTransactionId.value = null

                    // Persist user defaults for next time
                    userPreferencesDataStore.setLastUsedTransactionDefaults(
                        accountId = sourceAccId,
                        categoryId = catId,
                        type = state.selectedType.name
                    )

                    onSaved(savedTx)

                    if (andAddAnother) {
                        // Quick reset for rapid consecutive entries
                        _amountInput.value = "0"
                        _merchant.value = ""
                        _description.value = ""
                        _tags.value = emptyList()
                        // Keep account, category, and date choice active
                    }
                }
                is CreateTransactionResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    private fun parseAmountToMinor(input: String): Long {
        val clean = input.trim()
        val dbl = clean.toDoubleOrNull() ?: 0.0
        return (dbl * 100.0).toLong()
    }
}

private data class AmountAndEntities(
    val amountInput: String,
    val type: TransactionType,
    val sourceId: String?,
    val destId: String?,
    val catId: String?
)

private data class DetailsAndDate(
    val merchant: String,
    val description: String,
    val tags: List<String>,
    val dateChoice: QuickAddDateChoice,
    val timestamp: Long
)

private data class AccountsAndCategories(
    val accounts: List<Account>,
    val categories: List<Category>,
    val userPrefs: com.finpulse.app.core.datastore.UserPreferences
)

private data class StatusAndFeedback(
    val errorMessage: String?,
    val isSaving: Boolean,
    val lastSavedTransaction: Transaction?,
    val editingTransactionId: String?
)

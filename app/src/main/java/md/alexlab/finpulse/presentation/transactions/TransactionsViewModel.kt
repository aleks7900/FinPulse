package md.alexlab.finpulse.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.DateRangePreset
import md.alexlab.finpulse.domain.model.SavedFilter
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionFilterParams
import md.alexlab.finpulse.domain.model.TransactionPreset
import md.alexlab.finpulse.domain.model.TransactionPresets
import md.alexlab.finpulse.domain.model.TransactionSort
import md.alexlab.finpulse.domain.model.TransactionStatusFilter
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import md.alexlab.finpulse.domain.repository.SavedFilterRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val savedFilters: List<SavedFilter> = emptyList(),
    val filterParams: TransactionFilterParams = TransactionFilterParams(),
    val activePresetId: String? = null,
    val activeSavedFilterId: String? = null,
    val hideBalances: Boolean = false,
    val baseCurrency: String = "USD",
    val unreviewedCount: Int = 0,
    val isFilterSheetVisible: Boolean = false,
    val isSaveViewDialogVisible: Boolean = false,
    val isAddEditDialogVisible: Boolean = false,
    val editingTransaction: Transaction? = null
) {
    // Backward-compatible convenience properties
    val searchQuery: String get() = filterParams.query
    val selectedTypeFilter: TransactionType? get() = filterParams.type
    val selectedAccountFilter: String? get() = filterParams.accountId
    val selectedCategoryFilter: String? get() = filterParams.categoryId
    val sortOrder: TransactionSort get() = filterParams.sortOrder
    val filterOnlyUnreviewed: Boolean get() = filterParams.status == TransactionStatusFilter.NEEDS_REVIEW
    val activeFilterCount: Int get() = filterParams.activeFilterCount
}

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val savedFilterRepository: SavedFilterRepository? = null
) : ViewModel() {

    private val _filterParams = MutableStateFlow(TransactionFilterParams())
    private val _activePresetId = MutableStateFlow<String?>(null)
    private val _activeSavedFilterId = MutableStateFlow<String?>(null)
    private val _isFilterSheetVisible = MutableStateFlow(false)
    private val _isSaveViewDialogVisible = MutableStateFlow(false)
    private val _isAddEditDialogVisible = MutableStateFlow(false)
    private val _editingTransaction = MutableStateFlow<Transaction?>(null)

    private val _filteredTransactionsFlow = _filterParams
        .flatMapLatest { params ->
            transactionRepository.filterTransactionsFlow(params)
        }

    private val _savedFiltersFlow = savedFilterRepository?.getAllSavedFiltersFlow() ?: flowOf(emptyList())

    val uiState: StateFlow<TransactionsUiState> = combine(
        _filteredTransactionsFlow,
        accountRepository.getActiveAccountsFlow(),
        categoryRepository.getAllCategoriesFlow(),
        _savedFiltersFlow,
        userPreferencesDataStore.userPreferencesFlow,
        _filterParams,
        _activePresetId,
        _activeSavedFilterId,
        transactionRepository.getUnreviewedCountFlow(),
        combine(_isFilterSheetVisible, _isSaveViewDialogVisible, _isAddEditDialogVisible, _editingTransaction) { f, s, a, e ->
            DialogStates(f, s, a, e)
        }
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val transactions = params[0] as List<Transaction>
        @Suppress("UNCHECKED_CAST")
        val accounts = params[1] as List<Account>
        @Suppress("UNCHECKED_CAST")
        val categories = params[2] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val savedFilters = params[3] as List<SavedFilter>
        val userPrefs = params[4] as md.alexlab.finpulse.core.datastore.UserPreferences
        val filterParams = params[5] as TransactionFilterParams
        val activePreset = params[6] as String?
        val activeSaved = params[7] as String?
        val unreviewedCount = params[8] as Int
        val dialogs = params[9] as DialogStates

        TransactionsUiState(
            transactions = transactions,
            accounts = accounts,
            categories = categories,
            savedFilters = savedFilters,
            filterParams = filterParams,
            activePresetId = activePreset,
            activeSavedFilterId = activeSaved,
            hideBalances = userPrefs.hideBalances,
            baseCurrency = userPrefs.baseCurrencyCode,
            unreviewedCount = unreviewedCount,
            isFilterSheetVisible = dialogs.isFilterVisible,
            isSaveViewDialogVisible = dialogs.isSaveViewVisible,
            isAddEditDialogVisible = dialogs.isAddEditVisible,
            editingTransaction = dialogs.editingTransaction
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    private data class DialogStates(
        val isFilterVisible: Boolean,
        val isSaveViewVisible: Boolean,
        val isAddEditVisible: Boolean,
        val editingTransaction: Transaction?
    )

    fun onSearchQueryChange(query: String) {
        _filterParams.value = _filterParams.value.copy(query = query)
    }

    fun onTypeFilterChange(type: TransactionType?) {
        _filterParams.value = _filterParams.value.copy(type = type)
        _activePresetId.value = null
        _activeSavedFilterId.value = null
    }

    fun onAccountFilterChange(accountId: String?) {
        _filterParams.value = _filterParams.value.copy(accountId = accountId)
        _activePresetId.value = null
        _activeSavedFilterId.value = null
    }

    fun onCategoryFilterChange(categoryId: String?) {
        _filterParams.value = _filterParams.value.copy(categoryId = categoryId)
        _activePresetId.value = null
        _activeSavedFilterId.value = null
    }

    fun onDateRangePresetChange(preset: DateRangePreset, customStart: Long? = null, customEnd: Long? = null) {
        _filterParams.value = _filterParams.value.copy(
            dateRangePreset = preset,
            customStartDate = customStart,
            customEndDate = customEnd
        )
        _activePresetId.value = null
        _activeSavedFilterId.value = null
    }

    fun onAmountRangeChange(minMinor: Long?, maxMinor: Long?) {
        _filterParams.value = _filterParams.value.copy(
            minAmountMinor = minMinor,
            maxAmountMinor = maxMinor
        )
        _activePresetId.value = null
        _activeSavedFilterId.value = null
    }

    fun onCurrencyFilterChange(currencyCode: String?) {
        _filterParams.value = _filterParams.value.copy(currencyCode = currencyCode)
        _activePresetId.value = null
        _activeSavedFilterId.value = null
    }

    fun onStatusFilterChange(status: TransactionStatusFilter) {
        _filterParams.value = _filterParams.value.copy(status = status)
        _activePresetId.value = null
        _activeSavedFilterId.value = null
    }

    fun onSortOrderChange(sort: TransactionSort) {
        _filterParams.value = _filterParams.value.copy(sortOrder = sort)
    }

    fun toggleFilterOnlyUnreviewed() {
        val currentStatus = _filterParams.value.status
        if (currentStatus == TransactionStatusFilter.NEEDS_REVIEW) {
            _filterParams.value = _filterParams.value.copy(status = TransactionStatusFilter.ALL)
            _activePresetId.value = null
        } else {
            _filterParams.value = _filterParams.value.copy(status = TransactionStatusFilter.NEEDS_REVIEW)
            _activePresetId.value = TransactionPresets.UNCATEGORIZED.id
        }
        _activeSavedFilterId.value = null
    }

    fun setFilterOnlyUnreviewed(onlyUnreviewed: Boolean) {
        if (onlyUnreviewed) {
            _filterParams.value = _filterParams.value.copy(status = TransactionStatusFilter.NEEDS_REVIEW)
            _activePresetId.value = TransactionPresets.UNCATEGORIZED.id
        } else {
            if (_filterParams.value.status == TransactionStatusFilter.NEEDS_REVIEW) {
                _filterParams.value = _filterParams.value.copy(status = TransactionStatusFilter.ALL)
            }
            if (_activePresetId.value == TransactionPresets.UNCATEGORIZED.id) {
                _activePresetId.value = null
            }
        }
        _activeSavedFilterId.value = null
    }

    fun onSelectPreset(preset: TransactionPreset) {
        val currentQuery = _filterParams.value.query
        if (_activePresetId.value == preset.id) {
            // Deselect preset -> reset to default, preserving active search query
            _filterParams.value = TransactionFilterParams(query = currentQuery)
            _activePresetId.value = null
        } else {
            // Select preset -> apply preset filters, preserving active search query
            _filterParams.value = preset.params.copy(query = currentQuery)
            _activePresetId.value = preset.id
            _activeSavedFilterId.value = null
        }
    }

    fun onSelectSavedFilter(savedFilter: SavedFilter) {
        val currentQuery = _filterParams.value.query
        if (_activeSavedFilterId.value == savedFilter.id) {
            // Deselect saved filter -> reset to default, preserving active search query
            _filterParams.value = TransactionFilterParams(query = currentQuery)
            _activeSavedFilterId.value = null
        } else {
            val resolvedQuery = savedFilter.params.query.ifBlank { currentQuery }
            _filterParams.value = savedFilter.params.copy(query = resolvedQuery)
            _activeSavedFilterId.value = savedFilter.id
            _activePresetId.value = null
        }
    }

    fun onResetFilters() {
        _filterParams.value = TransactionFilterParams()
        _activePresetId.value = null
        _activeSavedFilterId.value = null
    }

    fun saveCurrentFilterAsView(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            val newView = SavedFilter(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                params = _filterParams.value
            )
            savedFilterRepository?.saveFilter(newView)
            _activeSavedFilterId.value = newView.id
            _isSaveViewDialogVisible.value = false
        }
    }

    fun deleteSavedFilter(id: String) {
        viewModelScope.launch {
            savedFilterRepository?.deleteFilter(id)
            if (_activeSavedFilterId.value == id) {
                _activeSavedFilterId.value = null
            }
        }
    }

    fun showFilterSheet(show: Boolean) {
        _isFilterSheetVisible.value = show
    }

    fun showSaveViewDialog(show: Boolean) {
        _isSaveViewDialogVisible.value = show
    }

    fun showAddEditDialog(show: Boolean, transaction: Transaction? = null) {
        _editingTransaction.value = transaction
        _isAddEditDialogVisible.value = show
    }

    fun saveTransaction(
        id: String?,
        amountMinor: Long,
        type: TransactionType,
        sourceAccountId: String,
        destinationAccountId: String?,
        categoryId: String,
        merchant: String?,
        description: String,
        tags: List<String>,
        notes: String?,
        timestamp: Long
    ) {
        viewModelScope.launch {
            val currency = uiState.value.accounts.find { it.id == sourceAccountId }?.balance?.currencyCode
                ?: uiState.value.baseCurrency

            val tx = Transaction(
                id = id ?: UUID.randomUUID().toString(),
                amount = Money(amountMinor, currency),
                type = type,
                sourceAccountId = sourceAccountId,
                destinationAccountId = destinationAccountId,
                categoryId = categoryId,
                merchant = merchant?.takeIf { it.isNotBlank() },
                timestamp = timestamp,
                description = description,
                tags = tags,
                notes = notes?.takeIf { it.isNotBlank() }
            )

            if (id != null) {
                transactionRepository.updateTransaction(tx)
            } else {
                transactionRepository.createTransaction(tx)
            }
            _isAddEditDialogVisible.value = false
            _editingTransaction.value = null
        }
    }

    fun duplicateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val duplicate = transaction.copy(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis()
            )
            transactionRepository.createTransaction(duplicate)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
            if (_editingTransaction.value?.id == id) {
                _isAddEditDialogVisible.value = false
                _editingTransaction.value = null
            }
        }
    }
}

package com.finpulse.app.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class TransactionSort {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
}

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val selectedTypeFilter: TransactionType? = null,
    val selectedAccountFilter: String? = null,
    val selectedCategoryFilter: String? = null,
    val sortOrder: TransactionSort = TransactionSort.DATE_DESC,
    val hideBalances: Boolean = false,
    val baseCurrency: String = "USD",
    val unreviewedCount: Int = 0,
    val filterOnlyUnreviewed: Boolean = false,
    val isFilterSheetVisible: Boolean = false,
    val isAddEditDialogVisible: Boolean = false,
    val editingTransaction: Transaction? = null
)

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    private val _selectedAccountFilter = MutableStateFlow<String?>(null)
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _sortOrder = MutableStateFlow(TransactionSort.DATE_DESC)
    private val _filterOnlyUnreviewed = MutableStateFlow(false)
    private val _isFilterSheetVisible = MutableStateFlow(false)
    private val _isAddEditDialogVisible = MutableStateFlow(false)
    private val _editingTransaction = MutableStateFlow<Transaction?>(null)

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.getAllTransactionsFlow(),
        accountRepository.getActiveAccountsFlow(),
        categoryRepository.getAllCategoriesFlow(),
        userPreferencesDataStore.userPreferencesFlow,
        _searchQuery,
        _selectedTypeFilter,
        _selectedAccountFilter,
        _selectedCategoryFilter,
        _sortOrder,
        _filterOnlyUnreviewed,
        transactionRepository.getUnreviewedCountFlow(),
        combine(_isFilterSheetVisible, _isAddEditDialogVisible, _editingTransaction) { f, a, e -> Triple(f, a, e) }
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val allTx = params[0] as List<Transaction>
        @Suppress("UNCHECKED_CAST")
        val accounts = params[1] as List<Account>
        @Suppress("UNCHECKED_CAST")
        val categories = params[2] as List<Category>
        val userPrefs = params[3] as com.finpulse.app.core.datastore.UserPreferences
        val query = params[4] as String
        val typeFilter = params[5] as TransactionType?
        val accountFilter = params[6] as String?
        val categoryFilter = params[7] as String?
        val sort = params[8] as TransactionSort
        val onlyUnreviewed = params[9] as Boolean
        val unreviewedCount = params[10] as Int
        @Suppress("UNCHECKED_CAST")
        val triple = params[11] as Triple<Boolean, Boolean, Transaction?>
        val isFilterVisible = triple.first
        val isAddVisible = triple.second
        val editingTx = triple.third

        // Filter
        var filtered = allTx.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.description.contains(query, ignoreCase = true) ||
                    (tx.merchant?.contains(query, ignoreCase = true) == true) ||
                    (tx.notes?.contains(query, ignoreCase = true) == true) ||
                    tx.tags.any { it.contains(query, ignoreCase = true) }

            val matchesType = typeFilter == null || tx.type == typeFilter
            val matchesAccount = accountFilter == null || tx.sourceAccountId == accountFilter || tx.destinationAccountId == accountFilter
            val matchesCategory = categoryFilter == null || tx.categoryId == categoryFilter
            val matchesReview = !onlyUnreviewed || (!tx.isCategoryConfirmed || tx.categoryId == "cat_uncategorized")

            matchesQuery && matchesType && matchesAccount && matchesCategory && matchesReview
        }

        // Sort
        filtered = when (sort) {
            TransactionSort.DATE_DESC -> filtered.sortedByDescending { it.timestamp }
            TransactionSort.DATE_ASC -> filtered.sortedBy { it.timestamp }
            TransactionSort.AMOUNT_DESC -> filtered.sortedByDescending { it.amount.amountMinor }
            TransactionSort.AMOUNT_ASC -> filtered.sortedBy { it.amount.amountMinor }
        }

        TransactionsUiState(
            transactions = filtered,
            accounts = accounts,
            categories = categories,
            searchQuery = query,
            selectedTypeFilter = typeFilter,
            selectedAccountFilter = accountFilter,
            selectedCategoryFilter = categoryFilter,
            sortOrder = sort,
            hideBalances = userPrefs.hideBalances,
            baseCurrency = userPrefs.baseCurrencyCode,
            unreviewedCount = unreviewedCount,
            filterOnlyUnreviewed = onlyUnreviewed,
            isFilterSheetVisible = isFilterVisible,
            isAddEditDialogVisible = isAddVisible,
            editingTransaction = editingTx
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTypeFilterChange(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun onAccountFilterChange(accountId: String?) {
        _selectedAccountFilter.value = accountId
    }

    fun onCategoryFilterChange(categoryId: String?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun onSortOrderChange(sort: TransactionSort) {
        _sortOrder.value = sort
    }

    fun toggleFilterOnlyUnreviewed() {
        _filterOnlyUnreviewed.value = !_filterOnlyUnreviewed.value
    }

    fun setFilterOnlyUnreviewed(onlyUnreviewed: Boolean) {
        _filterOnlyUnreviewed.value = onlyUnreviewed
    }

    fun showFilterSheet(show: Boolean) {
        _isFilterSheetVisible.value = show
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

package com.finpulse.app.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.ReviewInboxItem
import com.finpulse.app.domain.model.ReviewInboxSummary
import com.finpulse.app.domain.model.ReviewItemType
import com.finpulse.app.domain.model.SafeBulkSuggestion
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.usecase.categorization.RecordCategoryCorrectionUseCase
import com.finpulse.app.domain.usecase.recurring.MarkOccurrencePaidUseCase
import com.finpulse.app.domain.usecase.recurring.MarkPaidResult
import com.finpulse.app.domain.usecase.review.BulkCategorizeTransactionsUseCase
import com.finpulse.app.domain.usecase.review.GetReviewInboxUseCase
import com.finpulse.app.domain.usecase.review.ResolveDuplicateTransactionUseCase
import com.finpulse.app.domain.usecase.review.UpdateTransactionDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ReviewInboxTab(val label: String) {
    ALL("All"),
    UNCATEGORIZED("Uncategorized"),
    DUPLICATES("Duplicates"),
    BILLS("Bills & Recurring"),
    WARNINGS("Warnings")
}

data class ReviewInboxUiState(
    val selectedTab: ReviewInboxTab = ReviewInboxTab.ALL,
    val summary: ReviewInboxSummary = ReviewInboxSummary(),
    val filteredItems: List<ReviewInboxItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedItemIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,

    // Dialogs
    val isCategoryPickerDialogVisible: Boolean = false,
    val categorizingItem: ReviewInboxItem? = null,

    val isAccountPickerDialogVisible: Boolean = false,
    val changingAccountItem: ReviewInboxItem? = null,

    val isEditMerchantDialogVisible: Boolean = false,
    val editingMerchantItem: ReviewInboxItem? = null,

    val isResolveDuplicateDialogVisible: Boolean = false,
    val resolvingDuplicateItem: ReviewInboxItem? = null,

    val isPayBillDialogVisible: Boolean = false,
    val payingBillItem: ReviewInboxItem? = null,

    val isBulkCategoryPickerVisible: Boolean = false,

    val message: String? = null,
    val isLoading: Boolean = true
)

class ReviewInboxViewModel(
    private val getReviewInboxUseCase: GetReviewInboxUseCase,
    private val recordCategoryCorrectionUseCase: RecordCategoryCorrectionUseCase,
    private val resolveDuplicateTransactionUseCase: ResolveDuplicateTransactionUseCase,
    private val bulkCategorizeTransactionsUseCase: BulkCategorizeTransactionsUseCase,
    private val updateTransactionDetailsUseCase: UpdateTransactionDetailsUseCase,
    private val markOccurrencePaidUseCase: MarkOccurrencePaidUseCase,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ReviewInboxTab.ALL)
    private val _selectedItemIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isSelectionMode = MutableStateFlow(false)

    // Dialog state flows
    private val _isCategoryPickerDialogVisible = MutableStateFlow(false)
    private val _categorizingItem = MutableStateFlow<ReviewInboxItem?>(null)

    private val _isAccountPickerDialogVisible = MutableStateFlow(false)
    private val _changingAccountItem = MutableStateFlow<ReviewInboxItem?>(null)

    private val _isEditMerchantDialogVisible = MutableStateFlow(false)
    private val _editingMerchantItem = MutableStateFlow<ReviewInboxItem?>(null)

    private val _isResolveDuplicateDialogVisible = MutableStateFlow(false)
    private val _resolvingDuplicateItem = MutableStateFlow<ReviewInboxItem?>(null)

    private val _isPayBillDialogVisible = MutableStateFlow(false)
    private val _payingBillItem = MutableStateFlow<ReviewInboxItem?>(null)

    private val _isBulkCategoryPickerVisible = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ReviewInboxUiState> = combine(
        combine(
            _selectedTab,
            getReviewInboxUseCase(),
            categoryRepository.getAllCategoriesFlow(),
            accountRepository.getActiveAccountsFlow()
        ) { tab, summary, categories, accounts ->
            MainData(tab, summary, categories, accounts)
        },
        combine(
            _selectedItemIds,
            _isSelectionMode,
            _isCategoryPickerDialogVisible,
            _categorizingItem,
            _isAccountPickerDialogVisible
        ) { selectedIds, isSelMode, isCatPick, catItem, isAccPick ->
            SelectionAndPickers(selectedIds, isSelMode, isCatPick, catItem, isAccPick)
        },
        combine(
            _changingAccountItem,
            _isEditMerchantDialogVisible,
            _editingMerchantItem,
            _isResolveDuplicateDialogVisible,
            _resolvingDuplicateItem
        ) { changeAccItem, isEditMerch, editMerchItem, isResolveDup, resolveDupItem ->
            DialogsOne(changeAccItem, isEditMerch, editMerchItem, isResolveDup, resolveDupItem)
        },
        combine(
            _isPayBillDialogVisible,
            _payingBillItem,
            _isBulkCategoryPickerVisible,
            _message
        ) { isPayBill, payBillItem, isBulkCat, msg ->
            DialogsTwo(isPayBill, payBillItem, isBulkCat, msg)
        }
    ) { md, sp, d1, d2 ->
        val filtered = when (md.tab) {
            ReviewInboxTab.ALL -> md.summary.items
            ReviewInboxTab.UNCATEGORIZED -> md.summary.items.filter {
                it.type == ReviewItemType.UNCATEGORIZED || it.type == ReviewItemType.IMPORTED_CONFIRMATION
            }
            ReviewInboxTab.DUPLICATES -> md.summary.items.filter {
                it.type == ReviewItemType.SUSPECTED_DUPLICATE
            }
            ReviewInboxTab.BILLS -> md.summary.items.filter {
                it.type == ReviewItemType.OVERDUE_BILL || it.type == ReviewItemType.FAILED_RECURRING
            }
            ReviewInboxTab.WARNINGS -> md.summary.items.filter {
                it.type == ReviewItemType.UNUSUAL_AMOUNT || it.type == ReviewItemType.MISSING_MERCHANT
            }
        }

        ReviewInboxUiState(
            selectedTab = md.tab,
            summary = md.summary,
            filteredItems = filtered,
            categories = md.categories,
            accounts = md.accounts,
            selectedItemIds = sp.selectedIds,
            isSelectionMode = sp.isSelMode,
            isCategoryPickerDialogVisible = sp.isCatPick,
            categorizingItem = sp.catItem,
            isAccountPickerDialogVisible = sp.isAccPick,
            changingAccountItem = d1.changeAccItem,
            isEditMerchantDialogVisible = d1.isEditMerch,
            editingMerchantItem = d1.editMerchItem,
            isResolveDuplicateDialogVisible = d1.isResolveDup,
            resolvingDuplicateItem = d1.resolveDupItem,
            isPayBillDialogVisible = d2.isPayBill,
            payingBillItem = d2.payBillItem,
            isBulkCategoryPickerVisible = d2.isBulkCat,
            message = d2.msg,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReviewInboxUiState()
    )

    fun onTabSelected(tab: ReviewInboxTab) {
        _selectedTab.value = tab
    }

    fun onToggleSelectMode() {
        val current = _isSelectionMode.value
        _isSelectionMode.value = !current
        if (current) {
            _selectedItemIds.value = emptySet()
        }
    }

    fun onToggleItemSelection(itemId: String) {
        val current = _selectedItemIds.value
        _selectedItemIds.value = if (current.contains(itemId)) {
            current - itemId
        } else {
            current + itemId
        }
    }

    fun onSelectAll(select: Boolean) {
        val currentItems = uiState.value.filteredItems
        _selectedItemIds.value = if (select) {
            currentItems.map { it.id }.toSet()
        } else {
            emptySet()
        }
    }

    // -------------------------------------------------------------
    // 1. Confirm
    // -------------------------------------------------------------
    fun onConfirmItem(item: ReviewInboxItem) {
        viewModelScope.launch {
            val tx = item.transaction
            if (tx != null) {
                val targetCategoryId = item.suggestedCategoryId ?: tx.categoryId
                recordCategoryCorrectionUseCase(
                    transactionId = tx.id,
                    newCategoryId = targetCategoryId,
                    matchedRuleId = tx.matchedRuleId,
                    confidence = item.suggestedCategoryConfidence.takeIf { it > 0f } ?: 1.0f
                )
                // If it was missing merchant or imported, also dismiss the item flag
                userPreferencesDataStore.dismissInboxItem(item.id)
                _message.value = "Confirmed ${item.title}"
            } else {
                userPreferencesDataStore.dismissInboxItem(item.id)
                _message.value = "Confirmed"
            }
        }
    }

    // -------------------------------------------------------------
    // 2. Categorize
    // -------------------------------------------------------------
    fun onOpenCategoryPicker(item: ReviewInboxItem) {
        _categorizingItem.value = item
        _isCategoryPickerDialogVisible.value = true
    }

    fun onCategorizeItem(item: ReviewInboxItem, categoryId: String) {
        val tx = item.transaction ?: return
        viewModelScope.launch {
            recordCategoryCorrectionUseCase(
                transactionId = tx.id,
                newCategoryId = categoryId,
                confidence = 1.0f
            )
            userPreferencesDataStore.dismissInboxItem(item.id)
            _isCategoryPickerDialogVisible.value = false
            _categorizingItem.value = null
            _message.value = "Categorized & updated learning"
        }
    }

    // -------------------------------------------------------------
    // 3. Change Account
    // -------------------------------------------------------------
    fun onOpenAccountPicker(item: ReviewInboxItem) {
        _changingAccountItem.value = item
        _isAccountPickerDialogVisible.value = true
    }

    fun onChangeAccount(item: ReviewInboxItem, newAccountId: String) {
        val tx = item.transaction ?: return
        viewModelScope.launch {
            updateTransactionDetailsUseCase.changeAccount(tx.id, newAccountId)
            _isAccountPickerDialogVisible.value = false
            _changingAccountItem.value = null
            _message.value = "Account updated"
        }
    }

    // -------------------------------------------------------------
    // 4. Edit Merchant
    // -------------------------------------------------------------
    fun onOpenEditMerchant(item: ReviewInboxItem) {
        _editingMerchantItem.value = item
        _isEditMerchantDialogVisible.value = true
    }

    fun onSaveMerchant(item: ReviewInboxItem, newMerchant: String) {
        val tx = item.transaction ?: return
        viewModelScope.launch {
            updateTransactionDetailsUseCase.editMerchant(tx.id, newMerchant)
            userPreferencesDataStore.dismissInboxItem(item.id)
            _isEditMerchantDialogVisible.value = false
            _editingMerchantItem.value = null
            _message.value = "Merchant updated to '$newMerchant'"
        }
    }

    // -------------------------------------------------------------
    // 5. Merge / Resolve Duplicate
    // -------------------------------------------------------------
    fun onOpenResolveDuplicate(item: ReviewInboxItem) {
        _resolvingDuplicateItem.value = item
        _isResolveDuplicateDialogVisible.value = true
    }

    fun onResolveDuplicateKeepPrimary(item: ReviewInboxItem) {
        val txA = item.transaction ?: return
        val txB = item.duplicateCandidate ?: return
        viewModelScope.launch {
            resolveDuplicateTransactionUseCase.keepPrimaryAndDeleteDuplicate(
                keepTransactionId = txA.id,
                deleteTransactionId = txB.id,
                itemKey = item.id
            )
            _isResolveDuplicateDialogVisible.value = false
            _resolvingDuplicateItem.value = null
            _message.value = "Duplicate removed, original kept"
        }
    }

    fun onResolveDuplicateKeepCandidate(item: ReviewInboxItem) {
        val txA = item.transaction ?: return
        val txB = item.duplicateCandidate ?: return
        viewModelScope.launch {
            resolveDuplicateTransactionUseCase.keepPrimaryAndDeleteDuplicate(
                keepTransactionId = txB.id,
                deleteTransactionId = txA.id,
                itemKey = item.id
            )
            _isResolveDuplicateDialogVisible.value = false
            _resolvingDuplicateItem.value = null
            _message.value = "Original removed, candidate kept"
        }
    }

    fun onResolveDuplicateDismiss(item: ReviewInboxItem) {
        viewModelScope.launch {
            resolveDuplicateTransactionUseCase.dismissDuplicateWarning(item.id)
            _isResolveDuplicateDialogVisible.value = false
            _resolvingDuplicateItem.value = null
            _message.value = "Marked as not a duplicate"
        }
    }

    // -------------------------------------------------------------
    // 6. Mark Recurring Bill Paid
    // -------------------------------------------------------------
    fun onOpenPayBill(item: ReviewInboxItem) {
        _payingBillItem.value = item
        _isPayBillDialogVisible.value = true
    }

    fun onConfirmPayBill(item: ReviewInboxItem, actualAmountMinor: Long?, accountId: String?) {
        val occ = item.recurringOccurrence ?: return
        viewModelScope.launch {
            val result = markOccurrencePaidUseCase(
                ruleId = occ.ruleId,
                occurrenceDueDate = occ.dueDate,
                actualAmountMinor = actualAmountMinor,
                accountId = accountId
            )
            when (result) {
                is MarkPaidResult.Success -> {
                    userPreferencesDataStore.dismissInboxItem(item.id)
                    _isPayBillDialogVisible.value = false
                    _payingBillItem.value = null
                    _message.value = "Recorded payment for ${occ.ruleTitle}"
                }
                is MarkPaidResult.Error -> {
                    _message.value = result.message
                }
            }
        }
    }

    // -------------------------------------------------------------
    // 7. Dismiss Warning
    // -------------------------------------------------------------
    fun onDismissItem(item: ReviewInboxItem) {
        viewModelScope.launch {
            userPreferencesDataStore.dismissInboxItem(item.id)
            _message.value = "Dismissed"
        }
    }

    fun onDismissAllWarnings() {
        val currentWarnings = uiState.value.summary.items
            .filter { it.type == ReviewItemType.UNUSUAL_AMOUNT || it.type == ReviewItemType.SUSPECTED_DUPLICATE }
            .map { it.id }
        if (currentWarnings.isEmpty()) return

        viewModelScope.launch {
            userPreferencesDataStore.dismissInboxItems(currentWarnings)
            _message.value = "Dismissed ${currentWarnings.size} warnings"
        }
    }

    // -------------------------------------------------------------
    // 8. Bulk Categorize Where Safe & Multi-Select
    // -------------------------------------------------------------
    fun onApplySafeBulk(suggestion: SafeBulkSuggestion) {
        viewModelScope.launch {
            bulkCategorizeTransactionsUseCase(
                transactionIds = suggestion.transactionIds,
                targetCategoryId = suggestion.suggestedCategoryId
            )
            _message.value = "Categorized ${suggestion.count} '${suggestion.merchant}' transactions as ${suggestion.suggestedCategoryName}"
        }
    }

    fun onOpenBulkCategoryPicker() {
        if (_selectedItemIds.value.isNotEmpty()) {
            _isBulkCategoryPickerVisible.value = true
        }
    }

    fun onConfirmBulkCategorize(targetCategoryId: String) {
        val selectedIds = _selectedItemIds.value
        val itemsMap = uiState.value.summary.items.associateBy { it.id }
        val txIdsToUpdate = selectedIds.mapNotNull { itemsMap[it]?.transaction?.id }

        if (txIdsToUpdate.isNotEmpty()) {
            viewModelScope.launch {
                bulkCategorizeTransactionsUseCase(
                    transactionIds = txIdsToUpdate,
                    targetCategoryId = targetCategoryId
                )
                // Dismiss corresponding inbox item IDs
                userPreferencesDataStore.dismissInboxItems(selectedIds)
                _selectedItemIds.value = emptySet()
                _isSelectionMode.value = false
                _isBulkCategoryPickerVisible.value = false
                _message.value = "Bulk categorized ${txIdsToUpdate.size} transactions"
            }
        }
    }

    fun onDismissDialogs() {
        _isCategoryPickerDialogVisible.value = false
        _categorizingItem.value = null
        _isAccountPickerDialogVisible.value = false
        _changingAccountItem.value = null
        _isEditMerchantDialogVisible.value = false
        _editingMerchantItem.value = null
        _isResolveDuplicateDialogVisible.value = false
        _resolvingDuplicateItem.value = null
        _isPayBillDialogVisible.value = false
        _payingBillItem.value = null
        _isBulkCategoryPickerVisible.value = false
    }

    fun onClearMessage() {
        _message.value = null
    }
}

private data class MainData(
    val tab: ReviewInboxTab,
    val summary: ReviewInboxSummary,
    val categories: List<Category>,
    val accounts: List<Account>
)

private data class SelectionAndPickers(
    val selectedIds: Set<String>,
    val isSelMode: Boolean,
    val isCatPick: Boolean,
    val catItem: ReviewInboxItem?,
    val isAccPick: Boolean
)

private data class DialogsOne(
    val changeAccItem: ReviewInboxItem?,
    val isEditMerch: Boolean,
    val editMerchItem: ReviewInboxItem?,
    val isResolveDup: Boolean,
    val resolveDupItem: ReviewInboxItem?
)

private data class DialogsTwo(
    val isPayBill: Boolean,
    val payBillItem: ReviewInboxItem?,
    val isBulkCat: Boolean,
    val msg: String?
)

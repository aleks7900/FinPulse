package md.alexlab.finpulse.presentation.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.CategorizationRule
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.MatchType
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.CategorizationRuleRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import md.alexlab.finpulse.domain.usecase.categorization.ApplyRuleResult
import md.alexlab.finpulse.domain.usecase.categorization.ApplyRuleToExistingTransactionsUseCase
import md.alexlab.finpulse.domain.usecase.categorization.FindMatchingTransactionsForRuleUseCase
import md.alexlab.finpulse.domain.usecase.categorization.GetReviewQueueUseCase
import md.alexlab.finpulse.domain.usecase.categorization.ManageCategorizationRuleUseCase
import md.alexlab.finpulse.domain.usecase.categorization.RecordCategoryCorrectionUseCase
import md.alexlab.finpulse.domain.usecase.categorization.ReviewQueueItem
import md.alexlab.finpulse.domain.usecase.categorization.SaveRuleResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CategorizationTab(val label: String) {
    REVIEW_QUEUE("Review Queue"),
    RULES("Rules")
}

data class CategorizationRulesUiState(
    val selectedTab: CategorizationTab = CategorizationTab.REVIEW_QUEUE,
    val reviewQueue: List<ReviewQueueItem> = emptyList(),
    val rules: List<CategorizationRule> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isAddEditDialogVisible: Boolean = false,
    val editingRule: CategorizationRule? = null,
    val prefilledMerchant: String? = null,
    val prefilledCategoryId: String? = null,
    val isApplyDialogVisible: Boolean = false,
    val applyRulePreview: md.alexlab.finpulse.domain.usecase.categorization.RuleMatchPreview? = null,
    val overrideManualOnApply: Boolean = false,
    val isChangeCategoryDialogVisible: Boolean = false,
    val changingTransaction: Transaction? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class CategorizationRulesViewModel(
    private val ruleRepository: CategorizationRuleRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val getReviewQueueUseCase: GetReviewQueueUseCase,
    private val recordCategoryCorrectionUseCase: RecordCategoryCorrectionUseCase,
    private val manageRuleUseCase: ManageCategorizationRuleUseCase,
    private val findMatchingUseCase: FindMatchingTransactionsForRuleUseCase,
    private val applyRuleUseCase: ApplyRuleToExistingTransactionsUseCase
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(CategorizationTab.REVIEW_QUEUE)
    private val _isAddEditDialogVisible = MutableStateFlow(false)
    private val _editingRule = MutableStateFlow<CategorizationRule?>(null)
    private val _prefilledMerchant = MutableStateFlow<String?>(null)
    private val _prefilledCategoryId = MutableStateFlow<String?>(null)
    private val _isApplyDialogVisible = MutableStateFlow(false)
    private val _applyRulePreview = MutableStateFlow<md.alexlab.finpulse.domain.usecase.categorization.RuleMatchPreview?>(null)
    private val _overrideManualOnApply = MutableStateFlow(false)
    private val _isChangeCategoryDialogVisible = MutableStateFlow(false)
    private val _changingTransaction = MutableStateFlow<Transaction?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CategorizationRulesUiState> = combine(
        combine(
            _selectedTab,
            getReviewQueueUseCase(),
            ruleRepository.getAllRulesFlow(),
            categoryRepository.getAllCategoriesFlow(),
            accountRepository.getActiveAccountsFlow()
        ) { tab, queue, rules, categories, accounts ->
            QueueAndRules(tab, queue, rules, categories, accounts)
        },
        combine(
            _isAddEditDialogVisible,
            _editingRule,
            _prefilledMerchant,
            _prefilledCategoryId,
            _isApplyDialogVisible
        ) { isAdd, editR, prefillM, prefillC, isApply ->
            AddAndApplyDialog(isAdd, editR, prefillM, prefillC, isApply)
        },
        combine(
            _applyRulePreview,
            _overrideManualOnApply,
            _isChangeCategoryDialogVisible,
            _changingTransaction,
            _errorMessage
        ) { preview, overrideManual, isChangeCat, changeTx, err ->
            ApplyAndChangeDialog(preview, overrideManual, isChangeCat, changeTx, err)
        },
        _successMessage
    ) { qr, ad, ac, succ ->
        CategorizationRulesUiState(
            selectedTab = qr.tab,
            reviewQueue = qr.queue,
            rules = qr.rules,
            categories = qr.categories,
            accounts = qr.accounts,
            isAddEditDialogVisible = ad.isAdd,
            editingRule = ad.editR,
            prefilledMerchant = ad.prefillM,
            prefilledCategoryId = ad.prefillC,
            isApplyDialogVisible = ad.isApply,
            applyRulePreview = ac.preview,
            overrideManualOnApply = ac.overrideManual,
            isChangeCategoryDialogVisible = ac.isChangeCat,
            changingTransaction = ac.changeTx,
            errorMessage = ac.err,
            successMessage = succ
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategorizationRulesUiState()
    )

    fun onTabSelected(tab: CategorizationTab) {
        _selectedTab.value = tab
    }

    fun confirmSuggestion(item: ReviewQueueItem) {
        val catId = item.suggestion.categoryId ?: return
        viewModelScope.launch {
            recordCategoryCorrectionUseCase(
                transactionId = item.transaction.id,
                newCategoryId = catId,
                matchedRuleId = item.suggestion.matchedRuleId,
                confidence = item.suggestion.confidenceScore
            )
            _successMessage.value = "Category confirmed"
        }
    }

    fun openChangeCategoryDialog(transaction: Transaction) {
        _changingTransaction.value = transaction
        _isChangeCategoryDialogVisible.value = true
    }

    fun changeCategoryAndLearnSignal(transactionId: String, newCategoryId: String) {
        viewModelScope.launch {
            recordCategoryCorrectionUseCase(
                transactionId = transactionId,
                newCategoryId = newCategoryId,
                matchedRuleId = null,
                confidence = 1.0f
            )
            _isChangeCategoryDialogVisible.value = false
            _changingTransaction.value = null
            _successMessage.value = "Updated & learned category for future transactions"
        }
    }

    fun openCreateRuleFromTransaction(transaction: Transaction, suggestedCategoryId: String?) {
        _editingRule.value = null
        _prefilledMerchant.value = transaction.merchant?.takeIf { it.isNotBlank() } ?: transaction.description
        _prefilledCategoryId.value = suggestedCategoryId ?: transaction.categoryId
        _isAddEditDialogVisible.value = true
    }

    fun showAddEditRuleDialog(show: Boolean, rule: CategorizationRule? = null) {
        _editingRule.value = rule
        _prefilledMerchant.value = null
        _prefilledCategoryId.value = null
        _isAddEditDialogVisible.value = show
        _errorMessage.value = null
    }

    fun saveRule(
        id: String?,
        name: String,
        targetCategoryId: String,
        priority: Int,
        merchantPattern: String?,
        merchantMatchType: MatchType,
        descriptionPattern: String?,
        descriptionMatchType: MatchType,
        accountId: String?,
        minAmountMinor: Long?,
        maxAmountMinor: Long?,
        transactionType: TransactionType?,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            val result = manageRuleUseCase.saveRule(
                id = id,
                name = name,
                targetCategoryId = targetCategoryId,
                priority = priority,
                merchantPattern = merchantPattern,
                merchantMatchType = merchantMatchType,
                descriptionPattern = descriptionPattern,
                descriptionMatchType = descriptionMatchType,
                accountId = accountId,
                minAmountMinor = minAmountMinor,
                maxAmountMinor = maxAmountMinor,
                transactionType = transactionType,
                isActive = isActive
            )
            when (result) {
                is SaveRuleResult.Success -> {
                    _isAddEditDialogVisible.value = false
                    _editingRule.value = null
                    _prefilledMerchant.value = null
                    _prefilledCategoryId.value = null
                    _errorMessage.value = null
                    _successMessage.value = "Rule saved"
                }
                is SaveRuleResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun toggleRuleActive(rule: CategorizationRule) {
        viewModelScope.launch {
            manageRuleUseCase.setRuleActive(rule.id, !rule.isActive)
        }
    }

    fun updateRulePriority(ruleId: String, newPriority: Int) {
        viewModelScope.launch {
            manageRuleUseCase.updateRulePriority(ruleId, newPriority)
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            manageRuleUseCase.deleteRule(ruleId)
            _successMessage.value = "Rule deleted"
        }
    }

    fun openApplyRuleDialog(ruleId: String) {
        viewModelScope.launch {
            val preview = findMatchingUseCase(ruleId)
            _applyRulePreview.value = preview
            _overrideManualOnApply.value = false
            _isApplyDialogVisible.value = true
        }
    }

    fun setOverrideManualOnApply(override: Boolean) {
        _overrideManualOnApply.value = override
    }

    fun confirmApplyRule() {
        val preview = _applyRulePreview.value ?: return
        viewModelScope.launch {
            val result = applyRuleUseCase(
                ruleId = preview.rule.id,
                overrideManual = _overrideManualOnApply.value
            )
            _isApplyDialogVisible.value = false
            _applyRulePreview.value = null
            _successMessage.value = if (result.skippedManualCount > 0) {
                "Applied to ${result.updatedCount} transactions (${result.skippedManualCount} manual preserved)"
            } else {
                "Applied to ${result.updatedCount} transactions"
            }
        }
    }

    fun dismissDialogs() {
        _isAddEditDialogVisible.value = false
        _editingRule.value = null
        _isApplyDialogVisible.value = false
        _applyRulePreview.value = null
        _isChangeCategoryDialogVisible.value = false
        _changingTransaction.value = null
        _errorMessage.value = null
        _successMessage.value = null
    }
}

private data class QueueAndRules(
    val tab: CategorizationTab,
    val queue: List<ReviewQueueItem>,
    val rules: List<CategorizationRule>,
    val categories: List<Category>,
    val accounts: List<Account>
)

private data class AddAndApplyDialog(
    val isAdd: Boolean,
    val editR: CategorizationRule?,
    val prefillM: String?,
    val prefillC: String?,
    val isApply: Boolean
)

private data class ApplyAndChangeDialog(
    val preview: md.alexlab.finpulse.domain.usecase.categorization.RuleMatchPreview?,
    val overrideManual: Boolean,
    val isChangeCat: Boolean,
    val changeTx: Transaction?,
    val err: String?
)

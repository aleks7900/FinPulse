package md.alexlab.finpulse.domain.usecase.categorization

import md.alexlab.finpulse.domain.repository.CategorizationRuleRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository

data class ApplyRuleResult(
    val updatedCount: Int,
    val skippedManualCount: Int
)

class ApplyRuleToExistingTransactionsUseCase(
    private val ruleRepository: CategorizationRuleRepository,
    private val transactionRepository: TransactionRepository,
    private val findMatchingUseCase: FindMatchingTransactionsForRuleUseCase
) {
    suspend operator fun invoke(
        ruleId: String,
        overrideManual: Boolean = false,
        selectedTransactionIds: Set<String>? = null
    ): ApplyRuleResult {
        val rule = ruleRepository.getRuleById(ruleId)
            ?: return ApplyRuleResult(0, 0)

        val preview = findMatchingUseCase(rule)

        val targetTransactions = if (overrideManual) {
            preview.safeMatches + preview.manualMatches
        } else {
            preview.safeMatches
        }

        val filteredTransactions = if (selectedTransactionIds != null) {
            targetTransactions.filter { selectedTransactionIds.contains(it.id) }
        } else {
            targetTransactions
        }

        if (filteredTransactions.isNotEmpty()) {
            transactionRepository.bulkUpdateCategory(
                ids = filteredTransactions.map { it.id },
                categoryId = rule.targetCategoryId,
                isConfirmed = true,
                matchedRuleId = rule.id
            )
        }

        val skippedCount = if (!overrideManual) preview.manualMatches.size else 0

        return ApplyRuleResult(
            updatedCount = filteredTransactions.size,
            skippedManualCount = skippedCount
        )
    }
}

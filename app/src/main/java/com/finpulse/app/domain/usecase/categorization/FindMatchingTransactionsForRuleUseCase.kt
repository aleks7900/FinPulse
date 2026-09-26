package com.finpulse.app.domain.usecase.categorization

import com.finpulse.app.domain.engine.CategorizationEngine
import com.finpulse.app.domain.model.CategorizationCandidate
import com.finpulse.app.domain.model.CategorizationRule
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.repository.CategorizationRuleRepository
import com.finpulse.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first

data class RuleMatchPreview(
    val rule: CategorizationRule,
    val safeMatches: List<Transaction>,
    val manualMatches: List<Transaction>
) {
    val totalCount: Int get() = safeMatches.size + manualMatches.size
}

class FindMatchingTransactionsForRuleUseCase(
    private val ruleRepository: CategorizationRuleRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(ruleId: String): RuleMatchPreview? {
        val rule = ruleRepository.getRuleById(ruleId) ?: return null
        return invoke(rule)
    }

    suspend operator fun invoke(rule: CategorizationRule): RuleMatchPreview {
        val allTransactions = transactionRepository.getAllTransactionsFlow().first()
        val safeMatches = mutableListOf<Transaction>()
        val manualMatches = mutableListOf<Transaction>()

        for (tx in allTransactions) {
            val candidate = CategorizationCandidate(
                merchant = tx.merchant,
                description = tx.description,
                amountMinor = tx.amount.amountMinor,
                sourceAccountId = tx.sourceAccountId,
                type = tx.type
            )

            if (CategorizationEngine.matchesRule(candidate, rule)) {
                // If it already belongs to the target category, skip
                if (tx.categoryId == rule.targetCategoryId) continue

                val isSafeToUpdate = !tx.isCategoryConfirmed || tx.categoryId == "cat_uncategorized"
                if (isSafeToUpdate) {
                    safeMatches.add(tx)
                } else {
                    manualMatches.add(tx)
                }
            }
        }

        return RuleMatchPreview(
            rule = rule,
            safeMatches = safeMatches,
            manualMatches = manualMatches
        )
    }
}

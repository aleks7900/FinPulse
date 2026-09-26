package com.finpulse.app.domain.usecase.categorization

import com.finpulse.app.domain.engine.CategorizationEngine
import com.finpulse.app.domain.model.CategorizationCandidate
import com.finpulse.app.domain.model.CategorizationResult
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.repository.CategorizationRuleRepository
import com.finpulse.app.domain.repository.MerchantSignalRepository
import com.finpulse.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ReviewQueueItem(
    val transaction: Transaction,
    val suggestion: CategorizationResult
)

class GetReviewQueueUseCase(
    private val transactionRepository: TransactionRepository,
    private val ruleRepository: CategorizationRuleRepository,
    private val signalRepository: MerchantSignalRepository
) {
    operator fun invoke(): Flow<List<ReviewQueueItem>> {
        return combine(
            transactionRepository.getUnreviewedTransactionsFlow(),
            ruleRepository.getActiveRulesFlow(),
            signalRepository.getAllSignalsFlow()
        ) { unreviewedTxs, activeRules, signals ->
            unreviewedTxs.map { tx ->
                val candidate = CategorizationCandidate(
                    merchant = tx.merchant,
                    description = tx.description,
                    amountMinor = tx.amount.amountMinor,
                    sourceAccountId = tx.sourceAccountId,
                    type = tx.type
                )
                val suggestion = CategorizationEngine.categorize(
                    candidate = candidate,
                    rules = activeRules,
                    signals = signals
                )
                ReviewQueueItem(
                    transaction = tx,
                    suggestion = suggestion
                )
            }
        }
    }
}

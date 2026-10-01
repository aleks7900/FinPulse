package md.alexlab.finpulse.domain.usecase.categorization

import md.alexlab.finpulse.domain.engine.CategorizationEngine
import md.alexlab.finpulse.domain.model.CategorizationCandidate
import md.alexlab.finpulse.domain.model.CategorizationResult
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.repository.CategorizationRuleRepository
import md.alexlab.finpulse.domain.repository.MerchantSignalRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository
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

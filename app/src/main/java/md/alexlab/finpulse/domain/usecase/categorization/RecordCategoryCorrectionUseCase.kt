package md.alexlab.finpulse.domain.usecase.categorization

import md.alexlab.finpulse.domain.repository.MerchantSignalRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository

class RecordCategoryCorrectionUseCase(
    private val transactionRepository: TransactionRepository,
    private val signalRepository: MerchantSignalRepository
) {
    suspend operator fun invoke(
        transactionId: String,
        newCategoryId: String,
        matchedRuleId: String? = null,
        confidence: Float = 1.0f
    ) {
        val tx = transactionRepository.getTransactionById(transactionId) ?: return

        // 1. Confirm transaction category
        transactionRepository.confirmTransactionCategory(
            id = transactionId,
            categoryId = newCategoryId,
            matchedRuleId = matchedRuleId,
            confidence = confidence
        )

        // 2. Deterministic local learning signal from user correction
        val merchant = tx.merchant?.takeIf { it.isNotBlank() }
            ?: tx.description.takeIf { it.isNotBlank() }

        if (!merchant.isNullOrBlank()) {
            signalRepository.recordSignal(
                merchant = merchant,
                categoryId = newCategoryId
            )
        }
    }
}

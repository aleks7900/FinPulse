package com.finpulse.app.domain.usecase.review

import com.finpulse.app.domain.repository.MerchantSignalRepository
import com.finpulse.app.domain.repository.TransactionRepository

class BulkCategorizeTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val signalRepository: MerchantSignalRepository
) {
    suspend operator fun invoke(
        transactionIds: List<String>,
        targetCategoryId: String,
        isConfirmed: Boolean = true
    ) {
        if (transactionIds.isEmpty()) return

        // 1. Bulk update transactions
        transactionRepository.bulkUpdateCategory(
            ids = transactionIds,
            categoryId = targetCategoryId,
            isConfirmed = isConfirmed
        )

        // 2. Learn merchant signals from these transactions
        for (id in transactionIds) {
            val tx = transactionRepository.getTransactionById(id) ?: continue
            val merchant = tx.merchant?.takeIf { it.isNotBlank() } ?: tx.description.takeIf { it.isNotBlank() }
            if (!merchant.isNullOrBlank()) {
                signalRepository.recordSignal(merchant, targetCategoryId)
            }
        }
    }
}

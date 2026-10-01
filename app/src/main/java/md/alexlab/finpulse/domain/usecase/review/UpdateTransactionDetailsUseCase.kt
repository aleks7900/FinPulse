package md.alexlab.finpulse.domain.usecase.review

import md.alexlab.finpulse.domain.repository.MerchantSignalRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository

class UpdateTransactionDetailsUseCase(
    private val transactionRepository: TransactionRepository,
    private val signalRepository: MerchantSignalRepository
) {
    suspend fun changeAccount(transactionId: String, newAccountId: String) {
        val tx = transactionRepository.getTransactionById(transactionId) ?: return
        val updated = tx.copy(sourceAccountId = newAccountId)
        transactionRepository.updateTransaction(updated)
    }

    suspend fun editMerchant(transactionId: String, newMerchant: String, updateSignal: Boolean = true) {
        val tx = transactionRepository.getTransactionById(transactionId) ?: return
        val trimmed = newMerchant.trim()
        val updated = tx.copy(merchant = trimmed)
        transactionRepository.updateTransaction(updated)

        if (updateSignal && trimmed.isNotBlank() && tx.categoryId.isNotBlank()) {
            signalRepository.recordSignal(trimmed, tx.categoryId)
        }
    }
}

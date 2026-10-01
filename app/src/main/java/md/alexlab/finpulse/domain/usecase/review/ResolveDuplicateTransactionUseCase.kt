package md.alexlab.finpulse.domain.usecase.review

import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.domain.repository.TransactionRepository

class ResolveDuplicateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) {
    suspend fun keepPrimaryAndDeleteDuplicate(
        keepTransactionId: String,
        deleteTransactionId: String,
        itemKey: String
    ) {
        transactionRepository.deleteTransaction(deleteTransactionId)
        userPreferencesDataStore.dismissInboxItem(itemKey)
    }

    suspend fun dismissDuplicateWarning(itemKey: String) {
        userPreferencesDataStore.dismissInboxItem(itemKey)
    }
}

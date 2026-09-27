package com.finpulse.app.domain.usecase.review

import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.domain.repository.TransactionRepository

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

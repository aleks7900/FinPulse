package com.finpulse.app.domain.usecase.transaction

import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

data class QuickAddSuggestions(
    val defaultAccountId: String?,
    val defaultCategoryId: String?,
    val defaultType: TransactionType,
    val frequentCategories: List<Category>,
    val frequentMerchants: List<String>
)

class GetQuickAddSuggestionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) {
    fun getSuggestionsFlow(type: TransactionType = TransactionType.EXPENSE): Flow<QuickAddSuggestions> {
        val catType = when (type) {
            TransactionType.INCOME, TransactionType.REFUND -> CategoryType.INCOME
            else -> CategoryType.EXPENSE
        }

        return combine(
            transactionRepository.getFrequentCategoryIdsFlow(type),
            transactionRepository.getFrequentMerchantsFlow(),
            categoryRepository.getCategoriesByTypeFlow(catType),
            accountRepository.getActiveAccountsFlow(),
            userPreferencesDataStore.userPreferencesFlow
        ) { freqCatIds, merchants, allCategories, accounts, userPrefs ->
            val catMap = allCategories.associateBy { it.id }

            // Frequent categories: those matching freqCatIds, plus remaining defaults
            val frequentList = mutableListOf<Category>()
            for (id in freqCatIds) {
                catMap[id]?.let { frequentList.add(it) }
            }
            for (cat in allCategories) {
                if (frequentList.none { it.id == cat.id }) {
                    frequentList.add(cat)
                }
            }

            // Determine default account:
            // 1. Last used account if active
            // 2. First active account
            val defaultAccId = accounts.find { it.id == userPrefs.lastUsedAccountId }?.id
                ?: accounts.firstOrNull()?.id

            // Determine default category:
            // 1. Last used category if present in current type
            // 2. Most frequent category
            // 3. First category
            val defaultCatId = if (frequentList.any { it.id == userPrefs.lastUsedCategoryId }) {
                userPrefs.lastUsedCategoryId
            } else {
                frequentList.firstOrNull()?.id
            }

            val defaultTxType = try {
                TransactionType.valueOf(userPrefs.lastUsedTransactionType)
            } catch (_: Exception) {
                TransactionType.EXPENSE
            }

            QuickAddSuggestions(
                defaultAccountId = defaultAccId,
                defaultCategoryId = defaultCatId,
                defaultType = defaultTxType,
                frequentCategories = frequentList.take(8),
                frequentMerchants = merchants
            )
        }
    }

    suspend fun getSuggestedCategoryForMerchant(merchant: String): String? {
        if (merchant.isBlank()) return null
        return transactionRepository.getSuggestedCategoryForMerchant(merchant.trim())
    }
}

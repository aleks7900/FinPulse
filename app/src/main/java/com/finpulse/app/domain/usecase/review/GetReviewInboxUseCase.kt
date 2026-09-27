package com.finpulse.app.domain.usecase.review

import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.domain.engine.ReviewInboxEngine
import com.finpulse.app.domain.model.ReviewInboxSummary
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.CategorizationRuleRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.MerchantSignalRepository
import com.finpulse.app.domain.repository.RecurringRepository
import com.finpulse.app.domain.repository.TransactionRepository
import com.finpulse.app.domain.usecase.recurring.GetUpcomingOccurrencesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetReviewInboxUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val ruleRepository: CategorizationRuleRepository,
    private val signalRepository: MerchantSignalRepository,
    private val recurringRepository: RecurringRepository,
    private val getUpcomingOccurrencesUseCase: GetUpcomingOccurrencesUseCase,
    private val userPreferencesDataStore: UserPreferencesDataStore
) {
    operator fun invoke(): Flow<ReviewInboxSummary> {
        return combine(
            combine(
                transactionRepository.getAllTransactionsFlow(),
                categoryRepository.getAllCategoriesFlow(),
                accountRepository.getAllAccountsFlow(),
                ruleRepository.getActiveRulesFlow(),
                signalRepository.getAllSignalsFlow()
            ) { txs, cats, accs, rules, signals ->
                TxAndRules(txs, cats, accs, rules, signals)
            },
            combine(
                recurringRepository.getActiveRecurringFlow(),
                getUpcomingOccurrencesUseCase(windowDays = 30),
                userPreferencesDataStore.userPreferencesFlow
            ) { recurring, occurrences, userPrefs ->
                RecurringAndPrefs(recurring, occurrences, userPrefs.dismissedInboxItemIds)
            }
        ) { tr, rp ->
            ReviewInboxEngine.evaluate(
                transactions = tr.txs,
                categories = tr.cats,
                accounts = tr.accs,
                activeRules = tr.rules,
                signals = tr.signals,
                recurringRules = rp.recurring,
                occurrences = rp.occurrences,
                dismissedIds = rp.dismissedIds
            )
        }
    }
}

private data class TxAndRules(
    val txs: List<com.finpulse.app.domain.model.Transaction>,
    val cats: List<com.finpulse.app.domain.model.Category>,
    val accs: List<com.finpulse.app.domain.model.Account>,
    val rules: List<com.finpulse.app.domain.model.CategorizationRule>,
    val signals: List<com.finpulse.app.domain.model.MerchantSignal>
)

private data class RecurringAndPrefs(
    val recurring: List<com.finpulse.app.domain.model.RecurringTransaction>,
    val occurrences: List<com.finpulse.app.domain.model.RecurringOccurrence>,
    val dismissedIds: Set<String>
)

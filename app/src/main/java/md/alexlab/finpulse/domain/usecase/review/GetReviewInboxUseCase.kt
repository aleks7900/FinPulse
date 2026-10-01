package md.alexlab.finpulse.domain.usecase.review

import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.domain.engine.ReviewInboxEngine
import md.alexlab.finpulse.domain.model.ReviewInboxSummary
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.CategorizationRuleRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import md.alexlab.finpulse.domain.repository.MerchantSignalRepository
import md.alexlab.finpulse.domain.repository.RecurringRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository
import md.alexlab.finpulse.domain.usecase.recurring.GetUpcomingOccurrencesUseCase
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
    val txs: List<md.alexlab.finpulse.domain.model.Transaction>,
    val cats: List<md.alexlab.finpulse.domain.model.Category>,
    val accs: List<md.alexlab.finpulse.domain.model.Account>,
    val rules: List<md.alexlab.finpulse.domain.model.CategorizationRule>,
    val signals: List<md.alexlab.finpulse.domain.model.MerchantSignal>
)

private data class RecurringAndPrefs(
    val recurring: List<md.alexlab.finpulse.domain.model.RecurringTransaction>,
    val occurrences: List<md.alexlab.finpulse.domain.model.RecurringOccurrence>,
    val dismissedIds: Set<String>
)

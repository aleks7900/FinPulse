package com.finpulse.app.domain.usecase.digest

import com.finpulse.app.core.datastore.UserPreferences
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.domain.engine.FinancialDigestEngine
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.DigestPeriod
import com.finpulse.app.domain.model.FinancialDigest
import com.finpulse.app.domain.model.FinancialGoal
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.repository.BudgetRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.GoalRepository
import com.finpulse.app.domain.repository.RecurringRepository
import com.finpulse.app.domain.repository.TransactionRepository
import com.finpulse.app.domain.usecase.review.GetReviewInboxUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class GetFinancialDigestUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val recurringRepository: RecurringRepository,
    private val goalRepository: GoalRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val getReviewInboxUseCase: GetReviewInboxUseCase? = null,
    private val engine: FinancialDigestEngine = FinancialDigestEngine()
) {

    private data class CoreDigestData(
        val txs: List<Transaction>,
        val cats: List<Category>,
        val budgets: List<Budget>,
        val recurring: List<RecurringTransaction>
    )

    private data class SecondaryDigestData(
        val goals: List<FinancialGoal>,
        val unreviewedCount: Int,
        val prefs: UserPreferences
    )

    operator fun invoke(period: DigestPeriod): Flow<FinancialDigest> {
        val reviewFlow: Flow<Int> = getReviewInboxUseCase?.invoke()?.let { inboxFlow ->
            kotlinx.coroutines.flow.flow {
                inboxFlow.collect { inbox ->
                    emit(inbox.totalCount)
                }
            }
        } ?: transactionRepository.getUnreviewedCountFlow()

        val coreFlow = combine(
            transactionRepository.getAllTransactionsFlow(),
            categoryRepository.getAllCategoriesFlow(),
            budgetRepository.getAllActiveBudgetsFlow(),
            recurringRepository.getActiveRecurringFlow()
        ) { txs, cats, budgets, recurring ->
            CoreDigestData(txs, cats, budgets, recurring)
        }

        val secondaryFlow = combine(
            goalRepository.getAllGoalsFlow(),
            reviewFlow,
            userPreferencesDataStore.userPreferencesFlow
        ) { goals, unreviewedCount, prefs ->
            SecondaryDigestData(goals, unreviewedCount, prefs)
        }

        return combine(coreFlow, secondaryFlow) { core, sec ->
            engine.generateDigest(
                period = period,
                transactions = core.txs,
                categories = core.cats,
                budgets = core.budgets,
                recurringRules = core.recurring,
                goals = sec.goals,
                unreviewedCount = sec.unreviewedCount,
                baseCurrency = sec.prefs.baseCurrencyCode
            )
        }
    }

    suspend fun getSnapshot(period: DigestPeriod): FinancialDigest {
        val txs = transactionRepository.getAllTransactionsFlow().first()
        val cats = categoryRepository.getAllCategoriesFlow().first()
        val budgets = budgetRepository.getAllActiveBudgetsFlow().first()
        val recurring = recurringRepository.getActiveRecurringFlow().first()
        val goals = goalRepository.getAllGoalsFlow().first()
        val unreviewedCount = getReviewInboxUseCase?.invoke()?.first()?.totalCount
            ?: transactionRepository.getUnreviewedCountFlow().first()
        val prefs = userPreferencesDataStore.userPreferencesFlow.first()

        return engine.generateDigest(
            period = period,
            transactions = txs,
            categories = cats,
            budgets = budgets,
            recurringRules = recurring,
            goals = goals,
            unreviewedCount = unreviewedCount,
            baseCurrency = prefs.baseCurrencyCode
        )
    }
}

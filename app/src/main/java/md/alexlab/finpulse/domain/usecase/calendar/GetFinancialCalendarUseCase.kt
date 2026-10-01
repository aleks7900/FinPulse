package md.alexlab.finpulse.domain.usecase.calendar

import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.domain.engine.CashFlowProjectionEngine
import md.alexlab.finpulse.domain.model.CashFlowPeriodSummary
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import md.alexlab.finpulse.domain.repository.DebtRepository
import md.alexlab.finpulse.domain.repository.GoalRepository
import md.alexlab.finpulse.domain.repository.RecurringRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId

class GetFinancialCalendarUseCase(
    private val accountRepository: AccountRepository,
    private val recurringRepository: RecurringRepository,
    private val debtRepository: DebtRepository,
    private val goalRepository: GoalRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) {

    operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Flow<CashFlowPeriodSummary> {
        val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        val accountsFlow = accountRepository.getActiveAccountsFlow()
        val recurringRulesFlow = recurringRepository.getActiveRecurringFlow()
        val occurrencesFlow = recurringRepository.getOccurrencesInRangeFlow(startMillis - (30 * 86_400_000L), endMillis)
        val debtsFlow = debtRepository.getAllDebtsFlow()
        val goalsFlow = goalRepository.getAllGoalsFlow()
        val categoriesFlow = categoryRepository.getAllCategoriesFlow()
        val transactionsFlow = transactionRepository.getTransactionsByDateRangeFlow(startMillis, endMillis)
        val userPrefsFlow = userPreferencesDataStore.userPreferencesFlow

        return combine(
            combine(accountsFlow, recurringRulesFlow, occurrencesFlow) { accounts, rules, occurrences ->
                Triple(accounts, rules, occurrences)
            },
            combine(debtsFlow, goalsFlow, categoriesFlow) { debts, goals, categories ->
                Triple(debts, goals, categories)
            },
            combine(transactionsFlow, userPrefsFlow) { transactions, prefs ->
                Pair(transactions, prefs)
            }
        ) { (accounts, rules, occurrences), (debts, goals, categories), (transactions, prefs) ->
            val baseCurrency = prefs.baseCurrencyCode

            val startingBalance = CashFlowProjectionEngine.calculateStartingLiquidBalance(
                accounts = accounts,
                baseCurrency = baseCurrency
            )

            val events = CashFlowProjectionEngine.buildCalendarEvents(
                startDate = startDate,
                endDate = endDate,
                recurringRules = rules,
                persistedOccurrences = occurrences,
                debts = debts,
                goals = goals,
                recordedTransactions = transactions,
                accounts = accounts,
                categories = categories,
                baseCurrency = baseCurrency,
                nowMillis = nowMillis,
                zoneId = zoneId
            )

            CashFlowProjectionEngine.calculateCashFlowProjection(
                startDate = startDate,
                endDate = endDate,
                startingLiquidBalance = startingBalance,
                events = events,
                baseCurrency = baseCurrency,
                zoneId = zoneId
            )
        }
    }
}

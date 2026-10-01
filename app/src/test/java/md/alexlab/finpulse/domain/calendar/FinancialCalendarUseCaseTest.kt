package md.alexlab.finpulse.domain.calendar

import md.alexlab.finpulse.core.datastore.UserPreferences
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.CalendarEventType
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.CategoryType
import md.alexlab.finpulse.domain.model.Debt
import md.alexlab.finpulse.domain.model.DebtType
import md.alexlab.finpulse.domain.model.FinancialGoal
import md.alexlab.finpulse.domain.model.PaymentFrequency
import md.alexlab.finpulse.domain.model.RecurringOccurrence
import md.alexlab.finpulse.domain.model.RecurringTransaction
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import md.alexlab.finpulse.domain.repository.DebtRepository
import md.alexlab.finpulse.domain.repository.GoalRepository
import md.alexlab.finpulse.domain.repository.RecurringRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository
import md.alexlab.finpulse.domain.usecase.calendar.GetFinancialCalendarUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FinancialCalendarUseCaseTest {

    private val zoneId = ZoneId.of("UTC")

    @Test
    fun testGetFinancialCalendarUseCase_CombinesRepositoriesAndEmitsSummary() = runBlocking {
        val startDate = LocalDate.of(2026, 10, 1)
        val endDate = LocalDate.of(2026, 10, 31)

        val account = Account(
            id = "acc1",
            name = "Main Bank",
            type = AccountType.BANK,
            balance = Money(500_000, "USD"),
            availableBalance = Money(500_000, "USD")
        )

        val salaryDate = LocalDate.of(2026, 10, 15).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val salaryRule = RecurringTransaction(
            id = "salary_rule",
            title = "Monthly Salary",
            amount = Money(300_000, "USD"),
            type = TransactionType.INCOME,
            accountId = "acc1",
            categoryId = "cat_inc",
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = salaryDate
        )

        val wifiDate = LocalDate.of(2026, 10, 5).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wifiRule = RecurringTransaction(
            id = "wifi_rule",
            title = "Fiber Internet",
            amount = Money(7_000, "USD"),
            type = TransactionType.EXPENSE,
            accountId = "acc1",
            categoryId = "cat_util",
            frequency = PaymentFrequency.MONTHLY,
            isSubscription = false,
            nextDueDate = wifiDate
        )

        val fakeAccountRepo = FakeAccountRepository(listOf(account))
        val fakeRecurringRepo = FakeRecurringRepository(listOf(salaryRule, wifiRule))
        val fakeDebtRepo = FakeDebtRepository(emptyList())
        val fakeGoalRepo = FakeGoalRepository(emptyList())
        val fakeCategoryRepo = FakeCategoryRepository(emptyList())
        val fakeTxRepo = FakeTransactionRepository(emptyList())
        val fakeUserPrefs: UserPreferencesDataStore = io.mockk.mockk(relaxed = true)
        io.mockk.every { fakeUserPrefs.userPreferencesFlow } returns kotlinx.coroutines.flow.flowOf(UserPreferences(baseCurrencyCode = "USD"))

        val useCase = GetFinancialCalendarUseCase(
            accountRepository = fakeAccountRepo,
            recurringRepository = fakeRecurringRepo,
            debtRepository = fakeDebtRepo,
            goalRepository = fakeGoalRepo,
            categoryRepository = fakeCategoryRepo,
            transactionRepository = fakeTxRepo,
            userPreferencesDataStore = fakeUserPrefs
        )

        val summary = useCase(
            startDate = startDate,
            endDate = endDate,
            nowMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            zoneId = zoneId
        ).first()

        assertEquals(500_000L, summary.startingBalance.amountMinor)
        assertEquals(300_000L, summary.totalInflow.amountMinor)
        assertEquals(7_000L, summary.totalOutflow.amountMinor)
        assertEquals(293_000L, summary.netCashFlow.amountMinor)
        assertEquals(793_000L, summary.projectedEndingBalance.amountMinor)
        assertTrue(summary.isDeterministic)
    }

    // ---------------- Fake Test Implementations ----------------

    private class FakeAccountRepository(accounts: List<Account>) : AccountRepository {
        private val flow = MutableStateFlow(accounts)
        override fun getAllAccountsFlow(): Flow<List<Account>> = flow
        override fun getActiveAccountsFlow(): Flow<List<Account>> = flow
        override suspend fun getAccountById(id: String): Account? = flow.value.find { it.id == id }
        override suspend fun saveAccount(account: Account) {}
        override suspend fun deleteAccount(id: String) {}
        override suspend fun setArchived(id: String, isArchived: Boolean) {}
        override suspend fun updateBalances(accountId: String, balance: Money, availableBalance: Money) {}
    }

    private class FakeRecurringRepository(rules: List<RecurringTransaction>) : RecurringRepository {
        private val rulesFlow = MutableStateFlow(rules)
        private val occurrencesFlow = MutableStateFlow<List<RecurringOccurrence>>(emptyList())

        override fun getAllRecurringFlow(): Flow<List<RecurringTransaction>> = rulesFlow
        override fun getActiveRecurringFlow(): Flow<List<RecurringTransaction>> = rulesFlow
        override fun getActiveSubscriptionsFlow(): Flow<List<RecurringTransaction>> = rulesFlow
        override suspend fun getRecurringById(id: String): RecurringTransaction? = rulesFlow.value.find { it.id == id }
        override suspend fun getDueRecurring(timestamp: Long): List<RecurringTransaction> = emptyList()
        override suspend fun saveRecurring(recurring: RecurringTransaction) {}
        override suspend fun deleteRecurring(id: String) {}
        override suspend fun setRuleActive(id: String, isActive: Boolean) {}
        override suspend fun cancelRule(id: String) {}
        override suspend fun getOccurrenceById(id: String): RecurringOccurrence? = occurrencesFlow.value.find { it.id == id }
        override fun getOccurrencesInRangeFlow(startDate: Long, endDate: Long): Flow<List<RecurringOccurrence>> = occurrencesFlow
        override suspend fun getOccurrencesInRange(startDate: Long, endDate: Long): List<RecurringOccurrence> = occurrencesFlow.value
        override suspend fun saveOccurrence(occurrence: RecurringOccurrence) {}
        override suspend fun markOccurrencePaid(occurrenceId: String, paidDate: Long, transactionId: String) {}
        override suspend fun markOccurrenceSkipped(occurrenceId: String) {}
        override suspend fun advanceRuleDueDate(ruleId: String, newDueDate: Long, processedDate: Long) {}
        override suspend fun processDueRecurringTransactions(nowMillis: Long) {}
    }

    private class FakeDebtRepository(debts: List<Debt>) : DebtRepository {
        private val flow = MutableStateFlow(debts)
        override fun getAllDebtsFlow(): Flow<List<Debt>> = flow
        override suspend fun getDebtById(id: String): Debt? = flow.value.find { it.id == id }
        override suspend fun saveDebt(debt: Debt) {}
        override suspend fun deleteDebt(id: String) {}
        override suspend fun makePayment(debtId: String, paymentAmount: Money) {}
    }

    private class FakeGoalRepository(goals: List<FinancialGoal>) : GoalRepository {
        private val flow = MutableStateFlow(goals)
        override fun getAllGoalsFlow(): Flow<List<FinancialGoal>> = flow
        override suspend fun getGoalById(id: String): FinancialGoal? = flow.value.find { it.id == id }
        override suspend fun saveGoal(goal: FinancialGoal) {}
        override suspend fun deleteGoal(id: String) {}
        override suspend fun contributeToGoal(goalId: String, amount: Money) {}
    }

    private class FakeCategoryRepository(categories: List<Category>) : CategoryRepository {
        private val flow = MutableStateFlow(categories)
        override fun getAllCategoriesFlow(): Flow<List<Category>> = flow
        override fun getCategoriesByTypeFlow(type: CategoryType): Flow<List<Category>> = flow
        override suspend fun getCategoryById(id: String): Category? = flow.value.find { it.id == id }
        override suspend fun saveCategory(category: Category) {}
        override suspend fun seedDefaultCategoriesIfNeeded() {}
    }

    private class FakeTransactionRepository(transactions: List<Transaction>) : TransactionRepository {
        private val flow = MutableStateFlow(transactions)
        override fun getAllTransactionsFlow(): Flow<List<Transaction>> = flow
        override fun getRecentTransactionsFlow(limit: Int): Flow<List<Transaction>> = flow
        override fun getTransactionsByAccountFlow(accountId: String): Flow<List<Transaction>> = flow
        override fun getTransactionsByCategoryFlow(categoryId: String): Flow<List<Transaction>> = flow
        override fun getTransactionsByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<Transaction>> = flow
        override fun searchTransactionsFlow(query: String): Flow<List<Transaction>> = flow
        override suspend fun getTransactionById(id: String): Transaction? = flow.value.find { it.id == id }
        override suspend fun createTransaction(transaction: Transaction) {}
        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(id: String) {}
        override fun getFrequentCategoryIdsFlow(type: TransactionType, limit: Int): Flow<List<String>> = MutableStateFlow(emptyList())
        override fun getFrequentMerchantsFlow(limit: Int): Flow<List<String>> = MutableStateFlow(emptyList())
        override suspend fun getSuggestedCategoryForMerchant(merchant: String): String? = null
        override fun getUnreviewedTransactionsFlow(): Flow<List<Transaction>> = flow
        override fun getUnreviewedCountFlow(): Flow<Int> = MutableStateFlow(0)
        override suspend fun confirmTransactionCategory(id: String, categoryId: String, matchedRuleId: String?, confidence: Float) {}
        override suspend fun bulkUpdateCategory(ids: List<String>, categoryId: String, isConfirmed: Boolean, matchedRuleId: String?) {}
    }
}

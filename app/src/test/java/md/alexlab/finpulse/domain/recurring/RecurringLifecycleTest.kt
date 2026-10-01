package md.alexlab.finpulse.domain.recurring

import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.engine.RecurringDateEngine
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.CustomIntervalUnit
import md.alexlab.finpulse.domain.model.OccurrenceStatus
import md.alexlab.finpulse.domain.model.PaymentFrequency
import md.alexlab.finpulse.domain.model.RecurringOccurrence
import md.alexlab.finpulse.domain.model.RecurringTransaction
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.RecurringRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository
import md.alexlab.finpulse.domain.usecase.recurring.EditOccurrenceResult
import md.alexlab.finpulse.domain.usecase.recurring.EditOccurrenceUseCase
import md.alexlab.finpulse.domain.usecase.recurring.GetUpcomingOccurrencesUseCase
import md.alexlab.finpulse.domain.usecase.recurring.ManageRecurringRuleUseCase
import md.alexlab.finpulse.domain.usecase.recurring.MarkOccurrencePaidUseCase
import md.alexlab.finpulse.domain.usecase.recurring.MarkPaidResult
import md.alexlab.finpulse.domain.usecase.recurring.SaveRecurringRuleResult
import md.alexlab.finpulse.domain.usecase.recurring.SkipOccurrenceResult
import md.alexlab.finpulse.domain.usecase.recurring.SkipOccurrenceUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class RecurringLifecycleTest {

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var recurringRepository: FakeRecurringRepository

    private lateinit var manageRecurringRuleUseCase: ManageRecurringRuleUseCase
    private lateinit var markOccurrencePaidUseCase: MarkOccurrencePaidUseCase
    private lateinit var skipOccurrenceUseCase: SkipOccurrenceUseCase
    private lateinit var editOccurrenceUseCase: EditOccurrenceUseCase
    private lateinit var getUpcomingOccurrencesUseCase: GetUpcomingOccurrencesUseCase

    private val zoneId = ZoneId.of("UTC")
    private val baseTime = ZonedDateTime.of(2026, 9, 1, 10, 0, 0, 0, zoneId).toInstant().toEpochMilli()

    @Before
    fun setup() {
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        recurringRepository = FakeRecurringRepository(transactionRepository)

        manageRecurringRuleUseCase = ManageRecurringRuleUseCase(recurringRepository, accountRepository)
        markOccurrencePaidUseCase = MarkOccurrencePaidUseCase(recurringRepository, transactionRepository)
        skipOccurrenceUseCase = SkipOccurrenceUseCase(recurringRepository)
        editOccurrenceUseCase = EditOccurrenceUseCase(recurringRepository)
        getUpcomingOccurrencesUseCase = GetUpcomingOccurrencesUseCase(recurringRepository)

        // Seed accounts
        runBlocking {
            accountRepository.saveAccount(
                Account("acc-checking", "Checking", AccountType.BANK, Money(200000L, "USD"), Money(200000L, "USD"))
            )
            accountRepository.saveAccount(
                Account("acc-savings", "Savings", AccountType.SAVINGS, Money(500000L, "USD"), Money(500000L, "USD"))
            )
            accountRepository.saveAccount(
                Account("acc-archived", "Old Card", AccountType.CREDIT_CARD, Money(0L, "USD"), Money(0L, "USD"), isArchived = true)
            )
        }
    }

    @Test
    fun testCreateExpenseIncomeAndTransferRules() = runBlocking {
        // 1. Expense: Rent ($1,200.00 / mo)
        val rentResult = manageRecurringRuleUseCase.saveRule(
            title = "Apartment Rent",
            amountMinor = 120000L,
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-checking",
            categoryId = "cat-housing",
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = baseTime
        )
        assertTrue(rentResult is SaveRecurringRuleResult.Success)
        val rentRule = (rentResult as SaveRecurringRuleResult.Success).rule
        assertEquals("Apartment Rent", rentRule.title)
        assertEquals(TransactionType.EXPENSE, rentRule.type)
        assertEquals(1, rentRule.anchorDayOfMonth)

        // 2. Income: Salary ($3,000.00 bi-weekly)
        val salaryResult = manageRecurringRuleUseCase.saveRule(
            title = "Job Salary",
            amountMinor = 300000L,
            type = TransactionType.INCOME,
            sourceAccountId = "acc-checking",
            categoryId = "cat-salary",
            frequency = PaymentFrequency.BI_WEEKLY,
            nextDueDate = baseTime
        )
        assertTrue(salaryResult is SaveRecurringRuleResult.Success)
        val salaryRule = (salaryResult as SaveRecurringRuleResult.Success).rule
        assertEquals(TransactionType.INCOME, salaryRule.type)
        assertEquals(PaymentFrequency.BI_WEEKLY, salaryRule.frequency)

        // 3. Transfer: Savings Deposit ($500.00 monthly)
        val transferResult = manageRecurringRuleUseCase.saveRule(
            title = "Monthly Savings Transfer",
            amountMinor = 50000L,
            type = TransactionType.TRANSFER,
            sourceAccountId = "acc-checking",
            destinationAccountId = "acc-savings",
            categoryId = "cat-transfer",
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = baseTime
        )
        assertTrue(transferResult is SaveRecurringRuleResult.Success)
        val transferRule = (transferResult as SaveRecurringRuleResult.Success).rule
        assertEquals(TransactionType.TRANSFER, transferRule.type)
        assertEquals("acc-savings", transferRule.destinationAccountId)
    }

    @Test
    fun testRuleValidationRejections() = runBlocking {
        // Blank title
        val blankTitle = manageRecurringRuleUseCase.saveRule(
            title = "",
            amountMinor = 1000L,
            sourceAccountId = "acc-checking",
            categoryId = "cat-1",
            nextDueDate = baseTime
        )
        assertTrue(blankTitle is SaveRecurringRuleResult.Error)

        // Non-positive amount
        val zeroAmount = manageRecurringRuleUseCase.saveRule(
            title = "Free Item",
            amountMinor = 0L,
            sourceAccountId = "acc-checking",
            categoryId = "cat-1",
            nextDueDate = baseTime
        )
        assertTrue(zeroAmount is SaveRecurringRuleResult.Error)

        // Archived account
        val archivedAcc = manageRecurringRuleUseCase.saveRule(
            title = "Old Card Sub",
            amountMinor = 1000L,
            sourceAccountId = "acc-archived",
            categoryId = "cat-1",
            nextDueDate = baseTime
        )
        assertTrue(archivedAcc is SaveRecurringRuleResult.Error)

        // Transfer with same source and destination
        val sameAccountTransfer = manageRecurringRuleUseCase.saveRule(
            title = "Self Transfer",
            amountMinor = 5000L,
            type = TransactionType.TRANSFER,
            sourceAccountId = "acc-checking",
            destinationAccountId = "acc-checking",
            categoryId = "cat-1",
            nextDueDate = baseTime
        )
        assertTrue(sameAccountTransfer is SaveRecurringRuleResult.Error)
    }

    @Test
    fun testMarkOccurrencePaid_CreatesLedgerTransactionAndAdvancesDueDate() = runBlocking {
        // Create a monthly Netflix subscription due Sept 1
        val saveResult = manageRecurringRuleUseCase.saveRule(
            title = "Netflix",
            amountMinor = 1599L,
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-checking",
            categoryId = "cat-ent",
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = baseTime,
            isSubscription = true
        )
        val rule = (saveResult as SaveRecurringRuleResult.Success).rule

        // Mark Sept 1 occurrence as paid
        val paidResult = markOccurrencePaidUseCase(
            ruleId = rule.id,
            occurrenceDueDate = baseTime,
            actualAmountMinor = 1599L
        )
        assertTrue(paidResult is MarkPaidResult.Success)
        val success = paidResult as MarkPaidResult.Success

        // 1. Ledger transaction created
        assertEquals(1599L, success.transaction.amount.amountMinor)
        assertEquals("Netflix", success.transaction.merchant)
        assertEquals(rule.id, success.transaction.recurringRuleId)
        val allTx = transactionRepository.getAllTransactionsFlow().first()
        assertEquals(1, allTx.size)
        assertEquals(rule.id, allTx[0].recurringRuleId)

        // 2. Occurrence marked as PAID
        assertEquals(OccurrenceStatus.PAID, success.occurrence.status)
        assertEquals(baseTime, success.occurrence.dueDate)
        val storedOcc = recurringRepository.getOccurrenceById("${rule.id}_$baseTime")
        assertNotNull(storedOcc)
        assertEquals(OccurrenceStatus.PAID, storedOcc?.status)

        // 3. Rule nextDueDate advanced to Oct 1
        val updatedRule = recurringRepository.getRecurringById(rule.id)
        assertNotNull(updatedRule)
        val expectedOct1 = ZonedDateTime.of(2026, 10, 1, 10, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        assertEquals(expectedOct1, updatedRule?.nextDueDate)
    }

    @Test
    fun testMarkOccurrencePaid_VariableBillActualAmountOverride() = runBlocking {
        // Utility bill expected $80, but actual bill is $94.50
        val saveResult = manageRecurringRuleUseCase.saveRule(
            title = "Electric Utility",
            amountMinor = 8000L,
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-checking",
            categoryId = "cat-utilities",
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = baseTime,
            isVariableAmount = true
        )
        val rule = (saveResult as SaveRecurringRuleResult.Success).rule

        val paidResult = markOccurrencePaidUseCase(
            ruleId = rule.id,
            occurrenceDueDate = baseTime,
            actualAmountMinor = 9450L // Overridden actual amount
        )
        assertTrue(paidResult is MarkPaidResult.Success)
        val success = paidResult as MarkPaidResult.Success
        assertEquals(9450L, success.transaction.amount.amountMinor)
        assertEquals(9450L, success.occurrence.amount.amountMinor)

        // Parent rule expected default amount remains 8000L
        val refreshedRule = recurringRepository.getRecurringById(rule.id)
        assertEquals(8000L, refreshedRule?.amount?.amountMinor)
    }

    @Test
    fun testSkipOccurrence_AdvancesDueDateWithoutLedgerTransaction() = runBlocking {
        val saveResult = manageRecurringRuleUseCase.saveRule(
            title = "Gym Membership",
            amountMinor = 5000L,
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-checking",
            categoryId = "cat-fitness",
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = baseTime
        )
        val rule = (saveResult as SaveRecurringRuleResult.Success).rule

        val skipResult = skipOccurrenceUseCase(
            ruleId = rule.id,
            occurrenceDueDate = baseTime
        )
        assertTrue(skipResult is SkipOccurrenceResult.Success)
        val occurrence = (skipResult as SkipOccurrenceResult.Success).occurrence

        // Occurrence status is SKIPPED
        assertEquals(OccurrenceStatus.SKIPPED, occurrence.status)
        assertNull(occurrence.transactionId)

        // No ledger transaction created
        val allTx = transactionRepository.getAllTransactionsFlow().first()
        assertTrue(allTx.isEmpty())

        // Rule nextDueDate advanced to Oct 1
        val updatedRule = recurringRepository.getRecurringById(rule.id)
        val expectedOct1 = ZonedDateTime.of(2026, 10, 1, 10, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        assertEquals(expectedOct1, updatedRule?.nextDueDate)
    }

    @Test
    fun testEditIndividualOccurrence_OverridesOccurrenceWithoutModifyingRule() = runBlocking {
        val saveResult = manageRecurringRuleUseCase.saveRule(
            title = "Water Bill",
            amountMinor = 4000L,
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-checking",
            categoryId = "cat-utilities",
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = baseTime
        )
        val rule = (saveResult as SaveRecurringRuleResult.Success).rule

        // User overrides the Sept 1 occurrence amount to $55.00 with custom notes
        val editResult = editOccurrenceUseCase(
            ruleId = rule.id,
            originalDueDate = baseTime,
            newAmountMinor = 5500L,
            notes = "Higher usage due to lawn watering"
        )
        assertTrue(editResult is EditOccurrenceResult.Success)
        val editedOcc = (editResult as EditOccurrenceResult.Success).occurrence
        assertEquals(5500L, editedOcc.amount.amountMinor)
        assertEquals("Higher usage due to lawn watering", editedOcc.notes)

        // Rule itself remains unchanged ($40.00 default)
        val currentRule = recurringRepository.getRecurringById(rule.id)
        assertEquals(4000L, currentRule?.amount?.amountMinor)
    }

    @Test
    fun testRuleLifecycle_PauseResumeCancelDelete() = runBlocking {
        val saveResult = manageRecurringRuleUseCase.saveRule(
            title = "Streaming Service",
            amountMinor = 1200L,
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-checking",
            categoryId = "cat-ent",
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = baseTime
        )
        val rule = (saveResult as SaveRecurringRuleResult.Success).rule

        // 1. Pause rule
        manageRecurringRuleUseCase.setRuleActive(rule.id, false)
        var currentRule = recurringRepository.getRecurringById(rule.id)
        assertFalse(currentRule!!.isActive)

        // 2. Resume rule
        manageRecurringRuleUseCase.setRuleActive(rule.id, true)
        currentRule = recurringRepository.getRecurringById(rule.id)
        assertTrue(currentRule!!.isActive)

        // 3. Cancel rule
        manageRecurringRuleUseCase.cancelRule(rule.id)
        currentRule = recurringRepository.getRecurringById(rule.id)
        assertTrue(currentRule!!.isCancelled)
        assertFalse(currentRule.isActive)

        // Active flow should exclude cancelled rule
        val activeRules = recurringRepository.getActiveRecurringFlow().first()
        assertTrue(activeRules.none { it.id == rule.id })
    }

    @Test
    fun testHistoricalTransactionsPreservedWhenRuleDeleted() = runBlocking {
        val saveResult = manageRecurringRuleUseCase.saveRule(
            title = "Magazine",
            amountMinor = 1000L,
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-checking",
            categoryId = "cat-news",
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = baseTime
        )
        val rule = (saveResult as SaveRecurringRuleResult.Success).rule

        // Pay the first occurrence to generate a historical transaction
        markOccurrencePaidUseCase(rule.id, baseTime)

        // Verify transaction exists in ledger
        val transactionsBefore = transactionRepository.getAllTransactionsFlow().first()
        assertEquals(1, transactionsBefore.size)
        assertEquals(rule.id, transactionsBefore[0].recurringRuleId)

        // Delete the recurring rule
        manageRecurringRuleUseCase.deleteRule(rule.id)
        assertNull(recurringRepository.getRecurringById(rule.id))

        // Historical transaction MUST STILL EXIST in ledger!
        val transactionsAfter = transactionRepository.getAllTransactionsFlow().first()
        assertEquals(1, transactionsAfter.size)
        assertEquals(rule.id, transactionsAfter[0].recurringRuleId)
    }

    @Test
    fun testProcessDueRecurringTransactions_IdempotencyAndNoDuplicatesOnRetry() = runBlocking {
        val saveResult = manageRecurringRuleUseCase.saveRule(
            title = "Cloud Storage",
            amountMinor = 299L,
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-checking",
            categoryId = "cat-tech",
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = baseTime
        )
        val rule = (saveResult as SaveRecurringRuleResult.Success).rule

        // Simulate WorkManager execution at baseTime
        recurringRepository.processDueRecurringTransactions(baseTime)

        var allTx = transactionRepository.getAllTransactionsFlow().first()
        assertEquals(1, allTx.size)
        assertEquals(299L, allTx[0].amount.amountMinor)

        // Simulate a retry or second worker execution at the same timestamp
        recurringRepository.processDueRecurringTransactions(baseTime)

        // Must NOT create duplicate transactions!
        allTx = transactionRepository.getAllTransactionsFlow().first()
        assertEquals(1, allTx.size)
    }

    @Test
    fun testOverdueStatusNotAutomaticallyMarkedPaid() = runBlocking {
        // Due date was yesterday
        val yesterday = baseTime - 86_400_000L
        manageRecurringRuleUseCase.saveRule(
            title = "Past Due Bill",
            amountMinor = 5000L,
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-checking",
            categoryId = "cat-bills",
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = yesterday
        )

        // Query upcoming occurrences using baseTime as "now"
        val occurrences = getUpcomingOccurrencesUseCase(windowDays = 30, nowMillis = baseTime).first()
        val overdueOcc = occurrences.find { it.ruleTitle == "Past Due Bill" }
        assertNotNull(overdueOcc)

        // Important Behavior: Do not automatically mark a bill as paid merely because its due date passed!
        assertEquals(OccurrenceStatus.OVERDUE, overdueOcc?.status)
    }
}

// In-memory fake repositories for recurring unit tests
class FakeRecurringRepository(
    private val transactionRepository: FakeTransactionRepository
) : RecurringRepository {
    private val rules = MutableStateFlow<Map<String, RecurringTransaction>>(emptyMap())
    private val occurrences = MutableStateFlow<Map<String, RecurringOccurrence>>(emptyMap())

    override fun getAllRecurringFlow(): Flow<List<RecurringTransaction>> =
        rules.asStateFlow().map { it.values.toList() }

    override fun getActiveRecurringFlow(): Flow<List<RecurringTransaction>> =
        rules.asStateFlow().map { it.values.filter { r -> r.isActive && !r.isCancelled } }

    override fun getActiveSubscriptionsFlow(): Flow<List<RecurringTransaction>> =
        rules.asStateFlow().map { it.values.filter { r -> r.isActive && !r.isCancelled && r.isSubscription } }

    override suspend fun getRecurringById(id: String): RecurringTransaction? =
        rules.value[id]

    override suspend fun getDueRecurring(timestamp: Long): List<RecurringTransaction> =
        rules.value.values.filter { it.isActive && !it.isCancelled && it.nextDueDate <= timestamp }

    override suspend fun saveRecurring(recurring: RecurringTransaction) {
        rules.value = rules.value + (recurring.id to recurring)
    }

    override suspend fun deleteRecurring(id: String) {
        rules.value = rules.value - id
    }

    override suspend fun setRuleActive(id: String, isActive: Boolean) {
        val rule = rules.value[id] ?: return
        rules.value = rules.value + (id to rule.copy(isActive = isActive))
    }

    override suspend fun cancelRule(id: String) {
        val rule = rules.value[id] ?: return
        rules.value = rules.value + (id to rule.copy(isActive = false, isCancelled = true))
    }

    override suspend fun getOccurrenceById(id: String): RecurringOccurrence? =
        occurrences.value[id]

    override fun getOccurrencesInRangeFlow(startDate: Long, endDate: Long): Flow<List<RecurringOccurrence>> =
        occurrences.asStateFlow().map { it.values.filter { occ -> occ.dueDate in startDate..endDate } }

    override suspend fun getOccurrencesInRange(startDate: Long, endDate: Long): List<RecurringOccurrence> =
        occurrences.value.values.filter { it.dueDate in startDate..endDate }

    override suspend fun saveOccurrence(occurrence: RecurringOccurrence) {
        occurrences.value = occurrences.value + (occurrence.id to occurrence)
    }

    override suspend fun markOccurrencePaid(occurrenceId: String, paidDate: Long, transactionId: String) {
        val occ = occurrences.value[occurrenceId] ?: return
        occurrences.value = occurrences.value + (occurrenceId to occ.copy(
            status = OccurrenceStatus.PAID,
            paidDate = paidDate,
            transactionId = transactionId
        ))
    }

    override suspend fun markOccurrenceSkipped(occurrenceId: String) {
        val occ = occurrences.value[occurrenceId] ?: return
        occurrences.value = occurrences.value + (occurrenceId to occ.copy(
            status = OccurrenceStatus.SKIPPED
        ))
    }

    override suspend fun advanceRuleDueDate(ruleId: String, newDueDate: Long, processedDate: Long) {
        val rule = rules.value[ruleId] ?: return
        rules.value = rules.value + (ruleId to rule.copy(nextDueDate = newDueDate, lastProcessedDate = processedDate))
    }

    override suspend fun processDueRecurringTransactions(nowMillis: Long) {
        val dueRules = getDueRecurring(nowMillis)
        for (rule in dueRules) {
            val occurrenceId = "${rule.id}_${rule.nextDueDate}"
            val existingOcc = occurrences.value[occurrenceId]
            if (existingOcc != null && (existingOcc.status == OccurrenceStatus.PAID || existingOcc.status == OccurrenceStatus.SKIPPED || existingOcc.status == OccurrenceStatus.GENERATED)) {
                // Already processed, advance rule
                val nextDue = RecurringDateEngine.calculateNextDueDate(
                    currentDueDateMillis = rule.nextDueDate,
                    frequency = rule.frequency,
                    anchorDayOfMonth = rule.anchorDayOfMonth,
                    customIntervalValue = rule.customIntervalValue,
                    customIntervalUnit = rule.customIntervalUnit
                )
                advanceRuleDueDate(rule.id, nextDue, nowMillis)
                continue
            }

            // Create ledger transaction
            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                amount = rule.amount,
                type = rule.type,
                sourceAccountId = rule.accountId,
                destinationAccountId = rule.destinationAccountId,
                categoryId = rule.categoryId,
                merchant = rule.title,
                timestamp = rule.nextDueDate,
                description = "Recurring: ${rule.title}",
                recurringRuleId = rule.id
            )
            transactionRepository.createTransaction(tx)

            // Record occurrence
            val occ = RecurringOccurrence(
                id = occurrenceId,
                ruleId = rule.id,
                ruleTitle = rule.title,
                amount = rule.amount,
                type = rule.type,
                accountId = rule.accountId,
                destinationAccountId = rule.destinationAccountId,
                categoryId = rule.categoryId,
                dueDate = rule.nextDueDate,
                status = OccurrenceStatus.GENERATED,
                paidDate = nowMillis,
                transactionId = tx.id,
                isVariableAmount = rule.isVariableAmount,
                isSubscription = rule.isSubscription,
                notes = rule.notes
            )
            saveOccurrence(occ)

            val nextDue = RecurringDateEngine.calculateNextDueDate(
                currentDueDateMillis = rule.nextDueDate,
                frequency = rule.frequency,
                anchorDayOfMonth = rule.anchorDayOfMonth,
                customIntervalValue = rule.customIntervalValue,
                customIntervalUnit = rule.customIntervalUnit
            )
            advanceRuleDueDate(rule.id, nextDue, nowMillis)
        }
    }
}

class FakeAccountRepository : AccountRepository {
    private val accounts = MutableStateFlow<Map<String, Account>>(emptyMap())

    override fun getAllAccountsFlow(): Flow<List<Account>> =
        accounts.asStateFlow().map { it.values.toList() }

    override fun getActiveAccountsFlow(): Flow<List<Account>> =
        accounts.asStateFlow().map { it.values.filter { acc -> !acc.isArchived } }

    override suspend fun getAccountById(id: String): Account? =
        accounts.value[id]

    override suspend fun saveAccount(account: Account) {
        accounts.value = accounts.value + (account.id to account)
    }

    override suspend fun deleteAccount(id: String) {
        accounts.value = accounts.value - id
    }

    override suspend fun setArchived(id: String, isArchived: Boolean) {
        val acc = accounts.value[id] ?: return
        accounts.value = accounts.value + (id to acc.copy(isArchived = isArchived))
    }

    override suspend fun updateBalances(accountId: String, balance: Money, availableBalance: Money) {
        val acc = accounts.value[accountId] ?: return
        accounts.value = accounts.value + (accountId to acc.copy(balance = balance, availableBalance = availableBalance))
    }
}

class FakeTransactionRepository : TransactionRepository {
    private val transactions = MutableStateFlow<Map<String, Transaction>>(emptyMap())

    override fun getAllTransactionsFlow(): Flow<List<Transaction>> =
        transactions.asStateFlow().map { it.values.toList().sortedByDescending { tx -> tx.timestamp } }

    override fun getRecentTransactionsFlow(limit: Int): Flow<List<Transaction>> =
        transactions.asStateFlow().map { it.values.toList().sortedByDescending { tx -> tx.timestamp }.take(limit) }

    override fun getTransactionsByAccountFlow(accountId: String): Flow<List<Transaction>> =
        transactions.asStateFlow().map { it.values.filter { tx -> tx.sourceAccountId == accountId || tx.destinationAccountId == accountId } }

    override fun getTransactionsByCategoryFlow(categoryId: String): Flow<List<Transaction>> =
        transactions.asStateFlow().map { it.values.filter { tx -> tx.categoryId == categoryId } }

    override fun getTransactionsByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactions.asStateFlow().map { it.values.filter { tx -> tx.timestamp in startDate..endDate } }

    override fun searchTransactionsFlow(query: String): Flow<List<Transaction>> =
        transactions.asStateFlow().map {
            it.values.filter { tx ->
                tx.description.contains(query, ignoreCase = true) || (tx.merchant?.contains(query, ignoreCase = true) == true)
            }
        }

    override suspend fun getTransactionById(id: String): Transaction? =
        transactions.value[id]

    override suspend fun createTransaction(transaction: Transaction) {
        transactions.value = transactions.value + (transaction.id to transaction)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactions.value = transactions.value + (transaction.id to transaction)
    }

    override suspend fun deleteTransaction(id: String) {
        transactions.value = transactions.value - id
    }

    override fun getFrequentCategoryIdsFlow(type: TransactionType, limit: Int): Flow<List<String>> =
        transactions.asStateFlow().map { map ->
            map.values.filter { it.type == type }
                .groupingBy { it.categoryId }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key }
        }

    override fun getFrequentMerchantsFlow(limit: Int): Flow<List<String>> =
        transactions.asStateFlow().map { map ->
            map.values.mapNotNull { it.merchant?.takeIf { m -> m.isNotBlank() } }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key }
        }

    override suspend fun getSuggestedCategoryForMerchant(merchant: String): String? =
        transactions.value.values
            .filter { it.merchant.equals(merchant, ignoreCase = true) }
            .groupingBy { it.categoryId }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

    override fun getUnreviewedTransactionsFlow(): Flow<List<Transaction>> =
        transactions.asStateFlow().map { it.values.filter { tx -> !tx.isCategoryConfirmed || tx.categoryId == "cat_uncategorized" } }

    override fun getUnreviewedCountFlow(): Flow<Int> =
        transactions.asStateFlow().map { it.values.count { tx -> !tx.isCategoryConfirmed || tx.categoryId == "cat_uncategorized" } }

    override suspend fun confirmTransactionCategory(id: String, categoryId: String, matchedRuleId: String?, confidence: Float) {
        val tx = transactions.value[id] ?: return
        transactions.value = transactions.value + (id to tx.copy(
            categoryId = categoryId,
            isCategoryConfirmed = true,
            matchedRuleId = matchedRuleId,
            categorizationConfidence = confidence
        ))
    }

    override suspend fun bulkUpdateCategory(ids: List<String>, categoryId: String, isConfirmed: Boolean, matchedRuleId: String?) {
        val current = transactions.value.toMutableMap()
        for (id in ids) {
            val tx = current[id] ?: continue
            current[id] = tx.copy(
                categoryId = categoryId,
                isCategoryConfirmed = isConfirmed,
                matchedRuleId = matchedRuleId
            )
        }
        transactions.value = current
    }
}

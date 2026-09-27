package com.finpulse.app.domain.review

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.MerchantSignal
import com.finpulse.app.domain.model.OccurrenceStatus
import com.finpulse.app.domain.model.PaymentFrequency
import com.finpulse.app.domain.model.RecurringOccurrence
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.ReviewItemType
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.MerchantSignalRepository
import com.finpulse.app.domain.repository.RecurringRepository
import com.finpulse.app.domain.repository.TransactionRepository
import com.finpulse.app.domain.usecase.recurring.MarkOccurrencePaidUseCase
import com.finpulse.app.domain.usecase.recurring.MarkPaidResult
import com.finpulse.app.domain.usecase.review.BulkCategorizeTransactionsUseCase
import com.finpulse.app.domain.usecase.review.ResolveDuplicateTransactionUseCase
import com.finpulse.app.domain.usecase.review.UpdateTransactionDetailsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReviewInboxLifecycleTest {

    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var signalRepository: FakeMerchantSignalRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var recurringRepository: FakeRecurringRepository

    private lateinit var resolveDuplicateUseCase: ResolveDuplicateTransactionUseCase
    private lateinit var bulkCategorizeUseCase: BulkCategorizeTransactionsUseCase
    private lateinit var updateDetailsUseCase: UpdateTransactionDetailsUseCase
    private lateinit var markPaidUseCase: MarkOccurrencePaidUseCase

    private val dismissedKeys = mutableSetOf<String>()

    @Before
    fun setup() {
        transactionRepository = FakeTransactionRepository()
        signalRepository = FakeMerchantSignalRepository()
        accountRepository = FakeAccountRepository()
        recurringRepository = FakeRecurringRepository()

        bulkCategorizeUseCase = BulkCategorizeTransactionsUseCase(transactionRepository, signalRepository)
        updateDetailsUseCase = UpdateTransactionDetailsUseCase(transactionRepository, signalRepository)
        markPaidUseCase = MarkOccurrencePaidUseCase(recurringRepository, transactionRepository)
    }

    @Test
    fun `test resolve duplicate keeps primary and deletes duplicate`() = runBlocking {
        val tx1 = Transaction(
            id = "tx_keep",
            amount = Money(5000),
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc_1",
            categoryId = "cat_groceries",
            merchant = "Supermarket",
            timestamp = 1000L
        )

        val tx2 = Transaction(
            id = "tx_delete",
            amount = Money(5000),
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc_1",
            categoryId = "cat_groceries",
            merchant = "Supermarket",
            timestamp = 1000L
        )

        transactionRepository.createTransaction(tx1)
        transactionRepository.createTransaction(tx2)

        // Resolve by keeping tx1 and deleting tx2
        transactionRepository.deleteTransaction(tx2.id)
        dismissedKeys.add("inbox_dup_tx_keep_tx_delete")

        assertNotNull(transactionRepository.getTransactionById("tx_keep"))
        assertNull(transactionRepository.getTransactionById("tx_delete"))
        assertTrue(dismissedKeys.contains("inbox_dup_tx_keep_tx_delete"))
    }

    @Test
    fun `test bulk categorize updates all specified transactions and learns merchant signals`() = runBlocking {
        val tx1 = Transaction(
            id = "tx_1",
            amount = Money(450),
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc_1",
            categoryId = "cat_uncategorized",
            merchant = "Starbucks",
            isCategoryConfirmed = false
        )
        val tx2 = Transaction(
            id = "tx_2",
            amount = Money(550),
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc_1",
            categoryId = "cat_uncategorized",
            merchant = "Starbucks",
            isCategoryConfirmed = false
        )

        transactionRepository.createTransaction(tx1)
        transactionRepository.createTransaction(tx2)

        bulkCategorizeUseCase(
            transactionIds = listOf("tx_1", "tx_2"),
            targetCategoryId = "cat_coffee",
            isConfirmed = true
        )

        val updated1 = transactionRepository.getTransactionById("tx_1")
        val updated2 = transactionRepository.getTransactionById("tx_2")

        assertEquals("cat_coffee", updated1?.categoryId)
        assertTrue(updated1?.isCategoryConfirmed == true)
        assertEquals("cat_coffee", updated2?.categoryId)
        assertTrue(updated2?.isCategoryConfirmed == true)

        val signal = signalRepository.getSignal("starbucks")
        assertNotNull(signal)
        assertEquals("cat_coffee", signal?.categoryId)
    }

    @Test
    fun `test edit merchant updates transaction and records signal`() = runBlocking {
        val tx = Transaction(
            id = "tx_edit_m",
            amount = Money(2500),
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc_1",
            categoryId = "cat_groceries",
            merchant = null,
            description = "SAFEWAY STORE 1234",
            isCategoryConfirmed = true
        )
        transactionRepository.createTransaction(tx)

        updateDetailsUseCase.editMerchant(
            transactionId = "tx_edit_m",
            newMerchant = "Safeway",
            updateSignal = true
        )

        val updated = transactionRepository.getTransactionById("tx_edit_m")
        assertEquals("Safeway", updated?.merchant)

        val signal = signalRepository.getSignal("safeway")
        assertNotNull(signal)
        assertEquals("cat_groceries", signal?.categoryId)
    }

    @Test
    fun `test change account reassigns transaction sourceAccountId`() = runBlocking {
        val tx = Transaction(
            id = "tx_acc",
            amount = Money(1000),
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc_old",
            categoryId = "cat_groceries"
        )
        transactionRepository.createTransaction(tx)

        updateDetailsUseCase.changeAccount(
            transactionId = "tx_acc",
            newAccountId = "acc_new"
        )

        val updated = transactionRepository.getTransactionById("tx_acc")
        assertEquals("acc_new", updated?.sourceAccountId)
    }

    @Test
    fun `test mark overdue recurring bill paid generates transaction and advances occurrence`() = runBlocking {
        val rule = RecurringTransaction(
            id = "rule_electric",
            title = "Electricity",
            amount = Money(8500),
            type = TransactionType.EXPENSE,
            accountId = "acc_checking",
            categoryId = "cat_utilities",
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = 1000000L,
            isActive = true
        )
        recurringRepository.saveRecurring(rule)

        val result = markPaidUseCase(
            ruleId = "rule_electric",
            occurrenceDueDate = 1000000L,
            actualAmountMinor = 8500L,
            paidDate = 1000500L
        )

        assertTrue(result is MarkPaidResult.Success)
        val success = result as MarkPaidResult.Success
        assertEquals(Money(8500), success.transaction.amount)
        assertEquals(OccurrenceStatus.PAID, success.occurrence.status)
        assertEquals("Electricity", success.transaction.merchant)
    }
}

// -------------------------------------------------------------
// Test Doubles
// -------------------------------------------------------------

private class FakeTransactionRepository : TransactionRepository {
    val txMap = mutableMapOf<String, Transaction>()

    override fun getAllTransactionsFlow(): Flow<List<Transaction>> =
        MutableStateFlow(txMap.values.toList())

    override fun getRecentTransactionsFlow(limit: Int): Flow<List<Transaction>> =
        MutableStateFlow(txMap.values.take(limit))

    override fun getTransactionsByAccountFlow(accountId: String): Flow<List<Transaction>> =
        MutableStateFlow(txMap.values.filter { it.sourceAccountId == accountId })

    override fun getTransactionsByCategoryFlow(categoryId: String): Flow<List<Transaction>> =
        MutableStateFlow(txMap.values.filter { it.categoryId == categoryId })

    override fun getTransactionsByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        MutableStateFlow(txMap.values.filter { it.timestamp in startDate..endDate })

    override fun searchTransactionsFlow(query: String): Flow<List<Transaction>> =
        MutableStateFlow(txMap.values.filter { it.description.contains(query) })

    override suspend fun getTransactionById(id: String): Transaction? = txMap[id]

    override suspend fun createTransaction(transaction: Transaction) {
        txMap[transaction.id] = transaction
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        txMap[transaction.id] = transaction
    }

    override suspend fun deleteTransaction(id: String) {
        txMap.remove(id)
    }

    override fun getFrequentCategoryIdsFlow(type: TransactionType, limit: Int): Flow<List<String>> =
        MutableStateFlow(emptyList())

    override fun getFrequentMerchantsFlow(limit: Int): Flow<List<String>> =
        MutableStateFlow(emptyList())

    override suspend fun getSuggestedCategoryForMerchant(merchant: String): String? = null

    override fun getUnreviewedTransactionsFlow(): Flow<List<Transaction>> =
        MutableStateFlow(txMap.values.filter { !it.isCategoryConfirmed })

    override fun getUnreviewedCountFlow(): Flow<Int> =
        MutableStateFlow(txMap.values.count { !it.isCategoryConfirmed })

    override suspend fun confirmTransactionCategory(id: String, categoryId: String, matchedRuleId: String?, confidence: Float) {
        val tx = txMap[id] ?: return
        txMap[id] = tx.copy(categoryId = categoryId, isCategoryConfirmed = true, matchedRuleId = matchedRuleId, categorizationConfidence = confidence)
    }

    override suspend fun bulkUpdateCategory(ids: List<String>, categoryId: String, isConfirmed: Boolean, matchedRuleId: String?) {
        for (id in ids) {
            val tx = txMap[id] ?: continue
            txMap[id] = tx.copy(categoryId = categoryId, isCategoryConfirmed = isConfirmed)
        }
    }
}

private class FakeMerchantSignalRepository : MerchantSignalRepository {
    val signals = mutableMapOf<String, MerchantSignal>()

    override fun getAllSignalsFlow(): Flow<List<MerchantSignal>> =
        MutableStateFlow(signals.values.toList())

    override suspend fun getAllSignals(): List<MerchantSignal> = signals.values.toList()

    override suspend fun getSignal(normalizedMerchant: String): MerchantSignal? =
        signals[normalizedMerchant.lowercase().trim()]

    override suspend fun recordSignal(merchant: String, categoryId: String) {
        val key = merchant.lowercase().trim()
        val current = signals[key]
        if (current != null) {
            signals[key] = current.copy(categoryId = categoryId, useCount = current.useCount + 1)
        } else {
            signals[key] = MerchantSignal(key, categoryId, useCount = 1)
        }
    }

    override suspend fun clearAllSignals() {
        signals.clear()
    }
}

private class FakeAccountRepository : AccountRepository {
    val accounts = mutableMapOf<String, Account>()

    override fun getAllAccountsFlow(): Flow<List<Account>> = MutableStateFlow(accounts.values.toList())
    override fun getActiveAccountsFlow(): Flow<List<Account>> = MutableStateFlow(accounts.values.filter { !it.isArchived })
    override suspend fun getAccountById(id: String): Account? = accounts[id]
    override suspend fun saveAccount(account: Account) { accounts[account.id] = account }
    override suspend fun deleteAccount(id: String) { accounts.remove(id) }
    override suspend fun setArchived(id: String, isArchived: Boolean) {
        val acc = accounts[id] ?: return
        accounts[id] = acc.copy(isArchived = isArchived)
    }
    override suspend fun updateBalances(accountId: String, balance: Money, availableBalance: Money) {
        val acc = accounts[accountId] ?: return
        accounts[accountId] = acc.copy(balance = balance, availableBalance = availableBalance)
    }
}

private class FakeRecurringRepository : RecurringRepository {
    val rules = mutableMapOf<String, RecurringTransaction>()
    val occurrences = mutableMapOf<String, RecurringOccurrence>()

    override fun getAllRecurringFlow(): Flow<List<RecurringTransaction>> = MutableStateFlow(rules.values.toList())
    override fun getActiveRecurringFlow(): Flow<List<RecurringTransaction>> = MutableStateFlow(rules.values.filter { it.isActive })
    override fun getActiveSubscriptionsFlow(): Flow<List<RecurringTransaction>> = MutableStateFlow(rules.values.filter { it.isActive && it.isSubscription })
    override suspend fun getRecurringById(id: String): RecurringTransaction? = rules[id]
    override suspend fun getDueRecurring(timestamp: Long): List<RecurringTransaction> = rules.values.filter { it.nextDueDate <= timestamp }
    override suspend fun saveRecurring(recurring: RecurringTransaction) { rules[recurring.id] = recurring }
    override suspend fun deleteRecurring(id: String) { rules.remove(id) }
    override suspend fun setRuleActive(id: String, isActive: Boolean) {
        val r = rules[id] ?: return
        rules[id] = r.copy(isActive = isActive)
    }
    override suspend fun cancelRule(id: String) {
        val r = rules[id] ?: return
        rules[id] = r.copy(isCancelled = true)
    }
    override suspend fun getOccurrenceById(id: String): RecurringOccurrence? = occurrences[id]
    override fun getOccurrencesInRangeFlow(startDate: Long, endDate: Long): Flow<List<RecurringOccurrence>> =
        MutableStateFlow(occurrences.values.filter { it.dueDate in startDate..endDate })
    override suspend fun getOccurrencesInRange(startDate: Long, endDate: Long): List<RecurringOccurrence> =
        occurrences.values.filter { it.dueDate in startDate..endDate }
    override suspend fun saveOccurrence(occurrence: RecurringOccurrence) { occurrences[occurrence.id] = occurrence }
    override suspend fun markOccurrencePaid(occurrenceId: String, paidDate: Long, transactionId: String) {
        val o = occurrences[occurrenceId] ?: return
        occurrences[occurrenceId] = o.copy(status = OccurrenceStatus.PAID, paidDate = paidDate, transactionId = transactionId)
    }
    override suspend fun markOccurrenceSkipped(occurrenceId: String) {
        val o = occurrences[occurrenceId] ?: return
        occurrences[occurrenceId] = o.copy(status = OccurrenceStatus.SKIPPED)
    }
    override suspend fun advanceRuleDueDate(ruleId: String, newDueDate: Long, processedDate: Long) {
        val r = rules[ruleId] ?: return
        rules[ruleId] = r.copy(nextDueDate = newDueDate, lastProcessedDate = processedDate)
    }
    override suspend fun processDueRecurringTransactions(nowMillis: Long) {}
}

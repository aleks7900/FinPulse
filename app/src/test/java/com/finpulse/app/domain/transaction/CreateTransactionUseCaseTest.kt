package com.finpulse.app.domain.transaction

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.TransactionRepository
import com.finpulse.app.domain.usecase.transaction.CreateTransactionResult
import com.finpulse.app.domain.usecase.transaction.CreateTransactionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateTransactionUseCaseTest {

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var useCase: CreateTransactionUseCase

    @Before
    fun setup() {
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        useCase = CreateTransactionUseCase(transactionRepository, accountRepository)

        // Seed default test accounts
        runBlocking {
            accountRepository.saveAccount(
                Account(
                    id = "acc-main",
                    name = "Main Checking",
                    type = AccountType.BANK,
                    balance = Money(100000L, "USD"), // $1,000.00
                    availableBalance = Money(100000L, "USD"),
                    isArchived = false
                )
            )
            accountRepository.saveAccount(
                Account(
                    id = "acc-savings",
                    name = "Savings",
                    type = AccountType.SAVINGS,
                    balance = Money(500000L, "USD"), // $5,000.00
                    availableBalance = Money(500000L, "USD"),
                    isArchived = false
                )
            )
            accountRepository.saveAccount(
                Account(
                    id = "acc-eur",
                    name = "Euro Account",
                    type = AccountType.BANK,
                    balance = Money(200000L, "EUR"), // €2,000.00
                    availableBalance = Money(200000L, "EUR"),
                    isArchived = false
                )
            )
            accountRepository.saveAccount(
                Account(
                    id = "acc-archived",
                    name = "Old Card",
                    type = AccountType.CREDIT_CARD,
                    balance = Money(0L, "USD"),
                    availableBalance = Money(0L, "USD"),
                    isArchived = true
                )
            )
        }
    }

    @Test
    fun testCreateExpense_Success() = runBlocking {
        val result = useCase(
            amountMinor = 1550L, // $15.50
            currencyCode = "USD",
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-main",
            categoryId = "cat-groceries",
            merchant = "Trader Joe's",
            description = "Groceries run"
        )

        assertTrue("Result should be Success", result is CreateTransactionResult.Success)
        val tx = (result as CreateTransactionResult.Success).transaction
        assertEquals(1550L, tx.amount.amountMinor)
        assertEquals("USD", tx.amount.currencyCode)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("acc-main", tx.sourceAccountId)
        assertEquals("cat-groceries", tx.categoryId)
        assertEquals("Trader Joe's", tx.merchant)
        assertEquals("Groceries run", tx.description)

        // Verify transaction is stored in repository
        val stored = transactionRepository.getTransactionById(tx.id)
        assertNotNull(stored)
        assertEquals(tx.id, stored?.id)
    }

    @Test
    fun testCreateIncome_Success() = runBlocking {
        val result = useCase(
            amountMinor = 300000L, // $3,000.00
            currencyCode = "USD",
            type = TransactionType.INCOME,
            sourceAccountId = "acc-main",
            categoryId = "cat-salary",
            merchant = "Acme Corp",
            description = "Monthly Paycheck"
        )

        assertTrue(result is CreateTransactionResult.Success)
        val tx = (result as CreateTransactionResult.Success).transaction
        assertEquals(300000L, tx.amount.amountMinor)
        assertEquals(TransactionType.INCOME, tx.type)
    }

    @Test
    fun testCreateTransfer_Success() = runBlocking {
        val result = useCase(
            amountMinor = 50000L, // $500.00
            currencyCode = "USD",
            type = TransactionType.TRANSFER,
            sourceAccountId = "acc-main",
            destinationAccountId = "acc-savings",
            categoryId = "cat-transfer",
            description = "Move to savings"
        )

        assertTrue(result is CreateTransactionResult.Success)
        val tx = (result as CreateTransactionResult.Success).transaction
        assertEquals(TransactionType.TRANSFER, tx.type)
        assertEquals("acc-main", tx.sourceAccountId)
        assertEquals("acc-savings", tx.destinationAccountId)
    }

    @Test
    fun testRejectZeroOrNegativeAmount() = runBlocking {
        val zeroResult = useCase(
            amountMinor = 0L,
            currencyCode = "USD",
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-main",
            categoryId = "cat-groceries"
        )
        assertTrue(zeroResult is CreateTransactionResult.Error)
        assertEquals("Amount must be greater than zero", (zeroResult as CreateTransactionResult.Error).message)

        val negativeResult = useCase(
            amountMinor = -500L,
            currencyCode = "USD",
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-main",
            categoryId = "cat-groceries"
        )
        assertTrue(negativeResult is CreateTransactionResult.Error)
        assertEquals("Amount must be greater than zero", (negativeResult as CreateTransactionResult.Error).message)
    }

    @Test
    fun testRejectArchivedSourceAccount() = runBlocking {
        val result = useCase(
            amountMinor = 2000L,
            currencyCode = "USD",
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-archived",
            categoryId = "cat-groceries"
        )
        assertTrue(result is CreateTransactionResult.Error)
        assertEquals("Cannot record transactions on an archived account", (result as CreateTransactionResult.Error).message)
    }

    @Test
    fun testRejectArchivedDestinationAccountInTransfer() = runBlocking {
        val result = useCase(
            amountMinor = 2000L,
            currencyCode = "USD",
            type = TransactionType.TRANSFER,
            sourceAccountId = "acc-main",
            destinationAccountId = "acc-archived",
            categoryId = "cat-transfer"
        )
        assertTrue(result is CreateTransactionResult.Error)
        assertEquals("Cannot transfer to an archived account", (result as CreateTransactionResult.Error).message)
    }

    @Test
    fun testRejectSelfTransfer() = runBlocking {
        val result = useCase(
            amountMinor = 2000L,
            currencyCode = "USD",
            type = TransactionType.TRANSFER,
            sourceAccountId = "acc-main",
            destinationAccountId = "acc-main",
            categoryId = "cat-transfer"
        )
        assertTrue(result is CreateTransactionResult.Error)
        assertEquals("Source and destination accounts cannot be identical", (result as CreateTransactionResult.Error).message)
    }

    @Test
    fun testRejectCurrencyMismatchOnSource() = runBlocking {
        // Source account acc-main is USD, but tx currency is EUR
        val result = useCase(
            amountMinor = 2000L,
            currencyCode = "EUR",
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-main",
            categoryId = "cat-food"
        )
        assertTrue(result is CreateTransactionResult.Error)
        assertTrue((result as CreateTransactionResult.Error).message.contains("Currency mismatch"))
    }

    @Test
    fun testRejectDifferentCurrenciesInTransfer() = runBlocking {
        // Main is USD, acc-eur is EUR
        val result = useCase(
            amountMinor = 2000L,
            currencyCode = "USD",
            type = TransactionType.TRANSFER,
            sourceAccountId = "acc-main",
            destinationAccountId = "acc-eur",
            categoryId = "cat-transfer"
        )
        assertTrue(result is CreateTransactionResult.Error)
        assertTrue((result as CreateTransactionResult.Error).message.contains("Transfer between different currencies"))
    }

    @Test
    fun testRejectBlankCategory() = runBlocking {
        val result = useCase(
            amountMinor = 2000L,
            currencyCode = "USD",
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-main",
            categoryId = "   "
        )
        assertTrue(result is CreateTransactionResult.Error)
        assertEquals("Category must be specified", (result as CreateTransactionResult.Error).message)
    }

    @Test
    fun testUpdateExistingTransaction() = runBlocking {
        // Create initial transaction
        val createResult = useCase(
            amountMinor = 2500L,
            currencyCode = "USD",
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-main",
            categoryId = "cat-dining",
            merchant = "Starbucks"
        )
        val initialTx = (createResult as CreateTransactionResult.Success).transaction

        // Update the existing transaction with same ID
        val updateResult = useCase(
            id = initialTx.id,
            amountMinor = 3500L, // changed amount
            currencyCode = "USD",
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-main",
            categoryId = "cat-dining",
            merchant = "Starbucks Reserve"
        )

        assertTrue(updateResult is CreateTransactionResult.Success)
        val updatedTx = (updateResult as CreateTransactionResult.Success).transaction
        assertEquals(initialTx.id, updatedTx.id)
        assertEquals(3500L, updatedTx.amount.amountMinor)
        assertEquals("Starbucks Reserve", updatedTx.merchant)

        val inRepo = transactionRepository.getTransactionById(initialTx.id)
        assertEquals(3500L, inRepo?.amount?.amountMinor)
        assertEquals("Starbucks Reserve", inRepo?.merchant)
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

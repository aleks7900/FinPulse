package com.finpulse.app.domain.transaction

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.ExchangeRate
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.ExchangeRateProvider
import com.finpulse.app.domain.repository.TransactionRepository
import com.finpulse.app.domain.usecase.transaction.CreateTransactionResult
import com.finpulse.app.domain.usecase.transaction.CreateTransactionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CrossCurrencyTransferTest {

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var rateProvider: ExchangeRateProvider
    private lateinit var useCase: CreateTransactionUseCase

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()

        rateProvider = object : ExchangeRateProvider {
            override suspend fun getRate(fromCurrency: String, toCurrency: String): ExchangeRate {
                val from = fromCurrency.uppercase()
                val to = toCurrency.uppercase()
                if (from == to) return ExchangeRate(from, to, 1.0)
                if (from == "USD" && to == "EUR") return ExchangeRate("USD", "EUR", 0.92)
                if (from == "EUR" && to == "USD") return ExchangeRate("EUR", "USD", 1.0 / 0.92)
                if (from == "USD" && to == "JPY") return ExchangeRate("USD", "JPY", 155.0)
                if (from == "JPY" && to == "USD") return ExchangeRate("JPY", "USD", 1.0 / 155.0)
                error("Unsupported test rate: $from to $to")
            }

            override fun getRateFlow(fromCurrency: String, toCurrency: String): Flow<ExchangeRate> =
                flowOf(runBlocking { getRate(fromCurrency, toCurrency) })

            override suspend fun getAllRates(): List<ExchangeRate> = emptyList()
            override fun getAllRatesFlow(): Flow<List<ExchangeRate>> = flowOf(emptyList())
            override suspend fun setManualRate(fromCurrency: String, toCurrency: String, rate: Double) {}
            override suspend fun resetToDefault(fromCurrency: String, toCurrency: String) {}
            override fun getLastUpdatedTimestampFlow(): Flow<Long?> = flowOf(System.currentTimeMillis())
            override suspend fun refreshRates(): Result<Unit> = Result.success(Unit)
        }

        useCase = CreateTransactionUseCase(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            rateProvider = rateProvider
        )

        runBlocking {
            accountRepository.saveAccount(
                Account(
                    id = "acc-usd",
                    name = "USD Checking",
                    type = AccountType.BANK,
                    balance = Money(100000L, "USD"), // $1,000.00
                    availableBalance = Money(100000L, "USD")
                )
            )
            accountRepository.saveAccount(
                Account(
                    id = "acc-eur",
                    name = "EUR Travel Card",
                    type = AccountType.BANK,
                    balance = Money(50000L, "EUR"), // €500.00
                    availableBalance = Money(50000L, "EUR")
                )
            )
            accountRepository.saveAccount(
                Account(
                    id = "acc-jpy",
                    name = "JPY Wallet",
                    type = AccountType.CASH,
                    balance = Money(10000L, "JPY"), // ¥10,000 (0 decimals)
                    availableBalance = Money(10000L, "JPY")
                )
            )
        }
    }

    @Test
    fun `cross-currency transfer auto-computes destination amount and stores exchange rate`() = runBlocking {
        // Transfer $100.00 USD to EUR account using rate 0.92
        val result = useCase(
            amountMinor = 10000L, // $100.00
            currencyCode = "USD",
            type = TransactionType.TRANSFER,
            sourceAccountId = "acc-usd",
            destinationAccountId = "acc-eur",
            categoryId = "cat-transfer"
        )

        assertTrue("Transfer should succeed", result is CreateTransactionResult.Success)
        val tx = (result as CreateTransactionResult.Success).transaction

        // Original transaction currency and amount are strictly preserved
        assertEquals("USD", tx.amount.currencyCode)
        assertEquals(10000L, tx.amount.amountMinor)

        // Destination amount and rate are calculated and stored
        assertNotNull(tx.destinationAmount)
        assertEquals("EUR", tx.destinationAmount!!.currencyCode)
        assertEquals(9200L, tx.destinationAmount!!.amountMinor) // 100 * 0.92 = 92.00 EUR
        assertEquals(0.92, tx.exchangeRate!!, 0.0001)
        assertNotNull(tx.exchangeRateDate)
    }

    @Test
    fun `cross-currency transfer respects real bank conversion differences and fee spreads`() = runBlocking {
        // User sends $100.00 USD, but bank charges a spread and only deposits €89.50
        val result = useCase(
            amountMinor = 10000L, // $100.00
            currencyCode = "USD",
            type = TransactionType.TRANSFER,
            sourceAccountId = "acc-usd",
            destinationAccountId = "acc-eur",
            categoryId = "cat-transfer",
            destinationAmountMinor = 8950L // Real bank converted amount (€89.50)
        )

        assertTrue(result is CreateTransactionResult.Success)
        val tx = (result as CreateTransactionResult.Success).transaction

        assertEquals(10000L, tx.amount.amountMinor)
        assertEquals("USD", tx.amount.currencyCode)

        // Custom destination amount is stored exactly
        assertEquals(8950L, tx.destinationAmount!!.amountMinor)
        assertEquals("EUR", tx.destinationAmount!!.currencyCode)

        // Effective bank rate = 89.50 / 100.00 = 0.895
        assertEquals(0.895, tx.exchangeRate!!, 0.0001)
    }

    @Test
    fun `cross-currency transfer to zero-decimal currency JPY handles minor unit scaling correctly`() = runBlocking {
        // Transfer $50.00 USD to JPY at 155.0 = 7,750 JPY
        val result = useCase(
            amountMinor = 5000L, // $50.00
            currencyCode = "USD",
            type = TransactionType.TRANSFER,
            sourceAccountId = "acc-usd",
            destinationAccountId = "acc-jpy",
            categoryId = "cat-transfer"
        )

        assertTrue(result is CreateTransactionResult.Success)
        val tx = (result as CreateTransactionResult.Success).transaction

        assertEquals(5000L, tx.amount.amountMinor)
        assertEquals("USD", tx.amount.currencyCode)

        // JPY has 0 decimals, multiplier is 1
        assertEquals("JPY", tx.destinationAmount!!.currencyCode)
        assertEquals(7750L, tx.destinationAmount!!.amountMinor)
        assertEquals(155.0, tx.exchangeRate!!, 0.0001)
    }

    // Fake repositories for clean unit test isolation
    private class FakeAccountRepository : AccountRepository {
        val accounts = MutableStateFlow<Map<String, Account>>(emptyMap())

        override fun getAllAccountsFlow(): Flow<List<Account>> = accounts.asStateFlow().map { it.values.toList() }
        override fun getActiveAccountsFlow(): Flow<List<Account>> = accounts.asStateFlow().map { it.values.filter { !it.isArchived } }
        override suspend fun getAccountById(id: String): Account? = accounts.value[id]
        override suspend fun saveAccount(account: Account) {
            accounts.value = accounts.value + (account.id to account)
        }
        override suspend fun deleteAccount(id: String) {
            accounts.value = accounts.value - id
        }
        override suspend fun setArchived(id: String, isArchived: Boolean) {
            val a = accounts.value[id] ?: return
            accounts.value = accounts.value + (id to a.copy(isArchived = isArchived))
        }
        override suspend fun updateBalances(accountId: String, balance: Money, availableBalance: Money) {
            val a = accounts.value[accountId] ?: return
            accounts.value = accounts.value + (accountId to a.copy(balance = balance, availableBalance = availableBalance))
        }
    }

    private class FakeTransactionRepository : TransactionRepository {
        val transactions = MutableStateFlow<Map<String, Transaction>>(emptyMap())

        override fun getAllTransactionsFlow(): Flow<List<Transaction>> = transactions.asStateFlow().map { it.values.toList() }
        override fun getRecentTransactionsFlow(limit: Int): Flow<List<Transaction>> = transactions.asStateFlow().map { it.values.take(limit) }
        override fun getTransactionsByAccountFlow(accountId: String): Flow<List<Transaction>> =
            transactions.asStateFlow().map { it.values.filter { tx -> tx.sourceAccountId == accountId || tx.destinationAccountId == accountId } }
        override fun getTransactionsByCategoryFlow(categoryId: String): Flow<List<Transaction>> =
            transactions.asStateFlow().map { it.values.filter { tx -> tx.categoryId == categoryId } }
        override fun getTransactionsByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            transactions.asStateFlow().map { it.values.filter { tx -> tx.timestamp in startDate..endDate } }
        override fun searchTransactionsFlow(query: String): Flow<List<Transaction>> =
            transactions.asStateFlow().map { it.values.filter { tx -> tx.description.contains(query, ignoreCase = true) } }
        override suspend fun getTransactionById(id: String): Transaction? = transactions.value[id]
        override suspend fun createTransaction(transaction: Transaction) {
            transactions.value = transactions.value + (transaction.id to transaction)
        }
        override suspend fun updateTransaction(transaction: Transaction) {
            transactions.value = transactions.value + (transaction.id to transaction)
        }
        override suspend fun deleteTransaction(id: String) {
            transactions.value = transactions.value - id
        }
        override fun getFrequentCategoryIdsFlow(type: TransactionType, limit: Int): Flow<List<String>> = flowOf(emptyList())
        override fun getFrequentMerchantsFlow(limit: Int): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getSuggestedCategoryForMerchant(merchant: String): String? = null
        override fun getUnreviewedTransactionsFlow(): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getUnreviewedCountFlow(): Flow<Int> = flowOf(0)
        override suspend fun confirmTransactionCategory(id: String, categoryId: String, matchedRuleId: String?, confidence: Float) {}
        override suspend fun bulkUpdateCategory(ids: List<String>, categoryId: String, isConfirmed: Boolean, matchedRuleId: String?) {}
    }
}

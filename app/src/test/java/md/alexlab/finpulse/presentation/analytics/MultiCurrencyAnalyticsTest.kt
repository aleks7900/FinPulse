package md.alexlab.finpulse.presentation.analytics

import md.alexlab.finpulse.core.datastore.UserPreferences
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.engine.CurrencyConverter
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.ExchangeRate
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.DebtRepository
import md.alexlab.finpulse.domain.repository.ExchangeRateProvider
import md.alexlab.finpulse.domain.repository.GoalRepository
import md.alexlab.finpulse.domain.repository.InvestmentRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository
import md.alexlab.finpulse.domain.usecase.GetDashboardSummaryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class MultiCurrencyAnalyticsTest {

    private lateinit var rateProvider: ExchangeRateProvider
    private lateinit var currencyConverter: CurrencyConverter

    @Before
    fun setUp() {
        rateProvider = object : ExchangeRateProvider {
            override suspend fun getRate(fromCurrency: String, toCurrency: String): ExchangeRate {
                val from = fromCurrency.uppercase()
                val to = toCurrency.uppercase()
                if (from == to) return ExchangeRate(from, to, 1.0)
                if (from == "EUR" && to == "USD") return ExchangeRate("EUR", "USD", 1.10)
                if (from == "USD" && to == "EUR") return ExchangeRate("USD", "EUR", 1.0 / 1.10)
                if (from == "JPY" && to == "USD") return ExchangeRate("JPY", "USD", 1.0 / 150.0)
                if (from == "USD" && to == "JPY") return ExchangeRate("USD", "JPY", 150.0)
                error("No rate for $from to $to")
            }

            override fun getRateFlow(fromCurrency: String, toCurrency: String): Flow<ExchangeRate> =
                flowOf(runBlocking { getRate(fromCurrency, toCurrency) })

            override suspend fun getAllRates(): List<ExchangeRate> = emptyList()
            override fun getAllRatesFlow(): Flow<List<ExchangeRate>> = flowOf(emptyList())
            override suspend fun setManualRate(fromCurrency: String, toCurrency: String, rate: Double) {}
            override suspend fun resetToDefault(fromCurrency: String, toCurrency: String) {}
            override fun getLastUpdatedTimestampFlow(): Flow<Long?> = flowOf(1000L)
            override suspend fun refreshRates(): Result<Unit> = Result.success(Unit)
        }

        currencyConverter = CurrencyConverter(rateProvider)
    }

    @Test
    fun `analytics aggregation safely sums transactions across currencies into base currency`() = runBlocking {
        val tx1 = Transaction(
            id = "tx1",
            amount = Money(10000L, "USD"), // $100.00
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-usd",
            categoryId = "cat-food"
        )
        val tx2 = Transaction(
            id = "tx2",
            amount = Money(5000L, "EUR"), // €50.00 at 1.10 = $55.00
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-eur",
            categoryId = "cat-travel"
        )
        val tx3 = Transaction(
            id = "tx3",
            amount = Money(15000L, "JPY"), // ¥15,000 at (1/150) = $100.00
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-jpy",
            categoryId = "cat-shopping"
        )

        // If raw units were mistakenly combined: 10000 + 5000 + 15000 = 30000 ($300.00)
        // Correct conversion to USD: $100.00 + $55.00 + $100.00 = $255.00 (25500 minor)
        val totalUsd = currencyConverter.sumTransactionsIn(listOf(tx1, tx2, tx3), "USD")
        assertEquals("USD", totalUsd.currencyCode)
        assertEquals(25500L, totalUsd.amountMinor)
        assertEquals(BigDecimal("255.00"), totalUsd.amountBigDecimal)
    }

    @Test
    fun `historical transaction exchange rate is strictly preserved during aggregation`() = runBlocking {
        // txRecordedPast was recorded when EUR was 1.25 USD, rather than current 1.10 USD
        val txRecordedPast = Transaction(
            id = "tx-past",
            amount = Money(10000L, "EUR"), // €100.00
            exchangeRate = 1.25, // Stored historical rate: €1 = $1.25 -> $125.00
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-eur",
            categoryId = "cat-tech"
        )

        val totalUsd = currencyConverter.sumTransactionsIn(listOf(txRecordedPast), "USD")
        // Historical rate gives 100.00 * 1.25 = 125.00 USD (12500 minor)
        assertEquals(12500L, totalUsd.amountMinor)
    }

    @Test
    fun `dashboard summary converts multi-currency accounts into user base currency`() = runBlocking {
        val usdAccount = Account(
            id = "acc1",
            name = "USD Account",
            type = AccountType.BANK,
            balance = Money(100000L, "USD"), // $1,000.00
            availableBalance = Money(100000L, "USD")
        )
        val eurAccount = Account(
            id = "acc2",
            name = "EUR Account",
            type = AccountType.BANK,
            balance = Money(100000L, "EUR"), // €1,000.00 -> $1,100.00
            availableBalance = Money(100000L, "EUR")
        )

        val accounts = listOf(usdAccount, eurAccount)
        // Sum in USD: $1,000 + $1,100 = $2,100.00 USD
        val netWorth = currencyConverter.sumIn(accounts.map { it.balance }, "USD")
        assertEquals(210000L, netWorth.amountMinor)
        assertEquals("USD", netWorth.currencyCode)
    }
}

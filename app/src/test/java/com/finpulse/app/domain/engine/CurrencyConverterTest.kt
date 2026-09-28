package com.finpulse.app.domain.engine

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.ExchangeRate
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.ExchangeRateProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CurrencyConverterTest {

    private lateinit var mockRateProvider: ExchangeRateProvider
    private lateinit var converter: CurrencyConverter

    @Before
    fun setUp() {
        mockRateProvider = object : ExchangeRateProvider {
            override suspend fun getRate(fromCurrency: String, toCurrency: String): ExchangeRate {
                val from = fromCurrency.uppercase()
                val to = toCurrency.uppercase()
                if (from == to) return ExchangeRate(from, to, 1.0)
                if (from == "USD" && to == "EUR") return ExchangeRate("USD", "EUR", 0.92)
                if (from == "EUR" && to == "USD") return ExchangeRate("EUR", "USD", 1.0 / 0.92)
                if (from == "USD" && to == "JPY") return ExchangeRate("USD", "JPY", 150.0)
                if (from == "JPY" && to == "USD") return ExchangeRate("JPY", "USD", 1.0 / 150.0)
                if (from == "JPY" && to == "EUR") return ExchangeRate("JPY", "EUR", (1.0 / 150.0) * 0.92)
                if (from == "USD" && to == "BHD") return ExchangeRate("USD", "BHD", 0.376)
                error("No test rate configured for $from to $to")
            }

            override fun getRateFlow(fromCurrency: String, toCurrency: String): Flow<ExchangeRate> =
                flowOf(runBlocking { getRate(fromCurrency, toCurrency) })

            override suspend fun getAllRates(): List<ExchangeRate> = emptyList()
            override fun getAllRatesFlow(): Flow<List<ExchangeRate>> = flowOf(emptyList())
            override suspend fun setManualRate(fromCurrency: String, toCurrency: String, rate: Double) {}
            override suspend fun resetToDefault(fromCurrency: String, toCurrency: String) {}
            override fun getLastUpdatedTimestampFlow(): Flow<Long?> = flowOf(123456789L)
            override suspend fun refreshRates(): Result<Unit> = Result.success(Unit)
        }

        converter = CurrencyConverter(mockRateProvider)
    }

    @Test
    fun `convert with identical currency returns original Money without rounding loss`() = runBlocking {
        val original = Money(12345L, "USD")
        val converted = converter.convert(original, "USD")
        assertEquals(original, converted)
    }

    @Test
    fun `convert to standard two-decimal currency calculates correct minor units`() = runBlocking {
        // 100.00 USD at 0.92 = 92.00 EUR -> 9200 minor units
        val usdMoney = Money(10000L, "USD")
        val eurMoney = converter.convert(usdMoney, "EUR")

        assertEquals("EUR", eurMoney.currencyCode)
        assertEquals(9200L, eurMoney.amountMinor)
        assertEquals(BigDecimal("92.00"), eurMoney.amountBigDecimal)
    }

    @Test
    fun `convert to zero-decimal currency JPY applies proper rounding to integer Yen`() = runBlocking {
        // 10.50 USD at 150.0 = 1575.0 JPY -> 1575 minor units (0 decimals)
        val usdMoney = Money(1050L, "USD")
        val jpyMoney = converter.convert(usdMoney, "JPY")

        assertEquals("JPY", jpyMoney.currencyCode)
        assertEquals(1575L, jpyMoney.amountMinor)
        assertEquals(BigDecimal("1575"), jpyMoney.amountBigDecimal)
    }

    @Test
    fun `convert with custom manual rate overrides default provider rate`() = runBlocking {
        // Normal rate is 0.92, custom rate is 0.95
        val usdMoney = Money(10000L, "USD")
        val eurMoney = converter.convert(usdMoney, "EUR", customRate = 0.95)

        assertEquals(9500L, eurMoney.amountMinor)
    }

    @Test
    fun `convertHistorical preserves transaction recorded rate across future rate changes`() = runBlocking {
        val txWithHistoricalRate = Transaction(
            id = "tx1",
            amount = Money(10000L, "USD"), // $100
            exchangeRate = 0.85, // Rate when transaction happened in the past
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc1",
            categoryId = "cat1"
        )

        // Historical conversion uses 0.85 rather than current provider rate 0.92
        val converted = converter.convertHistorical(txWithHistoricalRate, "EUR")
        assertEquals(8500L, converted.amountMinor)
        assertEquals("EUR", converted.currencyCode)

        // If no rate was stored on transaction, falls back to provider rate 0.92
        val txWithoutRate = txWithHistoricalRate.copy(exchangeRate = null)
        val convertedFallback = converter.convertHistorical(txWithoutRate, "EUR")
        assertEquals(9200L, convertedFallback.amountMinor)
    }

    @Test
    fun `sumIn never silently combines raw values from different currencies`() = runBlocking {
        val amounts = listOf(
            Money(10000L, "USD"), // 100.00 USD -> 92.00 EUR
            Money(5000L, "EUR"),  // 50.00 EUR  -> 50.00 EUR
            Money(15000L, "JPY")  // 15000 JPY  -> 100.00 USD -> 92.00 EUR
        )

        // If raw units were silently combined, it would be 10000 + 5000 + 15000 = 30000!
        // Correct conversion in EUR: 92.00 + 50.00 + 92.00 = 234.00 EUR (23400 minor)
        val totalEur = converter.sumIn(amounts, "EUR")
        assertEquals("EUR", totalEur.currencyCode)
        assertEquals(23400L, totalEur.amountMinor)
    }

    @Test
    fun `computeTransferRate accurately derives effective exchange rate`() {
        val src = Money(10000L, "USD") // $100.00
        val dst = Money(9150L, "EUR")  // €91.50 (bank spread deduction)

        val rate = converter.computeTransferRate(src, dst)
        assertEquals(0.915, rate, 0.0001)
    }
}

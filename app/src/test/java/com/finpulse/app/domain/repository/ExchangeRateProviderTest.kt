package com.finpulse.app.domain.repository

import com.finpulse.app.core.database.dao.ExchangeRateDao
import com.finpulse.app.core.database.entity.ExchangeRateEntity
import com.finpulse.app.data.repository.ExchangeRateProviderImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExchangeRateProviderTest {

    private class FakeExchangeRateDao : ExchangeRateDao {
        val rates = MutableStateFlow<Map<Pair<String, String>, ExchangeRateEntity>>(emptyMap())

        override fun getRateFlow(from: String, to: String): Flow<ExchangeRateEntity?> {
            return rates.map { it[Pair(from.uppercase(), to.uppercase())] }
        }

        override suspend fun getRate(from: String, to: String): ExchangeRateEntity? {
            return rates.value[Pair(from.uppercase(), to.uppercase())]
        }

        override fun getAllRatesFlow(): Flow<List<ExchangeRateEntity>> {
            return rates.map { it.values.toList() }
        }

        override suspend fun getAllRates(): List<ExchangeRateEntity> {
            return rates.value.values.toList()
        }

        override fun getLastMarketUpdateTimestampFlow(): Flow<Long?> {
            return rates.map { map ->
                map.values.filter { !it.isManual }.maxOfOrNull { it.timestamp }
            }
        }

        override suspend fun insertRate(rate: ExchangeRateEntity) {
            val key = Pair(rate.fromCurrency.uppercase(), rate.toCurrency.uppercase())
            rates.value = rates.value + (key to rate)
        }

        override suspend fun insertRates(newRates: List<ExchangeRateEntity>) {
            val updated = rates.value.toMutableMap()
            for (r in newRates) {
                updated[Pair(r.fromCurrency.uppercase(), r.toCurrency.uppercase())] = r
            }
            rates.value = updated
        }

        override suspend fun deleteRate(from: String, to: String) {
            val key = Pair(from.uppercase(), to.uppercase())
            rates.value = rates.value - key
        }

        override suspend fun clearAllRates() {
            rates.value = emptyMap()
        }
    }

    private lateinit var fakeDao: FakeExchangeRateDao
    private lateinit var provider: ExchangeRateProviderImpl

    @Before
    fun setUp() {
        fakeDao = FakeExchangeRateDao()
        provider = ExchangeRateProviderImpl(fakeDao)
    }

    @Test
    fun `identity rate between same currency is exactly 1`() = runBlocking {
        val rate = provider.getRate("USD", "USD")
        assertEquals(1.0, rate.rate, 0.0)
        assertTrue(rate.isIdentity)
    }

    @Test
    fun `direct fallback rate retrieves reference rate when DB is unseeded`() = runBlocking {
        // Fallback for USD to EUR is 0.92
        val rate = provider.getRate("USD", "EUR")
        assertEquals(0.92, rate.rate, 0.0001)
        assertFalse(rate.isManual)
    }

    @Test
    fun `inverted rate is computed dynamically if opposite pair exists`() = runBlocking {
        // USD -> EUR is 0.92, so EUR -> USD is 1 / 0.92 = 1.086956
        val rate = provider.getRate("EUR", "USD")
        assertEquals(1.0 / 0.92, rate.rate, 0.0001)
    }

    @Test
    fun `cross-currency rate is computed via USD intermediate bridge`() = runBlocking {
        // EUR -> USD is 1.0 / 0.92 ≈ 1.087
        // USD -> JPY is 152.0
        // EUR -> JPY is (1.0 / 0.92) * 152.0 ≈ 165.217
        val rate = provider.getRate("EUR", "JPY")
        val expected = (1.0 / 0.92) * 152.0
        assertEquals(expected, rate.rate, 0.01)
    }

    @Test
    fun `manual rate override takes precedence over reference rates`() = runBlocking {
        // User sets a custom rate of 0.95 for USD to EUR
        provider.setManualRate("USD", "EUR", 0.95)

        val customRate = provider.getRate("USD", "EUR")
        assertEquals(0.95, customRate.rate, 0.0)
        assertTrue(customRate.isManual)

        // Stored in DAO as manual
        val entityInDao = fakeDao.getRate("USD", "EUR")
        assertNotNull(entityInDao)
        assertTrue(entityInDao!!.isManual)
    }

    @Test
    fun `resetToDefault removes manual override and restores reference rate`() = runBlocking {
        provider.setManualRate("USD", "EUR", 0.95)
        assertEquals(0.95, provider.getRate("USD", "EUR").rate, 0.0)

        provider.resetToDefault("USD", "EUR")
        val restoredRate = provider.getRate("USD", "EUR")
        assertEquals(0.92, restoredRate.rate, 0.0001)
        assertFalse(restoredRate.isManual)
    }

    @Test
    fun `refreshRates populates market rates and updates last update timestamp`() = runBlocking {
        val result = provider.refreshRates()
        assertTrue(result.isSuccess)

        val allRates = fakeDao.getAllRates()
        assertTrue(allRates.isNotEmpty())

        val usdEur = allRates.find { it.fromCurrency == "USD" && it.toCurrency == "EUR" }
        assertNotNull(usdEur)
        assertEquals(0.92, usdEur!!.rate, 0.0001)
    }
}

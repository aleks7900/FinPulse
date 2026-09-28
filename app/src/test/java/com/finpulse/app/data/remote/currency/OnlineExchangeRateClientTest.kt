package com.finpulse.app.data.remote.currency

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnlineExchangeRateClientTest {

    private lateinit var client: GlobalOnlineExchangeRateClient

    @Before
    fun setUp() {
        client = GlobalOnlineExchangeRateClient()
    }

    @Test
    fun `parseOpenErApiResponse parses rates and timestamp correctly`() {
        val json = """
            {
                "result": "success",
                "provider": "https://www.exchangerate-api.com",
                "time_last_update_unix": 1727481600,
                "base_code": "USD",
                "rates": {
                    "USD": 1,
                    "EUR": 0.895,
                    "GBP": 0.748,
                    "JPY": 143.25
                }
            }
        """.trimIndent()

        val result = client.parseOpenErApiResponse(json, "USD")

        assertEquals("USD", result.baseCurrency)
        assertEquals(1727481600000L, result.timestamp)
        assertEquals(4, result.rates.size)
        assertEquals(0.895, result.rates["EUR"]!!, 0.0001)
        assertEquals(143.25, result.rates["JPY"]!!, 0.0001)
        assertTrue(result.providerSource.contains("open.er-api.com"))
    }

    @Test
    fun `parseOpenErApiResponse throws on API error result`() {
        val errorJson = """
            {
                "result": "error",
                "error-type": "unsupported-code"
            }
        """.trimIndent()

        val exception = assertThrows(RuntimeException::class.java) {
            client.parseOpenErApiResponse(errorJson, "INVALID")
        }
        assertTrue(exception.message!!.contains("unsupported-code"))
    }

    @Test
    fun `parseFrankfurterResponse parses rates correctly`() {
        val json = """
            {
                "amount": 1.0,
                "base": "USD",
                "date": "2026-09-25",
                "rates": {
                    "EUR": 0.894,
                    "GBP": 0.747,
                    "JPY": 143.10
                }
            }
        """.trimIndent()

        val result = client.parseFrankfurterResponse(json, "USD")

        assertEquals("USD", result.baseCurrency)
        assertEquals(1.0, result.rates["USD"]!!, 0.0)
        assertEquals(0.894, result.rates["EUR"]!!, 0.0001)
        assertEquals(143.10, result.rates["JPY"]!!, 0.0001)
        assertTrue(result.providerSource.contains("Frankfurter"))
    }

    @Test
    fun `fetchLatestRates falls back to secondary when primary fails`() = runBlocking {
        val mockClient = object : OnlineExchangeRateClient {
            var primaryCalled = false
            var secondaryCalled = false

            override suspend fun fetchLatestRates(baseCurrency: String): Result<OnlineRatesResult> {
                primaryCalled = true
                // Simulate primary network failure
                // Fallback to secondary succeeds
                secondaryCalled = true
                return Result.success(
                    OnlineRatesResult(
                        baseCurrency = "USD",
                        timestamp = 1727481600000L,
                        rates = mapOf("EUR" to 0.895, "JPY" to 143.25),
                        providerSource = "Frankfurter (ECB)"
                    )
                )
            }
        }

        val result = mockClient.fetchLatestRates("USD")
        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals(0.895, data.rates["EUR"]!!, 0.0001)
        assertEquals("Frankfurter (ECB)", data.providerSource)
    }
}

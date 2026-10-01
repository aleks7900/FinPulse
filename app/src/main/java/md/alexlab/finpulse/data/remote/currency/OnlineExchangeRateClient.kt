package md.alexlab.finpulse.data.remote.currency

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class OnlineRatesResult(
    val baseCurrency: String,
    val timestamp: Long,
    val rates: Map<String, Double>,
    val providerSource: String
)

interface OnlineExchangeRateClient {
    suspend fun fetchLatestRates(baseCurrency: String = "USD"): Result<OnlineRatesResult>
}

class GlobalOnlineExchangeRateClient(
    private val primaryUrl: String = "https://open.er-api.com/v6/latest/",
    private val secondaryUrl: String = "https://api.frankfurter.app/latest?from="
) : OnlineExchangeRateClient {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun fetchLatestRates(baseCurrency: String): Result<OnlineRatesResult> = withContext(Dispatchers.IO) {
        val base = baseCurrency.uppercase()

        // 1. Try Primary Provider (open.er-api.com - Global CDN, daily updates, all ISO-4217 currencies)
        val primaryResult = tryFetchFromPrimary(base)
        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }

        // 2. Try Secondary Provider (Frankfurter / ECB reference rates)
        val secondaryResult = tryFetchFromSecondary(base)
        if (secondaryResult.isSuccess) {
            return@withContext secondaryResult
        }

        // If both failed, propagate primary error with details
        Result.failure(
            primaryResult.exceptionOrNull()
                ?: secondaryResult.exceptionOrNull()
                ?: RuntimeException("All online exchange rate providers failed")
        )
    }

    private fun tryFetchFromPrimary(baseCurrency: String): Result<OnlineRatesResult> = runCatching {
        val url = URL("$primaryUrl$baseCurrency")
        val responseBody = executeHttpGet(url)
        parseOpenErApiResponse(responseBody, baseCurrency)
    }

    private fun tryFetchFromSecondary(baseCurrency: String): Result<OnlineRatesResult> = runCatching {
        val url = URL("$secondaryUrl$baseCurrency")
        val responseBody = executeHttpGet(url)
        parseFrankfurterResponse(responseBody, baseCurrency)
    }

    private fun executeHttpGet(url: URL): String {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 10000
            setRequestProperty("User-Agent", "FinPulse/1.0 (Android)")
            setRequestProperty("Accept", "application/json")
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw RuntimeException("HTTP $responseCode from ${url.host}: $errorMsg")
            }

            return BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use {
                it.readText()
            }
        } finally {
            connection.disconnect()
        }
    }

    fun parseOpenErApiResponse(jsonString: String, baseCurrency: String): OnlineRatesResult {
        val root = jsonParser.parseToJsonElement(jsonString).jsonObject
        val resultStatus = root["result"]?.jsonPrimitive?.content
        if (resultStatus != null && !resultStatus.equals("success", ignoreCase = true)) {
            val errorType = root["error-type"]?.jsonPrimitive?.content ?: "unknown"
            throw RuntimeException("Open.er-api error: $errorType")
        }

        val unixTime = root["time_last_update_unix"]?.jsonPrimitive?.longOrNull
            ?: (System.currentTimeMillis() / 1000L)
        val timestampMillis = unixTime * 1000L

        val ratesObj = root["rates"]?.jsonObject
            ?: throw RuntimeException("Missing 'rates' object in response")

        val ratesMap = mutableMapOf<String, Double>()
        for ((curr, elem) in ratesObj) {
            val rateVal = elem.jsonPrimitive.doubleOrNull
            if (rateVal != null && rateVal > 0.0) {
                ratesMap[curr.uppercase()] = rateVal
            }
        }

        if (ratesMap.isEmpty()) {
            throw RuntimeException("Parsed 0 valid rates from primary provider")
        }

        return OnlineRatesResult(
            baseCurrency = baseCurrency.uppercase(),
            timestamp = timestampMillis,
            rates = ratesMap,
            providerSource = "ExchangeRate-API (open.er-api.com)"
        )
    }

    fun parseFrankfurterResponse(jsonString: String, baseCurrency: String): OnlineRatesResult {
        val root = jsonParser.parseToJsonElement(jsonString).jsonObject
        val ratesObj = root["rates"]?.jsonObject
            ?: throw RuntimeException("Missing 'rates' object in Frankfurter response")

        val ratesMap = mutableMapOf<String, Double>()
        ratesMap[baseCurrency.uppercase()] = 1.0 // Base is 1.0

        for ((curr, elem) in ratesObj) {
            val rateVal = elem.jsonPrimitive.doubleOrNull
            if (rateVal != null && rateVal > 0.0) {
                ratesMap[curr.uppercase()] = rateVal
            }
        }

        if (ratesMap.isEmpty()) {
            throw RuntimeException("Parsed 0 valid rates from secondary provider")
        }

        return OnlineRatesResult(
            baseCurrency = baseCurrency.uppercase(),
            timestamp = System.currentTimeMillis(),
            rates = ratesMap,
            providerSource = "Frankfurter (ECB)"
        )
    }
}

package md.alexlab.finpulse.data.repository

import md.alexlab.finpulse.core.database.dao.ExchangeRateDao
import md.alexlab.finpulse.data.mapper.toDomain
import md.alexlab.finpulse.data.mapper.toEntity
import md.alexlab.finpulse.domain.model.ExchangeRate
import md.alexlab.finpulse.domain.repository.ExchangeRateProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExchangeRateProviderImpl(
    private val dao: ExchangeRateDao,
    private val onlineClient: md.alexlab.finpulse.data.remote.currency.OnlineExchangeRateClient = md.alexlab.finpulse.data.remote.currency.GlobalOnlineExchangeRateClient()
) : ExchangeRateProvider {

    companion object {
        /**
         * Hardcoded offline fallback rates against USD (1 USD = X Currency).
         * Used whenever the app is fully offline, unseeded, or no custom rate has been entered.
         */
        val DEFAULT_USD_RATES = mapOf(
            "USD" to 1.0,
            "EUR" to 0.92,
            "GBP" to 0.78,
            "MDL" to 17.80,
            "RON" to 4.58,
            "UAH" to 41.20,
            "PLN" to 3.96,
            "CHF" to 0.89,
            "JPY" to 152.0,
            "CNY" to 7.23,
            "CAD" to 1.36,
            "AUD" to 1.52,
            "BRL" to 5.45,
            "TRY" to 34.10,
            "KRW" to 1380.0,
            "INR" to 83.50,
            "MXN" to 18.20,
            "SEK" to 10.45,
            "NOK" to 10.65,
            "DKK" to 6.85,
            "CZK" to 23.10,
            "HUF" to 365.0,
            "BGN" to 1.79,
            "SGD" to 1.32,
            "HKD" to 7.80,
            "NZD" to 1.63,
            "AED" to 3.67,
            "SAR" to 3.75,
            "ILS" to 3.70,
            "ZAR" to 18.20,
            "BHD" to 0.377,
            "KWD" to 0.307,
            "OMR" to 0.385
        )

        fun computeFallbackRate(from: String, to: String): Double {
            val fromCode = from.uppercase()
            val toCode = to.uppercase()
            if (fromCode == toCode) return 1.0

            val fromUsd = DEFAULT_USD_RATES[fromCode] ?: 1.0
            val toUsd = DEFAULT_USD_RATES[toCode] ?: 1.0

            // 1 USD = fromUsd (from) => 1 (from) = 1 / fromUsd (USD)
            // 1 USD = toUsd (to)   => 1 (from) = (1 / fromUsd) * toUsd
            return (1.0 / fromUsd) * toUsd
        }
    }

    override suspend fun getRate(fromCurrency: String, toCurrency: String): ExchangeRate {
        val from = fromCurrency.uppercase()
        val to = toCurrency.uppercase()

        if (from == to) {
            return ExchangeRate(from, to, 1.0, isManual = false)
        }

        // 1. Direct stored rate
        val direct = dao.getRate(from, to)
        if (direct != null) return direct.toDomain()

        // 2. Inverted stored rate
        val inverted = dao.getRate(to, from)
        if (inverted != null) return inverted.toDomain().invert()

        // 3. Deterministic offline fallback cross-rate via USD
        val fallbackRate = computeFallbackRate(from, to)
        return ExchangeRate(
            fromCurrency = from,
            toCurrency = to,
            rate = fallbackRate,
            isManual = false
        )
    }

    override fun getRateFlow(fromCurrency: String, toCurrency: String): Flow<ExchangeRate> {
        val from = fromCurrency.uppercase()
        val to = toCurrency.uppercase()

        return dao.getAllRatesFlow().map { rates ->
            if (from == to) {
                ExchangeRate(from, to, 1.0, isManual = false)
            } else {
                val direct = rates.firstOrNull { it.fromCurrency == from && it.toCurrency == to }
                if (direct != null) {
                    direct.toDomain()
                } else {
                    val inverted = rates.firstOrNull { it.fromCurrency == to && it.toCurrency == from }
                    if (inverted != null) {
                        inverted.toDomain().invert()
                    } else {
                        ExchangeRate(
                            fromCurrency = from,
                            toCurrency = to,
                            rate = computeFallbackRate(from, to),
                            isManual = false
                        )
                    }
                }
            }
        }
    }

    override suspend fun getAllRates(): List<ExchangeRate> {
        val stored = dao.getAllRates().map { it.toDomain() }.associateBy { "${it.fromCurrency}_${it.toCurrency}" }
        val result = mutableListOf<ExchangeRate>()

        // Combine with default USD rates for all currencies
        for ((code, _) in DEFAULT_USD_RATES) {
            if (code == "USD") continue
            val keyUsdToCode = "USD_$code"
            val keyCodeToUsd = "${code}_USD"

            val rateUsdToCode = stored[keyUsdToCode] ?: ExchangeRate(
                fromCurrency = "USD",
                toCurrency = code,
                rate = computeFallbackRate("USD", code),
                isManual = false
            )
            val rateCodeToUsd = stored[keyCodeToUsd] ?: rateUsdToCode.invert()

            result.add(rateUsdToCode)
            result.add(rateCodeToUsd)
        }

        // Add any additional custom pairs stored in DB
        for ((key, rate) in stored) {
            if (!result.any { "${it.fromCurrency}_${it.toCurrency}" == key }) {
                result.add(rate)
            }
        }

        return result
    }

    override fun getAllRatesFlow(): Flow<List<ExchangeRate>> {
        return dao.getAllRatesFlow().map { storedList ->
            val stored = storedList.map { it.toDomain() }.associateBy { "${it.fromCurrency}_${it.toCurrency}" }
            val result = mutableListOf<ExchangeRate>()

            for ((code, _) in DEFAULT_USD_RATES) {
                if (code == "USD") continue
                val keyUsdToCode = "USD_$code"
                val rateUsdToCode = stored[keyUsdToCode] ?: ExchangeRate(
                    fromCurrency = "USD",
                    toCurrency = code,
                    rate = computeFallbackRate("USD", code),
                    isManual = false
                )
                result.add(rateUsdToCode)
            }

            // Include any additional pairs
            for ((key, rate) in stored) {
                if (!result.any { "${it.fromCurrency}_${it.toCurrency}" == key }) {
                    result.add(rate)
                }
            }

            result.sortedBy { it.toCurrency }
        }
    }

    override suspend fun setManualRate(fromCurrency: String, toCurrency: String, rate: Double) {
        val from = fromCurrency.uppercase()
        val to = toCurrency.uppercase()
        val now = System.currentTimeMillis()

        require(rate > 0.0) { "Rate must be strictly positive" }

        val direct = ExchangeRate(from, to, rate, timestamp = now, isManual = true)
        val inverted = direct.invert()

        dao.insertRate(direct.toEntity())
        dao.insertRate(inverted.toEntity())
    }

    override suspend fun resetToDefault(fromCurrency: String, toCurrency: String) {
        val from = fromCurrency.uppercase()
        val to = toCurrency.uppercase()
        dao.deleteRate(from, to)
        dao.deleteRate(to, from)
    }

    override fun getLastUpdatedTimestampFlow(): Flow<Long?> {
        return dao.getLastMarketUpdateTimestampFlow()
    }

    override suspend fun refreshRates(): Result<Unit> {
        return try {
            val onlineResult = onlineClient.fetchLatestRates("USD")
            if (onlineResult.isSuccess) {
                val data = onlineResult.getOrThrow()
                val existingRates = dao.getAllRates().associateBy { Pair(it.fromCurrency, it.toCurrency) }

                val entitiesToInsert = mutableListOf<md.alexlab.finpulse.core.database.entity.ExchangeRateEntity>()

                for (code in md.alexlab.finpulse.core.model.CurrencyConfig.supportedCurrencyCodes) {
                    if (code == "USD") continue
                    val rateVal = data.rates[code] ?: DEFAULT_USD_RATES[code] ?: continue

                    val key = Pair("USD", code)
                    val existing = existingRates[key]
                    // Do NOT overwrite user's manual override
                    if (existing != null && existing.isManual) {
                        continue
                    }

                    entitiesToInsert.add(
                        ExchangeRate(
                            fromCurrency = "USD",
                            toCurrency = code,
                            rate = rateVal,
                            timestamp = data.timestamp,
                            isManual = false
                        ).toEntity()
                    )
                }

                if (entitiesToInsert.isNotEmpty()) {
                    dao.insertRates(entitiesToInsert)
                }
                Result.success(Unit)
            } else {
                seedOfflineDefaults()
                Result.success(Unit)
            }
        } catch (_: Exception) {
            seedOfflineDefaults()
            Result.success(Unit)
        }
    }

    private suspend fun seedOfflineDefaults() {
        val now = System.currentTimeMillis()
        val existingRates = dao.getAllRates().associateBy { Pair(it.fromCurrency, it.toCurrency) }
        val entities = DEFAULT_USD_RATES.filterKeys { it != "USD" }.mapNotNull { (code, rate) ->
            val existing = existingRates[Pair("USD", code)]
            if (existing != null && existing.isManual) {
                null
            } else {
                ExchangeRate(
                    fromCurrency = "USD",
                    toCurrency = code,
                    rate = rate,
                    timestamp = now,
                    isManual = false
                ).toEntity()
            }
        }
        if (entities.isNotEmpty()) {
            dao.insertRates(entities)
        }
    }
}

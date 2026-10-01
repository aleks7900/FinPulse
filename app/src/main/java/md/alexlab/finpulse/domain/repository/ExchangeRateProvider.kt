package md.alexlab.finpulse.domain.repository

import md.alexlab.finpulse.domain.model.ExchangeRate
import kotlinx.coroutines.flow.Flow

/**
 * Interface for providing currency exchange rates.
 * Supports reactive flows, historical rate lookup, manual user overrides, and offline market fallbacks.
 */
interface ExchangeRateProvider {

    /**
     * Retrieves the rate between [fromCurrency] and [toCurrency].
     * If both currencies are identical, returns identity rate 1.0.
     * Searches stored manual/cached rates first, then fallback market defaults, or cross-rate via USD.
     */
    suspend fun getRate(fromCurrency: String, toCurrency: String): ExchangeRate

    /**
     * Reactive flow emitting rate updates for [fromCurrency] to [toCurrency].
     */
    fun getRateFlow(fromCurrency: String, toCurrency: String): Flow<ExchangeRate>

    /**
     * Gets all stored and default exchange rates.
     */
    suspend fun getAllRates(): List<ExchangeRate>

    /**
     * Reactive flow of all exchange rates.
     */
    fun getAllRatesFlow(): Flow<List<ExchangeRate>>

    /**
     * Saves or overrides a custom manual rate configured by the user.
     */
    suspend fun setManualRate(fromCurrency: String, toCurrency: String, rate: Double)

    /**
     * Resets a manual rate back to default reference rate.
     */
    suspend fun resetToDefault(fromCurrency: String, toCurrency: String)

    /**
     * Flow emitting the timestamp of the last rate update or null if not yet updated.
     */
    fun getLastUpdatedTimestampFlow(): Flow<Long?>

    /**
     * Refreshes market rates (offline defaults or network if available).
     */
    suspend fun refreshRates(): Result<Unit>
}

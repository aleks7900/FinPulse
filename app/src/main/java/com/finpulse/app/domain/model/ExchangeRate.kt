package com.finpulse.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ExchangeRate(
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isManual: Boolean = false
) {
    init {
        require(rate > 0.0) { "Exchange rate must be strictly positive" }
    }

    val isIdentity: Boolean get() = fromCurrency.equals(toCurrency, ignoreCase = true)
    val isManualOverride: Boolean get() = isManual
    val lastUpdated: Long get() = timestamp

    fun invert(): ExchangeRate = copy(
        fromCurrency = toCurrency,
        toCurrency = fromCurrency,
        rate = 1.0 / rate
    )
}

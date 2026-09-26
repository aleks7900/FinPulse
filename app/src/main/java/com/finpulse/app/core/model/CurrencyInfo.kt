package com.finpulse.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String = ""
) {
    companion object {
        val SUPPORTED_CURRENCIES = listOf(
            CurrencyInfo("USD", "US Dollar", "$", "🇺🇸"),
            CurrencyInfo("EUR", "Euro", "€", "🇪🇺"),
            CurrencyInfo("GBP", "British Pound", "£", "🇬🇧"),
            CurrencyInfo("JPY", "Japanese Yen", "¥", "🇯🇵"),
            CurrencyInfo("CAD", "Canadian Dollar", "CA$", "🇨🇦"),
            CurrencyInfo("AUD", "Australian Dollar", "A$", "🇦🇺"),
            CurrencyInfo("CHF", "Swiss Franc", "CHF", "🇨🇭"),
            CurrencyInfo("CNY", "Chinese Yuan", "¥", "🇨🇳"),
            CurrencyInfo("INR", "Indian Rupee", "₹", "🇮🇳"),
            CurrencyInfo("BRL", "Brazilian Real", "R$", "🇧🇷"),
            CurrencyInfo("SGD", "Singapore Dollar", "S$", "🇸🇬"),
            CurrencyInfo("SEK", "Swedish Krona", "kr", "🇸🇪"),
            CurrencyInfo("NOK", "Norwegian Krone", "kr", "🇳🇴"),
            CurrencyInfo("MXN", "Mexican Peso", "$", "🇲🇽")
        )

        val DEFAULT = SUPPORTED_CURRENCIES.first()

        fun findByCode(code: String): CurrencyInfo {
            return SUPPORTED_CURRENCIES.find { it.code.equals(code, ignoreCase = true) }
                ?: CurrencyInfo(code, code, code, "🌐")
        }
    }
}

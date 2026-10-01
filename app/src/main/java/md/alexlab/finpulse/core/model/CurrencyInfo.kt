package md.alexlab.finpulse.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String = ""
) {
    companion object {
        val SUPPORTED_CURRENCIES: List<CurrencyInfo> = CurrencyConfig.supportedCurrencies.map {
            CurrencyInfo(
                code = it.code,
                name = it.displayName,
                symbol = it.symbol,
                flag = it.flagEmoji
            )
        }

        val DEFAULT = SUPPORTED_CURRENCIES.first()

        fun findByCode(code: String): CurrencyInfo {
            return SUPPORTED_CURRENCIES.find { it.code.equals(code, ignoreCase = true) }
                ?: run {
                    val meta = CurrencyConfig.getMetadata(code)
                    CurrencyInfo(meta.code, meta.displayName, meta.symbol, meta.flagEmoji)
                }
        }
    }
}

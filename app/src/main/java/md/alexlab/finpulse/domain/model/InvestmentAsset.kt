package md.alexlab.finpulse.domain.model

import md.alexlab.finpulse.core.model.Money
import kotlinx.serialization.Serializable

enum class AssetClass(val displayName: String) {
    STOCK("Stock"),
    ETF("ETF"),
    CRYPTO("Cryptocurrency"),
    BOND("Bond"),
    REAL_ESTATE("Real Estate"),
    COMMODITY("Commodity"),
    CASH("Cash Equivalent"),
    OTHER("Other Asset")
}

@Serializable
data class InvestmentAsset(
    val id: String,
    val name: String,
    val symbol: String,
    val assetClass: AssetClass,
    val quantity: Double,
    val purchasePrice: Money,
    val currentPrice: Money,
    val lastUpdated: Long = System.currentTimeMillis(),
    val notes: String? = null
) {
    val totalInvested: Money
        get() = purchasePrice * quantity

    val currentValue: Money
        get() = currentPrice * quantity

    val profitLoss: Money
        get() = currentValue - totalInvested

    val percentageReturn: Double
        get() {
            if (totalInvested.amountMinor == 0L) return 0.0
            return (profitLoss.amountMinor.toDouble() / totalInvested.amountMinor.toDouble()) * 100.0
        }
}

package com.finpulse.app.core.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Robust monetary value representation that guarantees mathematical safety.
 * Stored internally as minor units (e.g. cents, 1000 = $10.00) using 64-bit Long.
 * Prohibits floating-point precision loss.
 */
@Serializable
data class Money(
    val amountMinor: Long,
    val currencyCode: String = "USD"
) : Comparable<Money> {

    val amountBigDecimal: BigDecimal
        get() = CurrencyConfig.toMajor(amountMinor, currencyCode)

    val isZero: Boolean get() = amountMinor == 0L
    val isPositive: Boolean get() = amountMinor > 0L
    val isNegative: Boolean get() = amountMinor < 0L

    operator fun plus(other: Money): Money {
        require(currencyCode == other.currencyCode) {
            "Currency mismatch: cannot add ${other.currencyCode} to $currencyCode"
        }
        return copy(amountMinor = amountMinor + other.amountMinor)
    }

    operator fun minus(other: Money): Money {
        require(currencyCode == other.currencyCode) {
            "Currency mismatch: cannot subtract ${other.currencyCode} from $currencyCode"
        }
        return copy(amountMinor = amountMinor - other.amountMinor)
    }

    operator fun times(factor: Double): Money {
        val scaled = BigDecimal(amountMinor)
            .multiply(BigDecimal.valueOf(factor))
            .setScale(0, RoundingMode.HALF_EVEN)
            .toLong()
        return copy(amountMinor = scaled)
    }

    operator fun div(divisor: Double): Money {
        require(divisor != 0.0) { "Cannot divide Money by zero" }
        val scaled = BigDecimal(amountMinor)
            .divide(BigDecimal.valueOf(divisor), 0, RoundingMode.HALF_EVEN)
            .toLong()
        return copy(amountMinor = scaled)
    }

    operator fun unaryMinus(): Money = copy(amountMinor = -amountMinor)

    fun absolute(): Money = copy(amountMinor = kotlin.math.abs(amountMinor))

    fun ratio(other: Money): Double {
        require(currencyCode == other.currencyCode) {
            "Currency mismatch: cannot compute ratio between $currencyCode and ${other.currencyCode}"
        }
        if (other.amountMinor == 0L) return 0.0
        return amountMinor.toDouble() / other.amountMinor.toDouble()
    }

    fun formatted(locale: Locale = Locale.getDefault()): String {
        return CurrencyConfig.format(this, locale)
    }

    fun formattedCompact(): String {
        val majorValue = kotlin.math.abs(amountBigDecimal.toDouble())
        val sign = if (isNegative) "-" else ""
        val symbol = CurrencyConfig.getMetadata(currencyCode).symbol
        return when {
            majorValue >= 1_000_000 -> String.format(Locale.US, "%s%s%.1fM", sign, symbol, majorValue / 1_000_000.0)
            majorValue >= 1_000 -> String.format(Locale.US, "%s%s%.1fK", sign, symbol, majorValue / 1_000.0)
            else -> formatted()
        }
    }

    override fun compareTo(other: Money): Int {
        require(currencyCode == other.currencyCode) {
            "Currency mismatch: cannot compare $currencyCode and ${other.currencyCode}"
        }
        return amountMinor.compareTo(other.amountMinor)
    }

    companion object {
        fun zero(currencyCode: String = "USD") = Money(0L, currencyCode)

        fun fromMajor(majorAmount: BigDecimal, currencyCode: String = "USD"): Money {
            val minor = CurrencyConfig.toMinor(majorAmount, currencyCode)
            return Money(minor, currencyCode)
        }

        fun fromDouble(amount: Double, currencyCode: String = "USD"): Money {
            return fromMajor(BigDecimal.valueOf(amount), currencyCode)
        }

        fun fromCents(cents: Long, currencyCode: String = "USD"): Money {
            return Money(cents, currencyCode)
        }
    }
}

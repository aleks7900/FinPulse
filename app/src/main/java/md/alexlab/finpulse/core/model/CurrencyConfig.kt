package md.alexlab.finpulse.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Currency configuration and precision rules per ISO-4217 standard.
 *
 * Defines explicit decimal fraction digits, minor unit multipliers, and banker's rounding rules
 * (HALF_EVEN) to prevent any cumulative precision loss across multi-currency operations.
 */
object CurrencyConfig {

    data class CurrencyMetadata(
        val code: String,
        val displayName: String,
        val symbol: String,
        val fractionDigits: Int,
        val flagEmoji: String
    ) {
        val name: String get() = displayName
        val decimals: Int get() = fractionDigits
        val minorMultiplier: Long = when (fractionDigits) {
            0 -> 1L
            1 -> 10L
            2 -> 100L
            3 -> 1000L
            4 -> 10000L
            else -> 100L
        }
    }

    val supportedCurrencies = listOf(
        CurrencyMetadata("USD", "US Dollar", "$", 2, "🇺🇸"),
        CurrencyMetadata("EUR", "Euro", "€", 2, "🇪🇺"),
        CurrencyMetadata("GBP", "British Pound", "£", 2, "🇬🇧"),
        CurrencyMetadata("MDL", "Moldovan Leu", "L", 2, "🇲🇩"),
        CurrencyMetadata("RON", "Romanian Leu", "lei", 2, "🇷🇴"),
        CurrencyMetadata("UAH", "Ukrainian Hryvnia", "₴", 2, "🇺🇦"),
        CurrencyMetadata("PLN", "Polish Zloty", "zł", 2, "🇵🇱"),
        CurrencyMetadata("CHF", "Swiss Franc", "CHF", 2, "🇨🇭"),
        CurrencyMetadata("JPY", "Japanese Yen", "¥", 0, "🇯🇵"),
        CurrencyMetadata("CNY", "Chinese Yuan", "¥", 2, "🇨🇳"),
        CurrencyMetadata("CAD", "Canadian Dollar", "CA$", 2, "🇨🇦"),
        CurrencyMetadata("AUD", "Australian Dollar", "A$", 2, "🇦🇺"),
        CurrencyMetadata("BRL", "Brazilian Real", "R$", 2, "🇧🇷"),
        CurrencyMetadata("TRY", "Turkish Lira", "₺", 2, "🇹🇷"),
        CurrencyMetadata("KRW", "South Korean Won", "₩", 0, "🇰🇷"),
        CurrencyMetadata("INR", "Indian Rupee", "₹", 2, "🇮🇳"),
        CurrencyMetadata("MXN", "Mexican Peso", "Mex$", 2, "🇲🇽"),
        CurrencyMetadata("SEK", "Swedish Krona", "kr", 2, "🇸🇪"),
        CurrencyMetadata("NOK", "Norwegian Krone", "kr", 2, "🇳🇴"),
        CurrencyMetadata("DKK", "Danish Krone", "kr", 2, "🇩🇰"),
        CurrencyMetadata("CZK", "Czech Koruna", "Kč", 2, "🇨🇿"),
        CurrencyMetadata("HUF", "Hungarian Forint", "Ft", 0, "🇭🇺"),
        CurrencyMetadata("BGN", "Bulgarian Lev", "лв", 2, "🇧🇬"),
        CurrencyMetadata("SGD", "Singapore Dollar", "S$", 2, "🇸🇬"),
        CurrencyMetadata("HKD", "Hong Kong Dollar", "HK$", 2, "🇭🇰"),
        CurrencyMetadata("NZD", "New Zealand Dollar", "NZ$", 2, "🇳🇿"),
        CurrencyMetadata("AED", "UAE Dirham", "AED", 2, "🇦🇪"),
        CurrencyMetadata("SAR", "Saudi Riyal", "SAR", 2, "🇸🇦"),
        CurrencyMetadata("ILS", "Israeli New Shekel", "₪", 2, "🇮🇱"),
        CurrencyMetadata("ZAR", "South African Rand", "R", 2, "🇿🇦"),
        CurrencyMetadata("BHD", "Bahraini Dinar", "BD", 3, "🇧🇭"),
        CurrencyMetadata("KWD", "Kuwaiti Dinar", "KD", 3, "🇰🇼"),
        CurrencyMetadata("OMR", "Omani Rial", "OMR", 3, "🇴🇲")
    )

    val supportedCurrencyCodes: List<String> = supportedCurrencies.map { it.code }

    private val currencyMap = supportedCurrencies.associateBy { it.code.uppercase() }

    fun getAllCurrencies(): List<CurrencyMetadata> = supportedCurrencies

    fun getSymbol(currencyCode: String): String = getMetadata(currencyCode).symbol

    fun getName(currencyCode: String): String = getMetadata(currencyCode).displayName

    fun fromMajor(amount: Double, currencyCode: String): Money =
        Money.fromMajor(BigDecimal.valueOf(amount), currencyCode)

    fun fromMajor(amount: BigDecimal, currencyCode: String): Money =
        Money.fromMajor(amount, currencyCode)

    fun getMetadata(currencyCode: String): CurrencyMetadata {
        val code = currencyCode.uppercase()
        return currencyMap[code] ?: run {
            val decimals = try {
                val c = Currency.getInstance(code)
                val d = c.defaultFractionDigits
                if (d >= 0) d else 2
            } catch (_: Exception) {
                2
            }
            val symbol = try {
                Currency.getInstance(code).symbol
            } catch (_: Exception) {
                code
            }
            CurrencyMetadata(
                code = code,
                displayName = code,
                symbol = symbol,
                fractionDigits = decimals,
                flagEmoji = "🌐"
            )
        }
    }

    fun decimalsFor(currencyCode: String): Int = getMetadata(currencyCode).fractionDigits

    fun minorUnitsPerMajor(currencyCode: String): Long = getMetadata(currencyCode).minorMultiplier

    /**
     * Converts a major decimal value (e.g. 10.50 USD or 1500 JPY) into minor units (Long)
     * using Half-Even (Banker's) Rounding.
     */
    fun toMinor(amount: BigDecimal, currencyCode: String): Long {
        val multiplier = BigDecimal(minorUnitsPerMajor(currencyCode))
        return amount.multiply(multiplier)
            .setScale(0, RoundingMode.HALF_EVEN)
            .toLong()
    }

    /**
     * Converts minor units (Long) into a major BigDecimal according to the currency's decimal places.
     */
    fun toMajor(amountMinor: Long, currencyCode: String): BigDecimal {
        val decimals = decimalsFor(currencyCode)
        val divisor = BigDecimal(minorUnitsPerMajor(currencyCode))
        return BigDecimal(amountMinor).divide(divisor, decimals, RoundingMode.HALF_EVEN)
    }

    /**
     * Formats Money strictly according to its currency rules.
     */
    fun format(
        money: Money,
        locale: Locale = Locale.getDefault()
    ): String {
        val decimals = decimalsFor(money.currencyCode)
        val symbol = getMetadata(money.currencyCode).symbol
        return try {
            val formatter = NumberFormat.getCurrencyInstance(locale)
            if (formatter is java.text.DecimalFormat) {
                val symbols = formatter.decimalFormatSymbols
                symbols.currencySymbol = symbol
                formatter.decimalFormatSymbols = symbols
                formatter.maximumFractionDigits = decimals
                formatter.minimumFractionDigits = decimals
                formatter.format(toMajor(money.amountMinor, money.currencyCode))
            } else {
                val currency = Currency.getInstance(money.currencyCode)
                formatter.currency = currency
                formatter.maximumFractionDigits = decimals
                formatter.minimumFractionDigits = decimals
                formatter.format(toMajor(money.amountMinor, money.currencyCode))
            }
        } catch (_: Exception) {
            val sign = if (money.isNegative) "-" else ""
            val absMajor = toMajor(kotlin.math.abs(money.amountMinor), money.currencyCode)
            "$sign$symbol$absMajor"
        }
    }

    fun getCurrency(currencyCode: String): CurrencyMetadata = getMetadata(currencyCode)

    fun formatMoney(
        amount: BigDecimal,
        currencyCode: String,
        locale: Locale = Locale.getDefault()
    ): String = format(fromMajor(amount, currencyCode), locale)

    fun formatMoney(
        amount: Double,
        currencyCode: String,
        locale: Locale = Locale.getDefault()
    ): String = format(fromMajor(amount, currencyCode), locale)

    fun formatMoney(
        amountMinor: Long,
        currencyCode: String,
        locale: Locale = Locale.getDefault()
    ): String = format(Money(amountMinor, currencyCode), locale)
}

/**
 * Top-level centralized money formatter per Requirement 8:
 * formatMoney(amount, currencyCode, locale)
 */
fun formatMoney(
    amount: BigDecimal,
    currencyCode: String,
    locale: Locale = Locale.getDefault()
): String = CurrencyConfig.formatMoney(amount, currencyCode, locale)

fun formatMoney(
    amount: Double,
    currencyCode: String,
    locale: Locale = Locale.getDefault()
): String = CurrencyConfig.formatMoney(amount, currencyCode, locale)

fun formatMoney(
    amountMinor: Long,
    currencyCode: String,
    locale: Locale = Locale.getDefault()
): String = CurrencyConfig.formatMoney(amountMinor, currencyCode, locale)



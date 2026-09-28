package com.finpulse.app.domain.engine

import com.finpulse.app.core.model.CurrencyConfig
import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.repository.ExchangeRateProvider
import java.math.BigDecimal
import java.math.RoundingMode

class CurrencyConverter(
    private val rateProvider: ExchangeRateProvider
) {

    /**
     * Converts a given [money] value into [targetCurrency].
     * If currencies are identical, returns [money] unchanged without rounding alterations.
     * If [customRate] is provided, uses it directly; otherwise queries [rateProvider].
     */
    suspend fun convert(
        money: Money,
        targetCurrency: String,
        customRate: Double? = null
    ): Money {
        val target = targetCurrency.uppercase()
        if (money.currencyCode.equals(target, ignoreCase = true)) {
            return money
        }

        val rate = customRate ?: rateProvider.getRate(money.currencyCode, target).rate
        require(rate > 0.0) { "Conversion rate must be positive" }

        // Convert major amount using BigDecimal and target currency's fraction digits
        val sourceMajor = money.amountBigDecimal
        val targetMajor = sourceMajor.multiply(BigDecimal.valueOf(rate))

        return Money.fromMajor(targetMajor, target)
    }

    /**
     * Converts a transaction into [targetCurrency] respecting historical rates stored on the transaction.
     * If the transaction has an explicit [Transaction.exchangeRate] and it was converted to [targetCurrency],
     * uses the stored rate to preserve historical accuracy.
     */
    suspend fun convertHistorical(
        transaction: Transaction,
        targetCurrency: String
    ): Money {
        val target = targetCurrency.uppercase()
        if (transaction.amount.currencyCode.equals(target, ignoreCase = true)) {
            return transaction.amount
        }

        // Use stored historical rate if available
        val rateToUse = transaction.exchangeRate
        return convert(transaction.amount, target, rateToUse)
    }

    /**
     * Safely sums a list of [Money] values from any combination of currencies into [targetCurrency].
     * Never combines raw values directly across different currencies.
     */
    suspend fun sumIn(
        items: List<Money>,
        targetCurrency: String
    ): Money {
        val target = targetCurrency.uppercase()
        if (items.isEmpty()) return Money.zero(target)

        var totalMinor = 0L
        for (item in items) {
            val converted = convert(item, target)
            totalMinor += converted.amountMinor
        }
        return Money(totalMinor, target)
    }

    /**
     * Safely sums a list of [Transaction] items into [targetCurrency], using each transaction's
     * historical rate where recorded.
     */
    suspend fun sumTransactionsIn(
        transactions: List<Transaction>,
        targetCurrency: String
    ): Money {
        val target = targetCurrency.uppercase()
        if (transactions.isEmpty()) return Money.zero(target)

        var totalMinor = 0L
        for (tx in transactions) {
            val converted = convertHistorical(tx, target)
            totalMinor += converted.amountMinor
        }
        return Money(totalMinor, target)
    }

    /**
     * Computes the effective conversion rate for a cross-currency transfer.
     * Rate = Destination Major Amount / Source Major Amount
     */
    fun computeTransferRate(
        sourceAmount: Money,
        destinationAmount: Money
    ): Double {
        val srcMajor = sourceAmount.amountBigDecimal
        val dstMajor = destinationAmount.amountBigDecimal
        if (srcMajor.compareTo(BigDecimal.ZERO) == 0) return 1.0

        return dstMajor.divide(srcMajor, 6, RoundingMode.HALF_EVEN).toDouble()
    }
}

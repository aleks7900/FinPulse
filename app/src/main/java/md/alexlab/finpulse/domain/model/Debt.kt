package md.alexlab.finpulse.domain.model

import md.alexlab.finpulse.core.model.Money
import kotlinx.serialization.Serializable

enum class DebtType(val displayName: String) {
    CREDIT_CARD("Credit Card"),
    PERSONAL_LOAN("Personal Loan"),
    MORTGAGE("Mortgage"),
    STUDENT_LOAN("Student Loan"),
    AUTO_LOAN("Auto Loan"),
    OTHER("Other Debt")
}

@Serializable
data class Debt(
    val id: String,
    val name: String,
    val type: DebtType,
    val totalPrincipal: Money,
    val remainingBalance: Money,
    val interestRatePercent: Double,
    val minimumPayment: Money,
    val nextPaymentDate: Long,
    val linkedAccountId: String? = null,
    val notes: String? = null
) {
    val payoffProgressPercentage: Double
        get() {
            if (totalPrincipal.amountMinor <= 0L) return 0.0
            val paidMinor = (totalPrincipal.amountMinor - remainingBalance.amountMinor).coerceAtLeast(0L)
            return (paidMinor.toDouble() / totalPrincipal.amountMinor.toDouble()).coerceIn(0.0, 1.0)
        }

    fun estimateMonthsToPayoff(monthlyPayment: Money = minimumPayment): Int {
        if (remainingBalance.isZero) return 0
        if (monthlyPayment <= Money.zero(remainingBalance.currencyCode)) return Int.MAX_VALUE

        val balance = remainingBalance.amountBigDecimal.toDouble()
        val payment = monthlyPayment.amountBigDecimal.toDouble()
        val monthlyRate = (interestRatePercent / 100.0) / 12.0

        if (monthlyRate == 0.0) {
            return kotlin.math.ceil(balance / payment).toInt()
        }

        val monthlyInterest = balance * monthlyRate
        if (payment <= monthlyInterest) {
            // Payment doesn't even cover interest; infinite payoff
            return Int.MAX_VALUE
        }

        // N = -ln(1 - (r * B) / P) / ln(1 + r)
        val numerator = -kotlin.math.ln(1.0 - (monthlyRate * balance) / payment)
        val denominator = kotlin.math.ln(1.0 + monthlyRate)
        val months = kotlin.math.ceil(numerator / denominator).toInt()
        return months.coerceAtLeast(1)
    }
}

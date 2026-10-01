package md.alexlab.finpulse.domain.model

import md.alexlab.finpulse.core.model.Money
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Serializable
data class FinancialGoal(
    val id: String,
    val title: String,
    val targetAmount: Money,
    val currentAmount: Money,
    val targetDate: Long,
    val linkedAccountId: String? = null,
    val icon: String = "savings",
    val colorHex: Long = 0xFF4CAF50,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressPercentage: Double
        get() {
            if (targetAmount.amountMinor <= 0L) return 0.0
            return (currentAmount.amountMinor.toDouble() / targetAmount.amountMinor.toDouble()).coerceIn(0.0, 1.0)
        }

    val remainingAmount: Money
        get() {
            val remainingMinor = (targetAmount.amountMinor - currentAmount.amountMinor).coerceAtLeast(0L)
            return Money(remainingMinor, targetAmount.currencyCode)
        }

    fun calculateSuggestedMonthlyContribution(nowMillis: Long = System.currentTimeMillis()): Money {
        if (isCompleted || currentAmount >= targetAmount) {
            return Money.zero(targetAmount.currencyCode)
        }
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val target = Instant.ofEpochMilli(targetDate).atZone(zone).toLocalDate()

        val monthsRemaining = ChronoUnit.MONTHS.between(now, target).coerceAtLeast(1L)
        return remainingAmount / monthsRemaining.toDouble()
    }
}

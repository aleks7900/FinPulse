package md.alexlab.finpulse.domain.model

import md.alexlab.finpulse.core.model.Money
import kotlinx.serialization.Serializable

enum class PaymentFrequency(val displayName: String, val approxDays: Int) {
    DAILY("Daily", 1),
    WEEKLY("Weekly", 7),
    BI_WEEKLY("Every 2 Weeks", 14),
    MONTHLY("Monthly", 30),
    QUARTERLY("Quarterly", 90),
    YEARLY("Yearly", 365),
    CUSTOM("Custom", 30)
}

enum class CustomIntervalUnit(val displayName: String) {
    DAYS("Days"),
    WEEKS("Weeks"),
    MONTHS("Months"),
    YEARS("Years")
}

enum class OccurrenceStatus(val displayName: String) {
    EXPECTED("Upcoming"),
    OVERDUE("Overdue"),
    GENERATED("Generated"),
    PAID("Paid"),
    SKIPPED("Skipped")
}

@Serializable
data class RecurringTransaction(
    val id: String,
    val title: String,
    val amount: Money,
    val type: TransactionType = TransactionType.EXPENSE,
    val accountId: String,
    val destinationAccountId: String? = null,
    val categoryId: String,
    val frequency: PaymentFrequency = PaymentFrequency.MONTHLY,
    val customIntervalValue: Int = 1,
    val customIntervalUnit: CustomIntervalUnit = CustomIntervalUnit.MONTHS,
    val anchorDayOfMonth: Int = 1,
    val nextDueDate: Long,
    val lastProcessedDate: Long? = null,
    val isActive: Boolean = true,
    val isCancelled: Boolean = false,
    val isSubscription: Boolean = false,
    val isVariableAmount: Boolean = false,
    val reminderDaysBefore: Int = 1,
    val notes: String? = null
) {
    fun calculateMonthlyCost(): Money {
        val multiplier = when (frequency) {
            PaymentFrequency.DAILY -> 30.0
            PaymentFrequency.WEEKLY -> 52.0 / 12.0
            PaymentFrequency.BI_WEEKLY -> 26.0 / 12.0
            PaymentFrequency.MONTHLY -> 1.0
            PaymentFrequency.QUARTERLY -> 1.0 / 3.0
            PaymentFrequency.YEARLY -> 1.0 / 12.0
            PaymentFrequency.CUSTOM -> when (customIntervalUnit) {
                CustomIntervalUnit.DAYS -> 30.0 / maxOf(1, customIntervalValue)
                CustomIntervalUnit.WEEKS -> (52.0 / 12.0) / maxOf(1, customIntervalValue)
                CustomIntervalUnit.MONTHS -> 1.0 / maxOf(1, customIntervalValue)
                CustomIntervalUnit.YEARS -> (1.0 / 12.0) / maxOf(1, customIntervalValue)
            }
        }
        return amount * multiplier
    }

    fun calculateAnnualCost(): Money {
        val multiplier = when (frequency) {
            PaymentFrequency.DAILY -> 365.0
            PaymentFrequency.WEEKLY -> 52.0
            PaymentFrequency.BI_WEEKLY -> 26.0
            PaymentFrequency.MONTHLY -> 12.0
            PaymentFrequency.QUARTERLY -> 4.0
            PaymentFrequency.YEARLY -> 1.0
            PaymentFrequency.CUSTOM -> when (customIntervalUnit) {
                CustomIntervalUnit.DAYS -> 365.0 / maxOf(1, customIntervalValue)
                CustomIntervalUnit.WEEKS -> 52.0 / maxOf(1, customIntervalValue)
                CustomIntervalUnit.MONTHS -> 12.0 / maxOf(1, customIntervalValue)
                CustomIntervalUnit.YEARS -> 1.0 / maxOf(1, customIntervalValue)
            }
        }
        return amount * multiplier
    }
}

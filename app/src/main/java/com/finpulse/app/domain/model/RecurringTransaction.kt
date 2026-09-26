package com.finpulse.app.domain.model

import com.finpulse.app.core.model.Money
import kotlinx.serialization.Serializable

enum class PaymentFrequency(val displayName: String, val approxDays: Int) {
    DAILY("Daily", 1),
    WEEKLY("Weekly", 7),
    BI_WEEKLY("Every 2 Weeks", 14),
    MONTHLY("Monthly", 30),
    QUARTERLY("Quarterly", 90),
    YEARLY("Yearly", 365)
}

@Serializable
data class RecurringTransaction(
    val id: String,
    val title: String,
    val amount: Money,
    val accountId: String,
    val categoryId: String,
    val frequency: PaymentFrequency = PaymentFrequency.MONTHLY,
    val nextDueDate: Long,
    val lastProcessedDate: Long? = null,
    val isActive: Boolean = true,
    val isSubscription: Boolean = false,
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
        }
        return amount * multiplier
    }

    fun calculateAnnualCost(): Money {
        return calculateMonthlyCost() * 12.0
    }
}

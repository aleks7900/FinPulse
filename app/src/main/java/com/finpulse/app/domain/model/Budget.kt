package com.finpulse.app.domain.model

import com.finpulse.app.core.model.Money
import kotlinx.serialization.Serializable

enum class BudgetPeriod(val displayName: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM("Custom")
}

@Serializable
data class Budget(
    val id: String,
    val categoryId: String,
    val name: String,
    val limitAmount: Money,
    val periodType: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: Long,
    val endDate: Long,
    val notifyAt70: Boolean = true,
    val notifyAt90: Boolean = true,
    val notifyAt100: Boolean = true,
    val isArchived: Boolean = false
)

data class BudgetStatus(
    val budget: Budget,
    val categoryName: String,
    val categoryColorHex: Long,
    val spentAmount: Money,
    val remainingAmount: Money,
    val percentageConsumed: Double,
    val projectedSpend: Money,
    val isExceeded: Boolean,
    val isWarning: Boolean
)

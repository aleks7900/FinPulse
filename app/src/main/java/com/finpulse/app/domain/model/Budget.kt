package com.finpulse.app.domain.model

import com.finpulse.app.core.model.Money
import kotlinx.serialization.Serializable

enum class BudgetPeriod(val displayName: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM("Custom")
}

enum class PacingStatus(val displayName: String) {
    ON_TRACK("On Track"),
    UNDER_PACE("Under Pace"),
    AHEAD_OF_PACE("Ahead of Pace")
}

enum class BudgetAlertLevel {
    NORMAL,
    INFO_70,
    WARNING_THRESHOLD,
    EXCEEDED
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
    val isArchived: Boolean = false,
    val isOverall: Boolean = false,
    val isRolloverEnabled: Boolean = false,
    val rolloverAmountMinor: Long = 0L,
    val alertThresholdPercent: Int = 85
) {
    val effectiveLimit: Money
        get() {
            val totalMinor = if (isRolloverEnabled) {
                limitAmount.amountMinor + rolloverAmountMinor
            } else {
                limitAmount.amountMinor
            }
            return Money(maxOf(0L, totalMinor), limitAmount.currencyCode)
        }
}

data class PacingMetrics(
    val totalDays: Int,
    val currentDay: Int,
    val daysRemaining: Int,
    val targetDailySpend: Money,
    val actualDailySpend: Money,
    val allowedDailyRemaining: Money,
    val dailyPacingDelta: Money, // Positive means actual daily spend exceeds target daily spend
    val projectedSpend: Money,
    val pacingStatus: PacingStatus
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
    val isWarning: Boolean,
    val alertLevel: BudgetAlertLevel = BudgetAlertLevel.NORMAL,
    val pacingMetrics: PacingMetrics? = null,
    val isOverall: Boolean = false,
    val refundsAmount: Money = Money.zero(budget.limitAmount.currencyCode)
)

data class SafeToSpendBreakdown(
    val totalLiquidFunds: Money,
    val upcomingObligations: Money,
    val savingsTargets: Money,
    val overallBudgetConstraint: Money?,
    val discretionarySafeToSpend: Money,
    val dailySafeToSpend: Money,
    val weeklySafeToSpend: Money,
    val daysRemaining: Int,
    val assumptions: List<String>,
    val liquidAccountsCount: Int,
    val upcomingBillsCount: Int,
    val activeGoalsCount: Int,
    val multiCurrencyWarning: Boolean = false
)

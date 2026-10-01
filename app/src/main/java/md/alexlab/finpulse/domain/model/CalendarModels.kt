package md.alexlab.finpulse.domain.model

import md.alexlab.finpulse.core.model.Money
import kotlinx.serialization.Serializable
import java.time.LocalDate

enum class CalendarEventType(val displayName: String) {
    INCOME("Income"),
    BILL("Bill"),
    SUBSCRIPTION("Subscription"),
    LOAN_PAYMENT("Loan Payment"),
    RECURRING_TRANSFER("Transfer"),
    GOAL_CONTRIBUTION("Goal Contribution"),
    TRANSACTION("Transaction")
}

enum class CalendarEventStatus(val displayName: String) {
    EXPECTED("Upcoming"),
    OVERDUE("Overdue"),
    COMPLETED("Paid"),
    SKIPPED("Skipped")
}

@Serializable
data class CalendarEvent(
    val id: String,
    val title: String,
    val amount: Money,
    val dateMillis: Long,
    val type: CalendarEventType,
    val status: CalendarEventStatus,
    val sourceAccountId: String? = null,
    val sourceAccountName: String? = null,
    val destinationAccountId: String? = null,
    val destinationAccountName: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val categoryIcon: String? = null,
    val categoryColorHex: Long? = null,
    val underlyingRecurringRuleId: String? = null,
    val underlyingOccurrenceId: String? = null,
    val underlyingTransactionId: String? = null,
    val underlyingDebtId: String? = null,
    val underlyingGoalId: String? = null,
    val notes: String? = null,
    val hasReminder: Boolean = false,
    val reminderDaysBefore: Int = 1,
    val isVariableAmount: Boolean = false
) {
    val isCompleted: Boolean
        get() = status == CalendarEventStatus.COMPLETED

    val isInflow: Boolean
        get() = type == CalendarEventType.INCOME

    val isOutflow: Boolean
        get() = type in setOf(
            CalendarEventType.BILL,
            CalendarEventType.SUBSCRIPTION,
            CalendarEventType.LOAN_PAYMENT,
            CalendarEventType.GOAL_CONTRIBUTION
        )
}

data class DayCashFlow(
    val date: LocalDate,
    val dateMillis: Long,
    val events: List<CalendarEvent>,
    val totalInflow: Money,
    val totalOutflow: Money,
    val netCashFlow: Money,
    val projectedEndBalance: Money
)

data class CashFlowPeriodSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startingBalance: Money,
    val projectedEndingBalance: Money,
    val lowestProjectedBalance: Money,
    val lowestBalanceDate: LocalDate?,
    val totalInflow: Money,
    val totalOutflow: Money,
    val netCashFlow: Money,
    val incomeCount: Int,
    val billCount: Int,
    val subscriptionCount: Int,
    val loanPaymentCount: Int,
    val transferCount: Int,
    val goalContributionCount: Int,
    val totalCompletedCount: Int,
    val totalExpectedCount: Int,
    val totalOverdueCount: Int,
    val dailyProjections: List<DayCashFlow>,
    val allEvents: List<CalendarEvent>,
    val isDeterministic: Boolean = true
)

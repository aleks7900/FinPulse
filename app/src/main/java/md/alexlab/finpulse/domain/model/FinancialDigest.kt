package md.alexlab.finpulse.domain.model

import md.alexlab.finpulse.core.model.Money
import kotlinx.serialization.Serializable
import java.util.UUID

enum class DigestPeriod {
    DAILY,
    WEEKLY
}

enum class DigestFrequency {
    DAILY,
    WEEKLY,
    OFF;

    companion object {
        fun fromString(value: String): DigestFrequency {
            return when (value.uppercase()) {
                "DAILY" -> DAILY
                "WEEKLY" -> WEEKLY
                else -> OFF
            }
        }
    }
}

enum class DigestPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

enum class DigestInsightCategory {
    SPENDING_OVERVIEW,
    PERIOD_COMPARISON,
    TOP_EXPENSES,
    BUDGET_STATUS,
    UPCOMING_BILLS,
    REVIEW_QUEUE,
    SAVINGS_GOALS,
    ANOMALY_DETECTION,
    EXPLANATION
}

@Serializable
data class DigestInsight(
    val id: String = UUID.randomUUID().toString(),
    val category: DigestInsightCategory,
    val priority: DigestPriority,
    val title: String,
    val summary: String,
    /**
     * Optional detailed explanation.
     * Ready to be augmented by local deterministic templates, server-side summaries,
     * or on-device / cloud AI models without coupling to notification generation.
     */
    val detailedExplanation: String? = null,
    val metricValue: String? = null,
    val secondaryMetric: String? = null,
    val changePercentage: Double? = null,
    val actionRoute: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

data class TopExpenseItem(
    val title: String,
    val amount: Money,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: Long = 0xFF607D8B,
    val timestamp: Long,
    val percentageOfPeriodSpend: Double
)

data class BudgetDigestItem(
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: Long = 0xFF607D8B,
    val limitAmount: Money,
    val spentAmount: Money,
    val remainingAmount: Money,
    val percentageConsumed: Double,
    val isExceeded: Boolean,
    val isWarning: Boolean
)

data class UpcomingBillDigestItem(
    val id: String,
    val title: String,
    val amount: Money,
    val dueDateMillis: Long,
    val daysUntilDue: Long,
    val isOverdue: Boolean
)

data class GoalProgressDigestItem(
    val id: String,
    val title: String,
    val currentAmount: Money,
    val targetAmount: Money,
    val progressPercentage: Double
)

data class FinancialDigest(
    val id: String = UUID.randomUUID().toString(),
    val period: DigestPeriod,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val generatedAtMillis: Long = System.currentTimeMillis(),
    val totalSpend: Money,
    val previousPeriodSpend: Money,
    val spendDifference: Money,
    val spendChangePercentage: Double,
    val transactionCount: Int,
    val topExpenses: List<TopExpenseItem> = emptyList(),
    val budgetItems: List<BudgetDigestItem> = emptyList(),
    val upcomingBills: List<UpcomingBillDigestItem> = emptyList(),
    val unreviewedCount: Int = 0,
    val goalItems: List<GoalProgressDigestItem> = emptyList(),
    val insights: List<DigestInsight> = emptyList(),
    val headline: String,
    val narrativeSummary: String,
    /**
     * High-level AI or server explanation block.
     * Can be filled asynchronously or dynamically by external AI providers.
     */
    val aiExplanation: String? = null
)

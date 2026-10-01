package md.alexlab.finpulse.domain.model

import kotlinx.serialization.Serializable

enum class InsightType {
    INFO,
    WARNING,
    SUCCESS,
    DANGER
}

enum class InsightAction {
    VIEW_BUDGET,
    VIEW_SUBSCRIPTIONS,
    VIEW_TRANSACTIONS,
    VIEW_SAVINGS,
    NONE
}

@Serializable
data class FinancialInsight(
    val id: String,
    val type: InsightType,
    val title: String,
    val description: String,
    val actionType: InsightAction = InsightAction.NONE,
    val timestamp: Long = System.currentTimeMillis()
)

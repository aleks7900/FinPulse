package md.alexlab.finpulse.domain.model

import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.ui.getCategoryDisplayName
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
    val timestamp: Long = System.currentTimeMillis(),
    val titleRes: Int? = null,
    val titleArgs: List<String> = emptyList(),
    val descriptionRes: Int? = null,
    val descriptionArgs: List<String> = emptyList(),
    val categoryId: String? = null
)

@Composable
fun FinancialInsight.getLocalizedTitle(): String {
    if (titleRes != null) {
        val resolvedArgs = if (categoryId != null && titleArgs.isNotEmpty()) {
            val localizedCat = getCategoryDisplayName(categoryId, titleArgs[0])
            listOf(localizedCat) + titleArgs.drop(1)
        } else {
            titleArgs
        }
        return stringResource(titleRes, *resolvedArgs.toTypedArray())
    }
    return title
}

@Composable
fun FinancialInsight.getLocalizedDescription(): String {
    if (descriptionRes != null) {
        val resolvedArgs = if (categoryId != null && descriptionArgs.isNotEmpty()) {
            val localizedCat = getCategoryDisplayName(categoryId, descriptionArgs[0])
            listOf(localizedCat) + descriptionArgs.drop(1)
        } else {
            descriptionArgs
        }
        return stringResource(descriptionRes, *resolvedArgs.toTypedArray())
    }
    return description
}

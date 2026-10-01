package md.alexlab.finpulse.domain.model

import md.alexlab.finpulse.core.model.Money
import kotlinx.serialization.Serializable

@Serializable
data class RecurringOccurrence(
    val id: String, // "${ruleId}_${dueDate}"
    val ruleId: String,
    val ruleTitle: String,
    val amount: Money,
    val type: TransactionType,
    val accountId: String,
    val destinationAccountId: String? = null,
    val categoryId: String,
    val dueDate: Long,
    val status: OccurrenceStatus,
    val paidDate: Long? = null,
    val transactionId: String? = null,
    val isVariableAmount: Boolean = false,
    val isSubscription: Boolean = false,
    val notes: String? = null
)

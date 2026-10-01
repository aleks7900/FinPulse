package md.alexlab.finpulse.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("nextDueDate"),
        Index("type"),
        Index("isActive")
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val amountMinor: Long,
    val currencyCode: String = "USD",
    val type: String = "EXPENSE", // EXPENSE, INCOME, TRANSFER
    val accountId: String,
    val destinationAccountId: String? = null,
    val categoryId: String,
    val frequency: String, // DAILY, WEEKLY, BI_WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM
    val customIntervalValue: Int = 1,
    val customIntervalUnit: String = "MONTHS", // DAYS, WEEKS, MONTHS, YEARS
    val anchorDayOfMonth: Int = 1,
    val nextDueDate: Long,
    val lastProcessedDate: Long? = null,
    val isActive: Boolean = true,
    val isCancelled: Boolean = false,
    val isSubscription: Boolean = false,
    val isVariableAmount: Boolean = false,
    val reminderDaysBefore: Int = 1,
    val notes: String? = null
)

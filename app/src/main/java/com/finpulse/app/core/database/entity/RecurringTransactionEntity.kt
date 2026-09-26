package com.finpulse.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("nextDueDate")
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val amountMinor: Long,
    val currencyCode: String = "USD",
    val accountId: String,
    val categoryId: String,
    val frequency: String, // DAILY, WEEKLY, BI_WEEKLY, MONTHLY, QUARTERLY, YEARLY
    val nextDueDate: Long,
    val lastProcessedDate: Long? = null,
    val isActive: Boolean = true,
    val isSubscription: Boolean = false,
    val notes: String? = null
)

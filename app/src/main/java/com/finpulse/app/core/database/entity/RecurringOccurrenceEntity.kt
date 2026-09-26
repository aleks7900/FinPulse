package com.finpulse.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_occurrences",
    indices = [
        Index("ruleId"),
        Index("dueDate"),
        Index("status")
    ]
)
data class RecurringOccurrenceEntity(
    @PrimaryKey
    val id: String, // "${ruleId}_${dueDate}"
    val ruleId: String,
    val dueDate: Long,
    val status: String, // EXPECTED, OVERDUE, GENERATED, PAID, SKIPPED
    val amountMinor: Long? = null,
    val currencyCode: String? = null,
    val paidDate: Long? = null,
    val transactionId: String? = null,
    val notes: String? = null
)

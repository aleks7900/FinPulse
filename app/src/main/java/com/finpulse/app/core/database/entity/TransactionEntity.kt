package com.finpulse.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("sourceAccountId"),
        Index("destinationAccountId"),
        Index("categoryId"),
        Index("timestamp"),
        Index("type"),
        Index("isCategoryConfirmed"),
        Index("merchant"),
        Index("amountMinor"),
        Index("currencyCode"),
        Index("recurringRuleId"),
        Index("isExcludedFromBudget"),
        Index(value = ["timestamp", "amountMinor"]),
        Index(value = ["type", "timestamp"]),
        Index(value = ["categoryId", "timestamp"]),
        Index(value = ["sourceAccountId", "timestamp"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val amountMinor: Long,
    val currencyCode: String = "USD",
    val type: String, // INCOME, EXPENSE, TRANSFER, REFUND
    val sourceAccountId: String,
    val destinationAccountId: String? = null,
    val categoryId: String,
    val merchant: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String = "",
    val tags: String = "", // Comma-separated tags
    val notes: String? = null,
    val recurringRuleId: String? = null,
    val isExcludedFromBudget: Boolean = false,
    val isCategoryConfirmed: Boolean = true,
    val categorizationConfidence: Float = 1.0f,
    val matchedRuleId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

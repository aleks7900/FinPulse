package md.alexlab.finpulse.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categorization_rules",
    indices = [
        Index("priority"),
        Index("targetCategoryId"),
        Index("isActive")
    ]
)
data class CategorizationRuleEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val targetCategoryId: String,
    val priority: Int = 0,
    val merchantPattern: String? = null,
    val merchantMatchType: String = "CONTAINS",
    val descriptionPattern: String? = null,
    val descriptionMatchType: String = "CONTAINS",
    val accountId: String? = null,
    val minAmountMinor: Long? = null,
    val maxAmountMinor: Long? = null,
    val transactionType: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

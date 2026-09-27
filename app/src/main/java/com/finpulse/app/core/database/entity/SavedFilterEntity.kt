package com.finpulse.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_filters",
    indices = [
        Index("createdAt"),
        Index("name")
    ]
)
data class SavedFilterEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val searchQuery: String = "",
    val accountId: String? = null,
    val categoryId: String? = null,
    val transactionType: String? = null,
    val dateRangePreset: String = "ALL",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val minAmountMinor: Long? = null,
    val maxAmountMinor: Long? = null,
    val currencyCode: String? = null,
    val status: String = "ALL",
    val sortOrder: String = "DATE_DESC",
    val isPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

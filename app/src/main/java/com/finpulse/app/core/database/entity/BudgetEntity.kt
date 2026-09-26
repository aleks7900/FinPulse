package com.finpulse.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index("categoryId")
    ]
)
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String,
    val name: String,
    val limitAmountMinor: Long,
    val currencyCode: String = "USD",
    val periodType: String = "MONTHLY",
    val startDate: Long,
    val endDate: Long,
    val notifyAt70: Boolean = true,
    val notifyAt90: Boolean = true,
    val notifyAt100: Boolean = true,
    val isArchived: Boolean = false
)

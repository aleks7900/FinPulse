package com.finpulse.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String, // EXPENSE, INCOME
    val parentCategoryId: String? = null,
    val icon: String = "category",
    val colorHex: Long = 0xFF607D8B,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

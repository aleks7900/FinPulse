package md.alexlab.finpulse.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_goals",
    indices = [
        Index("linkedAccountId")
    ]
)
data class FinancialGoalEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val targetAmountMinor: Long,
    val currentAmountMinor: Long,
    val currencyCode: String = "USD",
    val targetDate: Long,
    val linkedAccountId: String? = null,
    val icon: String = "savings",
    val colorHex: Long = 0xFF4CAF50,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

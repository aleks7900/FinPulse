package md.alexlab.finpulse.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchant_signals",
    indices = [
        Index("categoryId"),
        Index("lastUsedAt")
    ]
)
data class MerchantSignalEntity(
    @PrimaryKey
    val normalizedMerchant: String,
    val categoryId: String,
    val useCount: Int = 1,
    val lastUsedAt: Long = System.currentTimeMillis()
)

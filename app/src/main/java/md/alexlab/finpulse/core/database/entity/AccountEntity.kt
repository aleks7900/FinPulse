package md.alexlab.finpulse.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String, // CASH, BANK, CREDIT_CARD, etc.
    val balanceMinor: Long,
    val availableBalanceMinor: Long,
    val creditLimitMinor: Long? = null,
    val currencyCode: String = "USD",
    val institution: String? = null,
    val icon: String = "account_balance",
    val colorHex: Long = 0xFF2196F3,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

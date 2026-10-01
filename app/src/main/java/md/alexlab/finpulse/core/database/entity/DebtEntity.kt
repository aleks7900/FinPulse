package md.alexlab.finpulse.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debts",
    indices = [
        Index("linkedAccountId")
    ]
)
data class DebtEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String, // CREDIT_CARD, PERSONAL_LOAN, etc.
    val totalPrincipalMinor: Long,
    val remainingBalanceMinor: Long,
    val currencyCode: String = "USD",
    val interestRatePercent: Double,
    val minimumPaymentMinor: Long,
    val nextPaymentDate: Long,
    val linkedAccountId: String? = null,
    val notes: String? = null
)

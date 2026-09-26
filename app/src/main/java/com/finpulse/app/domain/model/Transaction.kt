package com.finpulse.app.domain.model

import com.finpulse.app.core.model.Money
import kotlinx.serialization.Serializable

enum class TransactionType(val displayName: String) {
    INCOME("Income"),
    EXPENSE("Expense"),
    TRANSFER("Transfer"),
    REFUND("Refund")
}

@Serializable
data class Transaction(
    val id: String,
    val amount: Money,
    val type: TransactionType,
    val sourceAccountId: String,
    val destinationAccountId: String? = null,
    val categoryId: String,
    val merchant: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String = "",
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    val recurringRuleId: String? = null,
    val isExcludedFromBudget: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

package com.finpulse.app.domain.model

import com.finpulse.app.core.model.Money
import kotlinx.serialization.Serializable

enum class AccountType(val displayName: String) {
    CASH("Cash"),
    BANK("Bank Account"),
    CREDIT_CARD("Credit Card"),
    SAVINGS("Savings Account"),
    INVESTMENT("Investment Account"),
    WALLET("Digital Wallet"),
    LOAN("Loan"),
    OTHER("Other")
}

@Serializable
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val balance: Money,
    val availableBalance: Money,
    val creditLimit: Money? = null,
    val institution: String? = null,
    val icon: String = "account_balance",
    val colorHex: Long = 0xFF2196F3,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

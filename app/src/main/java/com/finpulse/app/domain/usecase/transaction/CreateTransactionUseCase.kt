package com.finpulse.app.domain.usecase.transaction

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.TransactionRepository
import java.util.UUID

sealed class CreateTransactionResult {
    data class Success(val transaction: Transaction) : CreateTransactionResult()
    data class Error(val message: String) : CreateTransactionResult()
}

class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(
        id: String? = null,
        amountMinor: Long,
        currencyCode: String,
        type: TransactionType,
        sourceAccountId: String,
        destinationAccountId: String? = null,
        categoryId: String,
        merchant: String? = null,
        description: String = "",
        tags: List<String> = emptyList(),
        notes: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): CreateTransactionResult {
        // 1. Validation: Amount must be strictly positive
        if (amountMinor <= 0L) {
            return CreateTransactionResult.Error("Amount must be greater than zero")
        }

        // 2. Validation: Source account must exist and must not be archived
        val sourceAccount = accountRepository.getAccountById(sourceAccountId)
            ?: return CreateTransactionResult.Error("Source account not found")

        if (sourceAccount.isArchived) {
            return CreateTransactionResult.Error("Cannot record transactions on an archived account")
        }

        // 3. Validation: Currency match with source account
        if (sourceAccount.balance.currencyCode != currencyCode) {
            return CreateTransactionResult.Error(
                "Currency mismatch: Account uses ${sourceAccount.balance.currencyCode}, but amount specified $currencyCode"
            )
        }

        // 4. Validation: Category must not be blank
        if (categoryId.isBlank()) {
            return CreateTransactionResult.Error("Category must be specified")
        }

        // 5. Validation: Transfer specific rules
        if (type == TransactionType.TRANSFER) {
            if (destinationAccountId.isNullOrBlank()) {
                return CreateTransactionResult.Error("Destination account is required for transfers")
            }
            if (destinationAccountId == sourceAccountId) {
                return CreateTransactionResult.Error("Source and destination accounts cannot be identical")
            }
            val destinationAccount = accountRepository.getAccountById(destinationAccountId)
                ?: return CreateTransactionResult.Error("Destination account not found")

            if (destinationAccount.isArchived) {
                return CreateTransactionResult.Error("Cannot transfer to an archived account")
            }
            if (destinationAccount.balance.currencyCode != currencyCode) {
                return CreateTransactionResult.Error(
                    "Transfer between different currencies (${sourceAccount.balance.currencyCode} to ${destinationAccount.balance.currencyCode}) requires currency conversion."
                )
            }
        }

        val transaction = Transaction(
            id = id ?: UUID.randomUUID().toString(),
            amount = Money(amountMinor, currencyCode),
            type = type,
            sourceAccountId = sourceAccountId,
            destinationAccountId = if (type == TransactionType.TRANSFER) destinationAccountId else null,
            categoryId = categoryId,
            merchant = merchant?.trim()?.takeIf { it.isNotEmpty() },
            description = description.trim(),
            tags = tags.map { it.trim() }.filter { it.isNotEmpty() },
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            timestamp = timestamp
        )

        if (id != null) {
            transactionRepository.updateTransaction(transaction)
        } else {
            transactionRepository.createTransaction(transaction)
        }

        return CreateTransactionResult.Success(transaction)
    }
}

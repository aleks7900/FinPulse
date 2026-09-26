package com.finpulse.app.domain.usecase.recurring

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.CustomIntervalUnit
import com.finpulse.app.domain.model.PaymentFrequency
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.RecurringRepository
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

sealed class SaveRecurringRuleResult {
    data class Success(val rule: RecurringTransaction) : SaveRecurringRuleResult()
    data class Error(val message: String) : SaveRecurringRuleResult()
}

class ManageRecurringRuleUseCase(
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository
) {
    suspend fun saveRule(
        id: String? = null,
        title: String,
        amountMinor: Long,
        currencyCode: String = "USD",
        type: TransactionType = TransactionType.EXPENSE,
        sourceAccountId: String,
        destinationAccountId: String? = null,
        categoryId: String,
        frequency: PaymentFrequency = PaymentFrequency.MONTHLY,
        customIntervalValue: Int = 1,
        customIntervalUnit: CustomIntervalUnit = CustomIntervalUnit.MONTHS,
        nextDueDate: Long,
        isSubscription: Boolean = false,
        isVariableAmount: Boolean = false,
        reminderDaysBefore: Int = 1,
        notes: String? = null
    ): SaveRecurringRuleResult {
        if (title.isBlank()) {
            return SaveRecurringRuleResult.Error("Title cannot be empty")
        }
        if (amountMinor <= 0L) {
            return SaveRecurringRuleResult.Error("Amount must be greater than zero")
        }
        val sourceAccount = accountRepository.getAccountById(sourceAccountId)
            ?: return SaveRecurringRuleResult.Error("Source account not found")

        if (sourceAccount.isArchived) {
            return SaveRecurringRuleResult.Error("Cannot use an archived account")
        }

        if (type == TransactionType.TRANSFER) {
            if (destinationAccountId.isNullOrBlank()) {
                return SaveRecurringRuleResult.Error("Destination account required for transfer")
            }
            if (destinationAccountId == sourceAccountId) {
                return SaveRecurringRuleResult.Error("Source and destination accounts must be distinct")
            }
            val destAcc = accountRepository.getAccountById(destinationAccountId)
                ?: return SaveRecurringRuleResult.Error("Destination account not found")
            if (destAcc.isArchived) {
                return SaveRecurringRuleResult.Error("Destination account is archived")
            }
        }

        // Determine anchor day of month from nextDueDate
        val date = Instant.ofEpochMilli(nextDueDate).atZone(ZoneId.systemDefault()).toLocalDate()
        val anchorDay = date.dayOfMonth

        val rule = RecurringTransaction(
            id = id ?: UUID.randomUUID().toString(),
            title = title.trim(),
            amount = Money(amountMinor, currencyCode),
            type = type,
            accountId = sourceAccountId,
            destinationAccountId = if (type == TransactionType.TRANSFER) destinationAccountId else null,
            categoryId = categoryId,
            frequency = frequency,
            customIntervalValue = customIntervalValue,
            customIntervalUnit = customIntervalUnit,
            anchorDayOfMonth = anchorDay,
            nextDueDate = nextDueDate,
            isActive = true,
            isCancelled = false,
            isSubscription = isSubscription,
            isVariableAmount = isVariableAmount,
            reminderDaysBefore = reminderDaysBefore,
            notes = notes?.trim()?.takeIf { it.isNotEmpty() }
        )

        recurringRepository.saveRecurring(rule)
        return SaveRecurringRuleResult.Success(rule)
    }

    suspend fun setRuleActive(id: String, isActive: Boolean) {
        recurringRepository.setRuleActive(id, isActive)
    }

    suspend fun cancelRule(id: String) {
        recurringRepository.cancelRule(id)
    }

    suspend fun deleteRule(id: String) {
        recurringRepository.deleteRecurring(id)
    }
}

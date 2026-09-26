package com.finpulse.app.domain.usecase.recurring

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.engine.RecurringDateEngine
import com.finpulse.app.domain.model.OccurrenceStatus
import com.finpulse.app.domain.model.RecurringOccurrence
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.repository.RecurringRepository
import com.finpulse.app.domain.repository.TransactionRepository
import java.util.UUID

sealed class MarkPaidResult {
    data class Success(val transaction: Transaction, val occurrence: RecurringOccurrence) : MarkPaidResult()
    data class Error(val message: String) : MarkPaidResult()
}

class MarkOccurrencePaidUseCase(
    private val recurringRepository: RecurringRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        ruleId: String,
        occurrenceDueDate: Long,
        actualAmountMinor: Long? = null,
        paidDate: Long = System.currentTimeMillis(),
        accountId: String? = null
    ): MarkPaidResult {
        val rule = recurringRepository.getRecurringById(ruleId)
            ?: return MarkPaidResult.Error("Recurring rule not found")

        val effectiveAmountMinor = actualAmountMinor ?: rule.amount.amountMinor
        if (effectiveAmountMinor <= 0L) {
            return MarkPaidResult.Error("Amount must be greater than zero")
        }

        val effectiveAccountId = accountId ?: rule.accountId
        val occurrenceId = "${ruleId}_$occurrenceDueDate"

        // 1. Create ledger transaction
        val tx = Transaction(
            id = UUID.randomUUID().toString(),
            amount = Money(effectiveAmountMinor, rule.amount.currencyCode),
            type = rule.type,
            sourceAccountId = effectiveAccountId,
            destinationAccountId = rule.destinationAccountId,
            categoryId = rule.categoryId,
            merchant = rule.title,
            timestamp = paidDate,
            description = "Recurring: ${rule.title}",
            recurringRuleId = rule.id
        )
        transactionRepository.createTransaction(tx)

        // 2. Record occurrence as PAID
        val occurrence = RecurringOccurrence(
            id = occurrenceId,
            ruleId = rule.id,
            ruleTitle = rule.title,
            amount = tx.amount,
            type = rule.type,
            accountId = effectiveAccountId,
            destinationAccountId = rule.destinationAccountId,
            categoryId = rule.categoryId,
            dueDate = occurrenceDueDate,
            status = OccurrenceStatus.PAID,
            paidDate = paidDate,
            transactionId = tx.id,
            isVariableAmount = rule.isVariableAmount,
            isSubscription = rule.isSubscription,
            notes = rule.notes
        )
        recurringRepository.saveOccurrence(occurrence)

        // 3. Advance rule nextDueDate if this occurrence was the current or older due date
        if (occurrenceDueDate >= rule.nextDueDate) {
            val nextDue = RecurringDateEngine.calculateNextDueDate(
                currentDueDateMillis = rule.nextDueDate,
                frequency = rule.frequency,
                anchorDayOfMonth = rule.anchorDayOfMonth,
                customIntervalValue = rule.customIntervalValue,
                customIntervalUnit = rule.customIntervalUnit
            )
            recurringRepository.advanceRuleDueDate(rule.id, nextDue, paidDate)
        }

        return MarkPaidResult.Success(tx, occurrence)
    }
}

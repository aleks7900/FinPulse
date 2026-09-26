package com.finpulse.app.domain.usecase.recurring

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.engine.RecurringDateEngine
import com.finpulse.app.domain.model.OccurrenceStatus
import com.finpulse.app.domain.model.RecurringOccurrence
import com.finpulse.app.domain.repository.RecurringRepository

sealed class EditOccurrenceResult {
    data class Success(val occurrence: RecurringOccurrence) : EditOccurrenceResult()
    data class Error(val message: String) : EditOccurrenceResult()
}

class EditOccurrenceUseCase(
    private val recurringRepository: RecurringRepository
) {
    suspend operator fun invoke(
        ruleId: String,
        originalDueDate: Long,
        newDueDate: Long = originalDueDate,
        newAmountMinor: Long? = null,
        notes: String? = null
    ): EditOccurrenceResult {
        val rule = recurringRepository.getRecurringById(ruleId)
            ?: return EditOccurrenceResult.Error("Recurring rule not found")

        val effectiveAmount = if (newAmountMinor != null && newAmountMinor > 0L) {
            Money(newAmountMinor, rule.amount.currencyCode)
        } else {
            rule.amount
        }

        val occurrenceId = "${ruleId}_$originalDueDate"
        val existing = recurringRepository.getOccurrenceById(occurrenceId)

        val status = existing?.status ?: RecurringDateEngine.determineStatus(
            dueDateMillis = newDueDate,
            isPaid = false,
            isSkipped = false,
            isGenerated = false
        )

        val updated = RecurringOccurrence(
            id = occurrenceId,
            ruleId = rule.id,
            ruleTitle = rule.title,
            amount = effectiveAmount,
            type = rule.type,
            accountId = rule.accountId,
            destinationAccountId = rule.destinationAccountId,
            categoryId = rule.categoryId,
            dueDate = newDueDate,
            status = status,
            paidDate = existing?.paidDate,
            transactionId = existing?.transactionId,
            isVariableAmount = rule.isVariableAmount,
            isSubscription = rule.isSubscription,
            notes = notes ?: existing?.notes ?: rule.notes
        )

        recurringRepository.saveOccurrence(updated)
        return EditOccurrenceResult.Success(updated)
    }
}

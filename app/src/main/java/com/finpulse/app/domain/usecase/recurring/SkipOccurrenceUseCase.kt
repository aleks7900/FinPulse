package com.finpulse.app.domain.usecase.recurring

import com.finpulse.app.domain.engine.RecurringDateEngine
import com.finpulse.app.domain.model.OccurrenceStatus
import com.finpulse.app.domain.model.RecurringOccurrence
import com.finpulse.app.domain.repository.RecurringRepository

sealed class SkipOccurrenceResult {
    data class Success(val occurrence: RecurringOccurrence) : SkipOccurrenceResult()
    data class Error(val message: String) : SkipOccurrenceResult()
}

class SkipOccurrenceUseCase(
    private val recurringRepository: RecurringRepository
) {
    suspend operator fun invoke(
        ruleId: String,
        occurrenceDueDate: Long
    ): SkipOccurrenceResult {
        val rule = recurringRepository.getRecurringById(ruleId)
            ?: return SkipOccurrenceResult.Error("Recurring rule not found")

        val occurrenceId = "${ruleId}_$occurrenceDueDate"

        val occurrence = RecurringOccurrence(
            id = occurrenceId,
            ruleId = rule.id,
            ruleTitle = rule.title,
            amount = rule.amount,
            type = rule.type,
            accountId = rule.accountId,
            destinationAccountId = rule.destinationAccountId,
            categoryId = rule.categoryId,
            dueDate = occurrenceDueDate,
            status = OccurrenceStatus.SKIPPED,
            paidDate = null,
            transactionId = null,
            isVariableAmount = rule.isVariableAmount,
            isSubscription = rule.isSubscription,
            notes = rule.notes
        )
        recurringRepository.saveOccurrence(occurrence)

        // Advance rule nextDueDate
        if (occurrenceDueDate >= rule.nextDueDate) {
            val nextDue = RecurringDateEngine.calculateNextDueDate(
                currentDueDateMillis = rule.nextDueDate,
                frequency = rule.frequency,
                anchorDayOfMonth = rule.anchorDayOfMonth,
                customIntervalValue = rule.customIntervalValue,
                customIntervalUnit = rule.customIntervalUnit
            )
            recurringRepository.advanceRuleDueDate(rule.id, nextDue, System.currentTimeMillis())
        }

        return SkipOccurrenceResult.Success(occurrence)
    }
}

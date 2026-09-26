package com.finpulse.app.domain.usecase.recurring

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.engine.RecurringDateEngine
import com.finpulse.app.domain.model.OccurrenceStatus
import com.finpulse.app.domain.model.RecurringOccurrence
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetUpcomingOccurrencesUseCase(
    private val recurringRepository: RecurringRepository
) {
    operator fun invoke(
        windowDays: Int = 30,
        nowMillis: Long = System.currentTimeMillis()
    ): Flow<List<RecurringOccurrence>> {
        val endDateMillis = nowMillis + (windowDays * 86_400_000L)
        // Also look back 30 days to capture any overdue occurrences!
        val startDateMillis = nowMillis - (30 * 86_400_000L)

        return combine(
            recurringRepository.getActiveRecurringFlow(),
            recurringRepository.getOccurrencesInRangeFlow(startDateMillis, endDateMillis)
        ) { activeRules, persistedOccurrences ->
            val persistedMap = persistedOccurrences.associateBy { it.id }
            val occurrences = mutableListOf<RecurringOccurrence>()

            for (rule in activeRules) {
                // Project occurrences within window
                val projectedDueDates = RecurringDateEngine.projectOccurrences(
                    rule = rule,
                    startFromMillis = nowMillis,
                    windowDays = windowDays
                )

                for (dueDate in projectedDueDates) {
                    val occId = "${rule.id}_$dueDate"
                    val persisted = persistedMap[occId]

                    if (persisted != null) {
                        // Use persisted record (e.g. Paid, Skipped, Overridden)
                        occurrences.add(persisted)
                    } else {
                        // Compute dynamic status
                        val status = RecurringDateEngine.determineStatus(
                            dueDateMillis = dueDate,
                            nowMillis = nowMillis,
                            isPaid = false,
                            isSkipped = false,
                            isGenerated = false
                        )
                        occurrences.add(
                            RecurringOccurrence(
                                id = occId,
                                ruleId = rule.id,
                                ruleTitle = rule.title,
                                amount = rule.amount,
                                type = rule.type,
                                accountId = rule.accountId,
                                destinationAccountId = rule.destinationAccountId,
                                categoryId = rule.categoryId,
                                dueDate = dueDate,
                                status = status,
                                isVariableAmount = rule.isVariableAmount,
                                isSubscription = rule.isSubscription,
                                notes = rule.notes
                            )
                        )
                    }
                }
            }

            occurrences.sortedBy { it.dueDate }
        }
    }
}

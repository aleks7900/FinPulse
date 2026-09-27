package com.finpulse.app.domain.usecase

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.BudgetAlertLevel
import com.finpulse.app.domain.model.BudgetPeriod
import com.finpulse.app.domain.model.BudgetStatus
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.PacingMetrics
import com.finpulse.app.domain.model.PacingStatus
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import java.time.LocalDate

class EvaluateBudgetStatusUseCase {

    operator fun invoke(
        budgets: List<Budget>,
        categories: List<Category>,
        transactions: List<Transaction>,
        currentDate: LocalDate = LocalDate.now()
    ): List<BudgetStatus> {
        val categoryMap = categories.associateBy { it.id }

        return budgets.map { budget ->
            val isOverallBudget = budget.isOverall || budget.categoryId == "overall" || budget.categoryId.isBlank()
            val category = if (!isOverallBudget) categoryMap[budget.categoryId] else null
            val catName = if (isOverallBudget) {
                budget.name.ifBlank { "Overall Monthly Budget" }
            } else {
                category?.name ?: budget.name
            }
            val catColor = if (isOverallBudget) 0xFF10B981 else (category?.colorHex ?: 0xFF607D8B)

            val currency = budget.limitAmount.currencyCode

            // Filter transactions:
            // 1. Exclude transfers
            // 2. Exclude transactions marked as isExcludedFromBudget
            // 3. Multi-currency safety: only include transactions in the budget currency
            // 4. Must fall within budget period
            val eligibleTransactions = transactions.filter { tx ->
                !tx.isExcludedFromBudget &&
                tx.type != TransactionType.TRANSFER &&
                tx.amount.currencyCode.equals(currency, ignoreCase = true) &&
                tx.timestamp in budget.startDate..budget.endDate &&
                (isOverallBudget || tx.categoryId == budget.categoryId)
            }

            val expenseMinor = eligibleTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount.amountMinor }

            val refundMinor = eligibleTransactions
                .filter { it.type == TransactionType.REFUND }
                .sumOf { it.amount.amountMinor }

            // Refunds correctly offset expenses for the category/budget
            val netSpentMinor = maxOf(0L, expenseMinor - refundMinor)
            val spent = Money(netSpentMinor, currency)
            val refundsAmount = Money(refundMinor, currency)

            // Effective limit includes rollover (surplus or deficit) when enabled
            val effectiveLimit = budget.effectiveLimit
            val effectiveLimitMinor = effectiveLimit.amountMinor
            val remainingMinor = effectiveLimitMinor - netSpentMinor
            val remaining = Money(maxOf(0L, remainingMinor), currency)

            val percentage = if (effectiveLimitMinor > 0L) {
                netSpentMinor.toDouble() / effectiveLimitMinor.toDouble()
            } else 0.0

            // Pacing & Days calculation
            val (totalDays, currentDay, daysRemaining) = when (budget.periodType) {
                BudgetPeriod.WEEKLY -> {
                    val total = 7
                    val current = currentDate.dayOfWeek.value.coerceIn(1, 7)
                    Triple(total, current, maxOf(1, total - current + 1))
                }
                BudgetPeriod.MONTHLY -> {
                    val total = currentDate.lengthOfMonth()
                    val current = currentDate.dayOfMonth.coerceIn(1, total)
                    Triple(total, current, maxOf(1, total - current + 1))
                }
                BudgetPeriod.CUSTOM -> {
                    val total = maxOf(1, ((budget.endDate - budget.startDate) / (1000L * 60 * 60 * 24)).toInt() + 1)
                    val current = currentDate.dayOfMonth.coerceIn(1, total)
                    Triple(total, current, maxOf(1, total - current + 1))
                }
            }

            val targetDailySpendMinor = if (totalDays > 0) effectiveLimitMinor / totalDays else 0L
            val actualDailySpendMinor = if (currentDay > 0) netSpentMinor / currentDay else 0L
            val allowedDailyRemainingMinor = if (daysRemaining > 0) maxOf(0L, remainingMinor) / daysRemaining else 0L
            val dailyPacingDeltaMinor = actualDailySpendMinor - targetDailySpendMinor
            val projectedSpendMinor = actualDailySpendMinor * totalDays

            val expectedSpentSoFar = targetDailySpendMinor * currentDay
            val pacingStatus = when {
                expectedSpentSoFar <= 0L -> if (netSpentMinor > 0L) PacingStatus.AHEAD_OF_PACE else PacingStatus.ON_TRACK
                netSpentMinor > (expectedSpentSoFar * 1.05).toLong() -> PacingStatus.AHEAD_OF_PACE
                netSpentMinor < (expectedSpentSoFar * 0.95).toLong() -> PacingStatus.UNDER_PACE
                else -> PacingStatus.ON_TRACK
            }

            val pacingMetrics = PacingMetrics(
                totalDays = totalDays,
                currentDay = currentDay,
                daysRemaining = daysRemaining,
                targetDailySpend = Money(targetDailySpendMinor, currency),
                actualDailySpend = Money(actualDailySpendMinor, currency),
                allowedDailyRemaining = Money(allowedDailyRemainingMinor, currency),
                dailyPacingDelta = Money(dailyPacingDeltaMinor, currency),
                projectedSpend = Money(projectedSpendMinor, currency),
                pacingStatus = pacingStatus
            )

            val isExceeded = netSpentMinor > effectiveLimitMinor
            val thresholdRatio = budget.alertThresholdPercent / 100.0
            val isWarning = percentage >= thresholdRatio

            val alertLevel = when {
                isExceeded -> BudgetAlertLevel.EXCEEDED
                percentage >= thresholdRatio -> BudgetAlertLevel.WARNING_THRESHOLD
                budget.notifyAt70 && percentage >= 0.70 -> BudgetAlertLevel.INFO_70
                else -> BudgetAlertLevel.NORMAL
            }

            BudgetStatus(
                budget = budget,
                categoryName = catName,
                categoryColorHex = catColor,
                spentAmount = spent,
                remainingAmount = remaining,
                percentageConsumed = percentage,
                projectedSpend = Money(projectedSpendMinor, currency),
                isExceeded = isExceeded,
                isWarning = isWarning,
                alertLevel = alertLevel,
                pacingMetrics = pacingMetrics,
                isOverall = isOverallBudget,
                refundsAmount = refundsAmount
            )
        }
    }
}

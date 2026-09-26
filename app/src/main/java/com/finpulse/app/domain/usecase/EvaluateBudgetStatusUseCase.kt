package com.finpulse.app.domain.usecase

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.BudgetStatus
import com.finpulse.app.domain.model.Category
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
            val category = categoryMap[budget.categoryId]
            val catName = category?.name ?: budget.name
            val catColor = category?.colorHex ?: 0xFF607D8B

            // Sum expenses for this category within budget date range
            val spentMinor = transactions
                .filter {
                    it.categoryId == budget.categoryId &&
                    it.type == TransactionType.EXPENSE &&
                    !it.isExcludedFromBudget &&
                    it.timestamp in budget.startDate..budget.endDate
                }
                .sumOf { it.amount.amountMinor }

            val spent = Money(spentMinor, budget.limitAmount.currencyCode)
            val remainingMinor = (budget.limitAmount.amountMinor - spentMinor).coerceAtLeast(0L)
            val remaining = Money(remainingMinor, budget.limitAmount.currencyCode)

            val percentage = if (budget.limitAmount.amountMinor > 0L) {
                spentMinor.toDouble() / budget.limitAmount.amountMinor.toDouble()
            } else 0.0

            // Spending Projection
            val totalDays = currentDate.lengthOfMonth()
            val dayOfMonth = currentDate.dayOfMonth.coerceIn(1, totalDays)
            val projectedSpendMinor = if (dayOfMonth > 0) {
                ((spentMinor.toDouble() / dayOfMonth.toDouble()) * totalDays).toLong()
            } else spentMinor

            val projectedSpend = Money(projectedSpendMinor, budget.limitAmount.currencyCode)

            BudgetStatus(
                budget = budget,
                categoryName = catName,
                categoryColorHex = catColor,
                spentAmount = spent,
                remainingAmount = remaining,
                percentageConsumed = percentage,
                projectedSpend = projectedSpend,
                isExceeded = spentMinor > budget.limitAmount.amountMinor,
                isWarning = percentage >= 0.85
            )
        }
    }
}

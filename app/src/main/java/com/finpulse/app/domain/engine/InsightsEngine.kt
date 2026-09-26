package com.finpulse.app.domain.engine

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.BudgetStatus
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.FinancialInsight
import com.finpulse.app.domain.model.InsightAction
import com.finpulse.app.domain.model.InsightType
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.usecase.DashboardSummary
import java.util.UUID

class InsightsEngine {

    fun generateInsights(
        summary: DashboardSummary,
        budgetStatuses: List<BudgetStatus>,
        transactions: List<Transaction>,
        categories: List<Category>,
        subscriptions: List<RecurringTransaction>
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        val categoryMap = categories.associateBy { it.id }

        // 1. Budget Warnings
        for (status in budgetStatuses) {
            if (status.isExceeded) {
                val overage = status.spentAmount - status.budget.limitAmount
                insights.add(
                    FinancialInsight(
                        id = UUID.randomUUID().toString(),
                        type = InsightType.DANGER,
                        title = "Budget Exceeded: ${status.categoryName}",
                        description = "You have exceeded your ${status.categoryName} budget by ${overage.formatted()}.",
                        actionType = InsightAction.VIEW_BUDGET
                    )
                )
            } else if (status.isWarning) {
                val percentInt = (status.percentageConsumed * 100).toInt()
                insights.add(
                    FinancialInsight(
                        id = UUID.randomUUID().toString(),
                        type = InsightType.WARNING,
                        title = "Approaching Limit: ${status.categoryName}",
                        description = "You have used $percentInt% of your ${status.categoryName} budget. ${status.remainingAmount.formatted()} remaining.",
                        actionType = InsightAction.VIEW_BUDGET
                    )
                )
            } else if (status.projectedSpend > status.budget.limitAmount) {
                insights.add(
                    FinancialInsight(
                        id = UUID.randomUUID().toString(),
                        type = InsightType.WARNING,
                        title = "Budget Projection Risk: ${status.categoryName}",
                        description = "At your current spending rate, you are projected to spend ${status.projectedSpend.formatted()}, exceeding your budget limit.",
                        actionType = InsightAction.VIEW_BUDGET
                    )
                )
            }
        }

        // 2. Largest Expense Category
        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE && !it.isExcludedFromBudget }
        val categorySpend = expenseTransactions
            .groupBy { it.categoryId }
            .mapValues { (_, txList) -> txList.sumOf { it.amount.amountMinor } }
            .maxByOrNull { it.value }

        if (categorySpend != null && categorySpend.value > 0L) {
            val topCategory = categoryMap[categorySpend.key]?.name ?: "Uncategorized"
            val totalExpenseMinor = summary.expenses.amountMinor
            val percentOfTotal = if (totalExpenseMinor > 0) {
                ((categorySpend.value.toDouble() / totalExpenseMinor.toDouble()) * 100).toInt()
            } else 0

            insights.add(
                FinancialInsight(
                    id = UUID.randomUUID().toString(),
                    type = InsightType.INFO,
                    title = "Largest Spending Category",
                    description = "$topCategory accounts for $percentOfTotal% of your total expenses this period (${Money(categorySpend.value, summary.expenses.currencyCode).formatted()}).",
                    actionType = InsightAction.VIEW_TRANSACTIONS
                )
            )
        }

        // 3. Subscription & Recurring Obligations Audit
        val activeSubs = subscriptions.filter { it.isActive && it.isSubscription }
        if (activeSubs.isNotEmpty()) {
            val totalMonthlyMinor = activeSubs.sumOf { it.calculateMonthlyCost().amountMinor }
            val totalAnnualMinor = totalMonthlyMinor * 12L
            insights.add(
                FinancialInsight(
                    id = UUID.randomUUID().toString(),
                    type = InsightType.INFO,
                    title = "Subscription Impact",
                    description = "You have ${activeSubs.size} active subscriptions totaling ${Money(totalMonthlyMinor, summary.expenses.currencyCode).formatted()}/month (${Money(totalAnnualMinor, summary.expenses.currencyCode).formatted()}/year).",
                    actionType = InsightAction.VIEW_SUBSCRIPTIONS
                )
            )
        }

        // 4. Savings Rate Assessment
        val savingsRate = summary.savingsRatePercentage
        if (summary.income.amountMinor > 0L) {
            when {
                savingsRate >= 20.0 -> {
                    insights.add(
                        FinancialInsight(
                            id = UUID.randomUUID().toString(),
                            type = InsightType.SUCCESS,
                            title = "Strong Savings Rate: ${"%.1f".format(savingsRate)}%",
                            description = "Excellent discipline! Your savings rate is in the top tier (>20%). Keep building your net worth.",
                            actionType = InsightAction.VIEW_SAVINGS
                        )
                    )
                }
                savingsRate < 5.0 && summary.expenses.amountMinor > 0L -> {
                    insights.add(
                        FinancialInsight(
                            id = UUID.randomUUID().toString(),
                            type = InsightType.WARNING,
                            title = "Low Savings Margin: ${"%.1f".format(savingsRate)}%",
                            description = "Your current savings rate is below 5%. Consider reviewing non-essential subscriptions and discretionary shopping.",
                            actionType = InsightAction.VIEW_SAVINGS
                        )
                    )
                }
            }
        }

        // 5. Positive Cash Flow Highlight
        if (summary.netCashFlow.isPositive && summary.income.amountMinor > 0L) {
            insights.add(
                FinancialInsight(
                    id = UUID.randomUUID().toString(),
                    type = InsightType.SUCCESS,
                    title = "Positive Net Cash Flow",
                    description = "You have a surplus cash flow of ${summary.netCashFlow.formatted()} this period.",
                    actionType = InsightAction.NONE
                )
            )
        }

        return insights
    }
}

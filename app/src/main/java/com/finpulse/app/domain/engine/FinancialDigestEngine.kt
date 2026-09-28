package com.finpulse.app.domain.engine

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.BudgetDigestItem
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.DigestInsight
import com.finpulse.app.domain.model.DigestInsightCategory
import com.finpulse.app.domain.model.DigestPeriod
import com.finpulse.app.domain.model.DigestPriority
import com.finpulse.app.domain.model.FinancialDigest
import com.finpulse.app.domain.model.FinancialGoal
import com.finpulse.app.domain.model.GoalProgressDigestItem
import com.finpulse.app.domain.model.OccurrenceStatus
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.TopExpenseItem
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.model.UpcomingBillDigestItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

class FinancialDigestEngine {

    fun generateDigest(
        period: DigestPeriod,
        transactions: List<Transaction>,
        categories: List<Category>,
        budgets: List<Budget>,
        recurringRules: List<RecurringTransaction>,
        goals: List<FinancialGoal>,
        unreviewedCount: Int,
        baseCurrency: String,
        referenceDate: LocalDate = LocalDate.now(ZoneId.systemDefault())
    ): FinancialDigest {
        val zone = ZoneId.systemDefault()
        val categoryMap = categories.associateBy { it.id }

        // 1. Calculate Period Boundaries
        val (currentStartMillis, currentEndMillis, prevStartMillis, prevEndMillis) = calculateBoundaries(period, referenceDate, zone)

        // 2. Filter Transactions
        val currentTxs = transactions.filter { it.timestamp in currentStartMillis..currentEndMillis }
        val prevTxs = transactions.filter { it.timestamp in prevStartMillis..prevEndMillis }

        val currentExpenses = currentTxs.filter { it.type == TransactionType.EXPENSE && !it.isExcludedFromBudget }
        val prevExpenses = prevTxs.filter { it.type == TransactionType.EXPENSE && !it.isExcludedFromBudget }

        val totalSpendMinor = currentExpenses.sumOf { it.amount.amountMinor }
        val prevSpendMinor = prevExpenses.sumOf { it.amount.amountMinor }
        val diffMinor = totalSpendMinor - prevSpendMinor

        val totalSpend = Money(totalSpendMinor, baseCurrency)
        val prevSpend = Money(prevSpendMinor, baseCurrency)
        val spendDifference = Money(diffMinor, baseCurrency)

        val changePercentage = if (prevSpendMinor > 0L) {
            ((totalSpendMinor - prevSpendMinor).toDouble() / prevSpendMinor.toDouble()) * 100.0
        } else if (totalSpendMinor > 0L) {
            100.0
        } else {
            0.0
        }

        // 3. Largest Expenses
        val topExpenses = currentExpenses
            .sortedByDescending { it.amount.amountMinor }
            .take(3)
            .map { tx ->
                val cat = categoryMap[tx.categoryId]
                val catName = cat?.name ?: "General"
                val catIcon = cat?.icon ?: "shopping_bag"
                val catColor = cat?.colorHex ?: 0xFF607D8B
                val percent = if (totalSpendMinor > 0L) {
                    (tx.amount.amountMinor.toDouble() / totalSpendMinor.toDouble()) * 100.0
                } else 0.0

                TopExpenseItem(
                    title = tx.merchant?.takeIf { it.isNotBlank() } ?: tx.description.takeIf { it.isNotBlank() } ?: catName,
                    amount = tx.amount,
                    categoryName = catName,
                    categoryIcon = catIcon,
                    categoryColorHex = catColor,
                    timestamp = tx.timestamp,
                    percentageOfPeriodSpend = percent
                )
            }

        // 4. Budget Status
        val budgetItems = evaluateBudgets(budgets, categoryMap, currentTxs, baseCurrency)

        // 5. Upcoming Bills
        val horizonDays = if (period == DigestPeriod.DAILY) 3 else 7
        val upcomingBills = evaluateUpcomingBills(recurringRules, referenceDate, horizonDays, zone)

        // 6. Savings Goals Progress
        val goalItems = goals.filter { it.targetAmount.amountMinor > 0L }.map { goal ->
            val progress = (goal.currentAmount.amountMinor.toDouble() / goal.targetAmount.amountMinor.toDouble()) * 100.0
            GoalProgressDigestItem(
                id = goal.id,
                title = goal.title,
                currentAmount = goal.currentAmount,
                targetAmount = goal.targetAmount,
                progressPercentage = progress.coerceIn(0.0, 100.0)
            )
        }

        // 7. Deterministic Anomaly Detection
        val anomalies = detectAnomalies(currentExpenses, transactions, categoryMap, baseCurrency, changePercentage)

        // 8. Synthesize Prioritized Insights
        val insights = synthesizeInsights(
            period = period,
            totalSpend = totalSpend,
            prevSpend = prevSpend,
            changePercentage = changePercentage,
            transactionCount = currentTxs.size,
            topExpenses = topExpenses,
            budgetItems = budgetItems,
            upcomingBills = upcomingBills,
            unreviewedCount = unreviewedCount,
            goalItems = goalItems,
            anomalies = anomalies
        )

        // 9. Build Habit-forming Headline & Narrative
        val (headline, narrative) = generateNarrative(
            period = period,
            totalSpend = totalSpend,
            changePercentage = changePercentage,
            txCount = currentTxs.size,
            budgetItems = budgetItems,
            upcomingBills = upcomingBills,
            unreviewedCount = unreviewedCount
        )

        return FinancialDigest(
            period = period,
            startDateMillis = currentStartMillis,
            endDateMillis = currentEndMillis,
            totalSpend = totalSpend,
            previousPeriodSpend = prevSpend,
            spendDifference = spendDifference,
            spendChangePercentage = changePercentage,
            transactionCount = currentTxs.size,
            topExpenses = topExpenses,
            budgetItems = budgetItems,
            upcomingBills = upcomingBills,
            unreviewedCount = unreviewedCount,
            goalItems = goalItems,
            insights = insights,
            headline = headline,
            narrativeSummary = narrative
        )
    }

    private fun calculateBoundaries(
        period: DigestPeriod,
        referenceDate: LocalDate,
        zone: ZoneId
    ): BoundaryResult {
        return when (period) {
            DigestPeriod.DAILY -> {
                val currentStart = referenceDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val currentEnd = referenceDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

                val prevDate = referenceDate.minusDays(1)
                val prevStart = prevDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val prevEnd = prevDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

                BoundaryResult(currentStart, currentEnd, prevStart, prevEnd)
            }
            DigestPeriod.WEEKLY -> {
                // Last 7 days vs previous 7 days
                val currentStart = referenceDate.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
                val currentEnd = referenceDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

                val prevStart = referenceDate.minusDays(13).atStartOfDay(zone).toInstant().toEpochMilli()
                val prevEnd = referenceDate.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli() - 1

                BoundaryResult(currentStart, currentEnd, prevStart, prevEnd)
            }
        }
    }

    private data class BoundaryResult(
        val currentStart: Long,
        val currentEnd: Long,
        val prevStart: Long,
        val prevEnd: Long
    )

    private fun evaluateBudgets(
        budgets: List<Budget>,
        categoryMap: Map<String, Category>,
        currentTxs: List<Transaction>,
        baseCurrency: String
    ): List<BudgetDigestItem> {
        val activeBudgets = budgets.filter { !it.isArchived }
        val categoryExpenses = currentTxs
            .filter { it.type == TransactionType.EXPENSE && !it.isExcludedFromBudget }
            .groupBy { it.categoryId }
            .mapValues { (_, txList) -> txList.sumOf { it.amount.amountMinor } }

        return activeBudgets.map { budget ->
            val cat = categoryMap[budget.categoryId]
            val catName = cat?.name ?: "Category"
            val catIcon = cat?.icon ?: "account_balance_wallet"
            val catColor = cat?.colorHex ?: 0xFF607D8B

            val spentMinor = categoryExpenses[budget.categoryId] ?: 0L
            val limitMinor = budget.limitAmount.amountMinor
            val remainingMinor = (limitMinor - spentMinor).coerceAtLeast(0L)
            val consumed = if (limitMinor > 0L) {
                (spentMinor.toDouble() / limitMinor.toDouble()) * 100.0
            } else 0.0

            val isExceeded = spentMinor > limitMinor
            val isWarning = !isExceeded && consumed >= 80.0

            BudgetDigestItem(
                categoryName = catName,
                categoryIcon = catIcon,
                categoryColorHex = catColor,
                limitAmount = budget.limitAmount,
                spentAmount = Money(spentMinor, baseCurrency),
                remainingAmount = Money(remainingMinor, baseCurrency),
                percentageConsumed = consumed,
                isExceeded = isExceeded,
                isWarning = isWarning
            )
        }
    }

    private fun evaluateUpcomingBills(
        recurringRules: List<RecurringTransaction>,
        referenceDate: LocalDate,
        horizonDays: Int,
        zone: ZoneId
    ): List<UpcomingBillDigestItem> {
        val nowMillis = System.currentTimeMillis()
        val activeBills = recurringRules.filter { it.isActive && !it.isCancelled && it.type == TransactionType.EXPENSE }

        return activeBills.mapNotNull { rule ->
            val dueDay = Instant.ofEpochMilli(rule.nextDueDate).atZone(zone).toLocalDate()
            val daysUntil = ChronoUnit.DAYS.between(referenceDate, dueDay)
            val isOverdue = daysUntil < 0L

            if (isOverdue || daysUntil in 0L..horizonDays.toLong()) {
                val status = RecurringDateEngine.determineStatus(rule.nextDueDate, nowMillis)
                if (status != OccurrenceStatus.PAID && status != OccurrenceStatus.SKIPPED) {
                    UpcomingBillDigestItem(
                        id = rule.id,
                        title = rule.title,
                        amount = rule.amount,
                        dueDateMillis = rule.nextDueDate,
                        daysUntilDue = daysUntil,
                        isOverdue = isOverdue
                    )
                } else null
            } else null
        }.sortedWith(compareBy({ !it.isOverdue }, { it.dueDateMillis }))
    }

    private fun detectAnomalies(
        currentExpenses: List<Transaction>,
        allTransactions: List<Transaction>,
        categoryMap: Map<String, Category>,
        baseCurrency: String,
        changePercentage: Double
    ): List<DigestInsight> {
        val insights = mutableListOf<DigestInsight>()

        // Historical 30-day baseline
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val historicalTxs = allTransactions.filter {
            it.type == TransactionType.EXPENSE && it.timestamp >= thirtyDaysAgo
        }

        if (historicalTxs.size >= 5) {
            val avgMinor = historicalTxs.map { it.amount.amountMinor }.average()
            val thresholdMinor = (avgMinor * 2.5).toLong().coerceAtLeast(3000L) // at least $30.00 equivalent

            // Check if any single expense is exceptionally large
            val outlier = currentExpenses.firstOrNull { it.amount.amountMinor >= thresholdMinor }
            if (outlier != null) {
                val catName = categoryMap[outlier.categoryId]?.name ?: "Expense"
                val name = outlier.merchant?.takeIf { it.isNotBlank() } ?: outlier.description.takeIf { it.isNotBlank() } ?: catName
                val ratio = "%.1f".format(outlier.amount.amountMinor.toDouble() / avgMinor.coerceAtLeast(1.0))

                insights.add(
                    DigestInsight(
                        category = DigestInsightCategory.ANOMALY_DETECTION,
                        priority = DigestPriority.HIGH,
                        title = "Unusual Expense Detected",
                        summary = "$name of ${outlier.amount.formatted()} is ${ratio}x higher than your 30-day average transaction.",
                        metricValue = outlier.amount.formatted(),
                        actionRoute = "transactions",
                        metadata = mapOf("txId" to outlier.id)
                    )
                )
            }
        }

        // Check if overall period has an extreme spending spike (>50%)
        if (changePercentage >= 50.0 && currentExpenses.isNotEmpty()) {
            insights.add(
                DigestInsight(
                    category = DigestInsightCategory.ANOMALY_DETECTION,
                    priority = DigestPriority.HIGH,
                    title = "Noticeable Spending Surge",
                    summary = "Your spending this period is +${"%.0f".format(changePercentage)}% above the previous period.",
                    metricValue = "+${"%.0f".format(changePercentage)}%",
                    actionRoute = "analytics"
                )
            )
        }

        return insights
    }

    private fun synthesizeInsights(
        period: DigestPeriod,
        totalSpend: Money,
        prevSpend: Money,
        changePercentage: Double,
        transactionCount: Int,
        topExpenses: List<TopExpenseItem>,
        budgetItems: List<BudgetDigestItem>,
        upcomingBills: List<UpcomingBillDigestItem>,
        unreviewedCount: Int,
        goalItems: List<GoalProgressDigestItem>,
        anomalies: List<DigestInsight>
    ): List<DigestInsight> {
        val result = mutableListOf<DigestInsight>()

        // 1. Add detected anomalies (Priority CRITICAL / HIGH)
        result.addAll(anomalies)

        // 2. Budget Alerts
        val exceededBudgets = budgetItems.filter { it.isExceeded }
        val warningBudgets = budgetItems.filter { it.isWarning }

        for (exceeded in exceededBudgets) {
            val overage = exceeded.spentAmount - exceeded.limitAmount
            result.add(
                DigestInsight(
                    category = DigestInsightCategory.BUDGET_STATUS,
                    priority = DigestPriority.CRITICAL,
                    title = "Budget Exceeded: ${exceeded.categoryName}",
                    summary = "You have surpassed your limit by ${overage.formatted()} (${exceeded.percentageConsumed.toInt()}% consumed).",
                    metricValue = exceeded.spentAmount.formatted(),
                    actionRoute = "budgets"
                )
            )
        }

        for (warning in warningBudgets) {
            result.add(
                DigestInsight(
                    category = DigestInsightCategory.BUDGET_STATUS,
                    priority = DigestPriority.HIGH,
                    title = "Approaching Limit: ${warning.categoryName}",
                    summary = "${warning.percentageConsumed.toInt()}% used. Only ${warning.remainingAmount.formatted()} remaining.",
                    metricValue = "${warning.percentageConsumed.toInt()}%",
                    actionRoute = "budgets"
                )
            )
        }

        // 3. Upcoming Bills Alerts
        val overdueBills = upcomingBills.filter { it.isOverdue }
        val dueSoonBills = upcomingBills.filter { !it.isOverdue && it.daysUntilDue <= 1L }

        if (overdueBills.isNotEmpty()) {
            val totalOverdue = overdueBills.sumOf { it.amount.amountMinor }
            result.add(
                DigestInsight(
                    category = DigestInsightCategory.UPCOMING_BILLS,
                    priority = DigestPriority.CRITICAL,
                    title = "${overdueBills.size} Overdue Bill${if (overdueBills.size > 1) "s" else ""}",
                    summary = "${Money(totalOverdue, totalSpend.currencyCode).formatted()} requires immediate payment.",
                    metricValue = overdueBills.size.toString(),
                    actionRoute = "calendar"
                )
            )
        }

        if (dueSoonBills.isNotEmpty()) {
            val totalDue = dueSoonBills.sumOf { it.amount.amountMinor }
            result.add(
                DigestInsight(
                    category = DigestInsightCategory.UPCOMING_BILLS,
                    priority = DigestPriority.HIGH,
                    title = "${dueSoonBills.size} Bill${if (dueSoonBills.size > 1) "s" else ""} Due Soon",
                    summary = "${Money(totalDue, totalSpend.currencyCode).formatted()} due within 24-48 hours.",
                    metricValue = dueSoonBills.first().title,
                    actionRoute = "calendar"
                )
            )
        }

        // 4. Review Inbox Items
        if (unreviewedCount > 0) {
            result.add(
                DigestInsight(
                    category = DigestInsightCategory.REVIEW_QUEUE,
                    priority = if (unreviewedCount >= 3) DigestPriority.HIGH else DigestPriority.MEDIUM,
                    title = "$unreviewedCount Items Need Review",
                    summary = "Keep your financial ledger clean: categorize pending transactions and resolve duplicates.",
                    metricValue = unreviewedCount.toString(),
                    actionRoute = "review_inbox"
                )
            )
        }

        // 5. Period Comparison Insight
        if (prevSpend.amountMinor > 0L) {
            val isDaily = period == DigestPeriod.DAILY
            val periodWord = if (isDaily) "yesterday" else "last week"
            if (changePercentage <= -10.0) {
                result.add(
                    DigestInsight(
                        category = DigestInsightCategory.PERIOD_COMPARISON,
                        priority = DigestPriority.MEDIUM,
                        title = "Spending Down ${"%.0f".format(-changePercentage)}%",
                        summary = "Great pacing! You spent ${totalSpend.formatted()}, down from ${prevSpend.formatted()} $periodWord.",
                        metricValue = "-${"%.0f".format(-changePercentage)}%",
                        changePercentage = changePercentage,
                        actionRoute = "analytics"
                    )
                )
            } else if (changePercentage in -10.0..10.0) {
                result.add(
                    DigestInsight(
                        category = DigestInsightCategory.PERIOD_COMPARISON,
                        priority = DigestPriority.LOW,
                        title = "Consistent Spending",
                        summary = "Your spending is on par with $periodWord (${totalSpend.formatted()} vs ${prevSpend.formatted()}).",
                        metricValue = "±0%",
                        changePercentage = changePercentage,
                        actionRoute = "analytics"
                    )
                )
            } else if (changePercentage > 10.0 && changePercentage < 50.0) {
                result.add(
                    DigestInsight(
                        category = DigestInsightCategory.PERIOD_COMPARISON,
                        priority = DigestPriority.MEDIUM,
                        title = "Spending Up +${"%.0f".format(changePercentage)}%",
                        summary = "You spent ${totalSpend.formatted()} vs ${prevSpend.formatted()} $periodWord.",
                        metricValue = "+${"%.0f".format(changePercentage)}%",
                        changePercentage = changePercentage,
                        actionRoute = "analytics"
                    )
                )
            }
        }

        // 6. Top Expense Highlight
        if (topExpenses.isNotEmpty()) {
            val top = topExpenses.first()
            result.add(
                DigestInsight(
                    category = DigestInsightCategory.TOP_EXPENSES,
                    priority = DigestPriority.LOW,
                    title = "Largest Expense: ${top.title}",
                    summary = "${top.amount.formatted()} (${top.percentageOfPeriodSpend.toInt()}% of period spending) in ${top.categoryName}.",
                    metricValue = top.amount.formatted(),
                    actionRoute = "transactions"
                )
            )
        }

        // 7. Goals Milestone
        val closeGoal = goalItems.firstOrNull { it.progressPercentage >= 80.0 && it.progressPercentage < 100.0 }
        if (closeGoal != null) {
            result.add(
                DigestInsight(
                    category = DigestInsightCategory.SAVINGS_GOALS,
                    priority = DigestPriority.MEDIUM,
                    title = "Goal In Reach: ${closeGoal.title}",
                    summary = "You're at ${closeGoal.progressPercentage.toInt()}% of your ${closeGoal.targetAmount.formatted()} goal. Keep the momentum going!",
                    metricValue = "${closeGoal.progressPercentage.toInt()}%",
                    actionRoute = "goals"
                )
            )
        }

        // Sort by priority (CRITICAL -> HIGH -> MEDIUM -> LOW)
        return result.sortedBy { it.priority.ordinal }
    }

    private fun generateNarrative(
        period: DigestPeriod,
        totalSpend: Money,
        changePercentage: Double,
        txCount: Int,
        budgetItems: List<BudgetDigestItem>,
        upcomingBills: List<UpcomingBillDigestItem>,
        unreviewedCount: Int
    ): Pair<String, String> {
        val isDaily = period == DigestPeriod.DAILY
        val timeLabel = if (isDaily) "Today" else "This week"
        val prevLabel = if (isDaily) "yesterday" else "last week"

        val exceededCount = budgetItems.count { it.isExceeded }
        val dueCount = upcomingBills.size

        val headline = when {
            exceededCount > 0 -> "Attention Needed: Budget Limit Exceeded"
            dueCount > 0 && upcomingBills.any { it.isOverdue } -> "Urgent: Overdue Bills Pending"
            totalSpend.amountMinor == 0L -> "$timeLabel at a Glance: Zero Expenses Recorded"
            changePercentage <= -10.0 -> "Solid Progress: Spending Down ${"%.0f".format(-changePercentage)}%"
            changePercentage >= 35.0 -> "$timeLabel's Digest: Spending is Elevated"
            else -> "$timeLabel's Financial Pulse: On Track"
        }

        val parts = mutableListOf<String>()

        if (totalSpend.amountMinor > 0L) {
            val changeStr = when {
                changePercentage <= -10.0 -> " (down ${"%.0f".format(-changePercentage)}% vs $prevLabel)"
                changePercentage >= 10.0 -> " (up ${"%.0f".format(changePercentage)}% vs $prevLabel)"
                else -> " (steady compared to $prevLabel)"
            }
            parts.add("$timeLabel you recorded $txCount transaction${if (txCount > 1) "s" else ""} totaling ${totalSpend.formatted()}$changeStr.")
        } else {
            parts.add("No expenses logged so far $timeLabel.")
        }

        if (exceededCount > 0) {
            parts.add("$exceededCount budget${if (exceededCount > 1) "s have" else " has"} been exceeded.")
        } else if (budgetItems.isNotEmpty()) {
            parts.add("All active budgets remain within healthy boundaries.")
        }

        if (dueCount > 0) {
            parts.add("$dueCount scheduled obligation${if (dueCount > 1) "s are" else " is"} coming up in the next few days.")
        }

        if (unreviewedCount > 0) {
            parts.add("$unreviewedCount unreviewed item${if (unreviewedCount > 1) "s are" else " is"} waiting in your Inbox.")
        }

        val narrative = parts.joinToString(" ")
        return Pair(headline, narrative)
    }
}

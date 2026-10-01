package md.alexlab.finpulse.domain.engine

import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.CalendarEvent
import md.alexlab.finpulse.domain.model.CalendarEventStatus
import md.alexlab.finpulse.domain.model.CalendarEventType
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.CashFlowPeriodSummary
import md.alexlab.finpulse.domain.model.DayCashFlow
import md.alexlab.finpulse.domain.model.Debt
import md.alexlab.finpulse.domain.model.FinancialGoal
import md.alexlab.finpulse.domain.model.OccurrenceStatus
import md.alexlab.finpulse.domain.model.RecurringOccurrence
import md.alexlab.finpulse.domain.model.RecurringTransaction
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object CashFlowProjectionEngine {

    /**
     * Determines starting liquid funds from active accounts.
     * Considers liquid account types: Cash, Bank, Savings, Digital Wallet.
     */
    fun calculateStartingLiquidBalance(
        accounts: List<Account>,
        baseCurrency: String
    ): Money {
        val liquidAccountTypes = setOf(
            AccountType.CASH,
            AccountType.BANK,
            AccountType.SAVINGS,
            AccountType.WALLET
        )
        val liquidMinor = accounts
            .filter { !it.isArchived && it.type in liquidAccountTypes }
            .filter { it.availableBalance.currencyCode.equals(baseCurrency, ignoreCase = true) }
            .sumOf { maxOf(0L, it.availableBalance.amountMinor) }

        return Money(liquidMinor, baseCurrency)
    }

    /**
     * Aggregates and normalizes all financial events for the specified date window.
     */
    fun buildCalendarEvents(
        startDate: LocalDate,
        endDate: LocalDate,
        recurringRules: List<RecurringTransaction>,
        persistedOccurrences: List<RecurringOccurrence>,
        debts: List<Debt>,
        goals: List<FinancialGoal>,
        recordedTransactions: List<Transaction>,
        accounts: List<Account>,
        categories: List<Category>,
        baseCurrency: String,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<CalendarEvent> {
        val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        val accountMap = accounts.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }
        val persistedMap = persistedOccurrences.associateBy { it.id }
        val recordedTxMap = recordedTransactions.associateBy { it.id }

        val events = mutableListOf<CalendarEvent>()
        val processedOccurrenceIds = mutableSetOf<String>()

        // 1. Project recurring transactions (Income, Bills, Subscriptions, Transfers)
        val activeRules = recurringRules.filter { it.isActive && !it.isCancelled }
        val windowDays = maxOf(1, ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1)

        for (rule in activeRules) {
            val dueDates = RecurringDateEngine.projectOccurrences(
                rule = rule,
                startFromMillis = startMillis,
                windowDays = windowDays + 30, // Include potential overdue buffer
                zoneId = zoneId
            )

            for (dueDate in dueDates) {
                if (dueDate < startMillis || dueDate > endMillis) continue

                val occId = "${rule.id}_$dueDate"
                processedOccurrenceIds.add(occId)
                val persisted = persistedMap[occId]

                val status = when {
                    persisted?.status == OccurrenceStatus.PAID -> CalendarEventStatus.COMPLETED
                    persisted?.status == OccurrenceStatus.SKIPPED -> CalendarEventStatus.SKIPPED
                    else -> {
                        val dueDay = Instant.ofEpochMilli(dueDate).atZone(zoneId).toLocalDate()
                        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
                        if (dueDay.isBefore(today)) CalendarEventStatus.OVERDUE else CalendarEventStatus.EXPECTED
                    }
                }

                val eventType = when (rule.type) {
                    TransactionType.INCOME -> CalendarEventType.INCOME
                    TransactionType.TRANSFER -> CalendarEventType.RECURRING_TRANSFER
                    TransactionType.EXPENSE, TransactionType.REFUND -> {
                        if (rule.isSubscription) CalendarEventType.SUBSCRIPTION else CalendarEventType.BILL
                    }
                }

                val effectiveAmount = persisted?.amount ?: rule.amount
                val normalizedAmount = if (effectiveAmount.currencyCode.equals(baseCurrency, ignoreCase = true)) {
                    effectiveAmount
                } else {
                    Money(effectiveAmount.amountMinor, baseCurrency)
                }

                val sourceAcc = accountMap[rule.accountId]
                val destAcc = rule.destinationAccountId?.let { accountMap[it] }
                val cat = categoryMap[rule.categoryId]

                events.add(
                    CalendarEvent(
                        id = "occ_$occId",
                        title = rule.title,
                        amount = normalizedAmount,
                        dateMillis = dueDate,
                        type = eventType,
                        status = status,
                        sourceAccountId = rule.accountId,
                        sourceAccountName = sourceAcc?.name,
                        destinationAccountId = rule.destinationAccountId,
                        destinationAccountName = destAcc?.name,
                        categoryId = rule.categoryId,
                        categoryName = cat?.name,
                        categoryIcon = cat?.icon,
                        categoryColorHex = cat?.colorHex,
                        underlyingRecurringRuleId = rule.id,
                        underlyingOccurrenceId = occId,
                        underlyingTransactionId = persisted?.transactionId,
                        notes = rule.notes,
                        hasReminder = rule.reminderDaysBefore > 0,
                        reminderDaysBefore = rule.reminderDaysBefore,
                        isVariableAmount = rule.isVariableAmount
                    )
                )
            }
        }

        // 2. Debts and Loan payments
        for (debt in debts) {
            if (debt.remainingBalance.isZero || debt.minimumPayment.isZero) continue

            var nextPayment = debt.nextPaymentDate
            val dueDay = Instant.ofEpochMilli(nextPayment).atZone(zoneId).toLocalDate()
            var currentProjDate = dueDay

            // Project debt monthly payment dates within the window
            var iterations = 0
            while (currentProjDate <= endDate && iterations < 12) {
                iterations++
                val projMillis = currentProjDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

                if (projMillis in startMillis..endMillis) {
                    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
                    val status = if (currentProjDate.isBefore(today)) CalendarEventStatus.OVERDUE else CalendarEventStatus.EXPECTED

                    val sourceAcc = debt.linkedAccountId?.let { accountMap[it] }
                    val normalizedAmount = if (debt.minimumPayment.currencyCode.equals(baseCurrency, ignoreCase = true)) {
                        debt.minimumPayment
                    } else {
                        Money(debt.minimumPayment.amountMinor, baseCurrency)
                    }

                    events.add(
                        CalendarEvent(
                            id = "debt_${debt.id}_$projMillis",
                            title = "${debt.name} (${debt.type.displayName})",
                            amount = normalizedAmount,
                            dateMillis = projMillis,
                            type = CalendarEventType.LOAN_PAYMENT,
                            status = status,
                            sourceAccountId = debt.linkedAccountId,
                            sourceAccountName = sourceAcc?.name,
                            underlyingDebtId = debt.id,
                            notes = debt.notes,
                            hasReminder = true,
                            reminderDaysBefore = 3
                        )
                    )
                }

                currentProjDate = currentProjDate.plusMonths(1)
            }
        }

        // 3. Financial Goals (Target dates and suggested monthly contributions)
        val activeGoals = goals.filter { !it.isCompleted && it.remainingAmount.amountMinor > 0L }
        for (goal in activeGoals) {
            val targetDay = Instant.ofEpochMilli(goal.targetDate).atZone(zoneId).toLocalDate()

            // A. Target milestone event if in range
            if (goal.targetDate in startMillis..endMillis) {
                val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
                val status = if (targetDay.isBefore(today)) CalendarEventStatus.OVERDUE else CalendarEventStatus.EXPECTED

                events.add(
                    CalendarEvent(
                        id = "goal_target_${goal.id}_${goal.targetDate}",
                        title = "🎯 Target: ${goal.title}",
                        amount = goal.remainingAmount,
                        dateMillis = goal.targetDate,
                        type = CalendarEventType.GOAL_CONTRIBUTION,
                        status = status,
                        underlyingGoalId = goal.id,
                        notes = "Target completion date for ${goal.title}"
                    )
                )
            }

            // B. Suggested monthly contribution if not already reached target
            val suggestedMonthly = goal.calculateSuggestedMonthlyContribution(nowMillis)
            if (suggestedMonthly.amountMinor > 0L) {
                // Place contribution on the anchor day of month or 1st/15th of the month
                val contributionDay = targetDay.dayOfMonth.coerceIn(1, 28)
                var monthCursor = startDate.withDayOfMonth(1)
                val endMonth = endDate.withDayOfMonth(1)

                while (!monthCursor.isAfter(endMonth)) {
                    val contributionDate = try {
                        monthCursor.withDayOfMonth(minOf(contributionDay, monthCursor.lengthOfMonth()))
                    } catch (_: Exception) {
                        monthCursor.withDayOfMonth(monthCursor.lengthOfMonth())
                    }

                    val contribMillis = contributionDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                    if (contribMillis in startMillis..endMillis) {
                        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
                        val status = if (contributionDate.isBefore(today)) CalendarEventStatus.OVERDUE else CalendarEventStatus.EXPECTED

                        events.add(
                            CalendarEvent(
                                id = "goal_contrib_${goal.id}_$contribMillis",
                                title = "Savings Target: ${goal.title}",
                                amount = suggestedMonthly,
                                dateMillis = contribMillis,
                                type = CalendarEventType.GOAL_CONTRIBUTION,
                                status = status,
                                underlyingGoalId = goal.id,
                                notes = "Suggested monthly savings to reach ${goal.targetAmount.formatted()} on time"
                            )
                        )
                    }
                    monthCursor = monthCursor.plusMonths(1)
                }
            }
        }

        // 4. Recorded standalone transactions (completed events)
        for (tx in recordedTransactions) {
            if (tx.timestamp !in startMillis..endMillis) continue

            // If transaction was generated by a recurring occurrence we already tracked, avoid duplicate
            val isLinkedToOccurrence = tx.recurringRuleId != null && events.any {
                it.underlyingRecurringRuleId == tx.recurringRuleId && it.status == CalendarEventStatus.COMPLETED
            }
            if (isLinkedToOccurrence) continue

            val eventType = when (tx.type) {
                TransactionType.INCOME -> CalendarEventType.INCOME
                TransactionType.EXPENSE -> CalendarEventType.TRANSACTION
                TransactionType.TRANSFER -> CalendarEventType.RECURRING_TRANSFER
                TransactionType.REFUND -> CalendarEventType.INCOME
            }

            val sourceAcc = accountMap[tx.sourceAccountId]
            val destAcc = tx.destinationAccountId?.let { accountMap[it] }
            val cat = categoryMap[tx.categoryId]

            val normalizedAmount = if (tx.amount.currencyCode.equals(baseCurrency, ignoreCase = true)) {
                tx.amount
            } else {
                Money(tx.amount.amountMinor, baseCurrency)
            }

            events.add(
                CalendarEvent(
                    id = "tx_${tx.id}",
                    title = tx.description.ifBlank { tx.merchant ?: cat?.name ?: "Transaction" },
                    amount = normalizedAmount,
                    dateMillis = tx.timestamp,
                    type = eventType,
                    status = CalendarEventStatus.COMPLETED,
                    sourceAccountId = tx.sourceAccountId,
                    sourceAccountName = sourceAcc?.name,
                    destinationAccountId = tx.destinationAccountId,
                    destinationAccountName = destAcc?.name,
                    categoryId = tx.categoryId,
                    categoryName = cat?.name,
                    categoryIcon = cat?.icon,
                    categoryColorHex = cat?.colorHex,
                    underlyingTransactionId = tx.id,
                    notes = tx.notes
                )
            )
        }

        return events.sortedBy { it.dateMillis }
    }

    /**
     * Calculates deterministic cash flow timeline and summary across a given period.
     * Guaranteed deterministic: strictly uses starting liquid funds plus scheduled events.
     */
    fun calculateCashFlowProjection(
        startDate: LocalDate,
        endDate: LocalDate,
        startingLiquidBalance: Money,
        events: List<CalendarEvent>,
        baseCurrency: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): CashFlowPeriodSummary {
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val dailyProjections = ArrayList<DayCashFlow>(totalDays)

        var runningBalanceMinor = startingLiquidBalance.amountMinor
        var lowestBalanceMinor = runningBalanceMinor
        var lowestBalanceDate: LocalDate? = startDate

        var totalInflowMinor = 0L
        var totalOutflowMinor = 0L

        var incomeCount = 0
        var billCount = 0
        var subscriptionCount = 0
        var loanPaymentCount = 0
        var transferCount = 0
        var goalContributionCount = 0
        var totalCompletedCount = 0
        var totalExpectedCount = 0
        var totalOverdueCount = 0

        // Group events by LocalDate
        val eventsByDate = events.groupBy {
            Instant.ofEpochMilli(it.dateMillis).atZone(zoneId).toLocalDate()
        }

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dayEvents = eventsByDate[currentDate] ?: emptyList()
            val dayStartMillis = currentDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

            var dayInflowMinor = 0L
            var dayOutflowMinor = 0L

            for (event in dayEvents) {
                // Record event counts
                when (event.type) {
                    CalendarEventType.INCOME -> incomeCount++
                    CalendarEventType.BILL -> billCount++
                    CalendarEventType.SUBSCRIPTION -> subscriptionCount++
                    CalendarEventType.LOAN_PAYMENT -> loanPaymentCount++
                    CalendarEventType.RECURRING_TRANSFER -> transferCount++
                    CalendarEventType.GOAL_CONTRIBUTION -> goalContributionCount++
                    CalendarEventType.TRANSACTION -> {
                        if (event.isInflow) incomeCount++ else billCount++
                    }
                }

                when (event.status) {
                    CalendarEventStatus.COMPLETED -> totalCompletedCount++
                    CalendarEventStatus.EXPECTED -> totalExpectedCount++
                    CalendarEventStatus.OVERDUE -> totalOverdueCount++
                    CalendarEventStatus.SKIPPED -> { /* skipped events do not affect flow */ }
                }

                // If skipped, do not include in cash flow calculations
                if (event.status == CalendarEventStatus.SKIPPED) continue

                val amt = event.amount.amountMinor
                if (event.isInflow) {
                    dayInflowMinor += amt
                } else if (event.isOutflow) {
                    dayOutflowMinor += amt
                }
            }

            totalInflowMinor += dayInflowMinor
            totalOutflowMinor += dayOutflowMinor

            val dayNetMinor = dayInflowMinor - dayOutflowMinor
            runningBalanceMinor += dayNetMinor

            if (runningBalanceMinor < lowestBalanceMinor) {
                lowestBalanceMinor = runningBalanceMinor
                lowestBalanceDate = currentDate
            }

            dailyProjections.add(
                DayCashFlow(
                    date = currentDate,
                    dateMillis = dayStartMillis,
                    events = dayEvents,
                    totalInflow = Money(dayInflowMinor, baseCurrency),
                    totalOutflow = Money(dayOutflowMinor, baseCurrency),
                    netCashFlow = Money(dayNetMinor, baseCurrency),
                    projectedEndBalance = Money(runningBalanceMinor, baseCurrency)
                )
            )

            currentDate = currentDate.plusDays(1)
        }

        val netCashFlowMinor = totalInflowMinor - totalOutflowMinor

        return CashFlowPeriodSummary(
            startDate = startDate,
            endDate = endDate,
            startingBalance = startingLiquidBalance,
            projectedEndingBalance = Money(runningBalanceMinor, baseCurrency),
            lowestProjectedBalance = Money(lowestBalanceMinor, baseCurrency),
            lowestBalanceDate = lowestBalanceDate,
            totalInflow = Money(totalInflowMinor, baseCurrency),
            totalOutflow = Money(totalOutflowMinor, baseCurrency),
            netCashFlow = Money(netCashFlowMinor, baseCurrency),
            incomeCount = incomeCount,
            billCount = billCount,
            subscriptionCount = subscriptionCount,
            loanPaymentCount = loanPaymentCount,
            transferCount = transferCount,
            goalContributionCount = goalContributionCount,
            totalCompletedCount = totalCompletedCount,
            totalExpectedCount = totalExpectedCount,
            totalOverdueCount = totalOverdueCount,
            dailyProjections = dailyProjections,
            allEvents = events,
            isDeterministic = true
        )
    }
}

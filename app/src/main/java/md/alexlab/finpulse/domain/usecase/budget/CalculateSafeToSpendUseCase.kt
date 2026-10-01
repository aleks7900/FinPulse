package md.alexlab.finpulse.domain.usecase.budget

import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.BudgetStatus
import md.alexlab.finpulse.domain.model.FinancialGoal
import md.alexlab.finpulse.domain.model.OccurrenceStatus
import md.alexlab.finpulse.domain.model.RecurringOccurrence
import md.alexlab.finpulse.domain.model.SafeToSpendBreakdown
import md.alexlab.finpulse.domain.model.TransactionType
import java.time.LocalDate

class CalculateSafeToSpendUseCase {

    operator fun invoke(
        accounts: List<Account>,
        upcomingOccurrences: List<RecurringOccurrence>,
        goals: List<FinancialGoal>,
        overallBudgetStatus: BudgetStatus? = null,
        baseCurrency: String = "USD",
        currentDate: LocalDate = LocalDate.now(),
        periodEndMillis: Long = System.currentTimeMillis()
    ): SafeToSpendBreakdown {
        // 1. Liquid Accounts: Cash, Bank, Savings, Digital Wallet
        val liquidAccountTypes = setOf(
            AccountType.CASH,
            AccountType.BANK,
            AccountType.SAVINGS,
            AccountType.WALLET
        )
        val activeLiquidAccounts = accounts.filter {
            !it.isArchived && it.type in liquidAccountTypes
        }

        var multiCurrencyDetected = false

        val matchingLiquidAccounts = activeLiquidAccounts.filter {
            val matches = it.availableBalance.currencyCode.equals(baseCurrency, ignoreCase = true)
            if (!matches) multiCurrencyDetected = true
            matches
        }

        val totalLiquidMinor = matchingLiquidAccounts.sumOf {
            maxOf(0L, it.availableBalance.amountMinor)
        }
        val totalLiquidMoney = Money(totalLiquidMinor, baseCurrency)

        // 2. Upcoming Obligations (scheduled bills due by periodEndMillis)
        val matchingOccurrences = upcomingOccurrences.filter { occ ->
            occ.type == TransactionType.EXPENSE &&
            (occ.status == OccurrenceStatus.EXPECTED || occ.status == OccurrenceStatus.OVERDUE) &&
            occ.dueDate <= periodEndMillis
        }

        val validCurrencyOccurrences = matchingOccurrences.filter { occ ->
            val matches = occ.amount.currencyCode.equals(baseCurrency, ignoreCase = true)
            if (!matches) multiCurrencyDetected = true
            matches
        }

        val upcomingObligationsMinor = validCurrencyOccurrences.sumOf { it.amount.amountMinor }
        val upcomingObligationsMoney = Money(upcomingObligationsMinor, baseCurrency)

        // 3. Configured Savings Targets (active financial goals)
        val activeGoals = goals.filter { !it.isCompleted }
        var savingsTargetsMinor = 0L
        val nowMillis = System.currentTimeMillis()

        activeGoals.forEach { goal ->
            val monthlySuggestion = goal.calculateSuggestedMonthlyContribution(nowMillis)
            if (monthlySuggestion.currencyCode.equals(baseCurrency, ignoreCase = true)) {
                savingsTargetsMinor += monthlySuggestion.amountMinor
            } else {
                multiCurrencyDetected = true
            }
        }
        val savingsTargetsMoney = Money(savingsTargetsMinor, baseCurrency)

        // 4. Available Discretionary Pool
        val rawDiscretionaryMinor = totalLiquidMinor - upcomingObligationsMinor - savingsTargetsMinor
        val discretionaryMinor = maxOf(0L, rawDiscretionaryMinor)

        // 5. Constrained by overall budget remaining (if configured)
        val overallConstraintMoney = overallBudgetStatus?.let {
            if (it.remainingAmount.currencyCode.equals(baseCurrency, ignoreCase = true)) {
                it.remainingAmount
            } else null
        }

        val safeToSpendMinor = if (overallConstraintMoney != null) {
            minOf(discretionaryMinor, maxOf(0L, overallConstraintMoney.amountMinor))
        } else {
            discretionaryMinor
        }
        val safeToSpendMoney = Money(safeToSpendMinor, baseCurrency)

        // 6. Days Remaining and Pacing
        val totalDaysInMonth = currentDate.lengthOfMonth()
        val currentDay = currentDate.dayOfMonth.coerceIn(1, totalDaysInMonth)
        val daysRemaining = maxOf(1, totalDaysInMonth - currentDay + 1)

        val dailySafeToSpendMinor = safeToSpendMinor / daysRemaining
        val dailySafeToSpend = Money(dailySafeToSpendMinor, baseCurrency)

        val weeklySafeToSpendMinor = dailySafeToSpendMinor * minOf(7, daysRemaining)
        val weeklySafeToSpend = Money(weeklySafeToSpendMinor, baseCurrency)

        // 7. Explanatory Assumptions
        val assumptions = mutableListOf<String>()
        assumptions.add("Liquid funds aggregated from ${matchingLiquidAccounts.size} cash, bank, savings, and wallet accounts (${totalLiquidMoney.formatted()}).")
        if (validCurrencyOccurrences.isNotEmpty()) {
            assumptions.add("Deducts ${validCurrencyOccurrences.size} upcoming bills due before period end (${upcomingObligationsMoney.formatted()}).")
        } else {
            assumptions.add("No upcoming scheduled bills due within the remaining period.")
        }
        if (activeGoals.isNotEmpty()) {
            assumptions.add("Reserves suggested monthly contributions for ${activeGoals.size} active savings goals (${savingsTargetsMoney.formatted()}).")
        } else {
            assumptions.add("No active savings targets deducting from discretionary funds.")
        }
        if (overallConstraintMoney != null) {
            assumptions.add("Capped by your Monthly Overall Budget remaining limit (${overallConstraintMoney.formatted()}).")
        }
        assumptions.add("Excludes transfers and credits refunds back to available category budgets.")
        assumptions.add("Calculated across $daysRemaining remaining days in the current budget cycle.")
        if (multiCurrencyDetected) {
            assumptions.add("Multi-currency notice: Non-$baseCurrency accounts or bills are excluded from this calculation for mathematical safety.")
        }

        return SafeToSpendBreakdown(
            totalLiquidFunds = totalLiquidMoney,
            upcomingObligations = upcomingObligationsMoney,
            savingsTargets = savingsTargetsMoney,
            overallBudgetConstraint = overallConstraintMoney,
            discretionarySafeToSpend = safeToSpendMoney,
            dailySafeToSpend = dailySafeToSpend,
            weeklySafeToSpend = weeklySafeToSpend,
            daysRemaining = daysRemaining,
            assumptions = assumptions,
            liquidAccountsCount = matchingLiquidAccounts.size,
            upcomingBillsCount = validCurrencyOccurrences.size,
            activeGoalsCount = activeGoals.size,
            multiCurrencyWarning = multiCurrencyDetected
        )
    }
}

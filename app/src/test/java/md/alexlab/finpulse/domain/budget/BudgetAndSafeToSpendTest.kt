package md.alexlab.finpulse.domain.budget

import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.Budget
import md.alexlab.finpulse.domain.model.BudgetAlertLevel
import md.alexlab.finpulse.domain.model.BudgetPeriod
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.CategoryType
import md.alexlab.finpulse.domain.model.FinancialGoal
import md.alexlab.finpulse.domain.model.OccurrenceStatus
import md.alexlab.finpulse.domain.model.PacingStatus
import md.alexlab.finpulse.domain.model.RecurringOccurrence
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.usecase.EvaluateBudgetStatusUseCase
import md.alexlab.finpulse.domain.usecase.budget.CalculateSafeToSpendUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetAndSafeToSpendTest {

    private val evaluateBudgetStatusUseCase = EvaluateBudgetStatusUseCase()
    private val calculateSafeToSpendUseCase = CalculateSafeToSpendUseCase()

    private val sept15Date = LocalDate.of(2026, 9, 15)
    private val startOfMonthMillis = 1000L
    private val endOfMonthMillis = 5000000000L

    @Test
    fun testZeroIncomeAndLiquidDeficit() {
        // Zero or negative liquid balance with upcoming obligations
        val accounts = listOf(
            Account("a1", "Empty Checking", AccountType.BANK, Money.zero("USD"), Money.zero("USD")),
            Account("a2", "Empty Cash", AccountType.CASH, Money.zero("USD"), Money.zero("USD"))
        )

        val upcomingOccurrences = listOf(
            RecurringOccurrence(
                id = "occ_1",
                ruleId = "rule_1",
                ruleTitle = "Electric Bill",
                amount = Money(15000L, "USD"),
                type = TransactionType.EXPENSE,
                accountId = "a1",
                categoryId = "cat_util",
                dueDate = 2000L,
                status = OccurrenceStatus.EXPECTED
            )
        )

        val goals = listOf(
            FinancialGoal(
                id = "g1",
                title = "Emergency Fund",
                targetAmount = Money(100000L, "USD"),
                currentAmount = Money(20000L, "USD"),
                targetDate = System.currentTimeMillis() + 86400000L * 60
            )
        )

        val breakdown = calculateSafeToSpendUseCase(
            accounts = accounts,
            upcomingOccurrences = upcomingOccurrences,
            goals = goals,
            baseCurrency = "USD",
            currentDate = sept15Date,
            periodEndMillis = endOfMonthMillis
        )

        // Discretionary pool must never be negative
        assertEquals(0L, breakdown.discretionarySafeToSpend.amountMinor)
        assertEquals(0L, breakdown.dailySafeToSpend.amountMinor)
        assertEquals(0L, breakdown.weeklySafeToSpend.amountMinor)
        assertEquals(0L, breakdown.totalLiquidFunds.amountMinor)
        assertEquals(15000L, breakdown.upcomingObligations.amountMinor)
    }

    @Test
    fun testOverspentBudget() {
        val catDining = Category("cat_dining", "Dining", CategoryType.EXPENSE)
        val budget = Budget(
            id = "b_dining",
            categoryId = "cat_dining",
            name = "Dining Out",
            limitAmount = Money(20000L, "USD"), // $200
            periodType = BudgetPeriod.MONTHLY,
            startDate = startOfMonthMillis,
            endDate = endOfMonthMillis,
            alertThresholdPercent = 85
        )

        // Expense $280.00
        val transactions = listOf(
            Transaction(
                id = "t1",
                amount = Money(28000L, "USD"),
                type = TransactionType.EXPENSE,
                sourceAccountId = "a1",
                categoryId = "cat_dining",
                timestamp = 2000L
            )
        )

        val statuses = evaluateBudgetStatusUseCase(
            budgets = listOf(budget),
            categories = listOf(catDining),
            transactions = transactions,
            currentDate = sept15Date
        )

        assertEquals(1, statuses.size)
        val status = statuses.first()
        assertEquals(28000L, status.spentAmount.amountMinor)
        assertEquals(0L, status.remainingAmount.amountMinor)
        assertTrue(status.isExceeded)
        assertEquals(BudgetAlertLevel.EXCEEDED, status.alertLevel)
        assertEquals(PacingStatus.AHEAD_OF_PACE, status.pacingMetrics?.pacingStatus)
        assertEquals(1.40, status.percentageConsumed, 0.001)
    }

    @Test
    fun testFutureBillsReservation() {
        val accounts = listOf(
            Account("a1", "Checking", AccountType.BANK, Money(100000L, "USD"), Money(100000L, "USD")), // $1000
            Account("a2", "Savings", AccountType.SAVINGS, Money(50000L, "USD"), Money(50000L, "USD"))  // $500
        )

        val currentPeriodEnd = 10000000L

        val occurrences = listOf(
            // Due within period and EXPECTED -> should be reserved
            RecurringOccurrence(
                id = "occ_due_soon",
                ruleId = "r1",
                ruleTitle = "Rent",
                amount = Money(35000L, "USD"), // $350
                type = TransactionType.EXPENSE,
                accountId = "a1",
                categoryId = "cat_rent",
                dueDate = 5000000L,
                status = OccurrenceStatus.EXPECTED
            ),
            // Already PAID -> should NOT be reserved again
            RecurringOccurrence(
                id = "occ_paid",
                ruleId = "r2",
                ruleTitle = "Water Bill",
                amount = Money(20000L, "USD"), // $200
                type = TransactionType.EXPENSE,
                accountId = "a1",
                categoryId = "cat_water",
                dueDate = 3000000L,
                status = OccurrenceStatus.PAID
            ),
            // Due next month (past currentPeriodEnd) -> should NOT be reserved now
            RecurringOccurrence(
                id = "occ_next_month",
                ruleId = "r3",
                ruleTitle = "Annual Subscription",
                amount = Money(50000L, "USD"), // $500
                type = TransactionType.EXPENSE,
                accountId = "a1",
                categoryId = "cat_sub",
                dueDate = currentPeriodEnd + 500000L,
                status = OccurrenceStatus.EXPECTED
            )
        )

        val breakdown = calculateSafeToSpendUseCase(
            accounts = accounts,
            upcomingOccurrences = occurrences,
            goals = emptyList(),
            baseCurrency = "USD",
            currentDate = sept15Date,
            periodEndMillis = currentPeriodEnd
        )

        // Total liquid: $1500
        // Upcoming bills reserved: $350 (only occ_due_soon)
        // Discretionary: 1500 - 350 = $1150
        assertEquals(150000L, breakdown.totalLiquidFunds.amountMinor)
        assertEquals(35000L, breakdown.upcomingObligations.amountMinor)
        assertEquals(1, breakdown.upcomingBillsCount)
        assertEquals(115000L, breakdown.discretionarySafeToSpend.amountMinor)
    }

    @Test
    fun testExpenseRefundOffsetsSpending() {
        val catElectronics = Category("cat_elec", "Electronics", CategoryType.EXPENSE)
        val budget = Budget(
            id = "b_elec",
            categoryId = "cat_elec",
            name = "Electronics Budget",
            limitAmount = Money(50000L, "USD"), // $500
            periodType = BudgetPeriod.MONTHLY,
            startDate = startOfMonthMillis,
            endDate = endOfMonthMillis
        )

        val transactions = listOf(
            // Expense $200
            Transaction(
                id = "t_exp",
                amount = Money(20000L, "USD"),
                type = TransactionType.EXPENSE,
                sourceAccountId = "a1",
                categoryId = "cat_elec",
                timestamp = 2000L
            ),
            // Refund $50 (e.g. returned mouse)
            Transaction(
                id = "t_ref",
                amount = Money(5000L, "USD"),
                type = TransactionType.REFUND,
                sourceAccountId = "a1",
                categoryId = "cat_elec",
                timestamp = 3000L
            )
        )

        val statuses = evaluateBudgetStatusUseCase(
            budgets = listOf(budget),
            categories = listOf(catElectronics),
            transactions = transactions,
            currentDate = sept15Date
        )

        val status = statuses.first()
        // Net spent should be $200 - $50 = $150
        assertEquals(15000L, status.spentAmount.amountMinor)
        assertEquals(5000L, status.refundsAmount.amountMinor)
        assertEquals(35000L, status.remainingAmount.amountMinor)
        assertFalse(status.isExceeded)
    }

    @Test
    fun testRefundExceedingExpenseClampsToZero() {
        val catShopping = Category("cat_shop", "Shopping", CategoryType.EXPENSE)
        val budget = Budget(
            id = "b_shop",
            categoryId = "cat_shop",
            name = "Shopping",
            limitAmount = Money(20000L, "USD"),
            periodType = BudgetPeriod.MONTHLY,
            startDate = startOfMonthMillis,
            endDate = endOfMonthMillis
        )

        val transactions = listOf(
            Transaction("t1", Money(4000L, "USD"), TransactionType.EXPENSE, "a1", null, "cat_shop", timestamp = 2000L),
            Transaction("t2", Money(7000L, "USD"), TransactionType.REFUND, "a1", null, "cat_shop", timestamp = 3000L)
        )

        val status = evaluateBudgetStatusUseCase(
            budgets = listOf(budget),
            categories = listOf(catShopping),
            transactions = transactions,
            currentDate = sept15Date
        ).first()

        // Spent cannot be negative
        assertEquals(0L, status.spentAmount.amountMinor)
        assertEquals(20000L, status.remainingAmount.amountMinor)
        assertEquals(7000L, status.refundsAmount.amountMinor)
    }

    @Test
    fun testTransfersExcludedFromSpending() {
        val catGroceries = Category("cat_groc", "Groceries", CategoryType.EXPENSE)
        val budget = Budget(
            id = "b_groc",
            categoryId = "cat_groc",
            name = "Groceries",
            limitAmount = Money(30000L, "USD"),
            periodType = BudgetPeriod.MONTHLY,
            startDate = startOfMonthMillis,
            endDate = endOfMonthMillis
        )

        val transactions = listOf(
            // Legitimate expense $80
            Transaction("t_exp", Money(8000L, "USD"), TransactionType.EXPENSE, "a1", null, "cat_groc", timestamp = 2000L),
            // Account transfer $1000 (even if mistakenly tagged with cat_groc)
            Transaction("t_trans", Money(100000L, "USD"), TransactionType.TRANSFER, "a1", "a2", "cat_groc", timestamp = 3000L),
            // Explicitly excluded expense $50
            Transaction("t_excl", Money(5000L, "USD"), TransactionType.EXPENSE, "a1", null, "cat_groc", timestamp = 4000L, isExcludedFromBudget = true)
        )

        val status = evaluateBudgetStatusUseCase(
            budgets = listOf(budget),
            categories = listOf(catGroceries),
            transactions = transactions,
            currentDate = sept15Date
        ).first()

        // Only t_exp ($80) should count
        assertEquals(8000L, status.spentAmount.amountMinor)
        assertEquals(22000L, status.remainingAmount.amountMinor)
    }

    @Test
    fun testRolloverSurplusAndDeficit() {
        val catTransport = Category("cat_trans", "Transport", CategoryType.EXPENSE)

        // Subtest A: Rollover enabled with SURPLUS (+$50 from previous month)
        val budgetWithSurplus = Budget(
            id = "b_surplus",
            categoryId = "cat_trans",
            name = "Transport",
            limitAmount = Money(40000L, "USD"), // $400
            periodType = BudgetPeriod.MONTHLY,
            startDate = startOfMonthMillis,
            endDate = endOfMonthMillis,
            isRolloverEnabled = true,
            rolloverAmountMinor = 5000L // +$50
        )
        // Effective limit should be $450
        assertEquals(45000L, budgetWithSurplus.effectiveLimit.amountMinor)

        val statusSurplus = evaluateBudgetStatusUseCase(
            budgets = listOf(budgetWithSurplus),
            categories = listOf(catTransport),
            transactions = listOf(Transaction("t1", Money(30000L, "USD"), TransactionType.EXPENSE, "a1", null, "cat_trans", timestamp = 2000L)),
            currentDate = sept15Date
        ).first()
        // Spent $300, remaining = $450 - $300 = $150
        assertEquals(15000L, statusSurplus.remainingAmount.amountMinor)

        // Subtest B: Rollover enabled with DEFICIT (-$100 from previous month)
        val budgetWithDeficit = Budget(
            id = "b_deficit",
            categoryId = "cat_trans",
            name = "Transport",
            limitAmount = Money(40000L, "USD"), // $400
            periodType = BudgetPeriod.MONTHLY,
            startDate = startOfMonthMillis,
            endDate = endOfMonthMillis,
            isRolloverEnabled = true,
            rolloverAmountMinor = -10000L // -$100
        )
        // Effective limit should be $300
        assertEquals(30000L, budgetWithDeficit.effectiveLimit.amountMinor)

        val statusDeficit = evaluateBudgetStatusUseCase(
            budgets = listOf(budgetWithDeficit),
            categories = listOf(catTransport),
            transactions = listOf(Transaction("t2", Money(25000L, "USD"), TransactionType.EXPENSE, "a1", null, "cat_trans", timestamp = 2000L)),
            currentDate = sept15Date
        ).first()
        // Spent $250, remaining = $300 - $250 = $50
        assertEquals(5000L, statusDeficit.remainingAmount.amountMinor)

        // Subtest C: Rollover DISABLED (rolloverAmountMinor ignored)
        val budgetDisabled = budgetWithSurplus.copy(isRolloverEnabled = false)
        assertEquals(40000L, budgetDisabled.effectiveLimit.amountMinor)
    }

    @Test
    fun testOverallMonthlyBudgetAndDiscretionaryCap() {
        val catFood = Category("c_food", "Food", CategoryType.EXPENSE)
        val catBills = Category("c_bills", "Bills", CategoryType.EXPENSE)

        // Overall Monthly Budget of $800.00
        val overallBudget = Budget(
            id = "b_overall",
            categoryId = "overall",
            name = "Overall Monthly Budget",
            limitAmount = Money(80000L, "USD"),
            periodType = BudgetPeriod.MONTHLY,
            startDate = startOfMonthMillis,
            endDate = endOfMonthMillis,
            isOverall = true
        )

        val transactions = listOf(
            Transaction("t1", Money(30000L, "USD"), TransactionType.EXPENSE, "a1", null, "c_food", timestamp = 2000L),
            Transaction("t2", Money(20000L, "USD"), TransactionType.EXPENSE, "a1", null, "c_bills", timestamp = 3000L)
        )

        val statuses = evaluateBudgetStatusUseCase(
            budgets = listOf(overallBudget),
            categories = listOf(catFood, catBills),
            transactions = transactions,
            currentDate = sept15Date
        )

        val overallStatus = statuses.first()
        assertTrue(overallStatus.isOverall)
        // Total spent across all categories = $300 + $200 = $500
        assertEquals(50000L, overallStatus.spentAmount.amountMinor)
        assertEquals(30000L, overallStatus.remainingAmount.amountMinor) // $300 left in overall budget

        // Liquid accounts = $1500
        val accounts = listOf(
            Account("a1", "Checking", AccountType.BANK, Money(150000L, "USD"), Money(150000L, "USD"))
        )

        // Safe to spend calculation:
        // Discretionary pool would be $1500, but overall budget remaining is $300.
        // Safe to spend MUST be capped at $300!
        val breakdown = calculateSafeToSpendUseCase(
            accounts = accounts,
            upcomingOccurrences = emptyList(),
            goals = emptyList(),
            overallBudgetStatus = overallStatus,
            baseCurrency = "USD",
            currentDate = sept15Date,
            periodEndMillis = endOfMonthMillis
        )

        assertEquals(30000L, breakdown.discretionarySafeToSpend.amountMinor)
        assertNotNull(breakdown.overallBudgetConstraint)
        assertEquals(30000L, breakdown.overallBudgetConstraint?.amountMinor)
    }

    @Test
    fun testMultiCurrencyBehaviorSafeHandling() {
        val baseCurrency = "USD"

        // Mixed accounts: USD and EUR
        val accounts = listOf(
            Account("a_usd", "USD Checking", AccountType.BANK, Money(100000L, "USD"), Money(100000L, "USD")),
            Account("a_eur", "EUR Checking", AccountType.BANK, Money(80000L, "EUR"), Money(80000L, "EUR"))
        )

        // Mixed bills: USD and GBP
        val occurrences = listOf(
            RecurringOccurrence(
                id = "occ_usd",
                ruleId = "r1",
                ruleTitle = "USD Bill",
                amount = Money(20000L, "USD"),
                type = TransactionType.EXPENSE,
                accountId = "a_usd",
                categoryId = "c1",
                dueDate = 5000L,
                status = OccurrenceStatus.EXPECTED
            ),
            RecurringOccurrence(
                id = "occ_gbp",
                ruleId = "r2",
                ruleTitle = "GBP Bill",
                amount = Money(15000L, "GBP"),
                type = TransactionType.EXPENSE,
                accountId = "a_usd",
                categoryId = "c1",
                dueDate = 5000L,
                status = OccurrenceStatus.EXPECTED
            )
        )

        // Mixed goals: USD and JPY
        val goals = listOf(
            FinancialGoal(
                id = "g_usd",
                title = "USD Goal",
                targetAmount = Money(100000L, "USD"),
                currentAmount = Money(10000L, "USD"),
                targetDate = System.currentTimeMillis() + 86400000L * 90
            ),
            FinancialGoal(
                id = "g_jpy",
                title = "JPY Goal",
                targetAmount = Money(5000000L, "JPY"),
                currentAmount = Money(1000000L, "JPY"),
                targetDate = System.currentTimeMillis() + 86400000L * 90
            )
        )

        // Must calculate without throwing Currency mismatch exception
        val breakdown = calculateSafeToSpendUseCase(
            accounts = accounts,
            upcomingOccurrences = occurrences,
            goals = goals,
            baseCurrency = baseCurrency,
            currentDate = sept15Date,
            periodEndMillis = endOfMonthMillis
        )

        assertEquals("USD", breakdown.discretionarySafeToSpend.currencyCode)
        assertEquals(100000L, breakdown.totalLiquidFunds.amountMinor) // Only USD account
        assertEquals(20000L, breakdown.upcomingObligations.amountMinor) // Only USD bill
        assertTrue(breakdown.multiCurrencyWarning)

        // Multi-currency transaction in budget evaluation
        val catFood = Category("cat_food", "Food", CategoryType.EXPENSE)
        val usdBudget = Budget("b1", "cat_food", "Food", Money(30000L, "USD"), BudgetPeriod.MONTHLY, startOfMonthMillis, endOfMonthMillis)

        val transactions = listOf(
            Transaction("t_usd", Money(5000L, "USD"), TransactionType.EXPENSE, "a1", null, "cat_food", timestamp = 2000L),
            // Foreign currency transaction safely converted using exchange rate
            Transaction("t_eur", Money(5000L, "EUR"), TransactionType.EXPENSE, "a1", null, "cat_food", timestamp = 3000L)
        )

        val status = evaluateBudgetStatusUseCase(
            budgets = listOf(usdBudget),
            categories = listOf(catFood),
            transactions = transactions,
            currentDate = sept15Date
        ).first()

        // $50.00 USD + €50.00 EUR (~$54.35 USD at fallback rate) = $104.35 USD (10435 minor)
        assertEquals(10435L, status.spentAmount.amountMinor)
        assertEquals("USD", status.spentAmount.currencyCode)
    }

    @Test
    fun testDailyAndWeeklyPacingIndicators() {
        val catLiving = Category("cat_living", "Living", CategoryType.EXPENSE)
        // Month of Sept (30 days), current date = Sept 15
        // Days remaining = 30 - 15 + 1 = 16 days
        val budget = Budget(
            id = "b_living",
            categoryId = "cat_living",
            name = "Living Budget",
            limitAmount = Money(30000L, "USD"), // $300 for 30 days = $10/day target
            periodType = BudgetPeriod.MONTHLY,
            startDate = startOfMonthMillis,
            endDate = endOfMonthMillis
        )

        // Case A: Spent $150 on day 15 (Expected = $150, exactly on track)
        val txOnTrack = listOf(
            Transaction("t1", Money(15000L, "USD"), TransactionType.EXPENSE, "a1", null, "cat_living", timestamp = 2000L)
        )
        val statusOnTrack = evaluateBudgetStatusUseCase(
            budgets = listOf(budget),
            categories = listOf(catLiving),
            transactions = txOnTrack,
            currentDate = sept15Date
        ).first()

        val pacingOnTrack = statusOnTrack.pacingMetrics
        assertNotNull(pacingOnTrack)
        assertEquals(30, pacingOnTrack?.totalDays)
        assertEquals(15, pacingOnTrack?.currentDay)
        assertEquals(16, pacingOnTrack?.daysRemaining)
        assertEquals(1000L, pacingOnTrack?.targetDailySpend?.amountMinor) // $10/day
        assertEquals(1000L, pacingOnTrack?.actualDailySpend?.amountMinor) // $10/day
        assertEquals(PacingStatus.ON_TRACK, pacingOnTrack?.pacingStatus)

        // Case B: Spent $240 on day 15 (Actual = $16/day vs target $10/day -> Ahead of pace)
        val txAhead = listOf(
            Transaction("t2", Money(24000L, "USD"), TransactionType.EXPENSE, "a1", null, "cat_living", timestamp = 2000L)
        )
        val statusAhead = evaluateBudgetStatusUseCase(
            budgets = listOf(budget),
            categories = listOf(catLiving),
            transactions = txAhead,
            currentDate = sept15Date
        ).first()

        assertEquals(PacingStatus.AHEAD_OF_PACE, statusAhead.pacingMetrics?.pacingStatus)
        assertEquals(1600L, statusAhead.pacingMetrics?.actualDailySpend?.amountMinor) // $16/day
        assertEquals(600L, statusAhead.pacingMetrics?.dailyPacingDelta?.amountMinor) // +$6/day ahead of pace

        // Case C: Spent $60 on day 15 (Actual = $4/day vs target $10/day -> Under pace)
        val txUnder = listOf(
            Transaction("t3", Money(6000L, "USD"), TransactionType.EXPENSE, "a1", null, "cat_living", timestamp = 2000L)
        )
        val statusUnder = evaluateBudgetStatusUseCase(
            budgets = listOf(budget),
            categories = listOf(catLiving),
            transactions = txUnder,
            currentDate = sept15Date
        ).first()

        assertEquals(PacingStatus.UNDER_PACE, statusUnder.pacingMetrics?.pacingStatus)
        assertEquals(400L, statusUnder.pacingMetrics?.actualDailySpend?.amountMinor) // $4/day
        assertEquals(-600L, statusUnder.pacingMetrics?.dailyPacingDelta?.amountMinor) // -$6/day delta
    }
}

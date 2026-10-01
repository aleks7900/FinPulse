package md.alexlab.finpulse.domain.engine

import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.CalendarEvent
import md.alexlab.finpulse.domain.model.CalendarEventStatus
import md.alexlab.finpulse.domain.model.CalendarEventType
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.CategoryType
import md.alexlab.finpulse.domain.model.Debt
import md.alexlab.finpulse.domain.model.DebtType
import md.alexlab.finpulse.domain.model.FinancialGoal
import md.alexlab.finpulse.domain.model.OccurrenceStatus
import md.alexlab.finpulse.domain.model.PaymentFrequency
import md.alexlab.finpulse.domain.model.RecurringOccurrence
import md.alexlab.finpulse.domain.model.RecurringTransaction
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CashFlowProjectionEngineTest {

    private val zoneId = ZoneId.of("UTC")

    @Test
    fun testCalculateStartingLiquidBalance_OnlyIncludesActiveLiquidAccounts() {
        val accounts = listOf(
            Account(
                id = "acc1",
                name = "Checking",
                type = AccountType.BANK,
                balance = Money(150_000, "USD"),
                availableBalance = Money(150_000, "USD")
            ),
            Account(
                id = "acc2",
                name = "Savings",
                type = AccountType.SAVINGS,
                balance = Money(300_000, "USD"),
                availableBalance = Money(300_000, "USD")
            ),
            Account(
                id = "acc3",
                name = "Wallet",
                type = AccountType.WALLET,
                balance = Money(5_000, "USD"),
                availableBalance = Money(5_000, "USD")
            ),
            Account(
                id = "acc4",
                name = "Archived Cash",
                type = AccountType.CASH,
                balance = Money(10_000, "USD"),
                availableBalance = Money(10_000, "USD"),
                isArchived = true
            ),
            Account(
                id = "acc5",
                name = "Credit Card",
                type = AccountType.CREDIT_CARD,
                balance = Money(-20_000, "USD"),
                availableBalance = Money(50_000, "USD")
            ),
            Account(
                id = "acc6",
                name = "Investment",
                type = AccountType.INVESTMENT,
                balance = Money(500_000, "USD"),
                availableBalance = Money(500_000, "USD")
            ),
            Account(
                id = "acc7",
                name = "EUR Account",
                type = AccountType.BANK,
                balance = Money(100_000, "EUR"),
                availableBalance = Money(100_000, "EUR")
            )
        )

        val liquidBalance = CashFlowProjectionEngine.calculateStartingLiquidBalance(
            accounts = accounts,
            baseCurrency = "USD"
        )

        // Only Checking (1500) + Savings (3000) + Wallet (50) = 4550.00 USD (455_000 minor)
        assertEquals(455_000L, liquidBalance.amountMinor)
        assertEquals("USD", liquidBalance.currencyCode)
    }

    @Test
    fun testBuildCalendarEvents_AllEventTypesGeneratedAndDistinguished() {
        val startDate = LocalDate.of(2026, 10, 1)
        val endDate = LocalDate.of(2026, 10, 31)
        val nowMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        // 1. Recurring Salary (Income)
        val salaryDueDate = LocalDate.of(2026, 10, 15).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val salaryRule = RecurringTransaction(
            id = "rule_salary",
            title = "Tech Corp Salary",
            amount = Money(400_000, "USD"),
            type = TransactionType.INCOME,
            accountId = "acc1",
            categoryId = "cat_salary",
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = salaryDueDate
        )

        // 2. Recurring Rent (Bill)
        val rentDueDate = LocalDate.of(2026, 10, 2).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val rentRule = RecurringTransaction(
            id = "rule_rent",
            title = "Apartment Rent",
            amount = Money(120_000, "USD"),
            type = TransactionType.EXPENSE,
            accountId = "acc1",
            categoryId = "cat_housing",
            frequency = PaymentFrequency.MONTHLY,
            isSubscription = false,
            nextDueDate = rentDueDate
        )

        // 3. Recurring Subscription (Netflix)
        val netflixDueDate = LocalDate.of(2026, 10, 10).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val netflixRule = RecurringTransaction(
            id = "rule_netflix",
            title = "Netflix",
            amount = Money(1_999, "USD"),
            type = TransactionType.EXPENSE,
            accountId = "acc1",
            categoryId = "cat_sub",
            frequency = PaymentFrequency.MONTHLY,
            isSubscription = true,
            nextDueDate = netflixDueDate
        )

        // 4. Debt (Car Loan)
        val debtDueDate = LocalDate.of(2026, 10, 20).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val carDebt = Debt(
            id = "debt_car",
            name = "Auto Loan",
            type = DebtType.AUTO_LOAN,
            totalPrincipal = Money(2_000_000, "USD"),
            remainingBalance = Money(1_200_000, "USD"),
            interestRatePercent = 4.5,
            minimumPayment = Money(35_000, "USD"),
            nextPaymentDate = debtDueDate
        )

        // 5. Goal (Emergency Fund)
        val goalTargetDate = LocalDate.of(2026, 10, 25).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val emergencyGoal = FinancialGoal(
            id = "goal_emergency",
            title = "Emergency Fund",
            targetAmount = Money(1_000_000, "USD"),
            currentAmount = Money(800_000, "USD"),
            targetDate = goalTargetDate
        )

        // Persisted occurrence: Rent is already marked PAID
        val paidOccurrence = RecurringOccurrence(
            id = "rule_rent_$rentDueDate",
            ruleId = "rule_rent",
            ruleTitle = "Apartment Rent",
            amount = Money(120_000, "USD"),
            type = TransactionType.EXPENSE,
            accountId = "acc1",
            categoryId = "cat_housing",
            dueDate = rentDueDate,
            status = OccurrenceStatus.PAID,
            paidDate = rentDueDate,
            transactionId = "tx_rent_paid"
        )

        val events = CashFlowProjectionEngine.buildCalendarEvents(
            startDate = startDate,
            endDate = endDate,
            recurringRules = listOf(salaryRule, rentRule, netflixRule),
            persistedOccurrences = listOf(paidOccurrence),
            debts = listOf(carDebt),
            goals = listOf(emergencyGoal),
            recordedTransactions = emptyList(),
            accounts = emptyList(),
            categories = emptyList(),
            baseCurrency = "USD",
            nowMillis = nowMillis,
            zoneId = zoneId
        )

        // Verify that events contain Income, Bill, Subscription, Loan, and Goal
        assertTrue(events.any { it.type == CalendarEventType.INCOME && it.title == "Tech Corp Salary" })
        assertTrue(events.any { it.type == CalendarEventType.BILL && it.title == "Apartment Rent" })
        assertTrue(events.any { it.type == CalendarEventType.SUBSCRIPTION && it.title == "Netflix" })
        assertTrue(events.any { it.type == CalendarEventType.LOAN_PAYMENT && it.title.contains("Auto Loan") })
        assertTrue(events.any { it.type == CalendarEventType.GOAL_CONTRIBUTION && it.title.contains("Emergency Fund") })

        // Verify status distinction: Rent is COMPLETED, Netflix is EXPECTED
        val rentEvent = events.first { it.title == "Apartment Rent" }
        assertEquals(CalendarEventStatus.COMPLETED, rentEvent.status)
        assertTrue(rentEvent.isCompleted)

        val netflixEvent = events.first { it.title == "Netflix" }
        assertEquals(CalendarEventStatus.EXPECTED, netflixEvent.status)
        assertFalse(netflixEvent.isCompleted)
    }

    @Test
    fun testCalculateCashFlowProjection_DeterministicTimelineAndNetFlow() {
        val startDate = LocalDate.of(2026, 11, 1)
        val endDate = LocalDate.of(2026, 11, 10) // 10 days
        val startingBalance = Money(100_000, "USD") // $1,000.00

        // Day 2: Income +$500.00
        val event1 = CalendarEvent(
            id = "e1",
            title = "Bonus",
            amount = Money(50_000, "USD"),
            dateMillis = LocalDate.of(2026, 11, 2).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            type = CalendarEventType.INCOME,
            status = CalendarEventStatus.EXPECTED
        )

        // Day 4: Rent -$300.00
        val event2 = CalendarEvent(
            id = "e2",
            title = "Rent",
            amount = Money(30_000, "USD"),
            dateMillis = LocalDate.of(2026, 11, 4).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            type = CalendarEventType.BILL,
            status = CalendarEventStatus.EXPECTED
        )

        // Day 7: Loan Payment -$800.00 (causes temporary dip)
        val event3 = CalendarEvent(
            id = "e3",
            title = "Loan",
            amount = Money(80_000, "USD"),
            dateMillis = LocalDate.of(2026, 11, 7).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            type = CalendarEventType.LOAN_PAYMENT,
            status = CalendarEventStatus.EXPECTED
        )

        // Day 8: Skipped subscription -$15.00 (MUST NOT affect balance!)
        val event4 = CalendarEvent(
            id = "e4",
            title = "Gym (Skipped)",
            amount = Money(1_500, "USD"),
            dateMillis = LocalDate.of(2026, 11, 8).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            type = CalendarEventType.SUBSCRIPTION,
            status = CalendarEventStatus.SKIPPED
        )

        val summary = CashFlowProjectionEngine.calculateCashFlowProjection(
            startDate = startDate,
            endDate = endDate,
            startingLiquidBalance = startingBalance,
            events = listOf(event1, event2, event3, event4),
            baseCurrency = "USD",
            zoneId = zoneId
        )

        // Expected totals:
        // Inflow: $500.00 (50_000 minor)
        // Outflow: $300 (Rent) + $800 (Loan) = $1,100.00 (110_000 minor). Skipped gym excluded!
        // Net: 50_000 - 110_000 = -60_000 (-$600.00)
        // Ending Balance: 100_000 - 60_000 = 40_000 ($400.00)
        assertEquals(50_000L, summary.totalInflow.amountMinor)
        assertEquals(110_000L, summary.totalOutflow.amountMinor)
        assertEquals(-60_000L, summary.netCashFlow.amountMinor)
        assertEquals(40_000L, summary.projectedEndingBalance.amountMinor)

        // Timeline validation:
        // Day 1: 100,000
        // Day 2 (+50,000): 150,000
        // Day 3: 150,000
        // Day 4 (-30,000): 120,000
        // Day 5: 120,000
        // Day 6: 120,000
        // Day 7 (-80,000): 40,000
        // Day 8 (skipped, no change): 40,000
        // Day 9: 40,000
        // Day 10: 40,000
        assertEquals(10, summary.dailyProjections.size)

        val day2 = summary.dailyProjections[1]
        assertEquals(150_000L, day2.projectedEndBalance.amountMinor)

        val day4 = summary.dailyProjections[3]
        assertEquals(120_000L, day4.projectedEndBalance.amountMinor)

        val day7 = summary.dailyProjections[6]
        assertEquals(40_000L, day7.projectedEndBalance.amountMinor)

        // Lowest projected balance is 40_000 on Nov 7
        assertEquals(40_000L, summary.lowestProjectedBalance.amountMinor)
        assertEquals(LocalDate.of(2026, 11, 7), summary.lowestBalanceDate)

        assertTrue(summary.isDeterministic)
    }

    @Test
    fun testZeroEvents_ProjectedBalanceRemainsFlat() {
        val startDate = LocalDate.of(2026, 12, 1)
        val endDate = LocalDate.of(2026, 12, 7)
        val startingBalance = Money(250_000, "USD")

        val summary = CashFlowProjectionEngine.calculateCashFlowProjection(
            startDate = startDate,
            endDate = endDate,
            startingLiquidBalance = startingBalance,
            events = emptyList(),
            baseCurrency = "USD",
            zoneId = zoneId
        )

        assertEquals(0L, summary.totalInflow.amountMinor)
        assertEquals(0L, summary.totalOutflow.amountMinor)
        assertEquals(0L, summary.netCashFlow.amountMinor)
        assertEquals(250_000L, summary.projectedEndingBalance.amountMinor)
        assertEquals(250_000L, summary.lowestProjectedBalance.amountMinor)
        assertEquals(7, summary.dailyProjections.size)
        assertTrue(summary.dailyProjections.all { it.projectedEndBalance.amountMinor == 250_000L })
    }
}

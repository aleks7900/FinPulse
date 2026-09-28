package com.finpulse.app.domain.engine

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.BudgetPeriod
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.DigestInsightCategory
import com.finpulse.app.domain.model.DigestPeriod
import com.finpulse.app.domain.model.DigestPriority
import com.finpulse.app.domain.model.FinancialGoal
import com.finpulse.app.domain.model.PaymentFrequency
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FinancialDigestEngineTest {

    private lateinit var engine: FinancialDigestEngine
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 9, 28)

    private val foodCategory = Category(
        id = "cat-food",
        name = "Groceries & Food",
        type = CategoryType.EXPENSE,
        icon = "restaurant",
        colorHex = 0xFFFF5722
    )
    private val techCategory = Category(
        id = "cat-tech",
        name = "Electronics",
        type = CategoryType.EXPENSE,
        icon = "devices",
        colorHex = 0xFF2196F3
    )

    private val categories = listOf(foodCategory, techCategory)

    @Before
    fun setUp() {
        engine = FinancialDigestEngine()
    }

    @Test
    fun testDailyDigestSpendAndComparison() {
        // Today's millis at noon
        val todayNoon = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        // Yesterday's millis at noon
        val yesterdayNoon = today.minusDays(1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

        val transactions = listOf(
            Transaction(
                id = "tx1",
                amount = Money(5000), // $50.00
                type = TransactionType.EXPENSE,
                sourceAccountId = "acc1",
                categoryId = foodCategory.id,
                merchant = "Supermarket",
                timestamp = todayNoon
            ),
            Transaction(
                id = "tx2",
                amount = Money(3000), // $30.00
                type = TransactionType.EXPENSE,
                sourceAccountId = "acc1",
                categoryId = foodCategory.id,
                merchant = "Cafe",
                timestamp = todayNoon
            ),
            Transaction(
                id = "tx_prev",
                amount = Money(10000), // $100.00 yesterday
                type = TransactionType.EXPENSE,
                sourceAccountId = "acc1",
                categoryId = foodCategory.id,
                merchant = "Market",
                timestamp = yesterdayNoon
            )
        )

        val digest = engine.generateDigest(
            period = DigestPeriod.DAILY,
            transactions = transactions,
            categories = categories,
            budgets = emptyList(),
            recurringRules = emptyList(),
            goals = emptyList(),
            unreviewedCount = 0,
            baseCurrency = "USD",
            referenceDate = today
        )

        assertEquals(8000L, digest.totalSpend.amountMinor) // $80.00 today
        assertEquals(10000L, digest.previousPeriodSpend.amountMinor) // $100.00 yesterday
        assertEquals(-20.0, digest.spendChangePercentage, 0.01) // 20% drop
        assertEquals(2, digest.transactionCount)
        assertTrue(digest.topExpenses.isNotEmpty())
        assertEquals("Supermarket", digest.topExpenses[0].title)
        assertEquals(5000L, digest.topExpenses[0].amount.amountMinor)
    }

    @Test
    fun testTopExpensesRankingAndPercentages() {
        val todayNoon = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

        val transactions = listOf(
            Transaction(
                id = "tx1",
                amount = Money(2000), // $20
                type = TransactionType.EXPENSE,
                sourceAccountId = "acc1",
                categoryId = foodCategory.id,
                merchant = "Bakery",
                timestamp = todayNoon
            ),
            Transaction(
                id = "tx2",
                amount = Money(7000), // $70
                type = TransactionType.EXPENSE,
                sourceAccountId = "acc1",
                categoryId = techCategory.id,
                merchant = "Gadget Store",
                timestamp = todayNoon
            ),
            Transaction(
                id = "tx3",
                amount = Money(1000), // $10
                type = TransactionType.EXPENSE,
                sourceAccountId = "acc1",
                categoryId = foodCategory.id,
                merchant = "Coffee",
                timestamp = todayNoon
            )
        )

        val digest = engine.generateDigest(
            period = DigestPeriod.DAILY,
            transactions = transactions,
            categories = categories,
            budgets = emptyList(),
            recurringRules = emptyList(),
            goals = emptyList(),
            unreviewedCount = 0,
            baseCurrency = "USD",
            referenceDate = today
        )

        assertEquals(3, digest.topExpenses.size)
        assertEquals("Gadget Store", digest.topExpenses[0].title)
        assertEquals(7000L, digest.topExpenses[0].amount.amountMinor)
        assertEquals(70.0, digest.topExpenses[0].percentageOfPeriodSpend, 0.1) // 70/100 = 70%

        assertEquals("Bakery", digest.topExpenses[1].title)
        assertEquals(20.0, digest.topExpenses[1].percentageOfPeriodSpend, 0.1)

        assertEquals("Coffee", digest.topExpenses[2].title)
        assertEquals(10.0, digest.topExpenses[2].percentageOfPeriodSpend, 0.1)
    }

    @Test
    fun testBudgetEvaluation_ExceededAndWarning() {
        val todayNoon = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

        val foodBudget = Budget(
            id = "b-food",
            categoryId = foodCategory.id,
            name = "Food Budget",
            limitAmount = Money(10000), // $100
            startDate = 0L,
            endDate = Long.MAX_VALUE
        )
        val techBudget = Budget(
            id = "b-tech",
            categoryId = techCategory.id,
            name = "Tech Budget",
            limitAmount = Money(10000), // $100
            startDate = 0L,
            endDate = Long.MAX_VALUE
        )

        val transactions = listOf(
            // Food spend: $120 (Exceeded)
            Transaction(
                id = "tx1",
                amount = Money(12000),
                type = TransactionType.EXPENSE,
                sourceAccountId = "acc1",
                categoryId = foodCategory.id,
                timestamp = todayNoon
            ),
            // Tech spend: $85 (Warning >= 80%)
            Transaction(
                id = "tx2",
                amount = Money(8500),
                type = TransactionType.EXPENSE,
                sourceAccountId = "acc1",
                categoryId = techCategory.id,
                timestamp = todayNoon
            )
        )

        val digest = engine.generateDigest(
            period = DigestPeriod.DAILY,
            transactions = transactions,
            categories = categories,
            budgets = listOf(foodBudget, techBudget),
            recurringRules = emptyList(),
            goals = emptyList(),
            unreviewedCount = 0,
            baseCurrency = "USD",
            referenceDate = today
        )

        val foodItem = digest.budgetItems.first { it.categoryName == foodCategory.name }
        assertTrue(foodItem.isExceeded)
        assertEquals(120.0, foodItem.percentageConsumed, 0.1)

        val techItem = digest.budgetItems.first { it.categoryName == techCategory.name }
        assertTrue(techItem.isWarning)
        assertEquals(85.0, techItem.percentageConsumed, 0.1)

        // Check synthesized insights for critical/high budget alerts
        val budgetInsights = digest.insights.filter { it.category == DigestInsightCategory.BUDGET_STATUS }
        assertTrue(budgetInsights.any { it.priority == DigestPriority.CRITICAL })
        assertTrue(budgetInsights.any { it.priority == DigestPriority.HIGH })
    }

    @Test
    fun testUpcomingBillsEvaluation() {
        val tomorrow = today.plusDays(1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val yesterday = today.minusDays(1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val nextMonth = today.plusDays(25).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()

        val billDueSoon = RecurringTransaction(
            id = "rec1",
            title = "Cloud Subscription",
            amount = Money(1500),
            accountId = "acc1",
            categoryId = techCategory.id,
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = tomorrow
        )

        val billOverdue = RecurringTransaction(
            id = "rec2",
            title = "Gym Membership",
            amount = Money(5000),
            accountId = "acc1",
            categoryId = foodCategory.id,
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = yesterday
        )

        val billFarAway = RecurringTransaction(
            id = "rec3",
            title = "Annual Insurance",
            amount = Money(60000),
            accountId = "acc1",
            categoryId = techCategory.id,
            frequency = PaymentFrequency.YEARLY,
            nextDueDate = nextMonth
        )

        val digest = engine.generateDigest(
            period = DigestPeriod.DAILY,
            transactions = emptyList(),
            categories = categories,
            budgets = emptyList(),
            recurringRules = listOf(billDueSoon, billOverdue, billFarAway),
            goals = emptyList(),
            unreviewedCount = 0,
            baseCurrency = "USD",
            referenceDate = today
        )

        // Far away bill should NOT be included in a 3-day daily digest
        assertEquals(2, digest.upcomingBills.size)
        // Overdue bill should be prioritized
        assertTrue(digest.upcomingBills.first().isOverdue)
        assertEquals("Gym Membership", digest.upcomingBills.first().title)

        // Due soon bill
        val dueSoon = digest.upcomingBills.first { !it.isOverdue }
        assertEquals("Cloud Subscription", dueSoon.title)
        assertEquals(1L, dueSoon.daysUntilDue)
    }

    @Test
    fun testSavingsGoalMilestoneInsight() {
        val goalClose = FinancialGoal(
            id = "goal1",
            title = "New Laptop",
            targetAmount = Money(100000), // $1000
            currentAmount = Money(85000),  // $850 (85%)
            targetDate = System.currentTimeMillis() + 10000000L
        )

        val digest = engine.generateDigest(
            period = DigestPeriod.WEEKLY,
            transactions = emptyList(),
            categories = categories,
            budgets = emptyList(),
            recurringRules = emptyList(),
            goals = listOf(goalClose),
            unreviewedCount = 2,
            baseCurrency = "USD",
            referenceDate = today
        )

        val goalInsight = digest.insights.firstOrNull { it.category == DigestInsightCategory.SAVINGS_GOALS }
        assertNotNull(goalInsight)
        assertEquals(DigestPriority.MEDIUM, goalInsight?.priority)
        assertTrue(goalInsight?.summary?.contains("85%") == true)
    }

    @Test
    fun testReviewQueueInsight() {
        val digest = engine.generateDigest(
            period = DigestPeriod.DAILY,
            transactions = emptyList(),
            categories = categories,
            budgets = emptyList(),
            recurringRules = emptyList(),
            goals = emptyList(),
            unreviewedCount = 5,
            baseCurrency = "USD",
            referenceDate = today
        )

        val reviewInsight = digest.insights.firstOrNull { it.category == DigestInsightCategory.REVIEW_QUEUE }
        assertNotNull(reviewInsight)
        assertEquals(DigestPriority.HIGH, reviewInsight?.priority)
        assertEquals("5", reviewInsight?.metricValue)
    }
}

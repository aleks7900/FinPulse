package com.finpulse.app.domain

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.engine.DebtPayoffEngine
import com.finpulse.app.domain.engine.DebtStrategy
import com.finpulse.app.domain.engine.InsightsEngine
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.BudgetPeriod
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.Debt
import com.finpulse.app.domain.model.DebtType
import com.finpulse.app.domain.model.PaymentFrequency
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.usecase.EvaluateBudgetStatusUseCase
import com.finpulse.app.domain.usecase.GetDashboardSummaryUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EnginesTest {

    @Test
    fun testDashboardSummaryCalculation() {
        val useCase = GetDashboardSummaryUseCase()

        val accounts = listOf(
            Account("a1", "Checking", AccountType.BANK, Money(200000L, "USD"), Money(200000L, "USD")),
            Account("a2", "Savings", AccountType.SAVINGS, Money(500000L, "USD"), Money(500000L, "USD"))
        )

        val transactions = listOf(
            Transaction("t1", Money(300000L, "USD"), TransactionType.INCOME, "a1", null, "cat_salary"),
            Transaction("t2", Money(100000L, "USD"), TransactionType.EXPENSE, "a1", null, "cat_food")
        )

        val summary = useCase(
            accounts = accounts,
            transactions = transactions,
            investments = emptyList(),
            debts = emptyList(),
            goals = emptyList()
        )

        assertEquals(700000L, summary.totalBalance.amountMinor)
        assertEquals(300000L, summary.income.amountMinor)
        assertEquals(100000L, summary.expenses.amountMinor)
        assertEquals(200000L, summary.netCashFlow.amountMinor)
        // Savings rate = (300000 - 100000) / 300000 = 66.67%
        assertTrue("Savings rate should be ~66.67%", summary.savingsRatePercentage > 66.0)
    }

    @Test
    fun testBudgetStatusEvaluation() {
        val useCase = EvaluateBudgetStatusUseCase()

        val catFood = Category("c1", "Food", CategoryType.EXPENSE)
        val budget = Budget("b1", "c1", "Food Budget", Money(50000L, "USD"), BudgetPeriod.MONTHLY, 0L, Long.MAX_VALUE)

        val transactions = listOf(
            Transaction("t1", Money(45000L, "USD"), TransactionType.EXPENSE, "a1", null, "c1", timestamp = 1000L)
        )

        val statuses = useCase(
            budgets = listOf(budget),
            categories = listOf(catFood),
            transactions = transactions,
            currentDate = LocalDate.of(2026, 9, 20)
        )

        assertEquals(1, statuses.size)
        val status = statuses.first()
        assertEquals(45000L, status.spentAmount.amountMinor)
        assertEquals(5000L, status.remainingAmount.amountMinor)
        assertEquals(0.90, status.percentageConsumed, 0.001)
        assertTrue(status.isWarning)
    }

    @Test
    fun testDebtPayoffEngineSorting() {
        val engine = DebtPayoffEngine()

        val debtSmallBalanceHighRate = Debt(
            "d1", "Credit Card", DebtType.CREDIT_CARD,
            Money(200000L, "USD"), Money(200000L, "USD"),
            22.0, Money(10000L, "USD"), 0L
        )

        val debtLargeBalanceLowRate = Debt(
            "d2", "Auto Loan", DebtType.AUTO_LOAN,
            Money(800000L, "USD"), Money(800000L, "USD"),
            5.0, Money(20000L, "USD"), 0L
        )

        val snowball = engine.generatePlan(listOf(debtLargeBalanceLowRate, debtSmallBalanceHighRate), DebtStrategy.SNOWBALL)
        assertEquals("d1", snowball.orderedDebts[0].id)
        assertEquals("d2", snowball.orderedDebts[1].id)

        val avalanche = engine.generatePlan(listOf(debtLargeBalanceLowRate, debtSmallBalanceHighRate), DebtStrategy.AVALANCHE)
        assertEquals("d1", avalanche.orderedDebts[0].id) // 22% > 5%
        assertEquals("d2", avalanche.orderedDebts[1].id)
    }
}

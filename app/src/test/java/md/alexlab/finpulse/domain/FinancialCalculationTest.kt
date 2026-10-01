package md.alexlab.finpulse.domain

import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Debt
import md.alexlab.finpulse.domain.model.DebtType
import md.alexlab.finpulse.domain.model.FinancialGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FinancialCalculationTest {

    @Test
    fun testDebtAmortizationEstimation() {
        // Loan with 0% interest: 1000 balance, 200/mo -> 5 months
        val noInterestDebt = Debt(
            id = "debt-1",
            name = "Interest-Free Loan",
            type = DebtType.PERSONAL_LOAN,
            totalPrincipal = Money(100000L, "USD"),
            remainingBalance = Money(100000L, "USD"),
            interestRatePercent = 0.0,
            minimumPayment = Money(20000L, "USD"),
            nextPaymentDate = System.currentTimeMillis()
        )
        assertEquals(5, noInterestDebt.estimateMonthsToPayoff())

        // Loan with 12% interest, $5,000 balance, $200/month payment
        val interestDebt = Debt(
            id = "debt-2",
            name = "Credit Card",
            type = DebtType.CREDIT_CARD,
            totalPrincipal = Money(500000L, "USD"),
            remainingBalance = Money(500000L, "USD"),
            interestRatePercent = 12.0,
            minimumPayment = Money(20000L, "USD"),
            nextPaymentDate = System.currentTimeMillis()
        )
        val months = interestDebt.estimateMonthsToPayoff()
        // Amortization formula should yield ~30-32 months
        assertTrue("Months to payoff should be between 28 and 34", months in 28..34)
    }

    @Test
    fun testFinancialGoalSuggestedContribution() {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.of(2026, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val targetDate = LocalDate.of(2026, 7, 1).atStartOfDay(zone).toInstant().toEpochMilli() // 6 months later

        val goal = FinancialGoal(
            id = "goal-1",
            title = "Emergency Fund",
            targetAmount = Money(600000L, "USD"), // $6,000
            currentAmount = Money(0L, "USD"),
            targetDate = targetDate
        )

        val monthly = goal.calculateSuggestedMonthlyContribution(now)
        // $6,000 / 6 months = $1,000 / month = 100,000 minor units
        assertEquals(100000L, monthly.amountMinor)
    }
}

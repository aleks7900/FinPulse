package md.alexlab.finpulse.domain.engine

import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Debt

enum class DebtStrategy {
    SNOWBALL, // Lowest balance first (psychological momentum)
    AVALANCHE // Highest interest rate first (mathematical optimization)
}

data class DebtPayoffPlan(
    val strategy: DebtStrategy,
    val orderedDebts: List<Debt>,
    val totalDebt: Money,
    val totalMonthlyMinimum: Money,
    val estimatedMonthsToFree: Int
)

class DebtPayoffEngine {

    fun generatePlan(debts: List<Debt>, strategy: DebtStrategy): DebtPayoffPlan {
        if (debts.isEmpty()) {
            return DebtPayoffPlan(
                strategy = strategy,
                orderedDebts = emptyList(),
                totalDebt = Money.zero(),
                totalMonthlyMinimum = Money.zero(),
                estimatedMonthsToFree = 0
            )
        }

        val currency = debts.first().remainingBalance.currencyCode

        val sorted = when (strategy) {
            DebtStrategy.SNOWBALL -> debts.sortedBy { it.remainingBalance.amountMinor }
            DebtStrategy.AVALANCHE -> debts.sortedByDescending { it.interestRatePercent }
        }

        val totalMinor = debts.sumOf { it.remainingBalance.amountMinor }
        val totalMinMonthlyMinor = debts.sumOf { it.minimumPayment.amountMinor }

        // Max months among active debts as upper baseline estimate
        val maxMonths = debts.maxOfOrNull { it.estimateMonthsToPayoff() } ?: 0

        return DebtPayoffPlan(
            strategy = strategy,
            orderedDebts = sorted,
            totalDebt = Money(totalMinor, currency),
            totalMonthlyMinimum = Money(totalMinMonthlyMinor, currency),
            estimatedMonthsToFree = maxMonths
        )
    }
}

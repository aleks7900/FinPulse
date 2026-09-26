package com.finpulse.app.domain.usecase

import com.finpulse.app.core.model.Money
import com.finpulse.app.core.model.TimePeriod
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.Debt
import com.finpulse.app.domain.model.FinancialGoal
import com.finpulse.app.domain.model.InvestmentAsset
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType

data class DashboardSummary(
    val totalBalance: Money,
    val availableBalance: Money,
    val income: Money,
    val expenses: Money,
    val netCashFlow: Money,
    val totalSavings: Money,
    val totalInvestments: Money,
    val totalDebt: Money,
    val savingsRatePercentage: Double
)

class GetDashboardSummaryUseCase {

    operator fun invoke(
        accounts: List<Account>,
        transactions: List<Transaction>,
        investments: List<InvestmentAsset>,
        debts: List<Debt>,
        goals: List<FinancialGoal>,
        baseCurrency: String = "USD"
    ): DashboardSummary {
        val activeAccounts = accounts.filter { !it.isArchived }

        // Total Balance & Available
        var totalBalMinor = 0L
        var totalAvailMinor = 0L
        var savingsBalMinor = 0L

        for (acc in activeAccounts) {
            totalBalMinor += acc.balance.amountMinor
            totalAvailMinor += acc.availableBalance.amountMinor
            if (acc.type == AccountType.SAVINGS) {
                savingsBalMinor += acc.balance.amountMinor
            }
        }

        // Transactions in period
        var incomeMinor = 0L
        var expenseMinor = 0L

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME, TransactionType.REFUND -> incomeMinor += tx.amount.amountMinor
                TransactionType.EXPENSE -> if (!tx.isExcludedFromBudget) expenseMinor += tx.amount.amountMinor
                TransactionType.TRANSFER -> { /* Transfers don't alter net cash flow */ }
            }
        }

        // Net Cash Flow
        val netCashFlowMinor = incomeMinor - expenseMinor

        // Investments total current value
        var totalInvestmentsMinor = 0L
        for (asset in investments) {
            totalInvestmentsMinor += asset.currentValue.amountMinor
        }

        // Debts total remaining
        var totalDebtMinor = 0L
        for (debt in debts) {
            totalDebtMinor += debt.remainingBalance.amountMinor
        }

        // Goal savings
        for (goal in goals) {
            savingsBalMinor += goal.currentAmount.amountMinor
        }

        // Savings Rate = (Income - Expenses) / Income * 100%
        val savingsRate = if (incomeMinor > 0L) {
            val saved = (incomeMinor - expenseMinor).coerceAtLeast(0L)
            (saved.toDouble() / incomeMinor.toDouble()) * 100.0
        } else 0.0

        return DashboardSummary(
            totalBalance = Money(totalBalMinor, baseCurrency),
            availableBalance = Money(totalAvailMinor, baseCurrency),
            income = Money(incomeMinor, baseCurrency),
            expenses = Money(expenseMinor, baseCurrency),
            netCashFlow = Money(netCashFlowMinor, baseCurrency),
            totalSavings = Money(savingsBalMinor, baseCurrency),
            totalInvestments = Money(totalInvestmentsMinor, baseCurrency),
            totalDebt = Money(totalDebtMinor, baseCurrency),
            savingsRatePercentage = savingsRate
        )
    }
}

package md.alexlab.finpulse.domain.usecase

import md.alexlab.finpulse.core.model.CurrencyConfig
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.data.repository.ExchangeRateProviderImpl
import md.alexlab.finpulse.domain.engine.CurrencyConverter
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.Debt
import md.alexlab.finpulse.domain.model.FinancialGoal
import md.alexlab.finpulse.domain.model.InvestmentAsset
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import java.math.BigDecimal

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

class GetDashboardSummaryUseCase(
    private val currencyConverter: CurrencyConverter? = null
) {

    private fun convertFallback(money: Money, targetCurrency: String, customRate: Double? = null): Money {
        val target = targetCurrency.uppercase()
        if (money.currencyCode.equals(target, ignoreCase = true)) return money

        val rate = customRate ?: ExchangeRateProviderImpl.computeFallbackRate(money.currencyCode, target)
        val targetMajor = money.amountBigDecimal.multiply(BigDecimal.valueOf(rate))
        return Money.fromMajor(targetMajor, target)
    }

    private fun convertTxFallback(tx: Transaction, targetCurrency: String): Money {
        val target = targetCurrency.uppercase()
        if (tx.amount.currencyCode.equals(target, ignoreCase = true)) return tx.amount
        return convertFallback(tx.amount, target, tx.exchangeRate)
    }

    suspend operator fun invoke(
        accounts: List<Account>,
        transactions: List<Transaction>,
        investments: List<InvestmentAsset>,
        debts: List<Debt>,
        goals: List<FinancialGoal>,
        baseCurrency: String = "USD"
    ): DashboardSummary {
        val activeAccounts = accounts.filter { !it.isArchived }
        val target = baseCurrency.uppercase()

        // 1. Total Balance & Available (converted to baseCurrency)
        var totalBalMinor = 0L
        var totalAvailMinor = 0L
        var savingsBalMinor = 0L

        for (acc in activeAccounts) {
            val convertedBal = currencyConverter?.convert(acc.balance, target)
                ?: convertFallback(acc.balance, target)
            val convertedAvail = currencyConverter?.convert(acc.availableBalance, target)
                ?: convertFallback(acc.availableBalance, target)

            totalBalMinor += convertedBal.amountMinor
            totalAvailMinor += convertedAvail.amountMinor

            if (acc.type == AccountType.SAVINGS) {
                savingsBalMinor += convertedBal.amountMinor
            }
        }

        // 2. Transactions in period (converted to baseCurrency respecting historical rates)
        var incomeMinor = 0L
        var expenseMinor = 0L

        for (tx in transactions) {
            val convertedTx = currencyConverter?.convertHistorical(tx, target)
                ?: convertTxFallback(tx, target)

            when (tx.type) {
                TransactionType.INCOME, TransactionType.REFUND -> incomeMinor += convertedTx.amountMinor
                TransactionType.EXPENSE -> if (!tx.isExcludedFromBudget) expenseMinor += convertedTx.amountMinor
                TransactionType.TRANSFER -> { /* Internal transfers don't alter net cash flow */ }
            }
        }

        // 3. Net Cash Flow
        val netCashFlowMinor = incomeMinor - expenseMinor

        // 4. Investments total current value (converted to baseCurrency)
        var totalInvestmentsMinor = 0L
        for (asset in investments) {
            val convertedInv = currencyConverter?.convert(asset.currentValue, target)
                ?: convertFallback(asset.currentValue, target)
            totalInvestmentsMinor += convertedInv.amountMinor
        }

        // 5. Debts total remaining (converted to baseCurrency)
        var totalDebtMinor = 0L
        for (debt in debts) {
            val convertedDebt = currencyConverter?.convert(debt.remainingBalance, target)
                ?: convertFallback(debt.remainingBalance, target)
            totalDebtMinor += convertedDebt.amountMinor
        }

        // 6. Goal savings (converted to baseCurrency)
        for (goal in goals) {
            val convertedGoal = currencyConverter?.convert(goal.currentAmount, target)
                ?: convertFallback(goal.currentAmount, target)
            savingsBalMinor += convertedGoal.amountMinor
        }

        // 7. Savings Rate
        val savingsRate = if (incomeMinor > 0L) {
            val saved = (incomeMinor - expenseMinor).coerceAtLeast(0L)
            (saved.toDouble() / incomeMinor.toDouble()) * 100.0
        } else 0.0

        return DashboardSummary(
            totalBalance = Money(totalBalMinor, target),
            availableBalance = Money(totalAvailMinor, target),
            income = Money(incomeMinor, target),
            expenses = Money(expenseMinor, target),
            netCashFlow = Money(netCashFlowMinor, target),
            totalSavings = Money(savingsBalMinor, target),
            totalInvestments = Money(totalInvestmentsMinor, target),
            totalDebt = Money(totalDebtMinor, target),
            savingsRatePercentage = savingsRate
        )
    }
}

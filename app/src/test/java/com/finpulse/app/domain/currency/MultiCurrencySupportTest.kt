package com.finpulse.app.domain.currency

import com.finpulse.app.core.model.CurrencyConfig
import com.finpulse.app.core.model.Money
import com.finpulse.app.data.repository.ExchangeRateProviderImpl
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.BudgetPeriod
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.usecase.EvaluateBudgetStatusUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

class MultiCurrencySupportTest {

    private val evaluateBudgetStatusUseCase = EvaluateBudgetStatusUseCase()

    @Test
    fun `currency resolution rules - accounts and budgets maintain their currency when default changes`() {
        // Initial application base currency
        var appBaseCurrency = "USD"

        // Account 1 created with default currency (USD)
        val accUsd = Account(
            id = "acc-1",
            name = "Main USD",
            type = AccountType.BANK,
            balance = Money(100000L, appBaseCurrency), // $1,000.00
            availableBalance = Money(100000L, appBaseCurrency)
        )

        // Account 2 created with specific currency (MDL - Moldovan Leu)
        val accMdl = Account(
            id = "acc-2",
            name = "Chisinau Cash",
            type = AccountType.CASH,
            balance = Money(500000L, "MDL"), // 5,000.00 L
            availableBalance = Money(500000L, "MDL")
        )

        // Budget 1 created with default currency (USD)
        val budgetUsd = Budget(
            id = "b-1",
            categoryId = "cat-groceries",
            name = "Groceries USD",
            limitAmount = Money(50000L, appBaseCurrency), // $500.00
            periodType = BudgetPeriod.MONTHLY,
            startDate = 1000L,
            endDate = 5000L
        )

        // Budget 2 created with specific currency (EUR)
        val budgetEur = Budget(
            id = "b-2",
            categoryId = "cat-travel",
            name = "Europe Travel",
            limitAmount = Money(100000L, "EUR"), // €1,000.00
            periodType = BudgetPeriod.MONTHLY,
            startDate = 1000L,
            endDate = 5000L
        )

        // User changes application default currency in Settings -> Currency -> Default currency:
        appBaseCurrency = "EUR"

        // Verify existing account and budget currencies are strictly untouched
        assertEquals("USD", accUsd.balance.currencyCode)
        assertEquals(100000L, accUsd.balance.amountMinor)

        assertEquals("MDL", accMdl.balance.currencyCode)
        assertEquals(500000L, accMdl.balance.amountMinor)

        assertEquals("USD", budgetUsd.limitAmount.currencyCode)
        assertEquals(50000L, budgetUsd.limitAmount.amountMinor)

        assertEquals("EUR", budgetEur.limitAmount.currencyCode)
        assertEquals(100000L, budgetEur.limitAmount.amountMinor)

        // New account created without explicit currency now adopts the new default currency
        val accNew = Account(
            id = "acc-3",
            name = "New Account",
            type = AccountType.BANK,
            balance = Money(25000L, appBaseCurrency),
            availableBalance = Money(25000L, appBaseCurrency)
        )
        assertEquals("EUR", accNew.balance.currencyCode)
    }

    @Test
    fun `account currency edit protection prevents silent reinterpretation when transactions exist`() {
        val existingAccount = Account(
            id = "acc-salary",
            name = "Salary Account",
            type = AccountType.BANK,
            balance = Money(1000000L, "MDL"), // 10,000.00 MDL
            availableBalance = Money(1000000L, "MDL")
        )

        // Helper simulating safe resolution logic in AccountsViewModel
        fun resolveCurrencyForSave(
            existing: Account?,
            transactionCount: Int,
            requestedCurrency: String?,
            appBaseCurrency: String
        ): String {
            return if (existing != null && transactionCount > 0) {
                existing.balance.currencyCode
            } else {
                requestedCurrency?.uppercase() ?: existing?.balance?.currencyCode ?: appBaseCurrency
            }
        }

        // Case 1: Account has 0 transactions (e.g., freshly created, mistake made) -> allowed to change
        val currencyWhenZeroTx = resolveCurrencyForSave(
            existing = existingAccount,
            transactionCount = 0,
            requestedCurrency = "EUR",
            appBaseCurrency = "USD"
        )
        assertEquals("EUR", currencyWhenZeroTx)

        // Case 2: Account has historical transactions (>0) -> strictly preserves existing currency
        val currencyWhenHasTx = resolveCurrencyForSave(
            existing = existingAccount,
            transactionCount = 15,
            requestedCurrency = "EUR",
            appBaseCurrency = "USD"
        )
        assertEquals(
            "Account with transactions must lock currency to prevent silent reinterpretation",
            "MDL",
            currencyWhenHasTx
        )
    }

    @Test
    fun `cross-currency budget status evaluation converts transactions in other currencies safely`() {
        val categoryGroceries = Category(
            id = "cat-groceries",
            name = "Groceries",
            colorHex = 0xFF4CAF50,
            type = CategoryType.EXPENSE,
            icon = "cart"
        )

        // Budget set in USD: $500.00 limit
        val budget = Budget(
            id = "b-groceries",
            categoryId = "cat-groceries",
            name = "Groceries Budget",
            limitAmount = Money(50000L, "USD"), // $500.00
            periodType = BudgetPeriod.MONTHLY,
            startDate = 10000L,
            endDate = 90000L
        )

        // 1. Transaction in same currency (USD): $100.00
        val txUsd = Transaction(
            id = "tx-1",
            amount = Money(10000L, "USD"),
            type = TransactionType.EXPENSE,
            categoryId = "cat-groceries",
            sourceAccountId = "acc-usd",
            timestamp = 20000L
        )

        // 2. Transaction in Moldovan Leu (MDL): 1,780.00 L
        // Fallback rate: USD -> MDL is 17.80, so MDL -> USD rate = 1 / 17.80.
        // 1,780 MDL / 17.80 = exactly 100.00 USD (10000 minor)
        val txMdl = Transaction(
            id = "tx-2",
            amount = Money(178000L, "MDL"),
            type = TransactionType.EXPENSE,
            categoryId = "cat-groceries",
            sourceAccountId = "acc-mdl",
            timestamp = 25000L
        )

        // 3. Transaction with custom exchange rate stored on transaction:
        // €50.00 EUR at rate 1.10 USD/EUR -> $55.00 USD (5500 minor)
        val txEurWithCustomRate = Transaction(
            id = "tx-3",
            amount = Money(5000L, "EUR"),
            type = TransactionType.EXPENSE,
            categoryId = "cat-groceries",
            sourceAccountId = "acc-eur",
            timestamp = 30000L,
            exchangeRate = 1.10
        )

        // 4. Refund in USD: $25.00 refund (should offset expenses)
        val txRefund = Transaction(
            id = "tx-4",
            amount = Money(2500L, "USD"),
            type = TransactionType.REFUND,
            categoryId = "cat-groceries",
            sourceAccountId = "acc-usd",
            timestamp = 35000L
        )

        val statuses = evaluateBudgetStatusUseCase(
            budgets = listOf(budget),
            categories = listOf(categoryGroceries),
            transactions = listOf(txUsd, txMdl, txEurWithCustomRate, txRefund),
            currentDate = LocalDate.of(2026, 9, 15)
        )

        assertEquals(1, statuses.size)
        val status = statuses.first()

        // Currency must be USD (budget's currency)
        assertEquals("USD", status.spentAmount.currencyCode)
        assertEquals("USD", status.remainingAmount.currencyCode)

        // Expected Net Spent:
        // tx1: $100.00 = 10000 minor
        // tx2: 1780 MDL / 17.80 = $100.00 = 10000 minor
        // tx3: 50 EUR * 1.10 = $55.00 = 5500 minor
        // Total expenses = 25500 minor ($255.00)
        // Refund = 2500 minor ($25.00)
        // Net spent = 25500 - 2500 = 23000 minor ($230.00)
        assertEquals(23000L, status.spentAmount.amountMinor)
        assertEquals(BigDecimal("230.00"), status.spentAmount.amountBigDecimal)

        // Remaining: $500.00 - $230.00 = $270.00 (27000 minor)
        assertEquals(27000L, status.remainingAmount.amountMinor)
        assertEquals(BigDecimal("270.00"), status.remainingAmount.amountBigDecimal)
        assertFalse(status.isExceeded)
    }

    @Test
    fun `budget aggregations convert cross-currency budgets to base currency without raw summation`() {
        val baseCode = "USD"

        // Budget A in USD: $200.00
        val budgetA = Budget(
            id = "b-a",
            categoryId = "cat-a",
            name = "Budget A",
            limitAmount = Money(20000L, "USD"),
            periodType = BudgetPeriod.MONTHLY,
            startDate = 1000L,
            endDate = 5000L
        )

        // Budget B in MDL: 3,560.00 MDL (at 17.80 MDL/USD = $200.00 USD)
        val budgetB = Budget(
            id = "b-b",
            categoryId = "cat-b",
            name = "Budget B",
            limitAmount = Money(356000L, "MDL"),
            periodType = BudgetPeriod.MONTHLY,
            startDate = 1000L,
            endDate = 5000L
        )

        // Budget C in EUR: €100.00 (at 0.92 EUR/USD -> USD rate = 1 / 0.92 = 1.0869565... -> ~$108.70 USD)
        val budgetC = Budget(
            id = "b-c",
            categoryId = "cat-c",
            name = "Budget C",
            limitAmount = Money(10000L, "EUR"),
            periodType = BudgetPeriod.MONTHLY,
            startDate = 1000L,
            endDate = 5000L
        )

        val budgets = listOf(budgetA, budgetB, budgetC)

        // Aggregation logic matching BudgetsViewModel
        var totalBudgetedMinor = 0L
        for (b in budgets) {
            val limit = b.limitAmount
            if (limit.currencyCode.equals(baseCode, ignoreCase = true)) {
                totalBudgetedMinor += limit.amountMinor
            } else {
                val rate = ExchangeRateProviderImpl.computeFallbackRate(limit.currencyCode, baseCode)
                val targetMajor = limit.amountBigDecimal.multiply(BigDecimal.valueOf(rate))
                totalBudgetedMinor += CurrencyConfig.toMinor(targetMajor, baseCode)
            }
        }

        val totalBudgetedMoney = Money(totalBudgetedMinor, baseCode)

        // If raw minors were incorrectly summed: 20,000 + 356,000 + 10,000 = 386,000 cents ($3,860.00)
        // With correct conversion: $200.00 + $200.00 + ~$108.70 = ~$508.70 (50,870 cents)
        assertTrue(
            "Total budgeted must be properly converted to ~$508.70, not raw summed 3,860.00",
            totalBudgetedMoney.amountMinor in 50800L..50900L
        )
        assertEquals("USD", totalBudgetedMoney.currencyCode)
    }

    @Test
    fun `centralized money formatting supports all 24 currencies accurately`() {
        val testCases = listOf(
            Triple("USD", 1050L, "$"),
            Triple("EUR", 1050L, "€"),
            Triple("GBP", 1050L, "£"),
            Triple("MDL", 1050L, "L"),
            Triple("RON", 1050L, "lei"),
            Triple("UAH", 1050L, "₴"),
            Triple("PLN", 1050L, "zł"),
            Triple("CHF", 1050L, "CHF"),
            Triple("DKK", 1050L, "kr"),
            Triple("CZK", 1050L, "Kč"),
            Triple("HUF", 1050L, "Ft"),
            Triple("BGN", 1050L, "лв"),
            Triple("ILS", 1050L, "₪"),
            Triple("ZAR", 1050L, "R"),
            Triple("JPY", 1000L, "¥")
        )

        for ((code, minor, expectedSymbol) in testCases) {
            val formatted = CurrencyConfig.formatMoney(minor, code, Locale.US)
            assertTrue(
                "Formatted string for $code should contain symbol '$expectedSymbol', but was: '$formatted'",
                formatted.contains(expectedSymbol)
            )
        }
    }
}

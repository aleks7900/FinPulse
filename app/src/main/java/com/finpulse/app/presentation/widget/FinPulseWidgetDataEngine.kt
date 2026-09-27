package com.finpulse.app.presentation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import com.finpulse.app.core.datastore.UserPreferences
import com.finpulse.app.core.model.Money
import com.finpulse.app.di.AppContainer
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

data class BalanceWidgetData(
    val totalAmount: Money,
    val displayAmount: String,
    val accountSubtitle: String,
    val isMasked: Boolean
)

data class SpendingWidgetData(
    val totalAmount: Money,
    val displayAmount: String,
    val subtitle: String,
    val isMasked: Boolean
)

data class BudgetWidgetData(
    val remainingAmount: Money,
    val displayRemaining: String,
    val totalBudgetAmount: Money,
    val totalSpentAmount: Money,
    val progressPercent: Int,
    val subtitle: String,
    val isMasked: Boolean
)

data class WidgetBillItem(
    val id: String,
    val title: String,
    val dueDateText: String,
    val displayAmount: String,
    val isOverdue: Boolean
)

data class UpcomingBillsWidgetData(
    val bills: List<WidgetBillItem>,
    val totalUpcomingCount: Int,
    val isMasked: Boolean
)

object FinPulseWidgetDataEngine {

    const val MASKED_STRING = "••••••"

    fun formatMaskedAmount(currencyCode: String): String {
        val symbol = try {
            Currency.getInstance(currencyCode).getSymbol(Locale.getDefault())
        } catch (_: Exception) {
            "$"
        }
        return "$symbol$MASKED_STRING"
    }

    fun shouldMask(
        context: Context,
        appWidgetId: Int,
        userPrefs: UserPreferences,
        appWidgetManager: AppWidgetManager? = null
    ): Boolean {
        // 1. Widget-specific configuration override
        val config = WidgetPreferences.getWidgetConfig(context, appWidgetId)
        if (config.privacySetting == WidgetPrivacySetting.ALWAYS_HIDE) return true
        if (config.privacySetting == WidgetPrivacySetting.ALWAYS_SHOW) return false

        // 2. Lock screen / Keyguard detection
        if (appWidgetManager != null && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            try {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val category = options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1)
                if (category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) {
                    return true
                }
            } catch (_: Exception) {
                // Ignore if unable to read options
            }
        }

        // 3. App Lock (Biometric or PIN enabled with widget masking on)
        if (userPrefs.widgetMaskOnAppLock && (userPrefs.isBiometricEnabled || userPrefs.isPinEnabled)) {
            return true
        }

        // 4. Global privacy mode or hide balances setting
        if (userPrefs.widgetPrivacyEnabled || userPrefs.hideBalances) {
            return true
        }

        return false
    }

    suspend fun getBalanceData(
        container: AppContainer,
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager? = null
    ): BalanceWidgetData {
        val userPrefs = container.userPreferencesDataStore.userPreferencesFlow.first()
        val config = WidgetPreferences.getWidgetConfig(context, appWidgetId)
        val isMasked = shouldMask(context, appWidgetId, userPrefs, appWidgetManager)
        val currency = userPrefs.baseCurrencyCode

        val accounts = container.accountRepository.getActiveAccountsFlow().first()

        val (targetMoney, subtitle) = if (config.accountId != null) {
            val account = accounts.find { it.id == config.accountId }
            if (account != null) {
                account.balance to account.name
            } else {
                calculateLiquidTotal(accounts, currency) to "${accounts.size} Accounts • Liquid"
            }
        } else {
            calculateLiquidTotal(accounts, currency) to "${accounts.size} Accounts • Liquid"
        }

        val display = if (isMasked) formatMaskedAmount(currency) else targetMoney.formatted()

        return BalanceWidgetData(
            totalAmount = targetMoney,
            displayAmount = display,
            accountSubtitle = subtitle,
            isMasked = isMasked
        )
    }

    private fun calculateLiquidTotal(accounts: List<com.finpulse.app.domain.model.Account>, currency: String): Money {
        val liquidTypes = setOf(
            AccountType.BANK,
            AccountType.SAVINGS,
            AccountType.CASH,
            AccountType.WALLET
        )
        val totalMinor: Long = accounts
            .filter { it.type in liquidTypes }
            .sumOf { it.balance.amountMinor }
        return Money(totalMinor, currency)
    }

    suspend fun getSpendingData(
        container: AppContainer,
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager? = null
    ): SpendingWidgetData {
        val userPrefs = container.userPreferencesDataStore.userPreferencesFlow.first()
        val isMasked = shouldMask(context, appWidgetId, userPrefs, appWidgetManager)
        val currency = userPrefs.baseCurrencyCode

        val currentMonth = YearMonth.now()
        val startOfMonthMillis = currentMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfMonthMillis = currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val transactions = container.transactionRepository
            .getTransactionsByDateRangeFlow(startOfMonthMillis, endOfMonthMillis)
            .first()

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpenseMinor: Long = expenses.sumOf { it.amount.amountMinor }
        val totalMoney = Money(totalExpenseMinor, currency)

        val monthName = currentMonth.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))
        val subtitle = "$monthName • ${expenses.size} Expenses"
        val display = if (isMasked) formatMaskedAmount(currency) else totalMoney.formatted()

        return SpendingWidgetData(
            totalAmount = totalMoney,
            displayAmount = display,
            subtitle = subtitle,
            isMasked = isMasked
        )
    }

    suspend fun getBudgetData(
        container: AppContainer,
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager? = null
    ): BudgetWidgetData {
        val userPrefs = container.userPreferencesDataStore.userPreferencesFlow.first()
        val isMasked = shouldMask(context, appWidgetId, userPrefs, appWidgetManager)
        val currency = userPrefs.baseCurrencyCode

        val activeBudgets = container.budgetRepository.getAllActiveBudgetsFlow().first()

        // Get spending for current month
        val currentMonth = YearMonth.now()
        val startOfMonthMillis = currentMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfMonthMillis = currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val transactions = container.transactionRepository
            .getTransactionsByDateRangeFlow(startOfMonthMillis, endOfMonthMillis)
            .first()
            .filter { it.type == TransactionType.EXPENSE }

        val totalBudgetMinor: Long = activeBudgets.sumOf { it.limitAmount.amountMinor }
        val totalSpentMinor: Long = transactions.sumOf { it.amount.amountMinor }
        val remainingMinor: Long = (totalBudgetMinor - totalSpentMinor).coerceAtLeast(0L)

        val progressPercent = if (totalBudgetMinor > 0) {
            ((totalSpentMinor.toDouble() / totalBudgetMinor.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val remainingMoney = Money(remainingMinor, currency)
        val budgetMoney = Money(totalBudgetMinor, currency)
        val spentMoney = Money(totalSpentMinor, currency)

        val displayRemaining = if (isMasked) {
            formatMaskedAmount(currency) + " left"
        } else {
            remainingMoney.formatted() + " left"
        }

        val subtitle = if (isMasked) {
            "Spent ${formatMaskedAmount(currency)} • $progressPercent%"
        } else {
            "Spent ${spentMoney.formatted()} of ${budgetMoney.formatted()}"
        }

        return BudgetWidgetData(
            remainingAmount = remainingMoney,
            displayRemaining = displayRemaining,
            totalBudgetAmount = budgetMoney,
            totalSpentAmount = spentMoney,
            progressPercent = progressPercent,
            subtitle = subtitle,
            isMasked = isMasked
        )
    }

    suspend fun getUpcomingBillsData(
        container: AppContainer,
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager? = null
    ): UpcomingBillsWidgetData {
        val userPrefs = container.userPreferencesDataStore.userPreferencesFlow.first()
        val isMasked = shouldMask(context, appWidgetId, userPrefs, appWidgetManager)
        val currency = userPrefs.baseCurrencyCode

        val now = LocalDate.now()
        val nowMillis = System.currentTimeMillis()
        val in14DaysMillis = now.plusDays(14).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val occurrences = container.recurringRepository
            .getOccurrencesInRange(nowMillis - 86400000L, in14DaysMillis)
            .filter { it.status != com.finpulse.app.domain.model.OccurrenceStatus.PAID && it.status != com.finpulse.app.domain.model.OccurrenceStatus.SKIPPED }
            .sortedBy { it.dueDate }

        val bills = occurrences.take(3).map { occ ->
            val dueLocalDate = Instant.ofEpochMilli(occ.dueDate).atZone(ZoneId.systemDefault()).toLocalDate()
            val dueDateStr = when {
                dueLocalDate.isBefore(now) -> "Overdue"
                dueLocalDate.isEqual(now) -> "Due Today"
                dueLocalDate.isEqual(now.plusDays(1)) -> "Due Tomorrow"
                else -> dueLocalDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
            }

            val display = if (isMasked) formatMaskedAmount(currency) else occ.amount.formatted()

            WidgetBillItem(
                id = occ.id,
                title = occ.ruleTitle,
                dueDateText = dueDateStr,
                displayAmount = display,
                isOverdue = dueLocalDate.isBefore(now)
            )
        }

        return UpcomingBillsWidgetData(
            bills = bills,
            totalUpcomingCount = occurrences.size,
            isMasked = isMasked
        )
    }
}

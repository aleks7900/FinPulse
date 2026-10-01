package md.alexlab.finpulse.presentation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle
import md.alexlab.finpulse.core.datastore.UserPreferences
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.di.AppContainer
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.Budget
import md.alexlab.finpulse.domain.model.OccurrenceStatus
import md.alexlab.finpulse.domain.model.RecurringOccurrence
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.BudgetRepository
import md.alexlab.finpulse.domain.repository.RecurringRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FinPulseWidgetDataEngineTest {

    private val container: AppContainer = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val appWidgetManager: AppWidgetManager = mockk(relaxed = true)
    private val userPreferencesDataStore: UserPreferencesDataStore = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val recurringRepository: RecurringRepository = mockk(relaxed = true)

    private val testAccounts = listOf(
        Account(
            id = "acc_bank",
            name = "Main Bank",
            type = AccountType.BANK,
            balance = Money(500_000, "USD"), // $5,000.00
            availableBalance = Money(500_000, "USD")
        ),
        Account(
            id = "acc_cash",
            name = "Wallet Cash",
            type = AccountType.CASH,
            balance = Money(25_000, "USD"), // $250.00
            availableBalance = Money(25_000, "USD")
        ),
        Account(
            id = "acc_loan",
            name = "Car Loan",
            type = AccountType.LOAN,
            balance = Money(-1_500_000, "USD"), // -$15,000.00
            availableBalance = Money(0, "USD")
        )
    )

    @Before
    fun setup() {
        mockkObject(WidgetPreferences)
        every { WidgetPreferences.getWidgetConfig(any(), any(), any()) } returns WidgetConfig(
            appWidgetId = 1,
            displayType = WidgetDisplayType.BALANCE,
            privacySetting = WidgetPrivacySetting.FOLLOW_APP,
            accountId = null
        )

        every { container.userPreferencesDataStore } returns userPreferencesDataStore
        every { container.accountRepository } returns accountRepository
        every { container.transactionRepository } returns transactionRepository
        every { container.budgetRepository } returns budgetRepository
        every { container.recurringRepository } returns recurringRepository

        every { accountRepository.getActiveAccountsFlow() } returns flowOf(testAccounts)
        every { userPreferencesDataStore.userPreferencesFlow } returns flowOf(
            UserPreferences(
                baseCurrencyCode = "USD",
                widgetPrivacyEnabled = false,
                hideBalances = false,
                widgetMaskOnAppLock = true,
                isBiometricEnabled = false,
                isPinEnabled = false
            )
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testGetBalanceData_UnmaskedLiquidBalance() = runTest {
        val data = FinPulseWidgetDataEngine.getBalanceData(container, context, 1)

        assertFalse(data.isMasked)
        assertEquals(525_000L, data.totalAmount.amountMinor) // $5,000 + $250 (excludes LOAN)
        val cleanDisplay = data.displayAmount.replace("\u00A0", "").replace("\u202F", "").replace(" ", "").replace(",", "")
        assertTrue(cleanDisplay.contains("5250"))
    }

    @Test
    fun testGetBalanceData_MaskedWhenPrivacyEnabled() = runTest {
        every { userPreferencesDataStore.userPreferencesFlow } returns flowOf(
            UserPreferences(
                baseCurrencyCode = "USD",
                widgetPrivacyEnabled = true,
                hideBalances = false
            )
        )

        val data = FinPulseWidgetDataEngine.getBalanceData(container, context, 1)

        assertTrue(data.isMasked)
        assertTrue(data.displayAmount.contains("••••••"))
    }

    @Test
    fun testGetBalanceData_SpecificAccountFilter() = runTest {
        every { WidgetPreferences.getWidgetConfig(any(), any(), any()) } returns WidgetConfig(
            appWidgetId = 1,
            displayType = WidgetDisplayType.BALANCE,
            privacySetting = WidgetPrivacySetting.FOLLOW_APP,
            accountId = "acc_cash"
        )

        val data = FinPulseWidgetDataEngine.getBalanceData(container, context, 1)

        assertEquals(25_000L, data.totalAmount.amountMinor)
        assertEquals("Wallet Cash", data.accountSubtitle)
    }

    @Test
    fun testShouldMask_AppLockEnabled() {
        val prefs = UserPreferences(
            widgetPrivacyEnabled = false,
            hideBalances = false,
            widgetMaskOnAppLock = true,
            isBiometricEnabled = true
        )

        val masked = FinPulseWidgetDataEngine.shouldMask(context, 1, prefs)
        assertTrue(masked)
    }

    @Test
    fun testShouldMask_PinLockEnabled() {
        val prefs = UserPreferences(
            widgetPrivacyEnabled = false,
            hideBalances = false,
            widgetMaskOnAppLock = true,
            isPinEnabled = true
        )

        val masked = FinPulseWidgetDataEngine.shouldMask(context, 1, prefs)
        assertTrue(masked)
    }

    @Test
    fun testShouldMask_KeyguardDetection() {
        val prefs = UserPreferences(
            widgetPrivacyEnabled = false,
            hideBalances = false,
            widgetMaskOnAppLock = false
        )

        val bundle = mockk<Bundle>()
        every { bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1) } returns AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD
        every { appWidgetManager.getAppWidgetOptions(1) } returns bundle

        val masked = FinPulseWidgetDataEngine.shouldMask(context, 1, prefs, appWidgetManager)
        assertTrue(masked)
    }

    @Test
    fun testShouldMask_WidgetOverrideAlwaysHide() {
        every { WidgetPreferences.getWidgetConfig(any(), any(), any()) } returns WidgetConfig(
            appWidgetId = 1,
            privacySetting = WidgetPrivacySetting.ALWAYS_HIDE
        )

        val prefs = UserPreferences(
            widgetPrivacyEnabled = false,
            hideBalances = false,
            widgetMaskOnAppLock = false
        )

        val masked = FinPulseWidgetDataEngine.shouldMask(context, 1, prefs)
        assertTrue(masked)
    }

    @Test
    fun testShouldMask_WidgetOverrideAlwaysShow() {
        every { WidgetPreferences.getWidgetConfig(any(), any(), any()) } returns WidgetConfig(
            appWidgetId = 1,
            privacySetting = WidgetPrivacySetting.ALWAYS_SHOW
        )

        val prefs = UserPreferences(
            widgetPrivacyEnabled = true, // Global privacy is true, but widget overrides to ALWAYS_SHOW
            hideBalances = true
        )

        val masked = FinPulseWidgetDataEngine.shouldMask(context, 1, prefs)
        assertFalse(masked)
    }

    @Test
    fun testGetSpendingData_CalculatesMonthlyExpenses() = runTest {
        val testTransactions = listOf(
            Transaction(
                id = "tx1",
                amount = Money(7_500, "USD"), // $75.00
                type = TransactionType.EXPENSE,
                sourceAccountId = "acc_bank",
                categoryId = "cat_groceries",
                timestamp = System.currentTimeMillis()
            ),
            Transaction(
                id = "tx2",
                amount = Money(2_500, "USD"), // $25.00
                type = TransactionType.EXPENSE,
                sourceAccountId = "acc_bank",
                categoryId = "cat_food",
                timestamp = System.currentTimeMillis()
            ),
            Transaction(
                id = "tx3",
                amount = Money(50_000, "USD"), // $500.00
                type = TransactionType.INCOME, // Excluded from spending
                sourceAccountId = "acc_bank",
                categoryId = "cat_salary",
                timestamp = System.currentTimeMillis()
            )
        )

        every { transactionRepository.getTransactionsByDateRangeFlow(any(), any()) } returns flowOf(testTransactions)

        val data = FinPulseWidgetDataEngine.getSpendingData(container, context, 1)

        assertEquals(10_000L, data.totalAmount.amountMinor) // $75 + $25 = $100.00
        assertTrue(data.displayAmount.contains("100"))
        assertTrue(data.subtitle.contains("2 Expenses"))
    }

    @Test
    fun testGetBudgetData_CalculatesRemainingBudget() = runTest {
        val budgets = listOf(
            Budget(
                id = "b1",
                categoryId = "cat_groceries",
                name = "Groceries",
                limitAmount = Money(50_000, "USD"), // $500.00
                startDate = 0L,
                endDate = 0L
            )
        )
        val transactions = listOf(
            Transaction(
                id = "tx1",
                amount = Money(20_000, "USD"), // $200.00 spent
                type = TransactionType.EXPENSE,
                sourceAccountId = "acc_bank",
                categoryId = "cat_groceries",
                timestamp = System.currentTimeMillis()
            )
        )

        every { budgetRepository.getAllActiveBudgetsFlow() } returns flowOf(budgets)
        every { transactionRepository.getTransactionsByDateRangeFlow(any(), any()) } returns flowOf(transactions)

        val data = FinPulseWidgetDataEngine.getBudgetData(container, context, 1)

        assertEquals(30_000L, data.remainingAmount.amountMinor) // $500 - $200 = $300.00
        assertEquals(40, data.progressPercent) // $200 / $500 = 40%
        assertTrue(data.displayRemaining.contains("300"))
    }

    @Test
    fun testGetUpcomingBillsData_FiltersPaidAndSkipped() = runTest {
        val occurrences = listOf(
            RecurringOccurrence(
                id = "occ1",
                ruleId = "r1",
                ruleTitle = "Internet Bill",
                amount = Money(7_900, "USD"),
                type = TransactionType.EXPENSE,
                accountId = "acc_bank",
                categoryId = "cat_bills",
                dueDate = System.currentTimeMillis() + 86400000L,
                status = OccurrenceStatus.EXPECTED
            ),
            RecurringOccurrence(
                id = "occ2",
                ruleId = "r2",
                ruleTitle = "Gym Membership",
                amount = Money(4_500, "USD"),
                type = TransactionType.EXPENSE,
                accountId = "acc_bank",
                categoryId = "cat_gym",
                dueDate = System.currentTimeMillis() + 172800000L,
                status = OccurrenceStatus.PAID // Should be filtered out
            ),
            RecurringOccurrence(
                id = "occ3",
                ruleId = "r3",
                ruleTitle = "Streaming",
                amount = Money(1_500, "USD"),
                type = TransactionType.EXPENSE,
                accountId = "acc_bank",
                categoryId = "cat_subs",
                dueDate = System.currentTimeMillis() + 259200000L,
                status = OccurrenceStatus.SKIPPED // Should be filtered out
            )
        )

        coEvery { recurringRepository.getOccurrencesInRange(any(), any()) } returns occurrences

        val data = FinPulseWidgetDataEngine.getUpcomingBillsData(container, context, 1)

        assertEquals(1, data.bills.size)
        assertEquals("Internet Bill", data.bills.first().title)
    }
}

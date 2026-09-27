package com.finpulse.app.presentation.calendar

import com.finpulse.app.core.datastore.UserPreferences
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.CalendarEvent
import com.finpulse.app.domain.model.CalendarEventStatus
import com.finpulse.app.domain.model.CalendarEventType
import com.finpulse.app.domain.model.CashFlowPeriodSummary
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.RecurringRepository
import com.finpulse.app.domain.usecase.calendar.GetFinancialCalendarUseCase
import com.finpulse.app.domain.usecase.recurring.EditOccurrenceUseCase
import com.finpulse.app.domain.usecase.recurring.ManageRecurringRuleUseCase
import com.finpulse.app.domain.usecase.recurring.MarkOccurrencePaidUseCase
import com.finpulse.app.domain.usecase.recurring.SkipOccurrenceUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialCalendarViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private val getFinancialCalendarUseCase: GetFinancialCalendarUseCase = mockk(relaxed = true)
    private val recurringRepository: RecurringRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val userPreferencesDataStore: UserPreferencesDataStore = mockk(relaxed = true)
    private val markOccurrencePaidUseCase: MarkOccurrencePaidUseCase = mockk(relaxed = true)
    private val skipOccurrenceUseCase: SkipOccurrenceUseCase = mockk(relaxed = true)
    private val editOccurrenceUseCase: EditOccurrenceUseCase = mockk(relaxed = true)
    private val manageRecurringRuleUseCase: ManageRecurringRuleUseCase = mockk(relaxed = true)

    private lateinit var viewModel: FinancialCalendarViewModel

    private val testSummary = CashFlowPeriodSummary(
        startDate = LocalDate.of(2026, 10, 1),
        endDate = LocalDate.of(2026, 10, 31),
        startingBalance = Money(100_000, "USD"),
        projectedEndingBalance = Money(150_000, "USD"),
        lowestProjectedBalance = Money(90_000, "USD"),
        lowestBalanceDate = LocalDate.of(2026, 10, 5),
        totalInflow = Money(80_000, "USD"),
        totalOutflow = Money(30_000, "USD"),
        netCashFlow = Money(50_000, "USD"),
        incomeCount = 1,
        billCount = 2,
        subscriptionCount = 1,
        loanPaymentCount = 0,
        transferCount = 0,
        goalContributionCount = 0,
        totalCompletedCount = 0,
        totalExpectedCount = 4,
        totalOverdueCount = 0,
        dailyProjections = emptyList(),
        allEvents = listOf(
            CalendarEvent(
                id = "evt1",
                title = "Salary",
                amount = Money(80_000, "USD"),
                dateMillis = 100000L,
                type = CalendarEventType.INCOME,
                status = CalendarEventStatus.EXPECTED
            ),
            CalendarEvent(
                id = "evt2",
                title = "Electric Bill",
                amount = Money(15_000, "USD"),
                dateMillis = 200000L,
                type = CalendarEventType.BILL,
                status = CalendarEventStatus.EXPECTED
            )
        ),
        isDeterministic = true
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { userPreferencesDataStore.userPreferencesFlow } returns flowOf(
            UserPreferences(baseCurrencyCode = "USD", hideBalances = false)
        )
        every { accountRepository.getActiveAccountsFlow() } returns flowOf(
            listOf(
                Account("acc1", "Checking", AccountType.BANK, Money(100_000, "USD"), Money(100_000, "USD"))
            )
        )
        every { categoryRepository.getAllCategoriesFlow() } returns flowOf(emptyList())
        every { getFinancialCalendarUseCase.invoke(any(), any(), any(), any()) } returns flowOf(testSummary)

        viewModel = FinancialCalendarViewModel(
            getFinancialCalendarUseCase = getFinancialCalendarUseCase,
            recurringRepository = recurringRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            userPreferencesDataStore = userPreferencesDataStore,
            markOccurrencePaidUseCase = markOccurrencePaidUseCase,
            skipOccurrenceUseCase = skipOccurrenceUseCase,
            editOccurrenceUseCase = editOccurrenceUseCase,
            manageRecurringRuleUseCase = manageRecurringRuleUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_LoadedCorrectly() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CalendarViewMode.MONTH, state.viewMode)
        assertEquals(PeriodPreset.SELECTED_MONTH, state.periodPreset)
        assertEquals(CalendarFilterType.ALL, state.selectedTypeFilter)
        assertEquals("USD", state.baseCurrency)
        assertFalse(state.hideBalances)
        assertNotNull(state.summary)
        assertEquals(2, state.filteredEvents.size)
    }

    @Test
    fun testSetViewMode_SwitchesToAgendaAndBack() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.setViewMode(CalendarViewMode.AGENDA)
        advanceUntilIdle()
        assertEquals(CalendarViewMode.AGENDA, viewModel.uiState.value.viewMode)

        viewModel.setViewMode(CalendarViewMode.MONTH)
        advanceUntilIdle()
        assertEquals(CalendarViewMode.MONTH, viewModel.uiState.value.viewMode)
    }

    @Test
    fun testSetPeriodPreset_UpdatesPreset() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.setPeriodPreset(PeriodPreset.NEXT_7_DAYS)
        advanceUntilIdle()
        assertEquals(PeriodPreset.NEXT_7_DAYS, viewModel.uiState.value.periodPreset)

        viewModel.setPeriodPreset(PeriodPreset.NEXT_30_DAYS)
        advanceUntilIdle()
        assertEquals(PeriodPreset.NEXT_30_DAYS, viewModel.uiState.value.periodPreset)
    }

    @Test
    fun testMonthNavigation_PrevNextToday() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val initialMonth = viewModel.uiState.value.currentMonth

        viewModel.selectNextMonth()
        advanceUntilIdle()
        assertEquals(initialMonth.plusMonths(1), viewModel.uiState.value.currentMonth)

        viewModel.selectPreviousMonth()
        advanceUntilIdle()
        assertEquals(initialMonth, viewModel.uiState.value.currentMonth)

        viewModel.selectToday()
        advanceUntilIdle()
        assertEquals(YearMonth.now(), viewModel.uiState.value.currentMonth)
        assertEquals(LocalDate.now(), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun testTypeFilter_FiltersEventsCorrectly() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Filter only INCOME
        viewModel.setTypeFilter(CalendarFilterType.INCOME)
        advanceUntilIdle()
        assertEquals(CalendarFilterType.INCOME, viewModel.uiState.value.selectedTypeFilter)
        val incomeEvents = viewModel.uiState.value.filteredEvents
        assertEquals(1, incomeEvents.size)
        assertEquals("Salary", incomeEvents.first().title)

        // Filter only BILLS
        viewModel.setTypeFilter(CalendarFilterType.BILLS)
        advanceUntilIdle()
        val billEvents = viewModel.uiState.value.filteredEvents
        assertEquals(1, billEvents.size)
        assertEquals("Electric Bill", billEvents.first().title)
    }

    @Test
    fun testEventDetailsSheet_OpenAndDismiss() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val event = testSummary.allEvents.first()
        viewModel.onEventClicked(event)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEventDetailSheetVisible)
        assertEquals(event, viewModel.uiState.value.selectedEventForDetails)

        viewModel.dismissEventDetails()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isEventDetailSheetVisible)
        assertNull(viewModel.uiState.value.selectedEventForDetails)
    }

    @Test
    fun testConfirmPayDialog_ShowAndDismiss() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val event = testSummary.allEvents.last()
        viewModel.showMarkPaidDialog(event)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isConfirmPayDialogVisible)
        assertEquals(event, viewModel.uiState.value.payingEvent)

        viewModel.dismissMarkPaidDialog()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isConfirmPayDialogVisible)
        assertNull(viewModel.uiState.value.payingEvent)
    }

    @Test
    fun testClearReminderMessage() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.clearReminderMessage()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.reminderMessage)
    }
}

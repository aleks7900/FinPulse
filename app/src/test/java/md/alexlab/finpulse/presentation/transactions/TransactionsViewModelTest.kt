package md.alexlab.finpulse.presentation.transactions

import md.alexlab.finpulse.core.datastore.UserPreferences
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.DateRangePreset
import md.alexlab.finpulse.domain.model.SavedFilter
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionFilterParams
import md.alexlab.finpulse.domain.model.TransactionPresets
import md.alexlab.finpulse.domain.model.TransactionSort
import md.alexlab.finpulse.domain.model.TransactionStatusFilter
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import md.alexlab.finpulse.domain.repository.SavedFilterRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val userPreferencesDataStore: UserPreferencesDataStore = mockk(relaxed = true)
    private val savedFilterRepository: SavedFilterRepository = mockk(relaxed = true)

    private val savedFiltersFlow = MutableStateFlow<List<SavedFilter>>(emptyList())
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())

    private lateinit var viewModel: TransactionsViewModel

    private val sampleTransactions = listOf(
        Transaction(
            id = "tx1",
            amount = Money(4500, "USD"),
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc_bank",
            categoryId = "cat_groceries",
            merchant = "Whole Foods",
            timestamp = 1700000000000L,
            description = "Groceries shopping",
            tags = listOf("food", "groceries")
        ),
        Transaction(
            id = "tx2",
            amount = Money(250000, "USD"),
            type = TransactionType.INCOME,
            sourceAccountId = "acc_bank",
            categoryId = "cat_salary",
            merchant = "Acme Corp",
            timestamp = 1700001000000L,
            description = "Monthly salary"
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { userPreferencesDataStore.userPreferencesFlow } returns flowOf(
            UserPreferences(baseCurrencyCode = "USD", hideBalances = false)
        )
        every { accountRepository.getActiveAccountsFlow() } returns flowOf(
            listOf(
                Account("acc_bank", "Main Checking", AccountType.BANK, Money(500000, "USD"), Money(500000, "USD"))
            )
        )
        every { categoryRepository.getAllCategoriesFlow() } returns flowOf(
            listOf(
                Category("cat_groceries", "Groceries", md.alexlab.finpulse.domain.model.CategoryType.EXPENSE),
                Category("cat_salary", "Salary", md.alexlab.finpulse.domain.model.CategoryType.INCOME)
            )
        )
        every { transactionRepository.getUnreviewedCountFlow() } returns flowOf(3)
        every { transactionRepository.filterTransactionsFlow(any()) } answers { transactionsFlow }
        every { savedFilterRepository.getAllSavedFiltersFlow() } returns savedFiltersFlow

        transactionsFlow.value = sampleTransactions

        viewModel = TransactionsViewModel(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            userPreferencesDataStore = userPreferencesDataStore,
            savedFilterRepository = savedFilterRepository
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
        assertEquals(2, state.transactions.size)
        assertEquals(1, state.accounts.size)
        assertEquals(2, state.categories.size)
        assertEquals(0, state.savedFilters.size)
        assertEquals(0, state.activeFilterCount)
        assertEquals(TransactionSort.DATE_DESC, state.sortOrder)
        assertNull(state.activePresetId)
        assertNull(state.activeSavedFilterId)
        assertEquals(3, state.unreviewedCount)
        assertFalse(state.hideBalances)
        assertEquals("USD", state.baseCurrency)
    }

    @Test
    fun testSearchQueryChange_UpdatesFilterParams() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Whole Foods")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Whole Foods", state.searchQuery)
        assertEquals(1, state.activeFilterCount)
    }

    @Test
    fun testIndividualFilters_CombinedCorrectly() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Account filter
        viewModel.onAccountFilterChange("acc_bank")
        // Category filter
        viewModel.onCategoryFilterChange("cat_groceries")
        // Type filter
        viewModel.onTypeFilterChange(TransactionType.EXPENSE)
        // Date range
        viewModel.onDateRangePresetChange(DateRangePreset.THIS_MONTH)
        // Amount range
        viewModel.onAmountRangeChange(1000L, 50000L)
        // Currency
        viewModel.onCurrencyFilterChange("USD")
        // Status
        viewModel.onStatusFilterChange(TransactionStatusFilter.CONFIRMED)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("acc_bank", state.filterParams.accountId)
        assertEquals("cat_groceries", state.filterParams.categoryId)
        assertEquals(TransactionType.EXPENSE, state.filterParams.type)
        assertEquals(DateRangePreset.THIS_MONTH, state.filterParams.dateRangePreset)
        assertEquals(1000L, state.filterParams.minAmountMinor)
        assertEquals(50000L, state.filterParams.maxAmountMinor)
        assertEquals("USD", state.filterParams.currencyCode)
        assertEquals(TransactionStatusFilter.CONFIRMED, state.filterParams.status)
        assertEquals(7, state.activeFilterCount)
    }

    @Test
    fun testSortOrderChanges() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSortOrderChange(TransactionSort.AMOUNT_DESC)
        advanceUntilIdle()
        assertEquals(TransactionSort.AMOUNT_DESC, viewModel.uiState.value.sortOrder)

        viewModel.onSortOrderChange(TransactionSort.AMOUNT_ASC)
        advanceUntilIdle()
        assertEquals(TransactionSort.AMOUNT_ASC, viewModel.uiState.value.sortOrder)

        viewModel.onSortOrderChange(TransactionSort.DATE_ASC)
        advanceUntilIdle()
        assertEquals(TransactionSort.DATE_ASC, viewModel.uiState.value.sortOrder)

        viewModel.onSortOrderChange(TransactionSort.DATE_DESC)
        advanceUntilIdle()
        assertEquals(TransactionSort.DATE_DESC, viewModel.uiState.value.sortOrder)
    }

    @Test
    fun testSelectPreset_AppliesPresetAndTogglesOff() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Select Subscriptions preset
        viewModel.onSelectPreset(TransactionPresets.SUBSCRIPTIONS)
        advanceUntilIdle()

        assertEquals(TransactionPresets.SUBSCRIPTIONS.id, viewModel.uiState.value.activePresetId)
        assertEquals(TransactionStatusFilter.RECURRING, viewModel.uiState.value.filterParams.status)

        // Select Large Expenses preset
        viewModel.onSelectPreset(TransactionPresets.LARGE_EXPENSES)
        advanceUntilIdle()

        assertEquals(TransactionPresets.LARGE_EXPENSES.id, viewModel.uiState.value.activePresetId)
        assertEquals(TransactionType.EXPENSE, viewModel.uiState.value.filterParams.type)
        assertEquals(100_00L, viewModel.uiState.value.filterParams.minAmountMinor)
        assertEquals(TransactionSort.AMOUNT_DESC, viewModel.uiState.value.sortOrder)

        // Tapping the same preset again toggles it off
        viewModel.onSelectPreset(TransactionPresets.LARGE_EXPENSES)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.activePresetId)
        assertEquals(0, viewModel.uiState.value.activeFilterCount)
    }

    @Test
    fun testSavedFilterPersistence_SaveAndSelectAndDelete() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val savedFilterSlot = slot<SavedFilter>()
        coEvery { savedFilterRepository.saveFilter(capture(savedFilterSlot)) } answers { }

        // Configure some filters
        viewModel.onCategoryFilterChange("cat_groceries")
        viewModel.onTypeFilterChange(TransactionType.EXPENSE)
        advanceUntilIdle()

        // Save view
        viewModel.saveCurrentFilterAsView("Grocery Expenses")
        advanceUntilIdle()

        coVerify { savedFilterRepository.saveFilter(any()) }
        assertEquals("Grocery Expenses", savedFilterSlot.captured.name)
        assertEquals("cat_groceries", savedFilterSlot.captured.params.categoryId)
        assertEquals(TransactionType.EXPENSE, savedFilterSlot.captured.params.type)

        // Simulate repository emission
        val saved = savedFilterSlot.captured
        savedFiltersFlow.value = listOf(saved)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.savedFilters.size)
        assertEquals(saved.id, viewModel.uiState.value.activeSavedFilterId)

        // Deselect saved view by clicking it again
        viewModel.onSelectSavedFilter(saved)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.activeSavedFilterId)
        assertEquals(0, viewModel.uiState.value.activeFilterCount)

        // Select it again
        viewModel.onSelectSavedFilter(saved)
        advanceUntilIdle()
        assertEquals(saved.id, viewModel.uiState.value.activeSavedFilterId)
        assertEquals("cat_groceries", viewModel.uiState.value.filterParams.categoryId)

        // Delete saved view
        viewModel.deleteSavedFilter(saved.id)
        advanceUntilIdle()
        coVerify { savedFilterRepository.deleteFilter(saved.id) }
        assertNull(viewModel.uiState.value.activeSavedFilterId)
    }

    @Test
    fun testResetFilters_RestoresDefaultState() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("test")
        viewModel.onTypeFilterChange(TransactionType.INCOME)
        viewModel.onAmountRangeChange(500L, null)
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.activeFilterCount)

        viewModel.onResetFilters()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertNull(state.filterParams.type)
        assertNull(state.filterParams.minAmountMinor)
        assertEquals(0, state.activeFilterCount)
        assertNull(state.activePresetId)
        assertNull(state.activeSavedFilterId)
    }

    @Test
    fun testToggleFilterOnlyUnreviewed() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.filterOnlyUnreviewed)

        viewModel.toggleFilterOnlyUnreviewed()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.filterOnlyUnreviewed)
        assertEquals(TransactionPresets.UNCATEGORIZED.id, viewModel.uiState.value.activePresetId)

        viewModel.toggleFilterOnlyUnreviewed()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.filterOnlyUnreviewed)
        assertNull(viewModel.uiState.value.activePresetId)
    }

    @Test
    fun testSheetAndDialogVisibilities() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFilterSheetVisible)
        assertFalse(viewModel.uiState.value.isSaveViewDialogVisible)
        assertFalse(viewModel.uiState.value.isAddEditDialogVisible)

        viewModel.showFilterSheet(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFilterSheetVisible)

        viewModel.showSaveViewDialog(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSaveViewDialogVisible)

        viewModel.showAddEditDialog(true, sampleTransactions.first())
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAddEditDialogVisible)
        assertEquals(sampleTransactions.first(), viewModel.uiState.value.editingTransaction)
    }
}

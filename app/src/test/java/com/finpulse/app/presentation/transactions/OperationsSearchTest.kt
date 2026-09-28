package com.finpulse.app.presentation.transactions

import com.finpulse.app.core.datastore.UserPreferences
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.DateRangePreset
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionFilterParams
import com.finpulse.app.domain.model.TransactionPresets
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.SavedFilterRepository
import com.finpulse.app.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OperationsSearchTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val userPreferencesDataStore: UserPreferencesDataStore = mockk(relaxed = true)
    private val savedFilterRepository: SavedFilterRepository = mockk(relaxed = true)

    private val allTransactions = listOf(
        Transaction(
            id = "tx_coffee",
            amount = Money(450, "USD"),
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc_checking",
            categoryId = "cat_food",
            merchant = "Coffee Corner",
            timestamp = 1700000000000L,
            description = "Morning Coffee",
            notes = "Espresso with oat milk",
            tags = listOf("coffee", "morning")
        ),
        Transaction(
            id = "tx_salary",
            amount = Money(500000, "USD"),
            type = TransactionType.INCOME,
            sourceAccountId = "acc_checking",
            categoryId = "cat_salary",
            merchant = "TechCorp Inc",
            timestamp = 1700050000000L,
            description = "Monthly compensation",
            notes = "Direct deposit",
            tags = listOf("salary", "payroll")
        ),
        Transaction(
            id = "tx_market",
            amount = Money(8500, "USD"),
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc_market",
            categoryId = "cat_groceries",
            merchant = "City Supermarket",
            timestamp = 1700100000000L,
            description = "Weekly market groceries",
            notes = "Fresh vegetables",
            tags = listOf("market", "groceries")
        )
    )

    private val categories = listOf(
        Category("cat_food", "Food & Dining", CategoryType.EXPENSE),
        Category("cat_salary", "Salary", CategoryType.INCOME),
        Category("cat_groceries", "Groceries", CategoryType.EXPENSE)
    )

    private val accounts = listOf(
        Account("acc_checking", "Main Checking", AccountType.BANK, Money(1000000, "USD"), Money(1000000, "USD")),
        Account("acc_market", "Market Card", AccountType.CREDIT_CARD, Money(50000, "USD"), Money(50000, "USD"))
    )

    private val currentParamsFlow = MutableStateFlow(TransactionFilterParams())
    private val emittedResultsFlow = MutableStateFlow<List<Transaction>>(emptyList())

    private lateinit var viewModel: TransactionsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { userPreferencesDataStore.userPreferencesFlow } returns flowOf(
            UserPreferences(baseCurrencyCode = "USD", hideBalances = false)
        )
        every { accountRepository.getActiveAccountsFlow() } returns flowOf(accounts)
        every { categoryRepository.getAllCategoriesFlow() } returns flowOf(categories)
        every { transactionRepository.getUnreviewedCountFlow() } returns flowOf(0)
        every { savedFilterRepository.getAllSavedFiltersFlow() } returns flowOf(emptyList())

        // Emulate database search and filter behavior matching TransactionQueryBuilder logic
        every { transactionRepository.filterTransactionsFlow(any()) } answers {
            val params = firstArg<TransactionFilterParams>()
            currentParamsFlow.value = params

            val filtered = allTransactions.filter { tx ->
                var matches = true

                // Search query
                if (params.query.isNotBlank()) {
                    val rawQ = params.query.trim().lowercase()
                    val cat = categories.find { it.id == tx.categoryId }
                    val srcAcc = accounts.find { it.id == tx.sourceAccountId }
                    val dstAcc = accounts.find { it.id == tx.destinationAccountId }

                    val matchesDesc = tx.description?.lowercase()?.contains(rawQ) == true
                    val matchesMerchant = tx.merchant?.lowercase()?.contains(rawQ) == true
                    val matchesNotes = tx.notes?.lowercase()?.contains(rawQ) == true
                    val matchesTags = tx.tags.any { it.lowercase().contains(rawQ) }
                    val matchesCategory = cat?.name?.lowercase()?.contains(rawQ) == true || cat?.id?.lowercase()?.contains(rawQ) == true
                    val matchesSourceAcc = srcAcc?.name?.lowercase()?.contains(rawQ) == true
                    val matchesDestAcc = dstAcc?.name?.lowercase()?.contains(rawQ) == true

                    // Amount matching
                    val cleanedAmount = rawQ.replace("$", "").replace("€", "").replace(",", "").trim()
                    val dbl = cleanedAmount.toDoubleOrNull()
                    val matchesAmount = if (dbl != null && dbl > 0.0) {
                        val minor2 = (dbl * 100).toLong()
                        val minor0 = dbl.toLong()
                        tx.amount.amountMinor == minor2 || tx.amount.amountMinor == minor0
                    } else false

                    matches = matchesDesc || matchesMerchant || matchesNotes || matchesTags ||
                            matchesCategory || matchesSourceAcc || matchesDestAcc || matchesAmount
                }

                // Account filter
                if (matches && !params.accountId.isNullOrBlank()) {
                    matches = tx.sourceAccountId == params.accountId || tx.destinationAccountId == params.accountId
                }

                // Category filter
                if (matches && !params.categoryId.isNullOrBlank()) {
                    matches = tx.categoryId == params.categoryId
                }

                // Date filter
                val (startDate, endDate) = params.effectiveDateRange
                if (matches && startDate != null) {
                    matches = tx.timestamp >= startDate
                }
                if (matches && endDate != null) {
                    matches = tx.timestamp <= endDate
                }

                matches
            }

            emittedResultsFlow.value = filtered
            emittedResultsFlow
        }

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
    fun testEmptyQuery_ReturnsAllApplicableOperations() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals(3, state.transactions.size)
    }

    @Test
    fun testSearchCoffee_Exact_CaseVariants_AndPartial() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // 1. "coffee" -> Coffee operation
        viewModel.onSearchQueryChange("coffee")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("tx_coffee", viewModel.uiState.value.transactions.first().id)

        // 2. "Coffee" -> Coffee operation
        viewModel.onSearchQueryChange("Coffee")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("tx_coffee", viewModel.uiState.value.transactions.first().id)

        // 3. "COFFEE" -> Coffee operation
        viewModel.onSearchQueryChange("COFFEE")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("tx_coffee", viewModel.uiState.value.transactions.first().id)

        // 4. "cof" -> Partial match
        viewModel.onSearchQueryChange("cof")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("tx_coffee", viewModel.uiState.value.transactions.first().id)
    }

    @Test
    fun testSearchCategoryName_Salary() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("salary")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("tx_salary", viewModel.uiState.value.transactions.first().id)
    }

    @Test
    fun testSearchAccountName_Market() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Market Card")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("tx_market", viewModel.uiState.value.transactions.first().id)
    }

    @Test
    fun testSearchAmount_SupportsNumericAndFormattedAmounts() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Search $4.50
        viewModel.onSearchQueryChange("$4.50")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("tx_coffee", viewModel.uiState.value.transactions.first().id)

        // Search 85.00
        viewModel.onSearchQueryChange("85.00")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("tx_market", viewModel.uiState.value.transactions.first().id)
    }

    @Test
    fun testUnknownQuery_ReturnsEmptyResult() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("nonexistent_operation_xyz")
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.transactions.size)
    }

    @Test
    fun testQueryWithLeadingTrailingSpaces_ReturnsCorrectResult() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("   coffee   ")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("tx_coffee", viewModel.uiState.value.transactions.first().id)
    }

    @Test
    fun testSearchCombinedWithCategoryFilter() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Active category filter: Food & Dining
        viewModel.onCategoryFilterChange("cat_food")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.transactions.size)

        // Search for coffee under Food & Dining -> matches tx_coffee
        viewModel.onSearchQueryChange("coffee")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("tx_coffee", viewModel.uiState.value.transactions.first().id)

        // Search for salary under Food & Dining -> intersection is empty
        viewModel.onSearchQueryChange("salary")
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.transactions.size)
    }

    @Test
    fun testSearchCombinedWithAccountFilter() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Filter by Main Checking
        viewModel.onAccountFilterChange("acc_checking")
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.transactions.size)

        // Search within Main Checking for "coffee" -> matches tx_coffee
        viewModel.onSearchQueryChange("coffee")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("tx_coffee", viewModel.uiState.value.transactions.first().id)

        // Search within Main Checking for "market" -> empty intersection
        viewModel.onSearchQueryChange("market")
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.transactions.size)
    }

    @Test
    fun testSearchCombinedWithDateFilter() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Filter by Custom Date Range matching tx_coffee
        viewModel.onDateRangePresetChange(
            preset = DateRangePreset.CUSTOM,
            customStart = 1699990000000L,
            customEnd = 1700010000000L
        )
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("tx_coffee", viewModel.uiState.value.transactions.first().id)

        // Search within this date range for "coffee" -> matches
        viewModel.onSearchQueryChange("coffee")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.transactions.size)

        // Search within this date range for "salary" -> empty
        viewModel.onSearchQueryChange("salary")
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.transactions.size)
    }

    @Test
    fun testClearingSearchQuery_RestoresActiveFilteredResults() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Account filter active
        viewModel.onAccountFilterChange("acc_checking")
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.transactions.size)

        // Search narrowed to 1
        viewModel.onSearchQueryChange("coffee")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.transactions.size)

        // Clear search -> restored back to the 2 account-filtered transactions
        viewModel.onSearchQueryChange("")
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.transactions.size)
    }

    @Test
    fun testPresetSelection_PreservesSearchQuery() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // User enters search query
        viewModel.onSearchQueryChange("coffee")
        advanceUntilIdle()
        assertEquals("coffee", viewModel.uiState.value.searchQuery)

        // User toggles preset "Subscriptions"
        viewModel.onSelectPreset(TransactionPresets.SUBSCRIPTIONS)
        advanceUntilIdle()

        // Search query MUST be preserved
        assertEquals("coffee", viewModel.uiState.value.searchQuery)
        assertEquals(TransactionPresets.SUBSCRIPTIONS.id, viewModel.uiState.value.activePresetId)

        // Toggle off preset
        viewModel.onSelectPreset(TransactionPresets.SUBSCRIPTIONS)
        advanceUntilIdle()
        assertEquals("coffee", viewModel.uiState.value.searchQuery)
        assertEquals(null, viewModel.uiState.value.activePresetId)
    }
}

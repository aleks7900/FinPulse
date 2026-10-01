package md.alexlab.finpulse.presentation.quickadd

import md.alexlab.finpulse.core.datastore.UserPreferences
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.CategoryType
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository
import md.alexlab.finpulse.domain.transaction.FakeAccountRepository
import md.alexlab.finpulse.domain.transaction.FakeTransactionRepository
import md.alexlab.finpulse.domain.usecase.transaction.CreateTransactionUseCase
import md.alexlab.finpulse.domain.usecase.transaction.GetQuickAddSuggestionsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
class QuickAddTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var userPreferencesDataStore: UserPreferencesDataStore

    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var suggestionsUseCase: GetQuickAddSuggestionsUseCase
    private lateinit var viewModel: QuickAddViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        categoryRepository = FakeCategoryRepository()
        userPreferencesDataStore = mockk(relaxed = true)

        every { userPreferencesDataStore.userPreferencesFlow } returns flowOf(
            UserPreferences(
                baseCurrencyCode = "USD",
                lastUsedAccountId = "acc-checking",
                lastUsedCategoryId = "cat-food"
            )
        )
        coEvery { userPreferencesDataStore.setLastUsedTransactionDefaults(any(), any(), any()) } returns Unit

        createTransactionUseCase = CreateTransactionUseCase(transactionRepository, accountRepository)
        suggestionsUseCase = GetQuickAddSuggestionsUseCase(
            transactionRepository,
            categoryRepository,
            accountRepository,
            userPreferencesDataStore
        )

        // Seed initial data
        runTest(testDispatcher) {
            accountRepository.saveAccount(
                Account(
                    id = "acc-checking",
                    name = "Checking",
                    type = AccountType.BANK,
                    balance = Money(150000L, "USD"),
                    availableBalance = Money(150000L, "USD"),
                    isArchived = false
                )
            )
            accountRepository.saveAccount(
                Account(
                    id = "acc-savings",
                    name = "Savings",
                    type = AccountType.SAVINGS,
                    balance = Money(300000L, "USD"),
                    availableBalance = Money(300000L, "USD"),
                    isArchived = false
                )
            )

            categoryRepository.saveCategory(
                Category(
                    id = "cat-food",
                    name = "Food & Dining",
                    type = CategoryType.EXPENSE,
                    icon = "restaurant",
                    colorHex = 0xFFFF5722
                )
            )
            categoryRepository.saveCategory(
                Category(
                    id = "cat-groceries",
                    name = "Groceries",
                    type = CategoryType.EXPENSE,
                    icon = "shopping_cart",
                    colorHex = 0xFF4CAF50
                )
            )
            categoryRepository.saveCategory(
                Category(
                    id = "cat-salary",
                    name = "Salary",
                    type = CategoryType.INCOME,
                    icon = "payments",
                    colorHex = 0xFF2196F3
                )
            )

            // Seed historical transactions for merchant-to-category predictions
            transactionRepository.createTransaction(
                Transaction(
                    id = "tx-hist-1",
                    amount = Money(4500L, "USD"),
                    type = TransactionType.EXPENSE,
                    sourceAccountId = "acc-checking",
                    categoryId = "cat-groceries",
                    merchant = "Trader Joe's",
                    description = "Weekly groceries",
                    timestamp = System.currentTimeMillis() - 86400000L
                )
            )
        }

        viewModel = QuickAddViewModel(
            createTransactionUseCase = createTransactionUseCase,
            suggestionsUseCase = suggestionsUseCase,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            userPreferencesDataStore = userPreferencesDataStore
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testNumpadEntry_DigitsAndBackspace() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onNumpadDigit('1')
        viewModel.onNumpadDigit('2')
        viewModel.onNumpadDigit('.')
        viewModel.onNumpadDigit('5')
        viewModel.onNumpadDigit('0')
        advanceUntilIdle()

        assertEquals("12.50", viewModel.uiState.value.amountInput)
        assertEquals(1250L, viewModel.uiState.value.amountMinor)

        // Multiple decimal points should be ignored
        viewModel.onNumpadDigit('.')
        advanceUntilIdle()
        assertEquals("12.50", viewModel.uiState.value.amountInput)

        // Backspace should remove last character
        viewModel.onNumpadDigit('⌫')
        advanceUntilIdle()
        assertEquals("12.5", viewModel.uiState.value.amountInput)
        assertEquals(1250L, viewModel.uiState.value.amountMinor)
    }

    @Test
    fun testQuickAmountIncrementChips() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onQuickAmountAdd(10) // +$10
        advanceUntilIdle()
        assertEquals("10.00", viewModel.uiState.value.amountInput)
        assertEquals(1000L, viewModel.uiState.value.amountMinor)

        viewModel.onQuickAmountAdd(50) // +$50 -> $60.00
        advanceUntilIdle()
        assertEquals("60.00", viewModel.uiState.value.amountInput)
        assertEquals(6000L, viewModel.uiState.value.amountMinor)
    }

    @Test
    fun testMerchantSelection_PredictsCategory() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Merchant "Trader Joe's" was previously seeded with category "cat-groceries"
        viewModel.onMerchantSelected("Trader Joe's")
        advanceUntilIdle()

        assertEquals("Trader Joe's", viewModel.uiState.value.merchant)
        assertEquals("cat-groceries", viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun testDateChoiceSelection() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(QuickAddDateChoice.TODAY, viewModel.uiState.value.dateChoice)

        viewModel.onDateChoiceSelected(QuickAddDateChoice.YESTERDAY)
        advanceUntilIdle()
        assertEquals(QuickAddDateChoice.YESTERDAY, viewModel.uiState.value.dateChoice)
        assertTrue(viewModel.uiState.value.selectedTimestamp < System.currentTimeMillis())

        val customEpoch = 1700000000000L
        viewModel.onCustomDateEpoch(customEpoch)
        advanceUntilIdle()
        assertEquals(QuickAddDateChoice.CUSTOM, viewModel.uiState.value.dateChoice)
        assertEquals(customEpoch, viewModel.uiState.value.selectedTimestamp)
    }

    @Test
    fun testSaveAndAddAnother_RapidEntryFlow() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // 1. Enter first transaction
        viewModel.onNumpadDigit('1')
        viewModel.onNumpadDigit('5') // $15
        viewModel.onMerchantSelected("Trader Joe's")
        viewModel.onDescriptionTextChange("Dinner items")
        advanceUntilIdle()

        var savedTx1: Transaction? = null
        viewModel.save(andAddAnother = true) { tx ->
            savedTx1 = tx
        }
        advanceUntilIdle()

        assertNotNull(savedTx1)
        assertEquals(1500L, savedTx1?.amount?.amountMinor)

        // After "Save & Add Another", amount and merchant/description should reset to ready-state,
        // but account and category must remain selected for rapid consecutive entries!
        assertEquals("0", viewModel.uiState.value.amountInput)
        assertEquals(0L, viewModel.uiState.value.amountMinor)
        assertEquals("", viewModel.uiState.value.merchant)
        assertEquals("", viewModel.uiState.value.description)
        assertEquals("acc-checking", viewModel.uiState.value.selectedAccountId)
        assertEquals("cat-groceries", viewModel.uiState.value.selectedCategoryId)

        // 2. Immediately enter second transaction with single tap (+20)
        viewModel.onQuickAmountAdd(20) // +$20
        advanceUntilIdle()

        var savedTx2: Transaction? = null
        viewModel.save(andAddAnother = false) { tx ->
            savedTx2 = tx
        }
        advanceUntilIdle()

        assertNotNull(savedTx2)
        assertEquals(2000L, savedTx2?.amount?.amountMinor)
        assertEquals("acc-checking", savedTx2?.sourceAccountId)
        assertEquals("cat-groceries", savedTx2?.categoryId)

        // Both transactions exist in repository
        assertNotNull(transactionRepository.getTransactionById(savedTx1!!.id))
        assertNotNull(transactionRepository.getTransactionById(savedTx2!!.id))
    }

    @Test
    fun testEditMode_LoadForEditAndSave() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val existingTx = Transaction(
            id = "tx-to-edit",
            amount = Money(4200L, "USD"), // $42.00
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-checking",
            categoryId = "cat-food",
            merchant = "Original Diner",
            description = "Breakfast",
            timestamp = 1705000000000L
        )
        transactionRepository.createTransaction(existingTx)

        // Load into ViewModel for edit
        viewModel.loadForEdit(existingTx)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditing)
        assertEquals("tx-to-edit", state.editingTransactionId)
        assertEquals("42.00", state.amountInput)
        assertEquals("Original Diner", state.merchant)
        assertEquals("Breakfast", state.description)

        // Modify amount and merchant
        viewModel.onNumpadDigit('⌫') // drops '0' -> "42.0"
        viewModel.onNumpadDigit('5') // adds '5' -> "42.05"
        viewModel.onMerchantTextChange("Diner & Bakery")
        advanceUntilIdle()

        var updatedTx: Transaction? = null
        viewModel.save(andAddAnother = false) { tx ->
            updatedTx = tx
        }
        advanceUntilIdle()

        assertNotNull(updatedTx)
        assertEquals("tx-to-edit", updatedTx?.id)
        assertEquals(4205L, updatedTx?.amount?.amountMinor)
        assertEquals("Diner & Bakery", updatedTx?.merchant)

        // Verified in repository: updated in place, no duplicate created
        val inRepo = transactionRepository.getTransactionById("tx-to-edit")
        assertEquals(4205L, inRepo?.amount?.amountMinor)
        assertEquals("Diner & Bakery", inRepo?.merchant)
        assertFalse(viewModel.uiState.value.isEditing)
    }

    @Test
    fun testZeroAmount_ShowsError() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        var callbackCalled = false
        viewModel.save(andAddAnother = false) {
            callbackCalled = true
        }
        advanceUntilIdle()

        assertFalse(callbackCalled)
        assertEquals("Enter an amount greater than zero", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testSmartCategorization_WithRule_AutoSelectsCategory() = runTest(testDispatcher) {
        val ruleRepo = md.alexlab.finpulse.domain.categorization.FakeCategorizationRuleRepository()
        val signalRepo = md.alexlab.finpulse.domain.categorization.FakeMerchantSignalRepository()
        val categorizeUseCase = md.alexlab.finpulse.domain.usecase.categorization.CategorizeTransactionUseCase(
            ruleRepository = ruleRepo,
            signalRepository = signalRepo,
            categoryRepository = categoryRepository
        )
        val correctionUseCase = md.alexlab.finpulse.domain.usecase.categorization.RecordCategoryCorrectionUseCase(
            transactionRepository = transactionRepository,
            signalRepository = signalRepo
        )

        ruleRepo.saveRule(
            md.alexlab.finpulse.domain.model.CategorizationRule(
                id = "rule-amazon",
                name = "Amazon Purchases",
                targetCategoryId = "cat-groceries",
                priority = 10,
                merchantPattern = "amazon"
            )
        )

        val smartViewModel = QuickAddViewModel(
            createTransactionUseCase = createTransactionUseCase,
            suggestionsUseCase = suggestionsUseCase,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            userPreferencesDataStore = userPreferencesDataStore,
            categorizeTransactionUseCase = categorizeUseCase,
            recordCategoryCorrectionUseCase = correctionUseCase
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { smartViewModel.uiState.collect() }
        advanceUntilIdle()

        smartViewModel.onMerchantTextChange("Amazon Marketplace Prime")
        advanceUntilIdle()

        assertEquals("cat-groceries", smartViewModel.uiState.value.selectedCategoryId)
        assertEquals(md.alexlab.finpulse.domain.model.CategorizationConfidence.EXACT_RULE, smartViewModel.uiState.value.suggestedConfidence)
        assertFalse(smartViewModel.uiState.value.isCategoryUserLocked)
    }

    @Test
    fun testManualCategorySelection_LocksCategory_NeverOverwrittenBySmartEngine() = runTest(testDispatcher) {
        val ruleRepo = md.alexlab.finpulse.domain.categorization.FakeCategorizationRuleRepository()
        val signalRepo = md.alexlab.finpulse.domain.categorization.FakeMerchantSignalRepository()
        val categorizeUseCase = md.alexlab.finpulse.domain.usecase.categorization.CategorizeTransactionUseCase(
            ruleRepository = ruleRepo,
            signalRepository = signalRepo,
            categoryRepository = categoryRepository
        )

        ruleRepo.saveRule(
            md.alexlab.finpulse.domain.model.CategorizationRule(
                id = "rule-starbucks",
                name = "Starbucks",
                targetCategoryId = "cat-food",
                priority = 10,
                merchantPattern = "starbucks"
            )
        )

        val smartViewModel = QuickAddViewModel(
            createTransactionUseCase = createTransactionUseCase,
            suggestionsUseCase = suggestionsUseCase,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            userPreferencesDataStore = userPreferencesDataStore,
            categorizeTransactionUseCase = categorizeUseCase
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { smartViewModel.uiState.collect() }
        advanceUntilIdle()

        // User explicitly taps category "cat-groceries"
        smartViewModel.onCategorySelected("cat-groceries")
        advanceUntilIdle()
        assertTrue(smartViewModel.uiState.value.isCategoryUserLocked)
        assertEquals("cat-groceries", smartViewModel.uiState.value.selectedCategoryId)

        // Then user types Starbucks (which matches rule for cat-food)
        smartViewModel.onMerchantTextChange("Starbucks Coffee")
        advanceUntilIdle()

        // Category MUST remain cat-groceries because user manually locked it!
        assertEquals("cat-groceries", smartViewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun testSavingTransaction_LearnsDeterministicSignal() = runTest(testDispatcher) {
        val ruleRepo = md.alexlab.finpulse.domain.categorization.FakeCategorizationRuleRepository()
        val signalRepo = md.alexlab.finpulse.domain.categorization.FakeMerchantSignalRepository()
        val categorizeUseCase = md.alexlab.finpulse.domain.usecase.categorization.CategorizeTransactionUseCase(
            ruleRepository = ruleRepo,
            signalRepository = signalRepo,
            categoryRepository = categoryRepository
        )
        val correctionUseCase = md.alexlab.finpulse.domain.usecase.categorization.RecordCategoryCorrectionUseCase(
            transactionRepository = transactionRepository,
            signalRepository = signalRepo
        )

        val smartViewModel = QuickAddViewModel(
            createTransactionUseCase = createTransactionUseCase,
            suggestionsUseCase = suggestionsUseCase,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            userPreferencesDataStore = userPreferencesDataStore,
            categorizeTransactionUseCase = categorizeUseCase,
            recordCategoryCorrectionUseCase = correctionUseCase
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { smartViewModel.uiState.collect() }
        advanceUntilIdle()

        smartViewModel.onNumpadDigit('5')
        smartViewModel.onMerchantTextChange("Patagonia Apparel")
        smartViewModel.onCategorySelected("cat-groceries")
        advanceUntilIdle()

        var saved: Transaction? = null
        smartViewModel.save(andAddAnother = false) { tx -> saved = tx }
        advanceUntilIdle()

        assertNotNull(saved)
        val signal = signalRepo.getSignal("patagonia apparel")
        assertNotNull(signal)
        assertEquals("cat-groceries", signal?.categoryId)
    }
}

class FakeCategoryRepository : CategoryRepository {
    private val categories = MutableStateFlow<Map<String, Category>>(emptyMap())

    override fun getAllCategoriesFlow(): Flow<List<Category>> =
        categories.asStateFlow().map { it.values.toList() }

    override fun getCategoriesByTypeFlow(type: CategoryType): Flow<List<Category>> =
        categories.asStateFlow().map { it.values.filter { cat -> cat.type == type } }

    override suspend fun getCategoryById(id: String): Category? =
        categories.value[id]

    override suspend fun saveCategory(category: Category) {
        categories.value = categories.value + (category.id to category)
    }

    override suspend fun seedDefaultCategoriesIfNeeded() {
        // No-op for tests
    }
}

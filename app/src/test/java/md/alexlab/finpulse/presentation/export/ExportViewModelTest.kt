package md.alexlab.finpulse.presentation.export

import md.alexlab.finpulse.core.datastore.UserPreferences
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.CategoryType
import md.alexlab.finpulse.domain.model.DateRangePreset
import md.alexlab.finpulse.domain.model.backup.BackupEntityCounts
import md.alexlab.finpulse.domain.model.backup.BackupExportPayload
import md.alexlab.finpulse.domain.model.backup.BackupMetadata
import md.alexlab.finpulse.domain.model.backup.BackupValidationResult
import md.alexlab.finpulse.domain.model.backup.FinPulseFullBackup
import md.alexlab.finpulse.domain.model.backup.RestoreSummary
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import md.alexlab.finpulse.domain.usecase.backup.CreateBackupUseCase
import md.alexlab.finpulse.domain.usecase.backup.ExportTransactionsUseCase
import md.alexlab.finpulse.domain.usecase.backup.RestoreBackupUseCase
import md.alexlab.finpulse.domain.usecase.backup.ValidateBackupUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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
class ExportViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private val createBackupUseCase: CreateBackupUseCase = mockk(relaxed = true)
    private val validateBackupUseCase: ValidateBackupUseCase = mockk(relaxed = true)
    private val restoreBackupUseCase: RestoreBackupUseCase = mockk(relaxed = true)
    private val exportTransactionsUseCase: ExportTransactionsUseCase = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val userPreferencesDataStore: UserPreferencesDataStore = mockk(relaxed = true)

    private val userPreferencesFlow = MutableStateFlow(
        UserPreferences(
            lastBackupTimestamp = System.currentTimeMillis() - (10 * 86_400_000L), // 10 days ago
            backupReminderInterval = "WEEKLY"
        )
    )

    private val testAccounts = listOf(
        Account(
            id = "acc-1",
            name = "Main Account",
            type = AccountType.BANK,
            balance = Money(50000L, "USD"),
            availableBalance = Money(50000L, "USD"),
            isArchived = false
        )
    )

    private val testCategories = listOf(
        Category(
            id = "cat-1",
            name = "Dining",
            type = CategoryType.EXPENSE,
            icon = "restaurant",
            colorHex = 0xFFFF5722
        )
    )

    private lateinit var viewModel: ExportViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { userPreferencesDataStore.userPreferencesFlow } returns userPreferencesFlow
        every { accountRepository.getActiveAccountsFlow() } returns flowOf(testAccounts)
        every { categoryRepository.getAllCategoriesFlow() } returns flowOf(testCategories)

        viewModel = ExportViewModel(
            createBackupUseCase = createBackupUseCase,
            validateBackupUseCase = validateBackupUseCase,
            restoreBackupUseCase = restoreBackupUseCase,
            exportTransactionsUseCase = exportTransactionsUseCase,
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
    fun `initial state calculates reminder due when weekly interval has elapsed`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("WEEKLY", state.backupReminderInterval)
        assertTrue(state.isReminderDue) // 10 days elapsed > 7 days
        assertEquals(1, state.accounts.size)
        assertEquals(1, state.categories.size)

        collectJob.cancel()
    }

    @Test
    fun `isReminderDue is false when reminder interval is OFF`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }
        userPreferencesFlow.value = userPreferencesFlow.value.copy(backupReminderInterval = "OFF")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isReminderDue)

        collectJob.cancel()
    }

    @Test
    fun `setReminderInterval delegates to userPreferencesDataStore`() = runTest(testDispatcher) {
        viewModel.setReminderInterval("MONTHLY")
        advanceUntilIdle()

        coVerify(exactly = 1) { userPreferencesDataStore.setBackupReminderInterval("MONTHLY") }
    }

    @Test
    fun `dismissReminder delegates to userPreferencesDataStore`() = runTest(testDispatcher) {
        viewModel.dismissReminder()
        advanceUntilIdle()

        coVerify(exactly = 1) { userPreferencesDataStore.setLastBackupReminderDismissedMillis(any()) }
    }

    @Test
    fun `createBackup invokes use case and triggers onReady callback`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }
        val samplePayload = BackupExportPayload(
            content = "{\"schemaVersion\":6}",
            isEncrypted = false,
            filename = "finpulse_backup.json",
            metadata = BackupMetadata()
        )
        coEvery { createBackupUseCase(null) } returns samplePayload

        var receivedPayload: BackupExportPayload? = null
        viewModel.createBackup(password = null) { payload ->
            receivedPayload = payload
        }
        advanceUntilIdle()

        assertNotNull(receivedPayload)
        assertEquals("finpulse_backup.json", receivedPayload?.filename)
        assertEquals("Backup created successfully", viewModel.uiState.value.successMessage)

        collectJob.cancel()
    }

    @Test
    fun `onFileContentSelectedForRestore triggers password prompt when password is required`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }
        val encryptedContent = "{\"format\":\"FINPULSE_ENCRYPTED_BACKUP\"}"

        every { validateBackupUseCase(encryptedContent, null) } returns BackupValidationResult(
            isValid = false,
            errors = listOf("PASSWORD_REQUIRED")
        )

        viewModel.onFileContentSelectedForRestore(encryptedContent)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isPasswordPromptVisible)
        assertFalse(viewModel.uiState.value.isRestoreConfirmDialogVisible)

        collectJob.cancel()
    }

    @Test
    fun `submitPasswordForRestore validates and opens confirmation dialog`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }
        val encryptedContent = "{\"format\":\"FINPULSE_ENCRYPTED_BACKUP\"}"

        every { validateBackupUseCase(encryptedContent, null) } returns BackupValidationResult(
            isValid = false,
            errors = listOf("PASSWORD_REQUIRED")
        )

        val fullBackup = FinPulseFullBackup(
            metadata = BackupMetadata(counts = BackupEntityCounts(accountsCount = 2)),
            accounts = testAccounts
        )
        every { validateBackupUseCase(encryptedContent, "MySecretPass") } returns BackupValidationResult(
            isValid = true,
            metadata = fullBackup.metadata,
            backupData = fullBackup
        )

        viewModel.onFileContentSelectedForRestore(encryptedContent)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isPasswordPromptVisible)

        viewModel.submitPasswordForRestore("MySecretPass")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPasswordPromptVisible)
        assertTrue(viewModel.uiState.value.isRestoreConfirmDialogVisible)
        assertNotNull(viewModel.uiState.value.validationResult?.backupData)

        collectJob.cancel()
    }

    @Test
    fun `confirmRestore executes restoreUseCase and updates summary and success message`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }
        val fullBackup = FinPulseFullBackup(
            metadata = BackupMetadata(counts = BackupEntityCounts(accountsCount = 1)),
            accounts = testAccounts
        )
        val summary = RestoreSummary(
            metadata = fullBackup.metadata,
            totalRestoredRecords = 1
        )
        coEvery { restoreBackupUseCase(fullBackup) } returns summary

        every { validateBackupUseCase("plain_content", null) } returns BackupValidationResult(
            isValid = true,
            metadata = fullBackup.metadata,
            backupData = fullBackup
        )

        viewModel.onFileContentSelectedForRestore("plain_content")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isRestoreConfirmDialogVisible)

        viewModel.confirmRestore()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRestoreConfirmDialogVisible)
        assertEquals(summary, viewModel.uiState.value.restoreSummary)
        assertTrue(viewModel.uiState.value.successMessage?.contains("Restore completed successfully") == true)

        collectJob.cancel()
    }

    @Test
    fun `filter parameters update correctly`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }

        viewModel.setFilterDatePreset(DateRangePreset.THIS_MONTH)
        viewModel.setSelectedAccount("acc-1")
        advanceUntilIdle()

        val filter = viewModel.uiState.value.filterParams
        assertEquals(DateRangePreset.THIS_MONTH, filter.dateRangePreset)
        assertEquals("acc-1", filter.selectedAccountId)

        collectJob.cancel()
    }
}

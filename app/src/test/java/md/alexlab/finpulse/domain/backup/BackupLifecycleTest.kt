package md.alexlab.finpulse.domain.backup

import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.core.security.BackupCrypto
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.CategoryType
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.model.backup.BackupEntityCounts
import md.alexlab.finpulse.domain.model.backup.BackupMetadata
import md.alexlab.finpulse.domain.model.backup.CURRENT_BACKUP_VERSION
import md.alexlab.finpulse.domain.model.backup.CURRENT_DATABASE_SCHEMA_VERSION
import md.alexlab.finpulse.domain.model.backup.ExportFilterParams
import md.alexlab.finpulse.domain.model.backup.FinPulseFullBackup
import md.alexlab.finpulse.domain.model.backup.RestoreSummary
import md.alexlab.finpulse.domain.repository.BackupRepository
import md.alexlab.finpulse.domain.usecase.backup.CreateBackupUseCase
import md.alexlab.finpulse.domain.usecase.backup.RestoreBackupUseCase
import md.alexlab.finpulse.domain.model.Budget
import md.alexlab.finpulse.domain.model.BudgetPeriod
import md.alexlab.finpulse.domain.model.RecurringTransaction
import md.alexlab.finpulse.domain.model.PaymentFrequency
import md.alexlab.finpulse.domain.model.CustomIntervalUnit
import md.alexlab.finpulse.domain.model.RecurringOccurrence
import md.alexlab.finpulse.domain.model.OccurrenceStatus
import md.alexlab.finpulse.domain.model.FinancialGoal
import md.alexlab.finpulse.domain.model.InvestmentAsset
import md.alexlab.finpulse.domain.model.AssetClass
import md.alexlab.finpulse.domain.model.Debt
import md.alexlab.finpulse.domain.model.DebtType
import md.alexlab.finpulse.domain.model.CategorizationRule
import md.alexlab.finpulse.domain.model.MatchType
import md.alexlab.finpulse.domain.model.SavedFilter
import md.alexlab.finpulse.domain.model.TransactionFilterParams
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupLifecycleTest {

    private lateinit var backupRepository: FakeLifecycleBackupRepository
    private lateinit var userPreferencesDataStore: UserPreferencesDataStore
    private lateinit var createBackupUseCase: CreateBackupUseCase
    private lateinit var restoreBackupUseCase: RestoreBackupUseCase

    private val json = Json { ignoreUnknownKeys = true }

    private val sampleBackup = FinPulseFullBackup(
        metadata = BackupMetadata(
            backupVersion = CURRENT_BACKUP_VERSION,
            appVersion = "1.0.0",
            schemaVersion = CURRENT_DATABASE_SCHEMA_VERSION,
            createdAt = 1718000000000L,
            counts = BackupEntityCounts(accountsCount = 1, categoriesCount = 1, transactionsCount = 1)
        ),
        accounts = listOf(
            Account(
                id = "acc-1",
                name = "Main",
                type = AccountType.BANK,
                balance = Money(50000L, "USD"),
                availableBalance = Money(50000L, "USD"),
                isArchived = false
            )
        ),
        categories = listOf(
            Category(
                id = "cat-1",
                name = "Food",
                type = CategoryType.EXPENSE,
                icon = "fastfood",
                colorHex = 0xFFFF5722
            )
        ),
        transactions = listOf(
            Transaction(
                id = "tx-1",
                sourceAccountId = "acc-1",
                categoryId = "cat-1",
                amount = Money(1200L, "USD"),
                type = TransactionType.EXPENSE,
                timestamp = 1718000000000L,
                merchant = "Burger Joint",
                description = "Lunch"
            )
        )
    )

    @Before
    fun setup() {
        backupRepository = FakeLifecycleBackupRepository(sampleBackup)
        userPreferencesDataStore = mockk(relaxed = true)
        createBackupUseCase = CreateBackupUseCase(backupRepository, userPreferencesDataStore)
        restoreBackupUseCase = RestoreBackupUseCase(backupRepository)
    }

    @Test
    fun `createBackup creates unencrypted JSON payload and records timestamp`() = runBlocking {
        val payload = createBackupUseCase(password = null)

        assertFalse(payload.isEncrypted)
        assertTrue(payload.filename.endsWith(".json"))
        assertFalse(payload.filename.contains("_encrypted"))
        assertEquals(CURRENT_BACKUP_VERSION, payload.metadata.backupVersion)
        assertEquals(CURRENT_DATABASE_SCHEMA_VERSION, payload.metadata.schemaVersion)

        // Verifies raw JSON deserialization
        val parsedBackup = json.decodeFromString<FinPulseFullBackup>(payload.content)
        assertEquals(1, parsedBackup.accounts.size)
        assertEquals(1, parsedBackup.categories.size)
        assertEquals(1, parsedBackup.transactions.size)
        assertEquals("Burger Joint", parsedBackup.transactions[0].merchant)

        // Preferences updated
        coVerify(exactly = 1) { userPreferencesDataStore.setLastBackupTimestamp(payload.metadata.createdAt) }
    }

    @Test
    fun `createBackup with password creates AES-256-GCM encrypted backup container`() = runBlocking {
        val password = "MySecurePassword2026"
        val payload = createBackupUseCase(password = password)

        assertTrue(payload.isEncrypted)
        assertTrue(payload.filename.contains("_encrypted.json"))

        // Verifies container is encrypted and decryptable
        assertTrue(BackupCrypto.isEncryptedBackup(payload.content))
        val container = BackupCrypto.parseEncryptedContainer(payload.content)
        assertNotNull(container)
        assertEquals("AES-256-GCM", container?.algorithm)

        val decryptedJson = BackupCrypto.decrypt(container!!, password.toCharArray())
        val parsedBackup = json.decodeFromString<FinPulseFullBackup>(decryptedJson)
        assertEquals(1, parsedBackup.accounts.size)
        assertEquals("Main", parsedBackup.accounts[0].name)
    }

    @Test
    fun `restoreBackup delegates to repository and returns restore summary`() = runBlocking {
        val summary = restoreBackupUseCase(sampleBackup)

        assertEquals(3, summary.totalRestoredRecords)
        assertEquals(sampleBackup.metadata, summary.metadata)
        assertEquals(sampleBackup, backupRepository.lastRestoredBackup)
    }

    @Test
    fun `serialization of complete FinPulseFullBackup with all entity types`() {
        val full = FinPulseFullBackup(
            metadata = BackupMetadata(),
            accounts = listOf(Account("a1", "A", AccountType.CASH, Money(100), Money(100))),
            categories = listOf(Category("c1", "C", CategoryType.EXPENSE)),
            transactions = listOf(Transaction("t1", Money(100), TransactionType.EXPENSE, "a1", null, "c1")),
            budgets = listOf(Budget("b1", "c1", "B", Money(100), BudgetPeriod.MONTHLY, 0L, 1000L)),
            recurringRules = listOf(RecurringTransaction("r1", "R", Money(100), TransactionType.EXPENSE, "a1", null, "c1", PaymentFrequency.MONTHLY, 1, CustomIntervalUnit.MONTHS, 1, 0L)),
            recurringOccurrences = listOf(RecurringOccurrence("o1", "r1", "R", Money(100), TransactionType.EXPENSE, "a1", null, "c1", 0L, OccurrenceStatus.EXPECTED)),
            financialGoals = listOf(FinancialGoal("g1", "G", Money(1000), Money(100), 10000L)),
            assets = listOf(InvestmentAsset("ast1", "Stock", "AAPL", AssetClass.STOCK, 1.0, Money(150), Money(160))),
            debts = listOf(Debt("d1", "Debt", DebtType.CREDIT_CARD, Money(1000), Money(500), 12.0, Money(50), 10000L)),
            categorizationRules = listOf(CategorizationRule("cr1", "Rule", "c1", 1, "test", MatchType.CONTAINS)),
            savedFilters = listOf(SavedFilter("sf1", "Filter", TransactionFilterParams()))
        )
        val encoded = json.encodeToString(FinPulseFullBackup.serializer(), full)
        val decoded = json.decodeFromString<FinPulseFullBackup>(encoded)
        assertEquals(full.budgets.size, decoded.budgets.size)
        assertEquals(full.debts.size, decoded.debts.size)
        assertEquals(full.categorizationRules.size, decoded.categorizationRules.size)
    }

    private class FakeLifecycleBackupRepository(
        private val backupToReturn: FinPulseFullBackup
    ) : BackupRepository {
        var lastRestoredBackup: FinPulseFullBackup? = null

        override suspend fun createFullBackup(): FinPulseFullBackup {
            return backupToReturn
        }

        override suspend fun restoreFullBackup(backup: FinPulseFullBackup): RestoreSummary {
            lastRestoredBackup = backup
            return RestoreSummary(
                restoredAt = System.currentTimeMillis(),
                metadata = backup.metadata,
                totalRestoredRecords = backup.metadata.counts.totalRecords
            )
        }

        override suspend fun getFilteredTransactions(params: ExportFilterParams): List<Transaction> {
            return emptyList()
        }
    }
}

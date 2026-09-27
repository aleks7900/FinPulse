package com.finpulse.app.domain.backup

import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.model.Money
import com.finpulse.app.core.security.BackupCrypto
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.model.backup.BackupEntityCounts
import com.finpulse.app.domain.model.backup.BackupMetadata
import com.finpulse.app.domain.model.backup.CURRENT_BACKUP_VERSION
import com.finpulse.app.domain.model.backup.CURRENT_DATABASE_SCHEMA_VERSION
import com.finpulse.app.domain.model.backup.ExportFilterParams
import com.finpulse.app.domain.model.backup.FinPulseFullBackup
import com.finpulse.app.domain.model.backup.RestoreSummary
import com.finpulse.app.domain.repository.BackupRepository
import com.finpulse.app.domain.usecase.backup.CreateBackupUseCase
import com.finpulse.app.domain.usecase.backup.RestoreBackupUseCase
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

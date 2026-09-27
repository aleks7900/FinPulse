package com.finpulse.app.domain.backup

import com.finpulse.app.core.model.Money
import com.finpulse.app.core.security.BackupCrypto
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.model.backup.BackupEntityCounts
import com.finpulse.app.domain.model.backup.BackupMetadata
import com.finpulse.app.domain.model.backup.CURRENT_BACKUP_VERSION
import com.finpulse.app.domain.model.backup.CURRENT_DATABASE_SCHEMA_VERSION
import com.finpulse.app.domain.model.backup.EncryptedBackupContainer
import com.finpulse.app.domain.model.backup.FinPulseFullBackup
import com.finpulse.app.domain.usecase.backup.ValidateBackupUseCase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ValidateBackupUseCaseTest {

    private lateinit var useCase: ValidateBackupUseCase
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val testAccount = Account(
        id = "acc-1",
        name = "Checking Account",
        type = AccountType.BANK,
        balance = Money(50000L, "USD"),
        availableBalance = Money(50000L, "USD"),
        isArchived = false
    )

    private val testCategory = Category(
        id = "cat-1",
        name = "Groceries",
        type = com.finpulse.app.domain.model.CategoryType.EXPENSE,
        icon = "shopping_cart",
        colorHex = 0xFF4CAF50
    )

    private val testTransaction = Transaction(
        id = "tx-1",
        sourceAccountId = "acc-1",
        categoryId = "cat-1",
        amount = Money(2500L, "USD"),
        type = TransactionType.EXPENSE,
        timestamp = Instant.now().toEpochMilli(),
        merchant = "Supermarket",
        description = "Weekly shopping"
    )

    @Before
    fun setup() {
        useCase = ValidateBackupUseCase()
    }

    private fun createSampleBackup(
        backupVersion: Int = CURRENT_BACKUP_VERSION,
        schemaVersion: Int = CURRENT_DATABASE_SCHEMA_VERSION,
        accounts: List<Account> = listOf(testAccount),
        categories: List<Category> = listOf(testCategory),
        transactions: List<Transaction> = listOf(testTransaction),
        tamperChecksum: Boolean = false
    ): FinPulseFullBackup {
        val createdAt = 1700000000000L
        val counts = BackupEntityCounts(
            accountsCount = accounts.size,
            categoriesCount = categories.size,
            transactionsCount = transactions.size
        )
        val manifest = "$createdAt:${counts.totalRecords}:${transactions.size}:${accounts.size}"
        val checksum = if (tamperChecksum) "invalid_checksum_hash" else BackupCrypto.computeSha256(manifest)

        val metadata = BackupMetadata(
            backupVersion = backupVersion,
            appVersion = "1.0.0",
            schemaVersion = schemaVersion,
            createdAt = createdAt,
            checksum = checksum,
            counts = counts
        )

        return FinPulseFullBackup(
            metadata = metadata,
            accounts = accounts,
            categories = categories,
            transactions = transactions
        )
    }

    @Test
    fun `validate returns error on empty or blank content`() {
        val resultBlank = useCase("")
        assertFalse(resultBlank.isValid)
        assertTrue(resultBlank.errors.contains("EMPTY_CONTENT"))

        val resultWhitespace = useCase("   \n\t  ")
        assertFalse(resultWhitespace.isValid)
        assertTrue(resultWhitespace.errors.contains("EMPTY_CONTENT"))
    }

    @Test
    fun `validate returns error on invalid json structure`() {
        val result = useCase("{ this is not valid json }")
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.startsWith("INVALID_JSON_STRUCTURE") })
    }

    @Test
    fun `validate passes for valid plaintext backup`() {
        val backup = createSampleBackup()
        val rawJson = json.encodeToString(backup)

        val result = useCase(rawJson)

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
        assertNotNull(result.backupData)
        assertEquals(CURRENT_BACKUP_VERSION, result.metadata?.backupVersion)
        assertEquals(CURRENT_DATABASE_SCHEMA_VERSION, result.metadata?.schemaVersion)
        assertEquals(1, result.backupData?.accounts?.size)
        assertEquals(1, result.backupData?.categories?.size)
        assertEquals(1, result.backupData?.transactions?.size)
    }

    @Test
    fun `validate rejects backup with newer backupVersion`() {
        val futureBackup = createSampleBackup(backupVersion = CURRENT_BACKUP_VERSION + 1)
        val rawJson = json.encodeToString(futureBackup)

        val result = useCase(rawJson)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.startsWith("UNSUPPORTED_BACKUP_VERSION") })
        assertNull(result.backupData)
    }

    @Test
    fun `validate rejects backup with newer schemaVersion`() {
        val futureSchemaBackup = createSampleBackup(schemaVersion = CURRENT_DATABASE_SCHEMA_VERSION + 1)
        val rawJson = json.encodeToString(futureSchemaBackup)

        val result = useCase(rawJson)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.startsWith("UNSUPPORTED_SCHEMA_VERSION") })
        assertNull(result.backupData)
    }

    @Test
    fun `validate warns when checksum does not match manifest`() {
        val tamperedBackup = createSampleBackup(tamperChecksum = true)
        val rawJson = json.encodeToString(tamperedBackup)

        val result = useCase(rawJson)

        // Checksum mismatch produces a warning
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.startsWith("CHECKSUM_MISMATCH") })
    }

    @Test
    fun `validate warns when transactions reference unknown accounts or categories`() {
        val orphanedTx = testTransaction.copy(
            id = "tx-orphan",
            sourceAccountId = "acc-unknown",
            categoryId = "cat-unknown"
        )
        val backup = createSampleBackup(transactions = listOf(orphanedTx))
        val rawJson = json.encodeToString(backup)

        val result = useCase(rawJson)

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.startsWith("ORPHANED_ACCOUNTS") })
        assertTrue(result.warnings.any { it.startsWith("ORPHANED_CATEGORIES") })
    }

    @Test
    fun `validate reports error if account contains empty ID`() {
        val corruptedAccount = testAccount.copy(id = "")
        val backup = createSampleBackup(accounts = listOf(corruptedAccount))
        val rawJson = json.encodeToString(backup)

        val result = useCase(rawJson)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.startsWith("CORRUPTED_ACCOUNT") })
    }

    @Test
    fun `validate encrypted backup requires password`() {
        val backup = createSampleBackup()
        val rawJson = json.encodeToString(backup)
        val container = BackupCrypto.encrypt(rawJson, "SecretPass".toCharArray())
        val containerJson = json.encodeToString(container)

        // No password provided
        val resultNoPass = useCase(containerJson, password = null)
        assertFalse(resultNoPass.isValid)
        assertTrue(resultNoPass.errors.contains("PASSWORD_REQUIRED"))

        // Blank password provided
        val resultBlankPass = useCase(containerJson, password = "   ")
        assertFalse(resultBlankPass.isValid)
        assertTrue(resultBlankPass.errors.contains("PASSWORD_REQUIRED"))
    }

    @Test
    fun `validate encrypted backup fails with invalid password`() {
        val backup = createSampleBackup()
        val rawJson = json.encodeToString(backup)
        val container = BackupCrypto.encrypt(rawJson, "CorrectPass123".toCharArray())
        val containerJson = json.encodeToString(container)

        val resultWrongPass = useCase(containerJson, password = "WrongPass456")
        assertFalse(resultWrongPass.isValid)
        assertTrue(resultWrongPass.errors.contains("INVALID_PASSWORD"))
    }

    @Test
    fun `validate encrypted backup succeeds with correct password`() {
        val backup = createSampleBackup()
        val rawJson = json.encodeToString(backup)
        val password = "CorrectPass123"
        val container = BackupCrypto.encrypt(rawJson, password.toCharArray())
        val containerJson = json.encodeToString(container)

        val result = useCase(containerJson, password = password)
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertNotNull(result.backupData)
        assertEquals(1, result.backupData?.transactions?.size)
        assertEquals("acc-1", result.backupData?.accounts?.firstOrNull()?.id)
    }
}

package com.finpulse.app.domain.usecase.backup

import com.finpulse.app.core.security.BackupCrypto
import com.finpulse.app.domain.model.backup.BackupValidationResult
import com.finpulse.app.domain.model.backup.CURRENT_BACKUP_VERSION
import com.finpulse.app.domain.model.backup.FinPulseFullBackup
import kotlinx.serialization.json.Json

class ValidateBackupUseCase {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    operator fun invoke(rawContent: String, password: String? = null): BackupValidationResult {
        if (rawContent.isBlank()) {
            return BackupValidationResult(
                isValid = false,
                errors = listOf("EMPTY_CONTENT")
            )
        }

        // Check if content is an encrypted backup container
        val isEncrypted = BackupCrypto.isEncryptedBackup(rawContent)
        val plainJson: String = if (isEncrypted) {
            if (password.isNullOrBlank()) {
                return BackupValidationResult(
                    isValid = false,
                    errors = listOf("PASSWORD_REQUIRED")
                )
            }

            val container = BackupCrypto.parseEncryptedContainer(rawContent)
                ?: return BackupValidationResult(
                    isValid = false,
                    errors = listOf("MALFORMED_ENCRYPTED_BACKUP")
                )

            try {
                BackupCrypto.decrypt(container, password.toCharArray())
            } catch (_: Exception) {
                return BackupValidationResult(
                    isValid = false,
                    errors = listOf("INVALID_PASSWORD")
                )
            }
        } else {
            rawContent
        }

        // Decode JSON into FinPulseFullBackup
        val backup: FinPulseFullBackup = try {
            json.decodeFromString<FinPulseFullBackup>(plainJson)
        } catch (e: Exception) {
            return BackupValidationResult(
                isValid = false,
                errors = listOf("INVALID_JSON_STRUCTURE: ${e.message ?: "Could not parse backup JSON"}")
            )
        }

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // 1. Version Compatibility Strategy
        if (backup.metadata.backupVersion > CURRENT_BACKUP_VERSION) {
            errors.add("UNSUPPORTED_BACKUP_VERSION: Backup version ${backup.metadata.backupVersion} is newer than current app version $CURRENT_BACKUP_VERSION. Please update FinPulse.")
        }
        if (backup.metadata.schemaVersion > com.finpulse.app.domain.model.backup.CURRENT_DATABASE_SCHEMA_VERSION) {
            errors.add("UNSUPPORTED_SCHEMA_VERSION: Database schema version ${backup.metadata.schemaVersion} is newer than current app schema ${com.finpulse.app.domain.model.backup.CURRENT_DATABASE_SCHEMA_VERSION}. Please update FinPulse to restore this backup.")
        }

        // 2. Data Integrity Validations
        val knownAccountIds = backup.accounts.map { it.id }.toSet()
        val knownCategoryIds = backup.categories.map { it.id }.toSet()

        if (backup.metadata.counts.totalRecords == 0 && backup.transactions.isEmpty() && backup.accounts.isEmpty()) {
            warnings.add("EMPTY_BACKUP: The backup contains 0 records.")
        }

        // Validate account records
        for (account in backup.accounts) {
            if (account.id.isBlank()) {
                errors.add("CORRUPTED_ACCOUNT: Account contains empty ID")
            }
            if (account.name.isBlank()) {
                warnings.add("ACCOUNT_BLANK_NAME: Account ${account.id} has no name")
            }
        }

        // Validate category records
        for (category in backup.categories) {
            if (category.id.isBlank()) {
                errors.add("CORRUPTED_CATEGORY: Category contains empty ID")
            }
        }

        // Validate transaction records & foreign keys
        var orphanedAccountsCount = 0
        var orphanedCategoriesCount = 0

        for (tx in backup.transactions) {
            if (tx.id.isBlank()) {
                errors.add("CORRUPTED_TRANSACTION: Transaction contains empty ID")
            }
            if (tx.sourceAccountId.isNotBlank() && !knownAccountIds.contains(tx.sourceAccountId)) {
                orphanedAccountsCount++
            }
            if (tx.categoryId.isNotBlank() && !knownCategoryIds.contains(tx.categoryId)) {
                orphanedCategoriesCount++
            }
        }

        if (orphanedAccountsCount > 0) {
            warnings.add("ORPHANED_ACCOUNTS: $orphanedAccountsCount transactions reference account IDs not found in backup accounts.")
        }
        if (orphanedCategoriesCount > 0) {
            warnings.add("ORPHANED_CATEGORIES: $orphanedCategoriesCount transactions reference category IDs not found in backup categories.")
        }

        // 3. Checksum verification
        if (backup.metadata.checksum.isNotBlank()) {
            val manifest = "${backup.metadata.createdAt}:${backup.metadata.counts.totalRecords}:${backup.transactions.size}:${backup.accounts.size}"
            val expectedHash = BackupCrypto.computeSha256(manifest)
            if (expectedHash != backup.metadata.checksum) {
                warnings.add("CHECKSUM_MISMATCH: Payload manifest checksum does not match metadata.")
            }
        }

        val isValid = errors.isEmpty()

        return BackupValidationResult(
            isValid = isValid,
            metadata = backup.metadata,
            warnings = warnings,
            errors = errors,
            backupData = if (isValid) backup else null
        )
    }
}

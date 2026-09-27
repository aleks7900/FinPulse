package com.finpulse.app.domain.usecase.backup

import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.security.BackupCrypto
import com.finpulse.app.domain.model.backup.BackupExportPayload
import com.finpulse.app.domain.model.backup.BackupMetadata
import com.finpulse.app.domain.model.backup.EncryptedBackupContainer
import com.finpulse.app.domain.model.backup.FinPulseFullBackup
import com.finpulse.app.domain.repository.BackupRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CreateBackupUseCase(
    private val backupRepository: BackupRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend operator fun invoke(password: String? = null): BackupExportPayload {
        val fullBackup = backupRepository.createFullBackup()
        val rawJson = json.encodeToString(fullBackup)

        val timestamp = fullBackup.metadata.createdAt
        val isEncrypted = !password.isNullOrBlank()

        val (content, filename) = if (isEncrypted) {
            val container = BackupCrypto.encrypt(rawJson, password!!.toCharArray())
            val containerJson = json.encodeToString<EncryptedBackupContainer>(container)
            Pair(containerJson, "finpulse_backup_${timestamp}_encrypted.json")
        } else {
            Pair(rawJson, "finpulse_backup_${timestamp}.json")
        }

        userPreferencesDataStore.setLastBackupTimestamp(timestamp)

        return BackupExportPayload(
            content = content,
            isEncrypted = isEncrypted,
            filename = filename,
            metadata = fullBackup.metadata
        )
    }
}

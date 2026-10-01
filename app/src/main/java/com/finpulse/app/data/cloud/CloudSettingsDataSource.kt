package com.finpulse.app.data.cloud

import com.finpulse.app.domain.model.sync.CloudSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface CloudSettingsDataSource {
    suspend fun fetchCloudSettings(uid: String): Result<CloudSettings?>
    suspend fun saveCloudSettings(uid: String, settings: CloudSettings): Result<Unit>
    suspend fun deleteCloudSettings(uid: String): Result<Unit>
}

class FirestoreCloudSettingsDataSource(
    private val cloudStorage: CloudStorageDataSource,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : CloudSettingsDataSource {

    override suspend fun fetchCloudSettings(uid: String): Result<CloudSettings?> = runCatching {
        val records = cloudStorage.downloadRecords(
            uid = uid,
            collection = CloudSettings.SETTINGS_COLLECTION,
            sinceTimestamp = 0L
        ).getOrThrow()

        val record = records.firstOrNull { it.id == CloudSettings.SETTINGS_DOCUMENT_ID && !it.isDeleted }
            ?: return@runCatching null

        if (record.jsonPayload.isBlank()) return@runCatching null

        json.decodeFromString<CloudSettings>(record.jsonPayload)
    }

    override suspend fun saveCloudSettings(uid: String, settings: CloudSettings): Result<Unit> = runCatching {
        val payload = json.encodeToString(settings)
        val record = CloudEntityRecord(
            id = CloudSettings.SETTINGS_DOCUMENT_ID,
            collection = CloudSettings.SETTINGS_COLLECTION,
            jsonPayload = payload,
            updatedAt = settings.updatedAt
        )
        cloudStorage.uploadRecords(
            uid = uid,
            collection = CloudSettings.SETTINGS_COLLECTION,
            records = listOf(record)
        ).getOrThrow()
        Unit
    }

    override suspend fun deleteCloudSettings(uid: String): Result<Unit> = runCatching {
        cloudStorage.recordTombstone(
            uid = uid,
            collection = CloudSettings.SETTINGS_COLLECTION,
            id = CloudSettings.SETTINGS_DOCUMENT_ID,
            deletedAt = System.currentTimeMillis()
        ).getOrThrow()
    }
}

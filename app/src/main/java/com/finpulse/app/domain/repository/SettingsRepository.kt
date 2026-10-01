package com.finpulse.app.domain.repository

import com.finpulse.app.domain.model.sync.CloudSettings
import com.finpulse.app.domain.model.sync.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed class SettingsSyncOutcome {
    data class RestoredFromCloud(val settings: CloudSettings) : SettingsSyncOutcome()
    data class UploadedToCloud(val settings: CloudSettings) : SettingsSyncOutcome()
    data class UpToDate(val settings: CloudSettings) : SettingsSyncOutcome()
}

interface SettingsRepository {
    val currentSettings: Flow<CloudSettings>
    val syncStatus: StateFlow<SyncStatus>
    val lastSyncedTimestamp: StateFlow<Long>

    suspend fun synchronize(uid: String, forceUpload: Boolean = false): Result<SettingsSyncOutcome>
    suspend fun deleteCloudSettings(uid: String): Result<Unit>
    suspend fun resetLocalTimestamp()
}

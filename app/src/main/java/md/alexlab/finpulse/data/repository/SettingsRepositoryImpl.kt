package md.alexlab.finpulse.data.repository

import android.os.Build
import md.alexlab.finpulse.data.cloud.CloudSettingsDataSource
import md.alexlab.finpulse.data.cloud.LocalSettingsDataSource
import md.alexlab.finpulse.domain.model.sync.CloudSettings
import md.alexlab.finpulse.domain.model.sync.SyncStatus
import md.alexlab.finpulse.domain.repository.SettingsRepository
import md.alexlab.finpulse.domain.repository.SettingsSyncOutcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SettingsRepositoryImpl(
    private val localDataSource: LocalSettingsDataSource,
    private val cloudDataSource: CloudSettingsDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : SettingsRepository {

    override val currentSettings: Flow<CloudSettings> = localDataSource.getSettingsFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncedTimestamp = MutableStateFlow(0L)
    override val lastSyncedTimestamp: StateFlow<Long> = _lastSyncedTimestamp.asStateFlow()

    override suspend fun synchronize(uid: String, forceUpload: Boolean): Result<SettingsSyncOutcome> = withContext(dispatcher) {
        _syncStatus.value = SyncStatus.SYNCING
        try {
            val local = localDataSource.getSettings()
            val remote = cloudDataSource.fetchCloudSettings(uid).getOrThrow()

            val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

            val outcome = when {
                forceUpload || remote == null -> {
                    // No remote settings exist or forced upload: push local
                    val toUpload = local.copy(
                        updatedAt = if (local.updatedAt == 0L) System.currentTimeMillis() else local.updatedAt,
                        deviceModel = deviceModel
                    )
                    cloudDataSource.saveCloudSettings(uid, toUpload).getOrThrow()
                    SettingsSyncOutcome.UploadedToCloud(toUpload)
                }
                local.updatedAt == 0L -> {
                    // Virgin / fresh installation on new device: restore cloud settings unconditionally
                    localDataSource.restoreSettings(remote)
                    SettingsSyncOutcome.RestoredFromCloud(remote)
                }
                remote.updatedAt > local.updatedAt -> {
                    // Remote has newer changes: restore to local DataStore
                    localDataSource.restoreSettings(remote)
                    SettingsSyncOutcome.RestoredFromCloud(remote)
                }
                local.updatedAt > remote.updatedAt -> {
                    // Local changes are newer: push to cloud
                    val toUpload = local.copy(deviceModel = deviceModel)
                    cloudDataSource.saveCloudSettings(uid, toUpload).getOrThrow()
                    SettingsSyncOutcome.UploadedToCloud(toUpload)
                }
                else -> {
                    // Timestamps are identical
                    SettingsSyncOutcome.UpToDate(local)
                }
            }

            val now = System.currentTimeMillis()
            _lastSyncedTimestamp.value = now
            _syncStatus.value = SyncStatus.SUCCESS
            Result.success(outcome)
        } catch (t: Throwable) {
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(t)
        }
    }

    override suspend fun deleteCloudSettings(uid: String): Result<Unit> = withContext(dispatcher) {
        _syncStatus.value = SyncStatus.SYNCING
        try {
            cloudDataSource.deleteCloudSettings(uid).getOrThrow()
            _syncStatus.value = SyncStatus.IDLE
            Result.success(Unit)
        } catch (t: Throwable) {
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(t)
        }
    }

    override suspend fun resetLocalTimestamp() {
        localDataSource.resetSettingsTimestamp()
    }
}

package com.finpulse.app.domain.repository

import com.finpulse.app.domain.model.sync.SyncResult
import com.finpulse.app.domain.model.sync.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CloudSyncRepository {
    val syncStatus: StateFlow<SyncStatus>
    val lastSyncTimestamp: StateFlow<Long>
    val pendingChangesCount: Flow<Int>

    suspend fun performFullSync(): Result<SyncResult>
    suspend fun uploadPendingChanges(): Result<SyncResult>
    suspend fun downloadRemoteChanges(): Result<SyncResult>
    suspend fun migrateLocalDataToCloud(uid: String): Result<SyncResult>
    suspend fun restoreCloudDataToLocal(uid: String): Result<SyncResult>
    suspend fun handleAccountSwitch(previousUid: String?, newUid: String): Result<Unit>
    suspend fun clearLocalData()
    suspend fun deleteCloudData(): Result<Unit>
    suspend fun syncSettings(): Result<Unit>
}


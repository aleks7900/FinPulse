package md.alexlab.finpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import md.alexlab.finpulse.core.database.entity.SyncRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncRecord(record: SyncRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncRecords(records: List<SyncRecordEntity>)

    @Query("SELECT * FROM sync_records WHERE syncStatus IN ('PENDING_UPSERT', 'FAILED') AND isDeleted = 0")
    suspend fun getPendingUpserts(): List<SyncRecordEntity>

    @Query("SELECT * FROM sync_records WHERE isDeleted = 1 AND syncStatus IN ('PENDING_DELETE', 'FAILED')")
    suspend fun getPendingDeletes(): List<SyncRecordEntity>

    @Query("SELECT COUNT(*) FROM sync_records WHERE syncStatus IN ('PENDING_UPSERT', 'PENDING_DELETE', 'FAILED')")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_records WHERE syncStatus IN ('PENDING_UPSERT', 'PENDING_DELETE', 'FAILED')")
    suspend fun getPendingCount(): Int

    @Query("SELECT * FROM sync_records WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun getSyncRecord(entityType: String, entityId: String): SyncRecordEntity?

    @Query("SELECT * FROM sync_records WHERE entityType = :entityType")
    suspend fun getSyncRecordsForType(entityType: String): List<SyncRecordEntity>

    @Query("SELECT * FROM sync_records")
    suspend fun getAllSyncRecords(): List<SyncRecordEntity>

    @Query("UPDATE sync_records SET syncStatus = 'SYNCED', cloudUpdatedAt = :cloudUpdatedAt, errorMessage = null WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun markAsSynced(entityType: String, entityId: String, cloudUpdatedAt: Long)

    @Query("UPDATE sync_records SET syncStatus = 'FAILED', errorMessage = :error WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun markAsFailed(entityType: String, entityId: String, error: String)

    @Query("DELETE FROM sync_records WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteSyncRecord(entityType: String, entityId: String)

    @Query("DELETE FROM sync_records WHERE isDeleted = 1 AND syncStatus = 'SYNCED' AND deletedAt < :olderThanTimestamp")
    suspend fun purgeObsoleteTombstones(olderThanTimestamp: Long)

    @Query("DELETE FROM sync_records")
    suspend fun deleteAllSyncRecords()
}

/**
 * Records a pending upsert for [entityId] of [entityType] with clock-skew protection.
 * Ensures the recorded localUpdatedAt is strictly greater than any previous local or cloud timestamp.
 */
suspend fun SyncRecordDao.recordPendingUpsert(
    entityType: String,
    entityId: String,
    timestamp: Long = System.currentTimeMillis()
): Long {
    val existing = getSyncRecord(entityType, entityId)
    val prevMax = maxOf(existing?.localUpdatedAt ?: 0L, existing?.cloudUpdatedAt ?: 0L)
    val safeTimestamp = maxOf(timestamp, prevMax + 1L)
    upsertSyncRecord(
        SyncRecordEntity(
            entityType = entityType,
            entityId = entityId,
            syncStatus = "PENDING_UPSERT",
            localUpdatedAt = safeTimestamp,
            cloudUpdatedAt = existing?.cloudUpdatedAt ?: 0L,
            isDeleted = false,
            deletedAt = null
        )
    )
    return safeTimestamp
}

/**
 * Records a pending delete (tombstone) for [entityId] of [entityType] with clock-skew protection.
 * Ensures the deletion timestamp is strictly greater than any previous local or cloud timestamp.
 */
suspend fun SyncRecordDao.recordPendingDelete(
    entityType: String,
    entityId: String,
    timestamp: Long = System.currentTimeMillis()
): Long {
    val existing = getSyncRecord(entityType, entityId)
    val prevMax = maxOf(existing?.localUpdatedAt ?: 0L, existing?.cloudUpdatedAt ?: 0L)
    val safeTimestamp = maxOf(timestamp, prevMax + 1L)
    upsertSyncRecord(
        SyncRecordEntity(
            entityType = entityType,
            entityId = entityId,
            syncStatus = "PENDING_DELETE",
            localUpdatedAt = safeTimestamp,
            cloudUpdatedAt = existing?.cloudUpdatedAt ?: 0L,
            isDeleted = true,
            deletedAt = safeTimestamp
        )
    )
    return safeTimestamp
}


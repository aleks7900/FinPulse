package md.alexlab.finpulse.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "sync_records",
    primaryKeys = ["entityType", "entityId"],
    indices = [
        Index("syncStatus"),
        Index("entityType"),
        Index("localUpdatedAt"),
        Index("isDeleted")
    ]
)
data class SyncRecordEntity(
    val entityType: String, // TRANSACTION, ACCOUNT, CATEGORY, BUDGET, RECURRING, GOAL, ASSET, DEBT, RULE, SAVED_FILTER, SETTINGS
    val entityId: String,
    val syncStatus: String, // SYNCED, PENDING_UPSERT, PENDING_DELETE, FAILED
    val localUpdatedAt: Long = System.currentTimeMillis(),
    val cloudUpdatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val errorMessage: String? = null
)

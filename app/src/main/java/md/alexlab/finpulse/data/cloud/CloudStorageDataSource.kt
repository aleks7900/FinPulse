package md.alexlab.finpulse.data.cloud

data class CloudEntityRecord(
    val id: String,
    val collection: String,
    val jsonPayload: String,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

data class CloudUserSummary(
    val totalRecords: Int,
    val collectionsPresent: Set<String>,
    val lastModifiedTimestamp: Long
)

interface CloudStorageDataSource {
    suspend fun uploadRecords(
        uid: String,
        collection: String,
        records: List<CloudEntityRecord>
    ): Result<Int>

    suspend fun downloadRecords(
        uid: String,
        collection: String,
        sinceTimestamp: Long = 0L
    ): Result<List<CloudEntityRecord>>

    suspend fun recordTombstone(
        uid: String,
        collection: String,
        id: String,
        deletedAt: Long
    ): Result<Unit>

    suspend fun getUserSummary(uid: String): Result<CloudUserSummary>

    suspend fun clearUserStorage(uid: String): Result<Unit>
}

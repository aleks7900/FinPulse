package md.alexlab.finpulse.data.cloud

import java.util.concurrent.ConcurrentHashMap

class InMemoryCloudStorageDataSource : CloudStorageDataSource {

    // Structure: uid -> (collection -> (recordId -> CloudEntityRecord))
    private val storage = ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, CloudEntityRecord>>>()

    var shouldSimulateNetworkError: Boolean = false

    override suspend fun uploadRecords(
        uid: String,
        collection: String,
        records: List<CloudEntityRecord>
    ): Result<Int> {
        if (shouldSimulateNetworkError) {
            return Result.failure(IllegalStateException("Simulated network error during upload"))
        }

        val userCollections = storage.computeIfAbsent(uid) { ConcurrentHashMap() }
        val collectionMap = userCollections.computeIfAbsent(collection) { ConcurrentHashMap() }

        var count = 0
        for (record in records) {
            collectionMap[record.id] = record
            count++
        }

        return Result.success(count)
    }

    override suspend fun downloadRecords(
        uid: String,
        collection: String,
        sinceTimestamp: Long
    ): Result<List<CloudEntityRecord>> {
        if (shouldSimulateNetworkError) {
            return Result.failure(IllegalStateException("Simulated network error during download"))
        }

        val userCollections = storage[uid] ?: return Result.success(emptyList())
        val collectionMap = userCollections[collection] ?: return Result.success(emptyList())

        val filtered = collectionMap.values
            .filter { it.updatedAt > sinceTimestamp }
            .toList()

        return Result.success(filtered)
    }

    override suspend fun recordTombstone(
        uid: String,
        collection: String,
        id: String,
        deletedAt: Long
    ): Result<Unit> {
        if (shouldSimulateNetworkError) {
            return Result.failure(IllegalStateException("Simulated network error during tombstone"))
        }

        val userCollections = storage.computeIfAbsent(uid) { ConcurrentHashMap() }
        val collectionMap = userCollections.computeIfAbsent(collection) { ConcurrentHashMap() }

        collectionMap[id] = CloudEntityRecord(
            id = id,
            collection = collection,
            jsonPayload = "",
            updatedAt = deletedAt,
            isDeleted = true,
            deletedAt = deletedAt
        )

        return Result.success(Unit)
    }

    override suspend fun getUserSummary(uid: String): Result<CloudUserSummary> {
        val userCollections = storage[uid] ?: return Result.success(CloudUserSummary(0, emptySet(), 0L))

        var total = 0
        var maxTime = 0L
        val collections = mutableSetOf<String>()

        for ((colName, colMap) in userCollections) {
            val nonDeleted = colMap.values.filter { !it.isDeleted }
            if (nonDeleted.isNotEmpty()) {
                collections.add(colName)
                total += nonDeleted.size
                val localMax = nonDeleted.maxOfOrNull { it.updatedAt } ?: 0L
                if (localMax > maxTime) maxTime = localMax
            }
        }

        return Result.success(CloudUserSummary(total, collections, maxTime))
    }

    override suspend fun clearUserStorage(uid: String): Result<Unit> {
        storage.remove(uid)
        return Result.success(Unit)
    }

    fun getAllRecordsForUser(uid: String): Map<String, List<CloudEntityRecord>> {
        val userCollections = storage[uid] ?: return emptyMap()
        return userCollections.mapValues { it.value.values.toList() }
    }
}

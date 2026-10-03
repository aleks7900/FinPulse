package md.alexlab.finpulse.data.cloud

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { exception -> continuation.resumeWithException(exception) }
        addOnCanceledListener { continuation.cancel() }
    }

class FirestoreCloudStorageDataSource(
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val errorReporter: md.alexlab.finpulse.core.reporting.ErrorReporter = md.alexlab.finpulse.core.reporting.NoOpErrorReporter()
) : CloudStorageDataSource {

    companion object {
        private const val TAG = "FinPulseFirestore"
    }

    private fun getFirestore(): FirebaseFirestore = firestoreProvider()

    private fun verifyAuthOwnership(targetUid: String) {
        val auth = runCatching { authProvider() }.getOrNull()
        val currentUid = auth?.currentUser?.uid
        if (currentUid.isNullOrBlank()) {
            val msg = "Firestore access denied: User is not authenticated with FirebaseAuth (currentUser is null). Target uid was '$targetUid'."
            Log.e(TAG, msg)
            throw IllegalStateException(msg)
        }
        if (currentUid != targetUid) {
            val msg = "Firestore security violation: FirebaseAuth currentUser ($currentUid) does not match target path uid ($targetUid)."
            Log.e(TAG, msg)
            throw SecurityException(msg)
        }
    }

    override suspend fun uploadRecords(
        uid: String,
        collection: String,
        records: List<CloudEntityRecord>
    ): Result<Int> = runCatching {
        if (records.isEmpty()) return@runCatching 0

        verifyAuthOwnership(uid)
        Log.d(TAG, "Uploading ${records.size} records to /users/$uid/$collection")

        val firestore = getFirestore()
        val userCol = firestore.collection("users").document(uid).collection(collection)

        // Firestore batch max size is 500
        val chunks = records.chunked(450)
        var totalUploaded = 0

        for (chunk in chunks) {
            val batch = firestore.batch()
            for (record in chunk) {
                val docRef = userCol.document(record.id)
                val data = hashMapOf(
                    "id" to record.id,
                    "collection" to record.collection,
                    "jsonPayload" to record.jsonPayload,
                    "updatedAt" to record.updatedAt,
                    "isDeleted" to record.isDeleted,
                    "deletedAt" to record.deletedAt
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            // Also touch root user doc with lastModifiedTimestamp
            val userDocRef = firestore.collection("users").document(uid)
            val rootData = hashMapOf(
                "uid" to uid,
                "lastModifiedTimestamp" to System.currentTimeMillis()
            )
            batch.set(userDocRef, rootData, SetOptions.merge())

            batch.commit().awaitTask()
            totalUploaded += chunk.size
        }

        Log.d(TAG, "Successfully uploaded $totalUploaded records to /users/$uid/$collection")
        totalUploaded
    }.onFailure { error ->
        Log.e(TAG, "Failed to upload records to /users/$uid/$collection: ${error.message}", error)
        errorReporter.recordException(
            throwable = error,
            context = mapOf(
                "feature" to "cloud_storage",
                "operation" to "upload_records",
                "collection" to collection
            )
        )
    }

    override suspend fun downloadRecords(
        uid: String,
        collection: String,
        sinceTimestamp: Long
    ): Result<List<CloudEntityRecord>> = runCatching {
        verifyAuthOwnership(uid)
        Log.d(TAG, "Downloading records from /users/$uid/$collection (since: $sinceTimestamp)")

        val firestore = getFirestore()
        val userCol = firestore.collection("users").document(uid).collection(collection)

        val querySnapshot = if (sinceTimestamp > 0L) {
            userCol.whereGreaterThan("updatedAt", sinceTimestamp).get().awaitTask()
        } else {
            userCol.get().awaitTask()
        }

        val records = querySnapshot.documents.mapNotNull { doc ->
            val id = doc.getString("id") ?: doc.id
            val col = doc.getString("collection") ?: collection
            val jsonPayload = doc.getString("jsonPayload") ?: ""
            val updatedAt = doc.getLong("updatedAt") ?: 0L
            val isDeleted = doc.getBoolean("isDeleted") ?: false
            val deletedAt = doc.getLong("deletedAt")

            CloudEntityRecord(
                id = id,
                collection = col,
                jsonPayload = jsonPayload,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
                deletedAt = deletedAt
            )
        }

        Log.d(TAG, "Downloaded ${records.size} records from /users/$uid/$collection")
        records
    }.onFailure { error ->
        Log.e(TAG, "Failed to download records from /users/$uid/$collection: ${error.message}", error)
        errorReporter.recordException(
            throwable = error,
            context = mapOf(
                "feature" to "cloud_storage",
                "operation" to "download_records",
                "collection" to collection
            )
        )
    }

    override suspend fun recordTombstone(
        uid: String,
        collection: String,
        id: String,
        deletedAt: Long
    ): Result<Unit> = runCatching {
        verifyAuthOwnership(uid)
        Log.d(TAG, "Recording tombstone for /users/$uid/$collection/$id at $deletedAt")

        val firestore = getFirestore()
        val docRef = firestore.collection("users").document(uid).collection(collection).document(id)

        val tombstoneData = hashMapOf(
            "id" to id,
            "collection" to collection,
            "jsonPayload" to "",
            "updatedAt" to deletedAt,
            "isDeleted" to true,
            "deletedAt" to deletedAt
        )

        docRef.set(tombstoneData, SetOptions.merge()).awaitTask()
        Log.d(TAG, "Recorded tombstone for /users/$uid/$collection/$id")
        Unit
    }.onFailure { error ->
        Log.e(TAG, "Failed to record tombstone /users/$uid/$collection/$id: ${error.message}", error)
        errorReporter.recordException(
            throwable = error,
            context = mapOf(
                "feature" to "cloud_storage",
                "operation" to "record_tombstone",
                "collection" to collection
            )
        )
    }

    override suspend fun getUserSummary(uid: String): Result<CloudUserSummary> = runCatching {
        verifyAuthOwnership(uid)
        Log.d(TAG, "Fetching user summary for /users/$uid")

        val firestore = getFirestore()
        val userDoc = firestore.collection("users").document(uid).get().awaitTask()

        var total = userDoc.getLong("totalRecords")?.toInt() ?: 0
        val lastModified = userDoc.getLong("lastModifiedTimestamp") ?: 0L

        // If totalRecords is not explicitly stored on root doc, probe key collections to detect existing cloud data
        if (total == 0) {
            val hasTx = !firestore.collection("users").document(uid).collection("transactions").limit(1).get().awaitTask().isEmpty
            val hasAcc = !firestore.collection("users").document(uid).collection("accounts").limit(1).get().awaitTask().isEmpty
            val hasSettings = firestore.collection("users").document(uid).collection("settings").document("app").get().awaitTask().exists()
            if (hasTx || hasAcc || hasSettings) {
                total = 1
            }
        }

        Log.d(TAG, "User summary for $uid: total=$total, lastModified=$lastModified")
        CloudUserSummary(
            totalRecords = total,
            collectionsPresent = emptySet(),
            lastModifiedTimestamp = lastModified
        )
    }.onFailure { error ->
        Log.e(TAG, "Failed to get user summary for $uid: ${error.message}", error)
        errorReporter.recordException(
            throwable = error,
            context = mapOf(
                "feature" to "cloud_storage",
                "operation" to "get_user_summary"
            )
        )
    }

    override suspend fun clearUserStorage(uid: String): Result<Unit> = runCatching {
        verifyAuthOwnership(uid)
        Log.d(TAG, "Clearing user storage for /users/$uid")

        val firestore = getFirestore()
        val knownCollections = listOf(
            "transactions", "accounts", "categories", "budgets",
            "recurring_rules", "goals", "assets", "debts",
            "categorization_rules", "saved_filters", "settings"
        )

        for (colName in knownCollections) {
            val docs = firestore.collection("users").document(uid).collection(colName).get().awaitTask()
            if (!docs.isEmpty) {
                val batch = firestore.batch()
                for (doc in docs.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().awaitTask()
                Log.d(TAG, "Deleted ${docs.size()} documents from /users/$uid/$colName")
            }
        }

        // Delete user root doc
        firestore.collection("users").document(uid).delete().awaitTask()
        Log.d(TAG, "Cleared user root document /users/$uid")
        Unit
    }.onFailure { error ->
        Log.e(TAG, "Failed to clear user storage for $uid: ${error.message}", error)
        errorReporter.recordException(
            throwable = error,
            context = mapOf(
                "feature" to "cloud_storage",
                "operation" to "clear_user_storage"
            )
        )
    }
}

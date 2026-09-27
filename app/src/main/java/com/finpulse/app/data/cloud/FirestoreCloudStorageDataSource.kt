package com.finpulse.app.data.cloud

import com.google.android.gms.tasks.Task
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
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) : CloudStorageDataSource {

    private fun getFirestore(): FirebaseFirestore = firestoreProvider()

    override suspend fun uploadRecords(
        uid: String,
        collection: String,
        records: List<CloudEntityRecord>
    ): Result<Int> = runCatching {
        if (records.isEmpty()) return@runCatching 0

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
            batch.commit().awaitTask()
            totalUploaded += chunk.size
        }

        totalUploaded
    }

    override suspend fun downloadRecords(
        uid: String,
        collection: String,
        sinceTimestamp: Long
    ): Result<List<CloudEntityRecord>> = runCatching {
        val firestore = getFirestore()
        val userCol = firestore.collection("users").document(uid).collection(collection)

        val querySnapshot = if (sinceTimestamp > 0L) {
            userCol.whereGreaterThan("updatedAt", sinceTimestamp).get().awaitTask()
        } else {
            userCol.get().awaitTask()
        }

        querySnapshot.documents.mapNotNull { doc ->
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
    }

    override suspend fun recordTombstone(
        uid: String,
        collection: String,
        id: String,
        deletedAt: Long
    ): Result<Unit> = runCatching {
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
        Unit
    }

    override suspend fun getUserSummary(uid: String): Result<CloudUserSummary> = runCatching {
        val firestore = getFirestore()
        val userDoc = firestore.collection("users").document(uid).get().awaitTask()

        // Check metadata document if present, or query collections
        val total = userDoc.getLong("totalRecords")?.toInt() ?: 0
        val lastModified = userDoc.getLong("lastModifiedTimestamp") ?: 0L

        CloudUserSummary(
            totalRecords = total,
            collectionsPresent = emptySet(),
            lastModifiedTimestamp = lastModified
        )
    }

    override suspend fun clearUserStorage(uid: String): Result<Unit> = runCatching {
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
            }
        }

        // Delete user root doc
        firestore.collection("users").document(uid).delete().awaitTask()
        Unit
    }
}

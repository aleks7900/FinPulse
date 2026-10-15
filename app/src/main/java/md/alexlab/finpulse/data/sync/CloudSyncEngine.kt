package md.alexlab.finpulse.data.sync

import android.util.Log
import androidx.room.withTransaction
import md.alexlab.finpulse.core.database.FinPulseDatabase
import md.alexlab.finpulse.core.database.entity.SyncRecordEntity
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.network.NetworkConnectivityMonitor
import md.alexlab.finpulse.data.cloud.CloudEntityRecord
import md.alexlab.finpulse.data.cloud.CloudStorageDataSource
import md.alexlab.finpulse.data.mapper.toDomain
import md.alexlab.finpulse.data.mapper.toEntity
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.Budget
import md.alexlab.finpulse.domain.model.CategorizationRule
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.Debt
import md.alexlab.finpulse.domain.model.FinancialGoal
import md.alexlab.finpulse.domain.model.InvestmentAsset
import md.alexlab.finpulse.domain.model.RecurringTransaction
import md.alexlab.finpulse.domain.model.SavedFilter
import md.alexlab.finpulse.data.cloud.DataStoreLocalSettingsDataSource
import md.alexlab.finpulse.data.cloud.FirestoreCloudSettingsDataSource
import md.alexlab.finpulse.data.repository.SettingsRepositoryImpl
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.sync.SyncEntityType
import md.alexlab.finpulse.domain.model.sync.SyncResult
import md.alexlab.finpulse.domain.model.sync.SyncStatus
import md.alexlab.finpulse.domain.repository.AuthRepository
import md.alexlab.finpulse.domain.repository.CloudSyncRepository
import md.alexlab.finpulse.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CloudSyncEngine(
    private val database: FinPulseDatabase,
    private val cloudStorage: CloudStorageDataSource,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository? = null,
    private val networkMonitor: NetworkConnectivityMonitor? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val errorReporter: md.alexlab.finpulse.core.reporting.ErrorReporter = md.alexlab.finpulse.core.reporting.NoOpErrorReporter()
) : CloudSyncRepository {

    companion object {
        private const val TAG = "FinPulseSyncEngine"
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val syncDao = database.syncRecordDao()
    private val engineScope = CoroutineScope(SupervisorJob() + dispatcher)

    private fun setSuccessStatus() {
        _syncStatus.value = SyncStatus.SUCCESS
        engineScope.launch {
            delay(3500L)
            if (_syncStatus.value == SyncStatus.SUCCESS) {
                _syncStatus.value = SyncStatus.IDLE
            }
        }
    }

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(0L)
    override val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    override val pendingChangesCount: Flow<Int> = syncDao.getPendingCountFlow()

    suspend fun initialize() {
        val prefs = userPreferencesDataStore.userPreferencesFlow.first()
        _lastSyncTimestamp.value = prefs.lastSyncTimestamp
        if (networkMonitor != null && !networkMonitor.isCurrentlyOnline()) {
            _syncStatus.value = SyncStatus.WAITING_FOR_NETWORK
        }
    }

    override suspend fun performFullSync(): Result<SyncResult> = withContext(dispatcher) {
        val user = authRepository.currentUser.value
        if (user == null) {
            _syncStatus.value = SyncStatus.IDLE
            Log.w(TAG, "performFullSync aborted: No authenticated user in authRepository")
            return@withContext Result.failure(IllegalStateException("No authenticated user for cloud sync"))
        }

        if (networkMonitor != null && !networkMonitor.isCurrentlyOnline()) {
            Log.w(TAG, "performFullSync aborted: Network is offline")
            _syncStatus.value = SyncStatus.WAITING_FOR_NETWORK
            return@withContext Result.failure(IllegalStateException("Device is offline / waiting for network connection"))
        }

        Log.i(TAG, "Starting performFullSync for user ${user.uid}")
        errorReporter.log("Starting full cloud sync")
        _syncStatus.value = SyncStatus.SYNCING
        try {
            val syncResult = reconcileEntities(user.uid)

            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            setSuccessStatus()
            userPreferencesDataStore.setSyncState("SUCCESS", now, null)

            Log.i(
                TAG,
                "performFullSync completed: uploaded=${syncResult.uploadedCount}, downloaded=${syncResult.downloadedCount}, deleted=${syncResult.deletedCount}, conflicts=${syncResult.conflictsResolvedCount}"
            )
            errorReporter.log("Full cloud sync completed successfully")
            Result.success(syncResult)
        } catch (t: Throwable) {
            Log.e(TAG, "performFullSync failed for ${user.uid}: ${t.message}", t)
            errorReporter.recordException(
                throwable = t,
                context = md.alexlab.finpulse.core.reporting.DiagnosticContext.build(
                    feature = md.alexlab.finpulse.core.reporting.DiagnosticContext.FEATURE_CLOUD_SYNC,
                    operation = md.alexlab.finpulse.core.reporting.DiagnosticContext.OP_FULL_SYNC,
                    extra = mapOf(
                        md.alexlab.finpulse.core.reporting.DiagnosticContext.KEY_SYNC_MODE to "cloud"
                    )
                )
            )
            val isOffline = networkMonitor?.isCurrentlyOnline() == false ||
                t.message?.contains("network", ignoreCase = true) == true ||
                t.message?.contains("unable to resolve", ignoreCase = true) == true
            _syncStatus.value = if (isOffline) SyncStatus.WAITING_FOR_NETWORK else SyncStatus.ERROR
            userPreferencesDataStore.setSyncState("ERROR", _lastSyncTimestamp.value, t.message)
            Result.failure(t)
        }
    }

    override suspend fun uploadPendingChanges(): Result<SyncResult> = withContext(dispatcher) {
        val user = authRepository.currentUser.value
            ?: return@withContext Result.failure(IllegalStateException("User not logged in"))
        _syncStatus.value = SyncStatus.SYNCING
        try {
            val result = reconcileEntities(user.uid)
            setSuccessStatus()
            Result.success(result)
        } catch (t: Throwable) {
            errorReporter.recordException(
                throwable = t,
                context = md.alexlab.finpulse.core.reporting.DiagnosticContext.build(
                    feature = md.alexlab.finpulse.core.reporting.DiagnosticContext.FEATURE_CLOUD_SYNC,
                    operation = md.alexlab.finpulse.core.reporting.DiagnosticContext.OP_UPLOAD_CHANGES
                )
            )
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(t)
        }
    }

    override suspend fun downloadRemoteChanges(): Result<SyncResult> = withContext(dispatcher) {
        val user = authRepository.currentUser.value
            ?: return@withContext Result.failure(IllegalStateException("User not logged in"))
        _syncStatus.value = SyncStatus.SYNCING
        try {
            val result = reconcileEntities(user.uid)
            setSuccessStatus()
            Result.success(result)
        } catch (t: Throwable) {
            errorReporter.recordException(
                throwable = t,
                context = md.alexlab.finpulse.core.reporting.DiagnosticContext.build(
                    feature = md.alexlab.finpulse.core.reporting.DiagnosticContext.FEATURE_CLOUD_SYNC,
                    operation = md.alexlab.finpulse.core.reporting.DiagnosticContext.OP_DOWNLOAD_CHANGES
                )
            )
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(t)
        }
    }

    /**
     * Executes bidirectional Latest-Change-Wins reconciliation across all entity collections.
     * Guarantees that:
     * - remote.updatedAt > local.updatedAt -> Remote wins, updates local
     * - local.updatedAt > remote.updatedAt -> Local wins, uploads to cloud
     * - local.updatedAt == remote.updatedAt -> No-op
     * - Local-only -> Uploads if pending or unsynced
     * - Remote-only -> Downloads locally unless tombstone
     * - Deletions propagate reliably via tombstones without resurrection
     * - Dependencies are preserved: Accounts & Categories applied before Transactions & Budgets
     */
    private suspend fun reconcileEntities(uid: String): SyncResult {
        var totalUploaded = 0
        var totalDownloaded = 0
        var totalDeleted = 0
        var totalConflicts = 0

        // Strict dependency order for entities (parents before children)
        val entityTypesInOrder = listOf(
            SyncEntityType.ACCOUNT.name,
            SyncEntityType.CATEGORY.name,
            SyncEntityType.CATEGORIZATION_RULE.name,
            SyncEntityType.SAVED_FILTER.name,
            SyncEntityType.GOAL.name,
            SyncEntityType.ASSET.name,
            SyncEntityType.DEBT.name,
            SyncEntityType.BUDGET.name,
            SyncEntityType.RECURRING_RULE.name,
            SyncEntityType.TRANSACTION.name
        )

        // Ensure all local entities in SQLite tables have corresponding sync_records
        for (type in entityTypesInOrder) {
            ensureLocalSyncRecordsForType(type)
        }

        data class ReconcileAction(
            val type: String,
            val id: String,
            val applyRemoteUpsert: CloudEntityRecord? = null,
            val applyRemoteDelete: CloudEntityRecord? = null,
            val pushLocalUpsert: Boolean = false,
            val pushLocalTombstone: Long? = null,
            val isConflict: Boolean = false
        )

        val actions = mutableListOf<ReconcileAction>()

        for (type in entityTypesInOrder) {
            val colName = getCollectionName(type)
            val remoteRecords = cloudStorage.downloadRecords(uid, colName, 0L).getOrThrow()
            val remoteMap = remoteRecords.associateBy { it.id }
            val localSyncMap = syncDao.getSyncRecordsForType(type).associateBy { it.entityId }

            val allIds = (remoteMap.keys + localSyncMap.keys).toSet()

            for (id in allIds) {
                val remote = remoteMap[id]
                val local = localSyncMap[id]

                if (remote != null && local != null) {
                    val isLocalPending = local.syncStatus == "PENDING_UPSERT" ||
                        local.syncStatus == "PENDING_DELETE" ||
                        local.syncStatus == "FAILED"

                    when {
                        // Both sides deleted
                        remote.isDeleted && local.isDeleted -> {
                            if (local.localUpdatedAt > remote.updatedAt) {
                                actions.add(
                                    ReconcileAction(
                                        type = type,
                                        id = id,
                                        pushLocalTombstone = local.deletedAt ?: local.localUpdatedAt,
                                        isConflict = isLocalPending
                                    )
                                )
                            } else {
                                syncDao.markAsSynced(type, id, remote.updatedAt)
                            }
                        }

                        // Remote is deleted, Local is active
                        remote.isDeleted && !local.isDeleted -> {
                            if (remote.updatedAt > local.localUpdatedAt) {
                                Log.d(
                                    TAG,
                                    "Conflict [$type $id]: Remote deletion (${remote.updatedAt}) > Local active (${local.localUpdatedAt}) -> Remote deletion wins"
                                )
                                actions.add(ReconcileAction(type, id, applyRemoteDelete = remote, isConflict = true))
                            } else if (local.localUpdatedAt > remote.updatedAt) {
                                Log.d(
                                    TAG,
                                    "Conflict [$type $id]: Local active (${local.localUpdatedAt}) > Remote deletion (${remote.updatedAt}) -> Local active wins"
                                )
                                actions.add(ReconcileAction(type, id, pushLocalUpsert = true, isConflict = true))
                            } else {
                                // Equal timestamps: if local was actively modified pending, local wins; else remote delete wins
                                if (isLocalPending) {
                                    actions.add(ReconcileAction(type, id, pushLocalUpsert = true, isConflict = true))
                                } else {
                                    actions.add(ReconcileAction(type, id, applyRemoteDelete = remote, isConflict = true))
                                }
                            }
                        }

                        // Remote is active, Local is deleted
                        !remote.isDeleted && local.isDeleted -> {
                            if (local.localUpdatedAt > remote.updatedAt) {
                                Log.d(
                                    TAG,
                                    "Conflict [$type $id]: Local deletion (${local.localUpdatedAt}) > Remote active (${remote.updatedAt}) -> Local deletion wins"
                                )
                                actions.add(
                                    ReconcileAction(
                                        type = type,
                                        id = id,
                                        pushLocalTombstone = local.deletedAt ?: local.localUpdatedAt,
                                        isConflict = true
                                    )
                                )
                            } else if (remote.updatedAt > local.localUpdatedAt) {
                                Log.d(
                                    TAG,
                                    "Conflict [$type $id]: Remote active (${remote.updatedAt}) > Local deletion (${local.localUpdatedAt}) -> Remote active wins"
                                )
                                actions.add(ReconcileAction(type, id, applyRemoteUpsert = remote, isConflict = true))
                            } else {
                                // Equal timestamps: if local was actively pending delete, local wins; else remote wins
                                if (isLocalPending) {
                                    actions.add(
                                        ReconcileAction(
                                            type = type,
                                            id = id,
                                            pushLocalTombstone = local.deletedAt ?: local.localUpdatedAt,
                                            isConflict = true
                                        )
                                    )
                                } else {
                                    actions.add(ReconcileAction(type, id, applyRemoteUpsert = remote, isConflict = true))
                                }
                            }
                        }

                        // Both sides active
                        !remote.isDeleted && !local.isDeleted -> {
                            when {
                                remote.updatedAt > local.localUpdatedAt -> {
                                    Log.d(
                                        TAG,
                                        "Reconcile [$type $id]: Remote (${remote.updatedAt}) > Local (${local.localUpdatedAt}) -> Remote wins"
                                    )
                                    actions.add(ReconcileAction(type, id, applyRemoteUpsert = remote, isConflict = isLocalPending))
                                }
                                local.localUpdatedAt > remote.updatedAt -> {
                                    Log.d(
                                        TAG,
                                        "Reconcile [$type $id]: Local (${local.localUpdatedAt}) > Remote (${remote.updatedAt}) -> Local wins"
                                    )
                                    if (isLocalPending || local.cloudUpdatedAt != local.localUpdatedAt) {
                                        actions.add(ReconcileAction(type, id, pushLocalUpsert = true, isConflict = isLocalPending))
                                    }
                                }
                                else -> {
                                    // Equal timestamps: no-op, ensure marked as SYNCED
                                    if (local.syncStatus != "SYNCED" || local.cloudUpdatedAt != remote.updatedAt) {
                                        syncDao.markAsSynced(type, id, remote.updatedAt)
                                    }
                                }
                            }
                        }
                    }
                } else if (remote != null && local == null) {
                    // Remote-only entity
                    if (remote.isDeleted) {
                        // Remote tombstone: record locally in sync_records to prevent resurrection
                        syncDao.upsertSyncRecord(
                            SyncRecordEntity(
                                entityType = type,
                                entityId = id,
                                syncStatus = "SYNCED",
                                localUpdatedAt = remote.updatedAt,
                                cloudUpdatedAt = remote.updatedAt,
                                isDeleted = true,
                                deletedAt = remote.deletedAt ?: remote.updatedAt
                            )
                        )
                    } else {
                        Log.d(TAG, "Reconcile [$type $id]: Remote-only -> Download locally")
                        actions.add(ReconcileAction(type = type, id = id, applyRemoteUpsert = remote))
                    }
                } else if (remote == null && local != null) {
                    // Local-only entity
                    if (local.isDeleted) {
                        if (local.syncStatus == "PENDING_DELETE" || local.syncStatus == "FAILED" || local.localUpdatedAt > local.cloudUpdatedAt) {
                            Log.d(TAG, "Reconcile [$type $id]: Local-only tombstone -> Push to cloud")
                            actions.add(
                                ReconcileAction(
                                    type = type,
                                    id = id,
                                    pushLocalTombstone = local.deletedAt ?: local.localUpdatedAt
                                )
                            )
                        } else {
                            syncDao.markAsSynced(type, id, local.localUpdatedAt)
                        }
                    } else {
                        Log.d(TAG, "Reconcile [$type $id]: Local-only -> Push to cloud")
                        actions.add(ReconcileAction(type = type, id = id, pushLocalUpsert = true))
                    }
                }
            }
        }

        // Apply Remote changes to Local Room Database in a single atomic transaction
        database.withTransaction {
            // Apply Remote Upserts in parent-to-child order
            for (type in entityTypesInOrder) {
                val upsertsForType = actions.filter { it.type == type && it.applyRemoteUpsert != null }
                for (action in upsertsForType) {
                    val remote = action.applyRemoteUpsert!!
                    deserializeAndUpsertLocally(type, remote.jsonPayload)
                    syncDao.upsertSyncRecord(
                        SyncRecordEntity(
                            entityType = type,
                            entityId = action.id,
                            syncStatus = "SYNCED",
                            localUpdatedAt = remote.updatedAt,
                            cloudUpdatedAt = remote.updatedAt,
                            isDeleted = false,
                            deletedAt = null
                        )
                    )
                    totalDownloaded++
                    if (action.isConflict) totalConflicts++
                }
            }

            // Apply Remote Deletions in child-to-parent order (reverse)
            for (type in entityTypesInOrder.reversed()) {
                val deletesForType = actions.filter { it.type == type && it.applyRemoteDelete != null }
                for (action in deletesForType) {
                    val remote = action.applyRemoteDelete!!
                    deleteEntityLocally(type, action.id)
                    syncDao.upsertSyncRecord(
                        SyncRecordEntity(
                            entityType = type,
                            entityId = action.id,
                            syncStatus = "SYNCED",
                            localUpdatedAt = remote.updatedAt,
                            cloudUpdatedAt = remote.updatedAt,
                            isDeleted = true,
                            deletedAt = remote.deletedAt ?: remote.updatedAt
                        )
                    )
                    totalDeleted++
                    if (action.isConflict) totalConflicts++
                }
            }
        }

        // Push Local Tombstones to Cloud
        val tombstones = actions.filter { it.pushLocalTombstone != null }
        for (t in tombstones) {
            val colName = getCollectionName(t.type)
            val deletedAt = t.pushLocalTombstone!!
            cloudStorage.recordTombstone(uid, colName, t.id, deletedAt).getOrThrow()
            syncDao.markAsSynced(t.type, t.id, deletedAt)
            totalDeleted++
            if (t.isConflict) totalConflicts++
        }

        // Push Local Upserts to Cloud in batches per collection
        for (type in entityTypesInOrder) {
            val upserts = actions.filter { it.type == type && it.pushLocalUpsert }
            if (upserts.isNotEmpty()) {
                val cloudRecords = mutableListOf<CloudEntityRecord>()
                val validActions = mutableListOf<ReconcileAction>()
                for (act in upserts) {
                    val payload = serializeEntity(type, act.id)
                    val localSync = syncDao.getSyncRecord(type, act.id)
                    val updatedAt = localSync?.localUpdatedAt ?: System.currentTimeMillis()
                    if (payload != null) {
                        cloudRecords.add(
                            CloudEntityRecord(
                                id = act.id,
                                collection = getCollectionName(type),
                                jsonPayload = payload,
                                updatedAt = updatedAt,
                                isDeleted = false
                            )
                        )
                        validActions.add(act)
                    }
                }

                if (cloudRecords.isNotEmpty()) {
                    val uploaded = cloudStorage.uploadRecords(uid, getCollectionName(type), cloudRecords).getOrThrow()
                    totalUploaded += uploaded
                    for ((idx, act) in validActions.withIndex()) {
                        val rec = cloudRecords[idx]
                        syncDao.markAsSynced(type, act.id, rec.updatedAt)
                        if (act.isConflict) totalConflicts++
                    }
                }
            }
        }

        // Synchronize portable account settings
        syncSettingsInternal(uid)

        return SyncResult(
            isSuccess = true,
            uploadedCount = totalUploaded,
            downloadedCount = totalDownloaded,
            deletedCount = totalDeleted,
            conflictsResolvedCount = totalConflicts,
            syncedAt = System.currentTimeMillis()
        )
    }

    private suspend fun ensureLocalSyncRecordsForType(type: String) {
        val existingSync = runCatching { syncDao.getSyncRecordsForType(type) }
            .getOrDefault(emptyList())
            .associateBy { it.entityId }

        val localEntities: List<Pair<String, Long>> = runCatching {
            when (type) {
                "ACCOUNT" -> database.accountDao().getAllAccounts().map { it.id to it.updatedAt }
                "CATEGORY" -> database.categoryDao().getAllCategories().map { it.id to System.currentTimeMillis() }
                "TRANSACTION" -> database.transactionDao().getAllTransactions().map { it.id to it.createdAt }
                "BUDGET" -> database.budgetDao().getAllBudgets().map { it.id to System.currentTimeMillis() }
                "RECURRING_RULE" -> database.recurringTransactionDao().getAllRecurring().map { it.id to System.currentTimeMillis() }
                "GOAL" -> database.financialGoalDao().getAllGoals().map { it.id to System.currentTimeMillis() }
                "ASSET" -> database.assetDao().getAllAssets().map { it.id to it.lastUpdated }
                "DEBT" -> database.debtDao().getAllDebts().map { it.id to System.currentTimeMillis() }
                "CATEGORIZATION_RULE" -> database.categorizationRuleDao().getAllRules().map { it.id to System.currentTimeMillis() }
                "SAVED_FILTER" -> database.savedFilterDao().getAllSavedFilters().map { it.id to System.currentTimeMillis() }
                else -> emptyList()
            }
        }.getOrDefault(emptyList())

        val missing = localEntities.filter { it.first !in existingSync }
        if (missing.isNotEmpty()) {
            val records = missing.map { (id, time) ->
                SyncRecordEntity(
                    entityType = type,
                    entityId = id,
                    syncStatus = "PENDING_UPSERT",
                    localUpdatedAt = time,
                    cloudUpdatedAt = 0L,
                    isDeleted = false
                )
            }
            syncDao.upsertSyncRecords(records)
        }
    }

    private suspend fun syncSettingsInternal(uid: String) {
        val actualSettingsRepo = settingsRepository ?: SettingsRepositoryImpl(
            localDataSource = DataStoreLocalSettingsDataSource(userPreferencesDataStore),
            cloudDataSource = FirestoreCloudSettingsDataSource(cloudStorage),
            dispatcher = dispatcher
        )
        actualSettingsRepo.synchronize(uid)
    }

    override suspend fun migrateLocalDataToCloud(uid: String): Result<SyncResult> = withContext(dispatcher) {
        _syncStatus.value = SyncStatus.SYNCING
        try {
            var uploadedCount = 0

            // 1. Accounts
            val accounts = database.accountDao().getAllAccounts().map { it.toDomain() }
            if (accounts.isNotEmpty()) {
                val records = accounts.map {
                    CloudEntityRecord(it.id, "accounts", json.encodeToString(it), it.updatedAt)
                }
                cloudStorage.uploadRecords(uid, "accounts", records)
                uploadedCount += records.size
                records.forEach { syncDao.upsertSyncRecord(SyncRecordEntity("ACCOUNT", it.id, "SYNCED", it.updatedAt, it.updatedAt)) }
            }

            // 2. Categories
            val categories = database.categoryDao().getAllCategories().map { it.toDomain() }
            if (categories.isNotEmpty()) {
                val records = categories.map {
                    CloudEntityRecord(it.id, "categories", json.encodeToString(it), System.currentTimeMillis())
                }
                cloudStorage.uploadRecords(uid, "categories", records)
                uploadedCount += records.size
                records.forEach { syncDao.upsertSyncRecord(SyncRecordEntity("CATEGORY", it.id, "SYNCED", it.updatedAt, it.updatedAt)) }
            }

            // 3. Transactions
            val transactions = database.transactionDao().getAllTransactions().map { it.toDomain() }
            if (transactions.isNotEmpty()) {
                val records = transactions.map {
                    CloudEntityRecord(it.id, "transactions", json.encodeToString(it), it.createdAt)
                }
                cloudStorage.uploadRecords(uid, "transactions", records)
                uploadedCount += records.size
                records.forEach { syncDao.upsertSyncRecord(SyncRecordEntity("TRANSACTION", it.id, "SYNCED", it.updatedAt, it.updatedAt)) }
            }

            // 4. Budgets
            val budgets = database.budgetDao().getAllBudgets().map { it.toDomain() }
            if (budgets.isNotEmpty()) {
                val records = budgets.map {
                    CloudEntityRecord(it.id, "budgets", json.encodeToString(it), System.currentTimeMillis())
                }
                cloudStorage.uploadRecords(uid, "budgets", records)
                uploadedCount += records.size
                records.forEach { syncDao.upsertSyncRecord(SyncRecordEntity("BUDGET", it.id, "SYNCED", it.updatedAt, it.updatedAt)) }
            }

            // 5. Recurring Rules
            val recurringRules = database.recurringTransactionDao().getAllRecurring().map { it.toDomain() }
            if (recurringRules.isNotEmpty()) {
                val records = recurringRules.map {
                    CloudEntityRecord(it.id, "recurring_rules", json.encodeToString(it), System.currentTimeMillis())
                }
                cloudStorage.uploadRecords(uid, "recurring_rules", records)
                uploadedCount += records.size
                records.forEach { syncDao.upsertSyncRecord(SyncRecordEntity("RECURRING_RULE", it.id, "SYNCED", it.updatedAt, it.updatedAt)) }
            }

            // 6. Goals, Assets, Debts, Rules, SavedFilters
            val goals = database.financialGoalDao().getAllGoals().map { it.toDomain() }
            if (goals.isNotEmpty()) {
                val records = goals.map { CloudEntityRecord(it.id, "goals", json.encodeToString(it), System.currentTimeMillis()) }
                cloudStorage.uploadRecords(uid, "goals", records)
                uploadedCount += records.size
                records.forEach { syncDao.upsertSyncRecord(SyncRecordEntity("GOAL", it.id, "SYNCED", it.updatedAt, it.updatedAt)) }
            }

            val assets = database.assetDao().getAllAssets().map { it.toDomain() }
            if (assets.isNotEmpty()) {
                val records = assets.map { CloudEntityRecord(it.id, "assets", json.encodeToString(it), it.lastUpdated) }
                cloudStorage.uploadRecords(uid, "assets", records)
                uploadedCount += records.size
                records.forEach { syncDao.upsertSyncRecord(SyncRecordEntity("ASSET", it.id, "SYNCED", it.updatedAt, it.updatedAt)) }
            }

            val debts = database.debtDao().getAllDebts().map { it.toDomain() }
            if (debts.isNotEmpty()) {
                val records = debts.map { CloudEntityRecord(it.id, "debts", json.encodeToString(it), System.currentTimeMillis()) }
                cloudStorage.uploadRecords(uid, "debts", records)
                uploadedCount += records.size
                records.forEach { syncDao.upsertSyncRecord(SyncRecordEntity("DEBT", it.id, "SYNCED", it.updatedAt, it.updatedAt)) }
            }

            val rules = database.categorizationRuleDao().getAllRules().map { it.toDomain() }
            if (rules.isNotEmpty()) {
                val records = rules.map { CloudEntityRecord(it.id, "categorization_rules", json.encodeToString(it), System.currentTimeMillis()) }
                cloudStorage.uploadRecords(uid, "categorization_rules", records)
                uploadedCount += records.size
                records.forEach { syncDao.upsertSyncRecord(SyncRecordEntity("CATEGORIZATION_RULE", it.id, "SYNCED", it.updatedAt, it.updatedAt)) }
            }

            val savedFilters = database.savedFilterDao().getAllSavedFilters().map { it.toDomain() }
            if (savedFilters.isNotEmpty()) {
                val records = savedFilters.map { CloudEntityRecord(it.id, "saved_filters", json.encodeToString(it), System.currentTimeMillis()) }
                cloudStorage.uploadRecords(uid, "saved_filters", records)
                uploadedCount += records.size
                records.forEach { syncDao.upsertSyncRecord(SyncRecordEntity("SAVED_FILTER", it.id, "SYNCED", it.updatedAt, it.updatedAt)) }
            }

            // Sync settings
            syncSettingsInternal(uid)

            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            setSuccessStatus()
            userPreferencesDataStore.setSyncState("SUCCESS", now, null)

            Result.success(SyncResult(isSuccess = true, uploadedCount = uploadedCount, syncedAt = now))
        } catch (t: Throwable) {
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(t)
        }
    }

    override suspend fun restoreCloudDataToLocal(uid: String): Result<SyncResult> = withContext(dispatcher) {
        _syncStatus.value = SyncStatus.SYNCING
        try {
            // Download all collections outside database transaction to prevent locking Room
            val categories = cloudStorage.downloadRecords(uid, "categories", 0L).getOrDefault(emptyList())
            val accounts = cloudStorage.downloadRecords(uid, "accounts", 0L).getOrDefault(emptyList())
            val transactions = cloudStorage.downloadRecords(uid, "transactions", 0L).getOrDefault(emptyList())
            val budgets = cloudStorage.downloadRecords(uid, "budgets", 0L).getOrDefault(emptyList())
            val recurring = cloudStorage.downloadRecords(uid, "recurring_rules", 0L).getOrDefault(emptyList())
            val goals = cloudStorage.downloadRecords(uid, "goals", 0L).getOrDefault(emptyList())
            val assets = cloudStorage.downloadRecords(uid, "assets", 0L).getOrDefault(emptyList())
            val debts = cloudStorage.downloadRecords(uid, "debts", 0L).getOrDefault(emptyList())
            val rules = cloudStorage.downloadRecords(uid, "categorization_rules", 0L).getOrDefault(emptyList())
            val filters = cloudStorage.downloadRecords(uid, "saved_filters", 0L).getOrDefault(emptyList())

            var restoredCount = 0

            database.withTransaction {
                // Clear local data for clean restoration
                clearLocalDatabaseTables()

                // 1. Categories
                categories.filter { !it.isDeleted }.forEach { rec ->
                    val cat = json.decodeFromString<Category>(rec.jsonPayload)
                    database.categoryDao().insertCategory(cat.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("CATEGORY", cat.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                // 2. Accounts
                accounts.filter { !it.isDeleted }.forEach { rec ->
                    val acc = json.decodeFromString<Account>(rec.jsonPayload)
                    database.accountDao().insertAccount(acc.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("ACCOUNT", acc.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                // 3. Transactions
                transactions.filter { !it.isDeleted }.forEach { rec ->
                    val tx = json.decodeFromString<Transaction>(rec.jsonPayload)
                    database.transactionDao().insertTransaction(tx.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("TRANSACTION", tx.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                // 4. Budgets
                budgets.filter { !it.isDeleted }.forEach { rec ->
                    val b = json.decodeFromString<Budget>(rec.jsonPayload)
                    database.budgetDao().insertBudget(b.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("BUDGET", b.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                // 5. Recurring Rules
                recurring.filter { !it.isDeleted }.forEach { rec ->
                    val r = json.decodeFromString<RecurringTransaction>(rec.jsonPayload)
                    database.recurringTransactionDao().insertRecurring(r.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("RECURRING_RULE", r.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                // 6. Goals, Assets, Debts, Rules, Filters
                goals.filter { !it.isDeleted }.forEach { rec ->
                    val g = json.decodeFromString<FinancialGoal>(rec.jsonPayload)
                    database.financialGoalDao().insertGoal(g.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("GOAL", g.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                assets.filter { !it.isDeleted }.forEach { rec ->
                    val a = json.decodeFromString<InvestmentAsset>(rec.jsonPayload)
                    database.assetDao().insertAsset(a.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("ASSET", a.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                debts.filter { !it.isDeleted }.forEach { rec ->
                    val d = json.decodeFromString<Debt>(rec.jsonPayload)
                    database.debtDao().insertDebt(d.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("DEBT", d.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                rules.filter { !it.isDeleted }.forEach { rec ->
                    val r = json.decodeFromString<CategorizationRule>(rec.jsonPayload)
                    database.categorizationRuleDao().insertRule(r.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("CATEGORIZATION_RULE", r.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                filters.filter { !it.isDeleted }.forEach { rec ->
                    val f = json.decodeFromString<SavedFilter>(rec.jsonPayload)
                    database.savedFilterDao().insertSavedFilter(f.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("SAVED_FILTER", f.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }
            }

            // Sync settings
            syncSettingsInternal(uid)

            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            setSuccessStatus()
            userPreferencesDataStore.setSyncState("SUCCESS", now, null)

            Result.success(SyncResult(isSuccess = true, downloadedCount = restoredCount, syncedAt = now))
        } catch (t: Throwable) {
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(t)
        }
    }

    override suspend fun handleAccountSwitch(previousUid: String?, newUid: String): Result<Unit> = withContext(dispatcher) {
        try {
            if (previousUid != null && previousUid != newUid) {
                clearLocalData()
            }

            performFullSync()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun clearLocalData() {
        database.withTransaction {
            clearLocalDatabaseTables()
            syncDao.deleteAllSyncRecords()
        }
        _lastSyncTimestamp.value = 0L
        userPreferencesDataStore.setSyncState("IDLE", 0L, null)
        userPreferencesDataStore.resetSettingsUpdatedAt()
    }

    override suspend fun deleteCloudData(): Result<Unit> = withContext(dispatcher) {
        val user = authRepository.currentUser.value
            ?: return@withContext Result.failure(IllegalStateException("No authenticated user for cloud data deletion"))
        try {
            _syncStatus.value = SyncStatus.SYNCING
            cloudStorage.clearUserStorage(user.uid).getOrThrow()
            _lastSyncTimestamp.value = 0L
            userPreferencesDataStore.setSyncState("IDLE", 0L, null)
            _syncStatus.value = SyncStatus.IDLE
            Result.success(Unit)
        } catch (t: Throwable) {
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(t)
        }
    }

    override suspend fun syncSettings(): Result<Unit> = withContext(dispatcher) {
        val user = authRepository.currentUser.value
            ?: return@withContext Result.failure(IllegalStateException("No authenticated user for cloud sync"))
        try {
            syncSettingsInternal(user.uid)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private suspend fun clearLocalDatabaseTables() {
        database.transactionDao().deleteAllTransactions()
        database.recurringTransactionDao().deleteAllOccurrences()
        database.recurringTransactionDao().deleteAllRecurring()
        database.budgetDao().deleteAllBudgets()
        database.financialGoalDao().deleteAllGoals()
        database.assetDao().deleteAllAssets()
        database.debtDao().deleteAllDebts()
        database.categorizationRuleDao().deleteAllRules()
        database.savedFilterDao().deleteAllSavedFilters()
        database.accountDao().deleteAllAccounts()
        database.categoryDao().deleteAllCategories()
    }

    private suspend fun serializeEntity(type: String, id: String): String? {
        return when (type) {
            "TRANSACTION" -> database.transactionDao().getTransactionById(id)?.toDomain()?.let { json.encodeToString(it) }
            "ACCOUNT" -> database.accountDao().getAccountById(id)?.toDomain()?.let { json.encodeToString(it) }
            "CATEGORY" -> database.categoryDao().getCategoryById(id)?.toDomain()?.let { json.encodeToString(it) }
            "BUDGET" -> database.budgetDao().getBudgetById(id)?.toDomain()?.let { json.encodeToString(it) }
            "RECURRING_RULE" -> database.recurringTransactionDao().getRecurringById(id)?.toDomain()?.let { json.encodeToString(it) }
            "GOAL" -> database.financialGoalDao().getGoalById(id)?.toDomain()?.let { json.encodeToString(it) }
            "ASSET" -> database.assetDao().getAssetById(id)?.toDomain()?.let { json.encodeToString(it) }
            "DEBT" -> database.debtDao().getDebtById(id)?.toDomain()?.let { json.encodeToString(it) }
            "CATEGORIZATION_RULE" -> database.categorizationRuleDao().getRuleById(id)?.toDomain()?.let { json.encodeToString(it) }
            "SAVED_FILTER" -> database.savedFilterDao().getSavedFilterById(id)?.toDomain()?.let { json.encodeToString(it) }
            else -> null
        }
    }

    private suspend fun deserializeAndUpsertLocally(type: String, payload: String) {
        if (payload.isBlank()) return
        when (type) {
            "ACCOUNT" -> {
                val entity = json.decodeFromString<Account>(payload).toEntity()
                database.accountDao().insertAccount(entity)
            }
            "CATEGORY" -> {
                val entity = json.decodeFromString<Category>(payload).toEntity()
                database.categoryDao().insertCategory(entity)
            }
            "TRANSACTION" -> {
                val entity = json.decodeFromString<Transaction>(payload).toEntity()
                database.transactionDao().insertTransaction(entity)
            }
            "BUDGET" -> {
                val entity = json.decodeFromString<Budget>(payload).toEntity()
                database.budgetDao().insertBudget(entity)
            }
            "RECURRING_RULE" -> {
                val entity = json.decodeFromString<RecurringTransaction>(payload).toEntity()
                database.recurringTransactionDao().insertRecurring(entity)
            }
            "GOAL" -> {
                val entity = json.decodeFromString<FinancialGoal>(payload).toEntity()
                database.financialGoalDao().insertGoal(entity)
            }
            "ASSET" -> {
                val entity = json.decodeFromString<InvestmentAsset>(payload).toEntity()
                database.assetDao().insertAsset(entity)
            }
            "DEBT" -> {
                val entity = json.decodeFromString<Debt>(payload).toEntity()
                database.debtDao().insertDebt(entity)
            }
            "CATEGORIZATION_RULE" -> {
                val entity = json.decodeFromString<CategorizationRule>(payload).toEntity()
                database.categorizationRuleDao().insertRule(entity)
            }
            "SAVED_FILTER" -> {
                val entity = json.decodeFromString<SavedFilter>(payload).toEntity()
                database.savedFilterDao().insertSavedFilter(entity)
            }
        }
    }

    private suspend fun deleteEntityLocally(type: String, id: String) {
        when (type) {
            "TRANSACTION" -> database.transactionDao().deleteTransactionById(id)
            "ACCOUNT" -> database.accountDao().deleteAccountById(id)
            "CATEGORY" -> database.categoryDao().deleteCategoryById(id)
            "BUDGET" -> database.budgetDao().deleteBudgetById(id)
            "RECURRING_RULE" -> database.recurringTransactionDao().deleteRecurringById(id)
            "GOAL" -> database.financialGoalDao().deleteGoalById(id)
            "ASSET" -> database.assetDao().deleteAssetById(id)
            "DEBT" -> database.debtDao().deleteDebtById(id)
            "CATEGORIZATION_RULE" -> database.categorizationRuleDao().deleteRuleById(id)
            "SAVED_FILTER" -> database.savedFilterDao().deleteSavedFilterById(id)
        }
    }

    private fun getCollectionName(type: String): String {
        return when (type) {
            "TRANSACTION" -> "transactions"
            "ACCOUNT" -> "accounts"
            "CATEGORY" -> "categories"
            "BUDGET" -> "budgets"
            "RECURRING_RULE" -> "recurring_rules"
            "GOAL" -> "goals"
            "ASSET" -> "assets"
            "DEBT" -> "debts"
            "CATEGORIZATION_RULE" -> "categorization_rules"
            "SAVED_FILTER" -> "saved_filters"
            "SETTINGS" -> "settings"
            else -> type.lowercase()
        }
    }
}

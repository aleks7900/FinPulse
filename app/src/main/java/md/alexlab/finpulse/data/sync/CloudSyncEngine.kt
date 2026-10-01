package md.alexlab.finpulse.data.sync

import android.util.Log
import androidx.room.withTransaction
import md.alexlab.finpulse.core.database.FinPulseDatabase
import md.alexlab.finpulse.core.database.entity.SyncRecordEntity
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
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
import md.alexlab.finpulse.domain.model.sync.CloudAccountSettings
import md.alexlab.finpulse.domain.model.sync.CloudSettings
import md.alexlab.finpulse.domain.model.sync.SyncEntityType
import md.alexlab.finpulse.domain.model.sync.SyncResult
import md.alexlab.finpulse.domain.model.sync.SyncStatus
import md.alexlab.finpulse.domain.repository.AuthRepository
import md.alexlab.finpulse.domain.repository.CloudSyncRepository
import md.alexlab.finpulse.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CloudSyncEngine(
    private val database: FinPulseDatabase,
    private val cloudStorage: CloudStorageDataSource,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : CloudSyncRepository {

    companion object {
        private const val TAG = "FinPulseSyncEngine"
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val syncDao = database.syncRecordDao()

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(0L)
    override val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    override val pendingChangesCount: Flow<Int> = syncDao.getPendingCountFlow()

    suspend fun initialize() {
        val prefs = userPreferencesDataStore.userPreferencesFlow.first()
        _lastSyncTimestamp.value = prefs.lastSyncTimestamp
    }

    override suspend fun performFullSync(): Result<SyncResult> = withContext(dispatcher) {
        val user = authRepository.currentUser.value
        if (user == null) {
            _syncStatus.value = SyncStatus.IDLE
            Log.w(TAG, "performFullSync aborted: No authenticated user in authRepository")
            return@withContext Result.failure(IllegalStateException("No authenticated user for cloud sync"))
        }

        Log.i(TAG, "Starting performFullSync for user ${user.uid}")
        _syncStatus.value = SyncStatus.SYNCING
        try {
            val prefs = userPreferencesDataStore.userPreferencesFlow.first()
            val sinceTimestamp = prefs.lastSyncTimestamp

            // 1. Upload pending local changes
            val uploadResult = uploadPendingChangesInternal(user.uid)

            // 2. Download remote changes since last sync
            val downloadResult = downloadRemoteChangesInternal(user.uid, sinceTimestamp)

            // 3. Sync portable account settings
            syncSettingsInternal(user.uid)

            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            _syncStatus.value = SyncStatus.SUCCESS
            userPreferencesDataStore.setSyncState("SUCCESS", now, null)

            val totalResult = SyncResult(
                isSuccess = true,
                uploadedCount = uploadResult.uploadedCount,
                downloadedCount = downloadResult.downloadedCount,
                deletedCount = uploadResult.deletedCount + downloadResult.deletedCount,
                conflictsResolvedCount = downloadResult.conflictsResolvedCount,
                syncedAt = now
            )
            Log.i(TAG, "performFullSync finished successfully: uploaded=${totalResult.uploadedCount}, downloaded=${totalResult.downloadedCount}, deleted=${totalResult.deletedCount}")
            Result.success(totalResult)
        } catch (t: Throwable) {
            Log.e(TAG, "performFullSync failed for ${user.uid}: ${t.message}", t)
            _syncStatus.value = SyncStatus.ERROR
            userPreferencesDataStore.setSyncState("ERROR", _lastSyncTimestamp.value, t.message)
            Result.failure(t)
        }
    }

    override suspend fun uploadPendingChanges(): Result<SyncResult> = withContext(dispatcher) {
        val user = authRepository.currentUser.value ?: return@withContext Result.failure(IllegalStateException("User not logged in"))
        _syncStatus.value = SyncStatus.SYNCING
        try {
            val res = uploadPendingChangesInternal(user.uid)
            _syncStatus.value = SyncStatus.SUCCESS
            Result.success(res)
        } catch (t: Throwable) {
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(t)
        }
    }

    override suspend fun downloadRemoteChanges(): Result<SyncResult> = withContext(dispatcher) {
        val user = authRepository.currentUser.value ?: return@withContext Result.failure(IllegalStateException("User not logged in"))
        _syncStatus.value = SyncStatus.SYNCING
        try {
            val since = _lastSyncTimestamp.value
            val res = downloadRemoteChangesInternal(user.uid, since)
            _syncStatus.value = SyncStatus.SUCCESS
            Result.success(res)
        } catch (t: Throwable) {
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(t)
        }
    }

    private suspend fun uploadPendingChangesInternal(uid: String): SyncResult {
        val pendingUpserts = syncDao.getPendingUpserts()
        val pendingDeletes = syncDao.getPendingDeletes()

        var uploaded = 0
        var deleted = 0

        // Handle tombstones (pending deletes)
        for (deleteRecord in pendingDeletes) {
            val collection = getCollectionName(deleteRecord.entityType)
            cloudStorage.recordTombstone(
                uid = uid,
                collection = collection,
                id = deleteRecord.entityId,
                deletedAt = deleteRecord.deletedAt ?: System.currentTimeMillis()
            )
            syncDao.markAsSynced(deleteRecord.entityType, deleteRecord.entityId, System.currentTimeMillis())
            deleted++
        }

        // Group upserts by entity type and upload in batches
        val byType = pendingUpserts.groupBy { it.entityType }
        for ((type, records) in byType) {
            val cloudRecords = mutableListOf<CloudEntityRecord>()
            for (rec in records) {
                val jsonPayload = serializeEntity(type, rec.entityId)
                if (jsonPayload != null) {
                    cloudRecords.add(
                        CloudEntityRecord(
                            id = rec.entityId,
                            collection = getCollectionName(type),
                            jsonPayload = jsonPayload,
                            updatedAt = rec.localUpdatedAt,
                            isDeleted = false
                        )
                    )
                }
            }

            if (cloudRecords.isNotEmpty()) {
                val count = cloudStorage.uploadRecords(uid, getCollectionName(type), cloudRecords).getOrThrow()
                uploaded += count
                val now = System.currentTimeMillis()
                for (rec in records) {
                    syncDao.markAsSynced(type, rec.entityId, now)
                }
            }
        }

        return SyncResult(isSuccess = true, uploadedCount = uploaded, deletedCount = deleted)
    }

    private suspend fun downloadRemoteChangesInternal(uid: String, sinceTimestamp: Long): SyncResult {
        var downloaded = 0
        var deleted = 0
        var conflictsResolved = 0

        val collections = listOf(
            SyncEntityType.ACCOUNT.name,
            SyncEntityType.CATEGORY.name,
            SyncEntityType.TRANSACTION.name,
            SyncEntityType.BUDGET.name,
            SyncEntityType.RECURRING_RULE.name,
            SyncEntityType.GOAL.name,
            SyncEntityType.ASSET.name,
            SyncEntityType.DEBT.name,
            SyncEntityType.CATEGORIZATION_RULE.name,
            SyncEntityType.SAVED_FILTER.name
        )

        for (type in collections) {
            val colName = getCollectionName(type)
            val remoteRecords = cloudStorage.downloadRecords(uid, colName, sinceTimestamp).getOrThrow()

            for (remote in remoteRecords) {
                val localSync = syncDao.getSyncRecord(type, remote.id)

                if (remote.isDeleted) {
                    // Tombstone received from remote: delete locally
                    deleteEntityLocally(type, remote.id)
                    syncDao.upsertSyncRecord(
                        SyncRecordEntity(
                            entityType = type,
                            entityId = remote.id,
                            syncStatus = "SYNCED",
                            localUpdatedAt = remote.updatedAt,
                            cloudUpdatedAt = remote.updatedAt,
                            isDeleted = true,
                            deletedAt = remote.deletedAt ?: remote.updatedAt
                        )
                    )
                    deleted++
                } else {
                    // Remote upsert: Check Conflict Resolution (Last-Write-Wins)
                    val shouldApplyRemote = if (localSync != null) {
                        if (localSync.syncStatus == "PENDING_UPSERT" || localSync.syncStatus == "PENDING_DELETE") {
                            conflictsResolved++
                            // LWW: remote is applied only if remote timestamp >= local timestamp
                            remote.updatedAt >= localSync.localUpdatedAt
                        } else {
                            true
                        }
                    } else {
                        true
                    }

                    if (shouldApplyRemote) {
                        deserializeAndUpsertLocally(type, remote.jsonPayload)
                        syncDao.upsertSyncRecord(
                            SyncRecordEntity(
                                entityType = type,
                                entityId = remote.id,
                                syncStatus = "SYNCED",
                                localUpdatedAt = remote.updatedAt,
                                cloudUpdatedAt = remote.updatedAt,
                                isDeleted = false
                            )
                        )
                        downloaded++
                    }
                }
            }
        }

        return SyncResult(
            isSuccess = true,
            downloadedCount = downloaded,
            deletedCount = deleted,
            conflictsResolvedCount = conflictsResolved
        )
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
            _syncStatus.value = SyncStatus.SUCCESS
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
            var restoredCount = 0

            database.withTransaction {
                // Clear local data for clean restoration
                clearLocalDatabaseTables()

                // Download all collections
                // 1. Categories
                val categories = cloudStorage.downloadRecords(uid, "categories", 0L).getOrDefault(emptyList())
                categories.filter { !it.isDeleted }.forEach { rec ->
                    val cat = json.decodeFromString<Category>(rec.jsonPayload)
                    database.categoryDao().insertCategory(cat.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("CATEGORY", cat.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                // 2. Accounts
                val accounts = cloudStorage.downloadRecords(uid, "accounts", 0L).getOrDefault(emptyList())
                accounts.filter { !it.isDeleted }.forEach { rec ->
                    val acc = json.decodeFromString<Account>(rec.jsonPayload)
                    database.accountDao().insertAccount(acc.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("ACCOUNT", acc.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                // 3. Transactions
                val transactions = cloudStorage.downloadRecords(uid, "transactions", 0L).getOrDefault(emptyList())
                transactions.filter { !it.isDeleted }.forEach { rec ->
                    val tx = json.decodeFromString<Transaction>(rec.jsonPayload)
                    database.transactionDao().insertTransaction(tx.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("TRANSACTION", tx.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                // 4. Budgets
                val budgets = cloudStorage.downloadRecords(uid, "budgets", 0L).getOrDefault(emptyList())
                budgets.filter { !it.isDeleted }.forEach { rec ->
                    val b = json.decodeFromString<Budget>(rec.jsonPayload)
                    database.budgetDao().insertBudget(b.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("BUDGET", b.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                // 5. Recurring Rules
                val recurring = cloudStorage.downloadRecords(uid, "recurring_rules", 0L).getOrDefault(emptyList())
                recurring.filter { !it.isDeleted }.forEach { rec ->
                    val r = json.decodeFromString<RecurringTransaction>(rec.jsonPayload)
                    database.recurringTransactionDao().insertRecurring(r.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("RECURRING_RULE", r.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                // 6. Goals, Assets, Debts, Rules, Filters
                val goals = cloudStorage.downloadRecords(uid, "goals", 0L).getOrDefault(emptyList())
                goals.filter { !it.isDeleted }.forEach { rec ->
                    val g = json.decodeFromString<FinancialGoal>(rec.jsonPayload)
                    database.financialGoalDao().insertGoal(g.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("GOAL", g.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                val assets = cloudStorage.downloadRecords(uid, "assets", 0L).getOrDefault(emptyList())
                assets.filter { !it.isDeleted }.forEach { rec ->
                    val a = json.decodeFromString<InvestmentAsset>(rec.jsonPayload)
                    database.assetDao().insertAsset(a.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("ASSET", a.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                val debts = cloudStorage.downloadRecords(uid, "debts", 0L).getOrDefault(emptyList())
                debts.filter { !it.isDeleted }.forEach { rec ->
                    val d = json.decodeFromString<Debt>(rec.jsonPayload)
                    database.debtDao().insertDebt(d.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("DEBT", d.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                val rules = cloudStorage.downloadRecords(uid, "categorization_rules", 0L).getOrDefault(emptyList())
                rules.filter { !it.isDeleted }.forEach { rec ->
                    val r = json.decodeFromString<CategorizationRule>(rec.jsonPayload)
                    database.categorizationRuleDao().insertRule(r.toEntity())
                    syncDao.upsertSyncRecord(SyncRecordEntity("CATEGORIZATION_RULE", r.id, "SYNCED", rec.updatedAt, rec.updatedAt))
                    restoredCount++
                }

                val filters = cloudStorage.downloadRecords(uid, "saved_filters", 0L).getOrDefault(emptyList())
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
            _syncStatus.value = SyncStatus.SUCCESS
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
                // Clear local data completely before loading new user data to guarantee account isolation
                clearLocalData()
            }

            // Inspect cloud data for newUid
            val summary = cloudStorage.getUserSummary(newUid).getOrDefault(null)
            val hasRemoteData = summary != null && summary.totalRecords > 0
            val localTxCount = database.transactionDao().getAllTransactions().size

            if (hasRemoteData && localTxCount == 0) {
                // New device/clean local state -> restore from cloud
                restoreCloudDataToLocal(newUid)
            } else if (!hasRemoteData && localTxCount > 0) {
                // Initial migration of existing local user
                migrateLocalDataToCloud(newUid)
            } else if (hasRemoteData && localTxCount > 0) {
                // Normal sync or merge
                performFullSync()
            } else {
                // Synchronize settings for initial setup
                syncSettingsInternal(newUid)
            }
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
            "TRANSACTION" -> {
                val entity = json.decodeFromString<Transaction>(payload).toEntity()
                database.transactionDao().insertTransaction(entity)
            }
            "ACCOUNT" -> {
                val entity = json.decodeFromString<Account>(payload).toEntity()
                database.accountDao().insertAccount(entity)
            }
            "CATEGORY" -> {
                val entity = json.decodeFromString<Category>(payload).toEntity()
                database.categoryDao().insertCategory(entity)
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

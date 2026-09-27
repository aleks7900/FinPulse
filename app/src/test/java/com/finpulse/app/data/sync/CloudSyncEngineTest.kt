package com.finpulse.app.data.sync

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.finpulse.app.core.database.FinPulseDatabase
import com.finpulse.app.core.database.dao.AccountDao
import com.finpulse.app.core.database.dao.CategoryDao
import com.finpulse.app.core.database.dao.SyncRecordDao
import com.finpulse.app.core.database.dao.TransactionDao
import com.finpulse.app.core.database.entity.AccountEntity
import com.finpulse.app.core.database.entity.CategoryEntity
import com.finpulse.app.core.database.entity.SyncRecordEntity
import com.finpulse.app.core.database.entity.TransactionEntity
import com.finpulse.app.core.datastore.UserPreferences
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.data.cloud.CloudEntityRecord
import com.finpulse.app.data.cloud.InMemoryCloudStorageDataSource
import com.finpulse.app.data.mapper.toDomain
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.model.sync.CloudUser
import com.finpulse.app.domain.model.sync.SyncStatus
import com.finpulse.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CloudSyncEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private lateinit var fakeSyncDao: FakeTestSyncRecordDao
    private lateinit var fakeTransactionDao: FakeTestTransactionDao
    private lateinit var fakeAccountDao: FakeTestAccountDao
    private lateinit var fakeCategoryDao: FakeTestCategoryDao
    private lateinit var mockDatabase: FinPulseDatabase
    private lateinit var inMemoryCloud: InMemoryCloudStorageDataSource
    private lateinit var mockDataStore: UserPreferencesDataStore
    private lateinit var fakeAuthRepo: FakeTestAuthRepository
    private lateinit var syncEngine: CloudSyncEngine

    private val userA = CloudUser(uid = "user-A", email = "userA@finpulse.app", displayName = "User A")
    private val userB = CloudUser(uid = "user-B", email = "userB@finpulse.app", displayName = "User B")

    @Before
    fun setup() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { any<RoomDatabase>().withTransaction<Any?>(any()) } coAnswers {
            val block = secondArg<suspend () -> Any?>()
            block()
        }

        fakeSyncDao = FakeTestSyncRecordDao()
        fakeTransactionDao = FakeTestTransactionDao()
        fakeAccountDao = FakeTestAccountDao()
        fakeCategoryDao = FakeTestCategoryDao()

        mockDatabase = mockk(relaxed = true)
        every { mockDatabase.syncRecordDao() } returns fakeSyncDao
        every { mockDatabase.transactionDao() } returns fakeTransactionDao
        every { mockDatabase.accountDao() } returns fakeAccountDao
        every { mockDatabase.categoryDao() } returns fakeCategoryDao

        inMemoryCloud = InMemoryCloudStorageDataSource()
        mockDataStore = mockk(relaxed = true)
        coEvery { mockDataStore.userPreferencesFlow } returns flowOf(UserPreferences(lastSyncTimestamp = 0L))

        fakeAuthRepo = FakeTestAuthRepository(currentUser = userA)

        syncEngine = CloudSyncEngine(
            database = mockDatabase,
            cloudStorage = inMemoryCloud,
            userPreferencesDataStore = mockDataStore,
            authRepository = fakeAuthRepo,
            dispatcher = testDispatcher
        )
    }

    @Test
    fun `local to cloud - create transaction queues as pending and uploads cleanly`() = testScope.runTest {
        val tx = createSampleTransactionEntity(id = "tx-1", amount = 2500L, updatedAt = 1000L)
        fakeTransactionDao.insertTransaction(tx)
        fakeSyncDao.upsertSyncRecord(
            SyncRecordEntity("TRANSACTION", tx.id, "PENDING_UPSERT", tx.updatedAt, null)
        )

        val result = syncEngine.uploadPendingChanges()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.uploadedCount)

        // Verify cloud document received the record
        val cloudDocs = inMemoryCloud.downloadRecords(userA.uid, "transactions", 0L).getOrNull()
        assertNotNull(cloudDocs)
        assertEquals(1, cloudDocs?.size)
        assertEquals("tx-1", cloudDocs?.first()?.id)

        // Verify local sync status updated to SYNCED
        val syncRec = fakeSyncDao.getSyncRecord("TRANSACTION", "tx-1")
        assertEquals("SYNCED", syncRec?.syncStatus)
    }

    @Test
    fun `local to cloud - edit transaction uploads updated payload and updates cloud`() = testScope.runTest {
        // Initial sync
        val initialTx = createSampleTransactionEntity(id = "tx-edit", amount = 1000L, updatedAt = 1000L)
        fakeTransactionDao.insertTransaction(initialTx)
        fakeSyncDao.upsertSyncRecord(SyncRecordEntity("TRANSACTION", initialTx.id, "PENDING_UPSERT", 1000L, null))
        syncEngine.uploadPendingChanges()

        // Edit locally
        val updatedTx = createSampleTransactionEntity(id = "tx-edit", amount = 1500L, updatedAt = 2000L)
        fakeTransactionDao.insertTransaction(updatedTx)
        fakeSyncDao.upsertSyncRecord(SyncRecordEntity("TRANSACTION", updatedTx.id, "PENDING_UPSERT", 2000L, 1000L))

        val result = syncEngine.uploadPendingChanges()
        assertTrue(result.isSuccess)

        val cloudDocs = inMemoryCloud.downloadRecords(userA.uid, "transactions", 0L).getOrNull()
        val downloadedTx = json.decodeFromString<Transaction>(cloudDocs!!.first().jsonPayload)
        assertEquals(1500L, downloadedTx.amount.amountMinor)
    }

    @Test
    fun `local to cloud - delete transaction uploads tombstone preventing resurrection`() = testScope.runTest {
        // Create tombstone locally
        fakeSyncDao.markAsDeleted("TRANSACTION", "tx-del", 3000L)

        val result = syncEngine.uploadPendingChanges()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.deletedCount)

        // Verify cloud contains tombstone
        val cloudDocs = inMemoryCloud.downloadRecords(userA.uid, "transactions", 0L).getOrNull()
        assertNotNull(cloudDocs)
        assertEquals(1, cloudDocs?.size)
        assertTrue(cloudDocs!!.first().isDeleted)
        assertEquals("tx-del", cloudDocs.first().id)
    }

    @Test
    fun `cloud to local - download remote transaction inserts into local database`() = testScope.runTest {
        val remoteTx = createSampleTransaction(id = "tx-remote", amount = 9900L, updatedAt = 5000L)
        val cloudRecord = CloudEntityRecord(
            id = remoteTx.id,
            collection = "transactions",
            jsonPayload = json.encodeToString(remoteTx),
            updatedAt = 5000L,
            isDeleted = false
        )
        inMemoryCloud.uploadRecords(userA.uid, "transactions", listOf(cloudRecord))

        val result = syncEngine.downloadRemoteChanges()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.downloadedCount)

        val localTx = fakeTransactionDao.getTransactionById("tx-remote")
        assertNotNull(localTx)
        assertEquals(9900L, localTx?.amount)
        assertEquals("SYNCED", fakeSyncDao.getSyncRecord("TRANSACTION", "tx-remote")?.syncStatus)
    }

    @Test
    fun `cloud to local - remote tombstone deletes local record`() = testScope.runTest {
        val localTx = createSampleTransactionEntity(id = "tx-peer-del", amount = 5000L, updatedAt = 1000L)
        fakeTransactionDao.insertTransaction(localTx)
        fakeSyncDao.upsertSyncRecord(SyncRecordEntity("TRANSACTION", localTx.id, "SYNCED", 1000L, 1000L))

        // Remote peer uploaded a tombstone with newer timestamp
        inMemoryCloud.recordTombstone(userA.uid, "transactions", "tx-peer-del", 4000L)

        val result = syncEngine.downloadRemoteChanges()
        assertTrue(result.isSuccess)

        // Verify local transaction was deleted
        assertNull(fakeTransactionDao.getTransactionById("tx-peer-del"))
    }

    @Test
    fun `conflict resolution - Last-Write-Wins selects newest modification`() = testScope.runTest {
        // Scenario 1: Remote is newer than local -> Remote overwrites local
        val localTx = createSampleTransactionEntity(id = "tx-conf", amount = 1000L, updatedAt = 1000L)
        fakeTransactionDao.insertTransaction(localTx)
        fakeSyncDao.upsertSyncRecord(SyncRecordEntity("TRANSACTION", "tx-conf", "SYNCED", 1000L, 1000L))

        val remoteNewerTx = createSampleTransaction(id = "tx-conf", amount = 2000L, updatedAt = 3000L)
        inMemoryCloud.uploadRecords(
            userA.uid,
            "transactions",
            listOf(CloudEntityRecord("tx-conf", "transactions", json.encodeToString(remoteNewerTx), 3000L))
        )

        val downloadResult = syncEngine.downloadRemoteChanges()
        assertTrue(downloadResult.isSuccess)

        val resolvedTx = fakeTransactionDao.getTransactionById("tx-conf")
        assertEquals(2000L, resolvedTx?.amount) // Remote won

        // Scenario 2: Local is newer than remote -> Local overwrites remote
        val localNewerTx = createSampleTransactionEntity(id = "tx-conf", amount = 5000L, updatedAt = 9000L)
        fakeTransactionDao.insertTransaction(localNewerTx)
        fakeSyncDao.upsertSyncRecord(SyncRecordEntity("TRANSACTION", "tx-conf", "PENDING_UPSERT", 9000L, 3000L))

        val uploadResult = syncEngine.uploadPendingChanges()
        assertTrue(uploadResult.isSuccess)

        val finalCloudDocs = inMemoryCloud.downloadRecords(userA.uid, "transactions", 0L).getOrNull()
        val parsed = json.decodeFromString<Transaction>(finalCloudDocs!!.first().jsonPayload)
        assertEquals(5000L, parsed.amount.amountMinor) // Local won
    }

    @Test
    fun `account isolation - user A data never leaks to user B`() = testScope.runTest {
        // User A uploads records
        val txA = createSampleTransactionEntity(id = "tx-user-a", amount = 7700L, updatedAt = 1000L)
        fakeTransactionDao.insertTransaction(txA)
        fakeSyncDao.upsertSyncRecord(SyncRecordEntity("TRANSACTION", txA.id, "PENDING_UPSERT", 1000L, null))
        syncEngine.uploadPendingChanges()

        // User A logs out and User B logs in
        syncEngine.clearLocalDataAndSwitchAccount(newUid = userB.uid)
        fakeAuthRepo.setUser(userB)

        // Local tables should be purged of User A data
        assertNull(fakeTransactionDao.getTransactionById("tx-user-a"))
        assertEquals(0, fakeTransactionDao.getAllTransactions().size)

        // User B cloud storage has 0 records
        val userBCloud = inMemoryCloud.downloadRecords(userB.uid, "transactions", 0L).getOrNull()
        assertTrue(userBCloud.isNullOrEmpty())

        // User A cloud storage still retains User A data
        val userACloud = inMemoryCloud.downloadRecords(userA.uid, "transactions", 0L).getOrNull()
        assertEquals(1, userACloud?.size)
        assertEquals("tx-user-a", userACloud?.first()?.id)
    }

    @Test
    fun `initial sign-in migration - existing local data uploaded safely to cloud without loss`() = testScope.runTest {
        // User created accounts and categories locally before ever signing into Google
        val localAccount = AccountEntity("acc-local", "Cash Wallet", "CASH", 10000L, "USD", 10000L, "USD", false, 1, 0, null, 1000L, 1000L)
        val localCategory = CategoryEntity("cat-local", "Groceries", "EXPENSE", null, "cart", 0xFF00FF, true, 1)
        val localTx = createSampleTransactionEntity(id = "tx-pre-auth", amount = 4200L, updatedAt = 1000L)

        fakeAccountDao.insertAccount(localAccount)
        fakeCategoryDao.insertCategory(localCategory)
        fakeTransactionDao.insertTransaction(localTx)

        val migrateResult = syncEngine.migrateLocalDataToCloud(userA.uid)
        assertTrue(migrateResult.isSuccess)
        assertTrue(migrateResult.getOrNull()!!.uploadedCount >= 3)

        // Verify all 3 entities exist in cloud under userA
        val cloudAccounts = inMemoryCloud.downloadRecords(userA.uid, "accounts", 0L).getOrNull()
        val cloudCategories = inMemoryCloud.downloadRecords(userA.uid, "categories", 0L).getOrNull()
        val cloudTxs = inMemoryCloud.downloadRecords(userA.uid, "transactions", 0L).getOrNull()

        assertEquals(1, cloudAccounts?.size)
        assertEquals(1, cloudCategories?.size)
        assertEquals(1, cloudTxs?.size)
    }

    @Test
    fun `offline to online sync - offline operations queue and flush when connectivity restored`() = testScope.runTest {
        inMemoryCloud.shouldSimulateNetworkError = true

        val tx = createSampleTransactionEntity(id = "tx-offline", amount = 1200L, updatedAt = 1000L)
        fakeTransactionDao.insertTransaction(tx)
        fakeSyncDao.upsertSyncRecord(SyncRecordEntity("TRANSACTION", tx.id, "PENDING_UPSERT", 1000L, null))

        // Sync while offline should fail gracefully without crashing or losing data
        val offlineResult = syncEngine.uploadPendingChanges()
        assertFalse(offlineResult.isSuccess)
        assertEquals(SyncStatus.ERROR, syncEngine.syncStatus.value)

        // Local record is still safely persisted in Room
        assertNotNull(fakeTransactionDao.getTransactionById("tx-offline"))

        // Connectivity restored
        inMemoryCloud.shouldSimulateNetworkError = false
        val onlineResult = syncEngine.uploadPendingChanges()
        assertTrue(onlineResult.isSuccess)
        assertEquals(SyncStatus.SUCCESS, syncEngine.syncStatus.value)

        val cloudDocs = inMemoryCloud.downloadRecords(userA.uid, "transactions", 0L).getOrNull()
        assertEquals(1, cloudDocs?.size)
    }

    @Test
    fun `idempotency - repeated sync invocations do not duplicate data`() = testScope.runTest {
        val tx = createSampleTransactionEntity(id = "tx-idempotent", amount = 3000L, updatedAt = 1000L)
        fakeTransactionDao.insertTransaction(tx)
        fakeSyncDao.upsertSyncRecord(SyncRecordEntity("TRANSACTION", tx.id, "PENDING_UPSERT", 1000L, null))

        // First sync
        syncEngine.performFullSync()
        val firstCloudCount = inMemoryCloud.downloadRecords(userA.uid, "transactions", 0L).getOrNull()?.size ?: 0
        assertEquals(1, firstCloudCount)

        // Repeat sync 3 times
        syncEngine.performFullSync()
        syncEngine.performFullSync()
        syncEngine.performFullSync()

        val finalCloudDocs = inMemoryCloud.downloadRecords(userA.uid, "transactions", 0L).getOrNull()
        assertEquals(1, finalCloudDocs?.size) // Still exactly 1 record, zero duplicates
        assertEquals(1, fakeTransactionDao.getAllTransactions().size)
    }

    // Helper builders
    private fun createSampleTransactionEntity(id: String, amount: Long, updatedAt: Long) = TransactionEntity(
        id = id,
        sourceAccountId = "acc-1",
        destinationAccountId = null,
        categoryId = "cat-1",
        amount = amount,
        currency = "USD",
        type = "EXPENSE",
        timestamp = 1718000000000L,
        merchant = "Market",
        description = "Groceries",
        note = null,
        receiptUri = null,
        isRecurring = false,
        recurringRuleId = null,
        transferFee = null,
        tags = emptyList(),
        isExcludedFromBudget = false,
        createdAt = updatedAt,
        updatedAt = updatedAt
    )

    private fun createSampleTransaction(id: String, amount: Long, updatedAt: Long) =
        createSampleTransactionEntity(id, amount, updatedAt).toDomain()
}

// In-Memory Test DAOs & Fakes
private class FakeTestSyncRecordDao : SyncRecordDao {
    private val records = mutableMapOf<Pair<String, String>, SyncRecordEntity>()
    private val flow = MutableStateFlow(0)

    override suspend fun upsertSyncRecord(record: SyncRecordEntity) {
        records[record.entityType to record.entityId] = record
        updatePendingCount()
    }

    override suspend fun upsertSyncRecords(recordsList: List<SyncRecordEntity>) {
        recordsList.forEach { records[it.entityType to it.entityId] = it }
        updatePendingCount()
    }

    override suspend fun getSyncRecord(entityType: String, entityId: String): SyncRecordEntity? =
        records[entityType to entityId]

    override suspend fun getPendingUpserts(): List<SyncRecordEntity> =
        records.values.filter { it.syncStatus == "PENDING_UPSERT" && !it.isDeleted }

    override suspend fun getPendingDeletes(): List<SyncRecordEntity> =
        records.values.filter { it.isDeleted || it.syncStatus == "PENDING_DELETE" }

    override suspend fun getTombstones(): List<SyncRecordEntity> =
        records.values.filter { it.isDeleted }

    override suspend fun markAsSynced(entityType: String, entityId: String, cloudUpdatedAt: Long) {
        val existing = records[entityType to entityId]
        if (existing != null) {
            records[entityType to entityId] = existing.copy(
                syncStatus = "SYNCED",
                cloudUpdatedAt = cloudUpdatedAt,
                errorMessage = null
            )
            updatePendingCount()
        }
    }

    override suspend fun markAsDeleted(entityType: String, entityId: String, deletedAt: Long) {
        val existing = records[entityType to entityId]
        records[entityType to entityId] = (existing ?: SyncRecordEntity(
            entityType = entityType,
            entityId = entityId,
            syncStatus = "PENDING_DELETE",
            localUpdatedAt = deletedAt
        )).copy(
            syncStatus = "PENDING_DELETE",
            isDeleted = true,
            deletedAt = deletedAt
        )
        updatePendingCount()
    }

    override suspend fun markAsError(entityType: String, entityId: String, error: String) {
        val existing = records[entityType to entityId]
        if (existing != null) {
            records[entityType to entityId] = existing.copy(
                syncStatus = "SYNC_ERROR",
                errorMessage = error
            )
        }
    }

    override fun getPendingCountFlow(): Flow<Int> = flow

    override suspend fun deleteSyncRecord(entityType: String, entityId: String) {
        records.remove(entityType to entityId)
        updatePendingCount()
    }

    override suspend fun purgeObsoleteTombstones(cutoffTimestamp: Long): Int {
        val toRemove = records.filter { it.value.isDeleted && (it.value.deletedAt ?: 0) < cutoffTimestamp }
        toRemove.forEach { records.remove(it.key) }
        return toRemove.size
    }

    override suspend fun deleteAllSyncRecords() {
        records.clear()
        updatePendingCount()
    }

    private fun updatePendingCount() {
        flow.value = records.values.count { it.syncStatus.startsWith("PENDING") }
    }
}

private class FakeTestTransactionDao : TransactionDao {
    private val storage = mutableMapOf<String, TransactionEntity>()

    override suspend fun insertTransaction(transaction: TransactionEntity) {
        storage[transaction.id] = transaction
    }

    override suspend fun insertTransactions(transactions: List<TransactionEntity>) {
        transactions.forEach { storage[it.id] = it }
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        storage[transaction.id] = transaction
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        storage.remove(transaction.id)
    }

    override suspend fun deleteTransactionById(id: String) {
        storage.remove(id)
    }

    override suspend fun deleteAllTransactions() {
        storage.clear()
    }

    override suspend fun getTransactionById(id: String): TransactionEntity? = storage[id]
    override suspend fun getAllTransactions(): List<TransactionEntity> = storage.values.toList()

    override fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> = flowOf(storage.values.toList())
    override fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>> = flowOf(storage.values.take(limit))
    override fun getTransactionsByAccountFlow(accountId: String): Flow<List<TransactionEntity>> = flowOf(storage.values.filter { it.sourceAccountId == accountId })
    override fun getTransactionsByCategoryFlow(categoryId: String): Flow<List<TransactionEntity>> = flowOf(storage.values.filter { it.categoryId == categoryId })
    override fun getTransactionsByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        flowOf(storage.values.filter { it.timestamp in startDate..endDate })
    override fun getTransactionsByTypeFlow(type: String): Flow<List<TransactionEntity>> =
        flowOf(storage.values.filter { it.type == type })
    override fun getTransactionsByAccountAndDateRangeFlow(accountId: String, startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        flowOf(storage.values.filter { it.sourceAccountId == accountId && it.timestamp in startDate..endDate })
    override fun getTransactionsByCategoryAndDateRangeFlow(categoryId: String, startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        flowOf(storage.values.filter { it.categoryId == categoryId && it.timestamp in startDate..endDate })
    override fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
        flowOf(storage.values.filter { it.description.contains(query, true) || it.merchant.contains(query, true) })
    override fun getTransactionsWithPendingReceipts(): Flow<List<TransactionEntity>> = flowOf(emptyList())
    override suspend fun getTransactionCount(): Int = storage.size
    override suspend fun getDistinctMerchants(): List<String> = storage.values.map { it.merchant }.distinct()
    override suspend fun getTransactionsByDateRange(startDate: Long, endDate: Long): List<TransactionEntity> =
        storage.values.filter { it.timestamp in startDate..endDate }
    override suspend fun getTransactionsByAccount(accountId: String): List<TransactionEntity> =
        storage.values.filter { it.sourceAccountId == accountId || it.destinationAccountId == accountId }
    override suspend fun getTransactionsByCategory(categoryId: String): List<TransactionEntity> =
        storage.values.filter { it.categoryId == categoryId }
    override fun getAllTransactionsRaw(query: androidx.sqlite.db.SupportSQLiteQuery): List<TransactionEntity> = storage.values.toList()
}

private class FakeTestAccountDao : AccountDao {
    private val storage = mutableMapOf<String, AccountEntity>()

    override suspend fun insertAccount(account: AccountEntity) { storage[account.id] = account }
    override suspend fun insertAccounts(accounts: List<AccountEntity>) { accounts.forEach { storage[it.id] = it } }
    override suspend fun updateAccount(account: AccountEntity) { storage[account.id] = account }
    override suspend fun deleteAccount(account: AccountEntity) { storage.remove(account.id) }
    override suspend fun deleteAllAccounts() { storage.clear() }
    override suspend fun getAccountById(id: String): AccountEntity? = storage[id]
    override suspend fun getAllAccounts(): List<AccountEntity> = storage.values.toList()
    override fun getAllAccountsFlow(): Flow<List<AccountEntity>> = flowOf(storage.values.toList())
    override fun getActiveAccountsFlow(): Flow<List<AccountEntity>> = flowOf(storage.values.filter { !it.isArchived })
    override fun getArchivedAccountsFlow(): Flow<List<AccountEntity>> = flowOf(storage.values.filter { it.isArchived })
    override fun getAccountByIdFlow(id: String): Flow<AccountEntity?> = flowOf(storage[id])
    override suspend fun updateAccountBalance(accountId: String, newBalance: Long, newAvailableBalance: Long, updatedAt: Long) {
        val existing = storage[accountId]
        if (existing != null) {
            storage[accountId] = existing.copy(balanceMinor = newBalance, availableBalanceMinor = newAvailableBalance, updatedAt = updatedAt)
        }
    }
    override suspend fun getAccountCount(): Int = storage.size
}

private class FakeTestCategoryDao : CategoryDao {
    private val storage = mutableMapOf<String, CategoryEntity>()

    override suspend fun insertCategory(category: CategoryEntity) { storage[category.id] = category }
    override suspend fun insertCategories(categories: List<CategoryEntity>) { categories.forEach { storage[it.id] = it } }
    override suspend fun insertCategoriesIgnore(categories: List<CategoryEntity>) { categories.forEach { if (!storage.containsKey(it.id)) storage[it.id] = it } }
    override suspend fun updateCategory(category: CategoryEntity) { storage[category.id] = category }
    override suspend fun deleteCategory(category: CategoryEntity) { storage.remove(category.id) }
    override suspend fun deleteCategoryById(id: String) { storage.remove(id) }
    override suspend fun deleteAllCategories() { storage.clear() }
    override suspend fun getCategoryById(id: String): CategoryEntity? = storage[id]
    override suspend fun getAllCategories(): List<CategoryEntity> = storage.values.toList()
    override fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = flowOf(storage.values.toList())
    override fun getCategoriesByTypeFlow(type: String): Flow<List<CategoryEntity>> = flowOf(storage.values.filter { it.type == type })
    override fun getCategoryByIdFlow(id: String): Flow<CategoryEntity?> = flowOf(storage[id])
    override suspend fun getAllCategoryIds(): List<String> = storage.keys.toList()
    override suspend fun getCategoryCount(): Int = storage.size
}

private class FakeTestAuthRepository(currentUser: CloudUser?) : AuthRepository {
    private val _currentUser = MutableStateFlow(currentUser)
    override val currentUser: StateFlow<CloudUser?> = _currentUser.asStateFlow()

    fun setUser(user: CloudUser?) { _currentUser.value = user }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<CloudUser> {
        val user = CloudUser("uid-token-$idToken", "token@google.com", "Token User")
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun signOut(): Result<Unit> {
        _currentUser.value = null
        return Result.success(Unit)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        _currentUser.value = null
        return Result.success(Unit)
    }
}

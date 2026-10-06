package md.alexlab.finpulse.data.sync

import android.content.Context
import md.alexlab.finpulse.core.datastore.UserPreferences
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.network.NetworkConnectivityMonitor
import md.alexlab.finpulse.core.reporting.NoOpErrorReporter
import md.alexlab.finpulse.domain.model.sync.CloudUser
import md.alexlab.finpulse.domain.model.sync.SyncResult
import md.alexlab.finpulse.domain.model.sync.SyncStatus
import md.alexlab.finpulse.domain.repository.AuthRepository
import md.alexlab.finpulse.domain.repository.CloudSyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CloudSyncCoordinatorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockContext: Context
    private lateinit var mockSyncRepo: CloudSyncRepository
    private lateinit var mockAuthRepo: AuthRepository
    private lateinit var mockNetworkMonitor: NetworkConnectivityMonitor
    private lateinit var mockDataStore: UserPreferencesDataStore

    private val currentUserFlow = MutableStateFlow<CloudUser?>(null)
    private val isOnlineFlow = MutableStateFlow(true)
    private val repoSyncStatusFlow = MutableStateFlow(SyncStatus.IDLE)
    private val lastSyncTimestampFlow = MutableStateFlow(0L)
    private val pendingChangesFlow = MutableStateFlow(0)
    private val userPrefsFlow = MutableStateFlow(UserPreferences())

    private var scheduledIntervals = mutableListOf<Long>()
    private var cancelCount = 0

    private val testUser = CloudUser(uid = "user-123", email = "test@finpulse.app", displayName = "Test User")

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.i(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0

        mockContext = mockk(relaxed = true)
        mockSyncRepo = mockk(relaxed = true)
        mockAuthRepo = mockk(relaxed = true)
        mockNetworkMonitor = mockk(relaxed = true)
        mockDataStore = mockk(relaxed = true)

        scheduledIntervals.clear()
        cancelCount = 0

        currentUserFlow.value = testUser
        isOnlineFlow.value = true
        repoSyncStatusFlow.value = SyncStatus.IDLE
        lastSyncTimestampFlow.value = 1000L
        pendingChangesFlow.value = 0
        userPrefsFlow.value = UserPreferences(
            isAutoSyncEnabled = true,
            isSyncOnStartupEnabled = true,
            syncIntervalMinutes = 60L
        )

        every { mockAuthRepo.currentUser } returns currentUserFlow.asStateFlow()
        every { mockAuthRepo.isAuthenticated } answers { currentUserFlow.value != null }

        every { mockNetworkMonitor.isOnline } returns isOnlineFlow.asStateFlow()
        every { mockNetworkMonitor.isCurrentlyOnline() } answers { isOnlineFlow.value }

        every { mockSyncRepo.syncStatus } returns repoSyncStatusFlow.asStateFlow()
        every { mockSyncRepo.lastSyncTimestamp } returns lastSyncTimestampFlow.asStateFlow()
        every { mockSyncRepo.pendingChangesCount } returns pendingChangesFlow.asStateFlow()

        every { mockDataStore.userPreferencesFlow } returns userPrefsFlow.asStateFlow()
        coEvery { mockDataStore.setAutoSyncEnabled(any()) } answers {
            userPrefsFlow.value = userPrefsFlow.value.copy(isAutoSyncEnabled = firstArg())
        }
        coEvery { mockDataStore.setSyncOnStartupEnabled(any()) } answers {
            userPrefsFlow.value = userPrefsFlow.value.copy(isSyncOnStartupEnabled = firstArg())
        }
        coEvery { mockDataStore.setSyncIntervalMinutes(any()) } answers {
            userPrefsFlow.value = userPrefsFlow.value.copy(syncIntervalMinutes = firstArg())
        }
    }

    private fun createCoordinator(): CloudSyncCoordinator {
        return CloudSyncCoordinator(
            context = mockContext,
            cloudSyncRepository = mockSyncRepo,
            authRepository = mockAuthRepo,
            networkMonitor = mockNetworkMonitor,
            userPreferencesDataStore = mockDataStore,
            errorReporter = NoOpErrorReporter(),
            dispatcher = testDispatcher,
            periodicWorkScheduler = { interval -> scheduledIntervals.add(interval) },
            periodicWorkCanceller = { cancelCount++ }
        )
    }

    @Test
    fun testManualSync_whenOnlineAndAuthenticated_succeeds() = testScope.runTest {
        val expectedResult = SyncResult(isSuccess = true, uploadedCount = 3, downloadedCount = 5)
        coEvery { mockSyncRepo.performFullSync() } returns Result.success(expectedResult)

        val coordinator = createCoordinator()
        val result = coordinator.sync(SyncTrigger.MANUAL)

        assertTrue(result.isSuccess)
        assertEquals(expectedResult, result.getOrNull())
        coVerify(exactly = 1) { mockSyncRepo.performFullSync() }
    }

    @Test
    fun testSync_whenUnauthenticated_abortsAndReturnsFailure() = testScope.runTest {
        currentUserFlow.value = null
        val coordinator = createCoordinator()
        advanceUntilIdle()

        val result = coordinator.sync(SyncTrigger.MANUAL)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { mockSyncRepo.performFullSync() }
        assertEquals(GlobalSyncState.AUTHENTICATION_REQUIRED, coordinator.globalSyncState.value)
    }

    @Test
    fun testSync_whenOffline_abortsAndReturnsFailure() = testScope.runTest {
        isOnlineFlow.value = false
        val coordinator = createCoordinator()
        advanceUntilIdle()

        val result = coordinator.sync(SyncTrigger.MANUAL)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { mockSyncRepo.performFullSync() }
        assertEquals(GlobalSyncState.OFFLINE, coordinator.globalSyncState.value)
    }

    @Test
    fun testConcurrentSync_reusesActiveDeferredWithoutDuplicateRuns() = testScope.runTest {
        val syncDeferred = CompletableDeferred<Result<SyncResult>>()
        coEvery { mockSyncRepo.performFullSync() } coAnswers { syncDeferred.await() }

        val coordinator = createCoordinator()

        // Launch first sync in background
        val job1 = async { coordinator.sync(SyncTrigger.MANUAL) }
        testDispatcher.scheduler.runCurrent()

        assertTrue(coordinator.isSyncing.value)

        // Launch second sync concurrently
        val job2 = async { coordinator.sync(SyncTrigger.SCHEDULED) }
        testDispatcher.scheduler.runCurrent()

        // Complete the underlying operation
        val expected = SyncResult(isSuccess = true, uploadedCount = 1, downloadedCount = 2)
        syncDeferred.complete(Result.success(expected))
        advanceUntilIdle()

        val result1 = job1.await()
        val result2 = job2.await()

        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertEquals(expected, result1.getOrNull())
        assertEquals(expected, result2.getOrNull())

        // Verify that performFullSync was only executed ONCE
        coVerify(exactly = 1) { mockSyncRepo.performFullSync() }
        assertFalse(coordinator.isSyncing.value)
    }

    @Test
    fun testStartupSync_whenEnabled_triggersSyncOnce() = testScope.runTest {
        val expected = SyncResult(isSuccess = true, uploadedCount = 0, downloadedCount = 1)
        coEvery { mockSyncRepo.performFullSync() } returns Result.success(expected)

        val coordinator = createCoordinator()
        coordinator.triggerStartupSync()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSyncRepo.performFullSync() }

        // Second startup sync trigger should be ignored
        coordinator.triggerStartupSync()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSyncRepo.performFullSync() }
    }

    @Test
    fun testStartupSync_whenDisabled_skipsExecution() = testScope.runTest {
        userPrefsFlow.value = userPrefsFlow.value.copy(isSyncOnStartupEnabled = false)
        val coordinator = createCoordinator()

        coordinator.triggerStartupSync()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockSyncRepo.performFullSync() }
    }

    @Test
    fun testStartupSync_whenAutoSyncDisabled_skipsExecution() = testScope.runTest {
        userPrefsFlow.value = userPrefsFlow.value.copy(isAutoSyncEnabled = false)
        val coordinator = createCoordinator()

        coordinator.triggerStartupSync()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockSyncRepo.performFullSync() }
    }

    @Test
    fun testPeriodicSync_viaScheduledTrigger_executesReconciliation() = testScope.runTest {
        val expected = SyncResult(isSuccess = true, uploadedCount = 4, downloadedCount = 0)
        coEvery { mockSyncRepo.performFullSync() } returns Result.success(expected)

        val coordinator = createCoordinator()
        val result = coordinator.sync(SyncTrigger.SCHEDULED)

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
        coVerify(exactly = 1) { mockSyncRepo.performFullSync() }
    }

    @Test
    fun testSetAutoSyncEnabled_true_schedulesWork() = testScope.runTest {
        val coordinator = createCoordinator()
        scheduledIntervals.clear()

        coordinator.setAutoSyncEnabled(true)
        advanceUntilIdle()

        coVerify { mockDataStore.setAutoSyncEnabled(true) }
        assertTrue(scheduledIntervals.contains(60L))
    }

    @Test
    fun testSetAutoSyncEnabled_false_cancelsWork() = testScope.runTest {
        val coordinator = createCoordinator()
        coordinator.setAutoSyncEnabled(false)
        advanceUntilIdle()

        coVerify { mockDataStore.setAutoSyncEnabled(false) }
        assertTrue(cancelCount > 0)
    }

    @Test
    fun testSetSyncIntervalMinutes_persistsAndReschedules() = testScope.runTest {
        val coordinator = createCoordinator()
        scheduledIntervals.clear()

        coordinator.setSyncIntervalMinutes(180L)
        advanceUntilIdle()

        coVerify { mockDataStore.setSyncIntervalMinutes(180L) }
        assertTrue(scheduledIntervals.contains(180L))
    }

    @Test
    fun testSetSyncIntervalMinutes_clampsToMinimum15Minutes() = testScope.runTest {
        val coordinator = createCoordinator()
        scheduledIntervals.clear()

        coordinator.setSyncIntervalMinutes(5L)
        advanceUntilIdle()

        coVerify { mockDataStore.setSyncIntervalMinutes(15L) }
        assertTrue(scheduledIntervals.contains(15L))
    }

    @Test
    fun testSetSyncOnStartupEnabled_persistsSetting() = testScope.runTest {
        val coordinator = createCoordinator()
        coordinator.setSyncOnStartupEnabled(false)
        advanceUntilIdle()

        coVerify { mockDataStore.setSyncOnStartupEnabled(false) }
    }

    @Test
    fun testSyncFailure_andSuccessfulRetry() = testScope.runTest {
        val networkError = IOException("Firestore unavailable")
        coEvery { mockSyncRepo.performFullSync() } returns Result.failure(networkError)

        val coordinator = createCoordinator()
        val failResult = coordinator.sync(SyncTrigger.MANUAL)

        assertTrue(failResult.isFailure)
        assertEquals(networkError, failResult.exceptionOrNull())

        // Retry succeeds
        val successResult = SyncResult(isSuccess = true, uploadedCount = 0, downloadedCount = 0)
        coEvery { mockSyncRepo.performFullSync() } returns Result.success(successResult)

        val retryResult = coordinator.sync(SyncTrigger.MANUAL)
        assertTrue(retryResult.isSuccess)
        assertEquals(successResult, retryResult.getOrNull())
        coVerify(exactly = 2) { mockSyncRepo.performFullSync() }
    }

    @Test
    fun testAccountChange_andLogoutState() = testScope.runTest {
        val coordinator = createCoordinator()
        advanceUntilIdle()

        // Authenticated and online
        assertEquals(GlobalSyncState.IDLE, coordinator.globalSyncState.value)

        // User logs out
        currentUserFlow.value = null
        advanceUntilIdle()

        assertEquals(GlobalSyncState.AUTHENTICATION_REQUIRED, coordinator.globalSyncState.value)

        // New user logs in
        val newUser = CloudUser(uid = "user-456", email = "other@finpulse.app", displayName = "New User")
        currentUserFlow.value = newUser
        advanceUntilIdle()

        assertEquals(GlobalSyncState.IDLE, coordinator.globalSyncState.value)
    }

    @Test
    fun testLastSyncTimestamp_observesRepository() = testScope.runTest {
        val coordinator = createCoordinator()
        assertEquals(1000L, coordinator.lastSyncTimestamp.value)

        lastSyncTimestampFlow.value = 2500L
        advanceUntilIdle()

        assertEquals(2500L, coordinator.lastSyncTimestamp.value)
    }
}

package md.alexlab.finpulse.data.sync

import android.content.Context
import android.util.Log
import md.alexlab.finpulse.core.network.NetworkConnectivityMonitor
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.reporting.DiagnosticContext
import md.alexlab.finpulse.core.reporting.ErrorReporter
import md.alexlab.finpulse.core.reporting.NoOpErrorReporter
import md.alexlab.finpulse.core.work.SyncWorker
import md.alexlab.finpulse.domain.model.sync.SyncResult
import md.alexlab.finpulse.domain.model.sync.SyncStatus
import md.alexlab.finpulse.domain.repository.AuthRepository
import md.alexlab.finpulse.domain.repository.CloudSyncRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

enum class SyncTrigger {
    MANUAL,
    STARTUP,
    SCHEDULED,
    FOREGROUND,
    CONNECTIVITY_RESTORED,
    LOCAL_CHANGE,
    LOGIN
}

enum class GlobalSyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    OFFLINE,
    AUTHENTICATION_REQUIRED
}

/**
 * Single synchronization coordinator responsible for:
 * - Manual sync (from global sync button or settings)
 * - Startup sync (post-launch background reconciliation)
 * - Scheduled periodic sync (WorkManager background sync)
 *
 * Guarantees no concurrent competing sync operations: if a sync is already running,
 * any incoming trigger reuses the active operation.
 */
class CloudSyncCoordinator(
    private val context: Context,
    private val cloudSyncRepository: CloudSyncRepository,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkConnectivityMonitor,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val errorReporter: ErrorReporter = NoOpErrorReporter(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val periodicWorkScheduler: ((Long) -> Unit)? = null,
    private val periodicWorkCanceller: (() -> Unit)? = null
) {

    companion object {
        private const val TAG = "FinPulseSyncCoordinator"
        const val DEFAULT_SYNC_INTERVAL_MINUTES = 60L
    }

    private val coordinatorScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val startupSyncExecuted = AtomicBoolean(false)

    private val syncLock = Any()
    private var activeSyncDeferred: Deferred<Result<SyncResult>>? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _recentSuccessTimestamp = MutableStateFlow<Long?>(null)

    val lastSyncTimestamp: StateFlow<Long> = cloudSyncRepository.lastSyncTimestamp
    val pendingChangesCount: Flow<Int> = cloudSyncRepository.pendingChangesCount

    val globalSyncState: StateFlow<GlobalSyncState> = combine(
        authRepository.currentUser,
        networkMonitor.isOnline,
        cloudSyncRepository.syncStatus,
        _isSyncing,
        _recentSuccessTimestamp
    ) { user, isOnline, engineStatus, running, recentSuccess ->
        when {
            user == null -> GlobalSyncState.AUTHENTICATION_REQUIRED
            !isOnline -> GlobalSyncState.OFFLINE
            running || engineStatus == SyncStatus.SYNCING -> GlobalSyncState.SYNCING
            engineStatus == SyncStatus.ERROR -> GlobalSyncState.ERROR
            recentSuccess != null && (System.currentTimeMillis() - recentSuccess < 4000L) -> GlobalSyncState.SUCCESS
            engineStatus == SyncStatus.SUCCESS -> GlobalSyncState.SUCCESS
            else -> GlobalSyncState.IDLE
        }
    }.stateIn(
        scope = coordinatorScope,
        started = SharingStarted.Eagerly,
        initialValue = GlobalSyncState.IDLE
    )

    init {
        // Observe settings changes and update WorkManager schedule accordingly
        coordinatorScope.launch {
            userPreferencesDataStore.userPreferencesFlow.collect { prefs ->
                if (authRepository.isAuthenticated) {
                    if (prefs.isAutoSyncEnabled) {
                        schedulePeriodic(prefs.syncIntervalMinutes)
                    } else {
                        cancelPeriodic()
                    }
                }
            }
        }
    }

    private fun schedulePeriodic(intervalMinutes: Long) {
        if (periodicWorkScheduler != null) {
            periodicWorkScheduler.invoke(intervalMinutes)
        } else {
            SyncWorker.schedulePeriodicSync(context, intervalMinutes)
        }
    }

    private fun cancelPeriodic() {
        if (periodicWorkCanceller != null) {
            periodicWorkCanceller.invoke()
        } else {
            SyncWorker.cancelPeriodicSync(context)
        }
    }

    /**
     * Executes cloud synchronization through the single coordinator.
     * Prevents concurrent runs: if an operation is already active, callers await and share its result.
     */
    suspend fun sync(trigger: SyncTrigger): Result<SyncResult> = withContext(dispatcher) {
        val existingDeferred = synchronized(syncLock) { activeSyncDeferred }
        if (existingDeferred != null && existingDeferred.isActive) {
            Log.d(TAG, "Reusing already active sync for trigger: $trigger")
            return@withContext existingDeferred.await()
        }

        // Validate basic conditions before launching
        if (!authRepository.isAuthenticated) {
            Log.w(TAG, "Sync aborted for trigger $trigger: User is not authenticated")
            return@withContext Result.failure(IllegalStateException("Authentication required"))
        }

        if (!networkMonitor.isCurrentlyOnline()) {
            Log.w(TAG, "Sync aborted for trigger $trigger: Device is offline")
            return@withContext Result.failure(IllegalStateException("No internet connection"))
        }

        val newDeferred = coordinatorScope.async {
            executeSyncInternal(trigger)
        }

        synchronized(syncLock) {
            activeSyncDeferred = newDeferred
        }

        _isSyncing.value = true
        try {
            val result = newDeferred.await()
            if (result.isSuccess) {
                _recentSuccessTimestamp.value = System.currentTimeMillis()
                // Clear temporary success display after 3.5 seconds
                coordinatorScope.launch {
                    delay(3500L)
                    _recentSuccessTimestamp.value = null
                }
            }
            result
        } finally {
            synchronized(syncLock) {
                if (activeSyncDeferred == newDeferred) {
                    activeSyncDeferred = null
                }
            }
            _isSyncing.value = false
        }
    }

    private suspend fun executeSyncInternal(trigger: SyncTrigger): Result<SyncResult> {
        Log.i(TAG, "Starting coordinated cloud sync triggered by: $trigger")
        errorReporter.log("Starting cloud sync triggered by: $trigger")

        return try {
            val result = cloudSyncRepository.performFullSync()
            if (result.isSuccess) {
                val syncResult = result.getOrNull()
                Log.i(TAG, "Coordinated sync completed successfully: $syncResult")
            } else {
                val ex = result.exceptionOrNull()
                Log.e(TAG, "Coordinated sync failed for trigger $trigger: ${ex?.message}", ex)
                if (ex != null) {
                    errorReporter.recordException(
                        throwable = ex,
                        context = DiagnosticContext.build(
                            feature = DiagnosticContext.FEATURE_CLOUD_SYNC,
                            operation = "coordinator_sync",
                            extra = mapOf("trigger" to trigger.name)
                        )
                    )
                }
            }
            result
        } catch (t: Throwable) {
            Log.e(TAG, "Unexpected error in executeSyncInternal for trigger $trigger: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Non-blocking startup synchronization called during application launch.
     * Guaranteed to execute at most once per process lifetime.
     */
    fun triggerStartupSync() {
        if (startupSyncExecuted.compareAndSet(false, true)) {
            coordinatorScope.launch {
                try {
                    val prefs = userPreferencesDataStore.userPreferencesFlow.first()
                    if (!prefs.isAutoSyncEnabled || !prefs.isSyncOnStartupEnabled) {
                        Log.d(TAG, "Startup sync skipped: disabled in user settings (autoSync=${prefs.isAutoSyncEnabled}, onStartup=${prefs.isSyncOnStartupEnabled})")
                        return@launch
                    }
                    if (!authRepository.isAuthenticated) {
                        Log.d(TAG, "Startup sync skipped: user not authenticated")
                        return@launch
                    }
                    if (!networkMonitor.isCurrentlyOnline()) {
                        Log.d(TAG, "Startup sync skipped: device is offline")
                        return@launch
                    }

                    Log.i(TAG, "Triggering automatic cloud sync after app launch")
                    sync(SyncTrigger.STARTUP)
                } catch (t: Throwable) {
                    Log.e(TAG, "Error evaluating startup sync preconditions: ${t.message}", t)
                }
            }
        } else {
            Log.d(TAG, "Startup sync already executed for this process, ignoring subsequent call")
        }
    }

    /**
     * Non-blocking foreground synchronization when returning from background.
     */
    fun triggerForegroundSync() {
        coordinatorScope.launch {
            try {
                val prefs = userPreferencesDataStore.userPreferencesFlow.first()
                if (!prefs.isAutoSyncEnabled) return@launch
                if (!authRepository.isAuthenticated || !networkMonitor.isCurrentlyOnline()) return@launch

                sync(SyncTrigger.FOREGROUND)
            } catch (t: Throwable) {
                Log.w(TAG, "Foreground sync failed: ${t.message}")
            }
        }
    }

    /**
     * Updates the automatic cloud sync setting and schedules/cancels WorkManager periodic tasks.
     */
    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        userPreferencesDataStore.setAutoSyncEnabled(enabled)
        val prefs = userPreferencesDataStore.userPreferencesFlow.first()
        if (enabled && authRepository.isAuthenticated) {
            schedulePeriodic(prefs.syncIntervalMinutes)
        } else {
            cancelPeriodic()
        }
    }

    /**
     * Updates the startup synchronization toggle.
     */
    suspend fun setSyncOnStartupEnabled(enabled: Boolean) {
        userPreferencesDataStore.setSyncOnStartupEnabled(enabled)
    }

    /**
     * Updates the periodic synchronization interval and re-enqueues WorkManager.
     */
    suspend fun setSyncIntervalMinutes(minutes: Long) {
        val safeMinutes = maxOf(minutes, 15L)
        userPreferencesDataStore.setSyncIntervalMinutes(safeMinutes)
        val prefs = userPreferencesDataStore.userPreferencesFlow.first()
        if (prefs.isAutoSyncEnabled && authRepository.isAuthenticated) {
            schedulePeriodic(safeMinutes)
        }
    }
}

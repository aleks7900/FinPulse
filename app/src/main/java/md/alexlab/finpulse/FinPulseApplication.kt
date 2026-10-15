package md.alexlab.finpulse

import android.app.Application
import md.alexlab.finpulse.di.AppContainer
import md.alexlab.finpulse.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FinPulseApplication : Application() {

    lateinit var container: AppContainer
        private set

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Log initial application startup breadcrumb
        container.errorReporter.log("FinPulse application created")

        // Bind / unbind anonymous user context for crash reporting
        applicationScope.launch {
            container.authRepository.currentUser.collect { user ->
                if (user != null) {
                    container.errorReporter.setUserContext(user.uid)
                } else {
                    container.errorReporter.clearUserContext()
                }
            }
        }

        // Seed default categories, rules & import profiles in background
        applicationScope.launch {
            container.categoryRepository.seedDefaultCategoriesIfNeeded()
            container.categorizationRuleRepository.seedDefaultRulesIfNeeded()
            container.importProfileRepository.seedDefaultProfilesIfNeeded()
        }

        // Schedule periodic WorkManager recurring check & reminders
        md.alexlab.finpulse.core.work.RecurringCheckWorker.schedulePeriodicCheck(this)

        // Initialize cloud sync engine & trigger automatic startup sync via coordinator
        applicationScope.launch {
            (container.cloudSyncRepository as? md.alexlab.finpulse.data.sync.CloudSyncEngine)?.initialize()
            container.cloudSyncCoordinator.triggerStartupSync()
        }

        // Connectivity restoration trigger via CloudSyncCoordinator
        applicationScope.launch {
            container.networkMonitor.isOnline
                .drop(1)
                .collect { online ->
                    if (online && container.authRepository.isAuthenticated && !container.cloudSyncCoordinator.isSyncing.value) {
                        val prefs = container.userPreferencesDataStore.userPreferencesFlow.first()
                        if (prefs.isAutoSyncEnabled) {
                            container.cloudSyncCoordinator.sync(md.alexlab.finpulse.data.sync.SyncTrigger.CONNECTIVITY_RESTORED)
                        }
                    }
                }
        }

        // Auto-push trigger after local modifications when online (debounced) via coordinator
        applicationScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            container.cloudSyncCoordinator.pendingChangesCount
                .drop(1)
                .debounce(1500L)
                .filter { it > 0 }
                .collect {
                    if (!container.cloudSyncCoordinator.isSyncing.value) {
                        val prefs = container.userPreferencesDataStore.userPreferencesFlow.first()
                        if (prefs.isAutoSyncEnabled && container.authRepository.isAuthenticated && container.networkMonitor.isCurrentlyOnline()) {
                            container.cloudSyncCoordinator.sync(md.alexlab.finpulse.data.sync.SyncTrigger.LOCAL_CHANGE)
                        }
                    }
                }
        }

        // Schedule periodic financial digest checks
        md.alexlab.finpulse.core.work.FinancialDigestWorker.schedulePeriodicDigest(this)

        // Schedule periodic global currency exchange rate sync
        md.alexlab.finpulse.core.work.ExchangeRateSyncWorker.schedulePeriodicSync(this)
    }
}


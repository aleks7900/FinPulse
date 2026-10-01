package md.alexlab.finpulse

import android.app.Application
import md.alexlab.finpulse.di.AppContainer
import md.alexlab.finpulse.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FinPulseApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Seed default categories, rules & import profiles in background
        applicationScope.launch {
            container.categoryRepository.seedDefaultCategoriesIfNeeded()
            container.categorizationRuleRepository.seedDefaultRulesIfNeeded()
            container.importProfileRepository.seedDefaultProfilesIfNeeded()
        }

        // Schedule periodic WorkManager recurring check & reminders
        md.alexlab.finpulse.core.work.RecurringCheckWorker.schedulePeriodicCheck(this)

        // Schedule periodic background cloud synchronization
        md.alexlab.finpulse.core.work.SyncWorker.schedulePeriodicSync(this)

        // Schedule periodic financial digest checks
        md.alexlab.finpulse.core.work.FinancialDigestWorker.schedulePeriodicDigest(this)

        // Schedule periodic global currency exchange rate sync
        md.alexlab.finpulse.core.work.ExchangeRateSyncWorker.schedulePeriodicSync(this)
    }
}

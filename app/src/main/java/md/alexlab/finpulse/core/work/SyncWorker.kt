package md.alexlab.finpulse.core.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import md.alexlab.finpulse.FinPulseApplication
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = appContext.applicationContext as? FinPulseApplication ?: return Result.success()
        val prefs = app.container.userPreferencesDataStore.userPreferencesFlow.first()

        if (!prefs.isAutoSyncEnabled) {
            cancelPeriodicSync(appContext)
            return Result.success()
        }

        if (!app.container.authRepository.isAuthenticated) {
            return Result.success()
        }

        val result = app.container.cloudSyncCoordinator.sync(md.alexlab.finpulse.data.sync.SyncTrigger.SCHEDULED)
        return if (result.isSuccess) {
            Result.success()
        } else {
            val ex = result.exceptionOrNull()
            if (ex != null && runAttemptCount >= 2) {
                app.container.errorReporter.recordException(
                    throwable = ex,
                    context = md.alexlab.finpulse.core.reporting.DiagnosticContext.build(
                        feature = md.alexlab.finpulse.core.reporting.DiagnosticContext.FEATURE_CLOUD_SYNC,
                        operation = "sync_worker"
                    )
                )
            }
            // Retry if it was a temporary network or cloud failure
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC_WORK_NAME = "finpulse_periodic_cloud_sync"
        private const val UNIQUE_ONE_TIME_WORK_NAME = "finpulse_one_time_sync"

        fun enqueueOneTimeSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun schedulePeriodicSync(context: Context, intervalMinutes: Long = 60L) {
            val safeInterval = maxOf(intervalMinutes, 15L) // WorkManager enforces minimum 15 minutes
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(safeInterval, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
        }
    }
}

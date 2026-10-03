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
import java.util.concurrent.TimeUnit

class ExchangeRateSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = appContext.applicationContext as? FinPulseApplication ?: return Result.success()
        val exchangeRateProvider = app.container.exchangeRateProvider

        return try {
            val result = exchangeRateProvider.refreshRates()
            if (result.isSuccess) {
                Result.success()
            } else {
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    app.container.errorReporter.recordException(
                        throwable = result.exceptionOrNull() ?: RuntimeException("Exchange rate sync failed"),
                        context = md.alexlab.finpulse.core.reporting.DiagnosticContext.build(
                            feature = md.alexlab.finpulse.core.reporting.DiagnosticContext.FEATURE_EXCHANGE_RATES,
                            operation = "worker_sync"
                        )
                    )
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                app.container.errorReporter.recordException(
                    throwable = e,
                    context = md.alexlab.finpulse.core.reporting.DiagnosticContext.build(
                        feature = md.alexlab.finpulse.core.reporting.DiagnosticContext.FEATURE_EXCHANGE_RATES,
                        operation = "worker_sync_exception"
                    )
                )
                Result.failure()
            }
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC_WORK_NAME = "finpulse_periodic_exchange_rates_sync"
        private const val UNIQUE_ONE_TIME_WORK_NAME = "finpulse_one_time_exchange_rates_sync"

        fun enqueueOneTimeSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ExchangeRateSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<ExchangeRateSyncWorker>(
                repeatInterval = 12,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = 2,
                flexTimeIntervalUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}

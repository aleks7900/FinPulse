package md.alexlab.finpulse.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import md.alexlab.finpulse.FinPulseApplication
import md.alexlab.finpulse.core.util.DigestNotificationHelper
import md.alexlab.finpulse.domain.model.DigestPeriod
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class FinancialDigestWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = appContext.applicationContext as? FinPulseApplication ?: return Result.success()
            val userPrefsDataStore = app.container.userPreferencesDataStore
            val prefs = userPrefsDataStore.userPreferencesFlow.first()

            val frequency = prefs.digestFrequency
            if (frequency == "OFF") {
                return Result.success()
            }

            val zone = ZoneId.systemDefault()
            val now = Instant.now()
            val today = now.atZone(zone).toLocalDate()
            val currentHour = now.atZone(zone).toLocalTime().hour

            val lastSentDate = if (prefs.lastDigestSentMillis > 0L) {
                Instant.ofEpochMilli(prefs.lastDigestSentMillis).atZone(zone).toLocalDate()
            } else null

            val targetHour = prefs.digestDeliveryHour

            when (frequency) {
                "DAILY" -> {
                    // Check if already sent today
                    if (lastSentDate != null && lastSentDate.isEqual(today)) {
                        return Result.success()
                    }

                    // Check if delivery hour reached
                    if (currentHour < targetHour) {
                        return Result.success()
                    }

                    val digest = app.container.getFinancialDigestUseCase.getSnapshot(DigestPeriod.DAILY)
                    DigestNotificationHelper.sendDigestNotification(
                        context = appContext,
                        digest = digest,
                        privacyMode = prefs.digestPrivacyEnabled
                    )
                    userPrefsDataStore.setLastDigestSentMillis(now.toEpochMilli())
                }
                "WEEKLY" -> {
                    // Weekly digest sent on Sunday (or if 7+ days elapsed)
                    val isSunday = today.dayOfWeek == DayOfWeek.SUNDAY
                    val daysSinceLast = if (lastSentDate != null) java.time.temporal.ChronoUnit.DAYS.between(lastSentDate, today) else 99L

                    if ((isSunday && currentHour >= targetHour && daysSinceLast >= 6L) || daysSinceLast >= 7L) {
                        val digest = app.container.getFinancialDigestUseCase.getSnapshot(DigestPeriod.WEEKLY)
                        DigestNotificationHelper.sendDigestNotification(
                            context = appContext,
                            digest = digest,
                            privacyMode = prefs.digestPrivacyEnabled
                        )
                        userPrefsDataStore.setLastDigestSentMillis(now.toEpochMilli())
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            val app = appContext.applicationContext as? FinPulseApplication
            if (runAttemptCount >= 2) {
                app?.container?.errorReporter?.recordException(
                    throwable = e,
                    context = md.alexlab.finpulse.core.reporting.DiagnosticContext.build(
                        feature = md.alexlab.finpulse.core.reporting.DiagnosticContext.FEATURE_DIGEST,
                        operation = "worker_digest"
                    )
                )
            }
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "finpulse_financial_digest_worker"

        fun schedulePeriodicDigest(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<FinancialDigestWorker>(
                repeatInterval = 3,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}

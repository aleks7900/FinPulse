package com.finpulse.app.core.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.finpulse.app.FinPulseApplication
import com.finpulse.app.MainActivity
import com.finpulse.app.domain.engine.RecurringDateEngine
import com.finpulse.app.domain.model.OccurrenceStatus
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class RecurringCheckWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = appContext.applicationContext as? FinPulseApplication ?: return Result.success()
            val recurringRepository = app.container.recurringRepository
            val nowMillis = System.currentTimeMillis()

            // 1. Process due recurring transactions (handles idempotency & duplicate prevention internally)
            recurringRepository.processDueRecurringTransactions(nowMillis)

            // 2. Check for upcoming reminders
            val activeRules = recurringRepository.getActiveRecurringFlow()
            activeRules.first().forEach { rule ->
                val dueDay = Instant.ofEpochMilli(rule.nextDueDate).atZone(ZoneId.systemDefault()).toLocalDate()
                val today = LocalDate.now(ZoneId.systemDefault())
                val daysUntilDue = ChronoUnit.DAYS.between(today, dueDay)

                // If within reminder window and not yet due in the distant future
                if (daysUntilDue in 0..rule.reminderDaysBefore) {
                    val status = RecurringDateEngine.determineStatus(
                        dueDateMillis = rule.nextDueDate,
                        nowMillis = nowMillis
                    )
                    if (status != OccurrenceStatus.PAID && status != OccurrenceStatus.SKIPPED) {
                        sendReminderNotification(
                            ruleId = rule.id,
                            title = rule.title,
                            amountFormatted = rule.amount.formatted(),
                            daysUntilDue = daysUntilDue
                        )
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            // Worker retry
            Result.retry()
        }
    }

    private fun sendReminderNotification(
        ruleId: String,
        title: String,
        amountFormatted: String,
        daysUntilDue: Long
    ) {
        createNotificationChannel()

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            ruleId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dueText = when (daysUntilDue) {
            0L -> appContext.getString(com.finpulse.app.R.string.notif_bill_due_today, amountFormatted)
            1L -> appContext.getString(com.finpulse.app.R.string.notif_bill_due_tomorrow, amountFormatted)
            else -> appContext.getString(com.finpulse.app.R.string.notif_bill_due_in_days, amountFormatted, daysUntilDue.toInt())
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(appContext.getString(com.finpulse.app.R.string.notif_bill_upcoming_title, title))
            .setContentText(dueText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(ruleId.hashCode(), notification)
        } catch (_: SecurityException) {
            // In Android 13+, POST_NOTIFICATIONS permission might not yet be granted
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                appContext.getString(com.finpulse.app.R.string.notif_channel_recurring_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = appContext.getString(com.finpulse.app.R.string.notif_channel_recurring_desc)
            }
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "finpulse_recurring_reminders"
        private const val WORK_NAME = "RecurringCheckWorker"

        fun schedulePeriodicCheck(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<RecurringCheckWorker>(
                12, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}

package md.alexlab.finpulse.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import md.alexlab.finpulse.MainActivity
import md.alexlab.finpulse.R
import md.alexlab.finpulse.domain.model.CalendarEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object ObligationReminderHelper {

    const val CHANNEL_ID = "finpulse_obligation_reminders"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notif_channel_recurring_name)
            val descriptionText = context.getString(R.string.notif_channel_recurring_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendEventReminderNotification(
        context: Context,
        event: CalendarEvent
    ): Boolean {
        return try {
            ensureNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                event.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val dueDateStr = Instant.ofEpochMilli(event.dateMillis)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))

            val title = context.getString(R.string.calendar_reminder_notif_title, event.title)
            val message = context.getString(
                R.string.calendar_reminder_notif_body,
                event.amount.formatted(),
                dueDateStr
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(event.id.hashCode(), notification)
            true
        } catch (_: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission not granted
            false
        } catch (_: Exception) {
            false
        }
    }
}

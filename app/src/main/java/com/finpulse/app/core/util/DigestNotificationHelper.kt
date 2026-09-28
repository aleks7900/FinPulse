package com.finpulse.app.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.finpulse.app.MainActivity
import com.finpulse.app.R
import com.finpulse.app.domain.model.DigestPeriod
import com.finpulse.app.domain.model.FinancialDigest

object DigestNotificationHelper {

    const val CHANNEL_ID = "finpulse_digest_channel"
    private const val NOTIFICATION_ID_DAILY = 4001
    private const val NOTIFICATION_ID_WEEKLY = 4002

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notif_channel_digest_name)
            val descriptionText = context.getString(R.string.notif_channel_digest_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendDigestNotification(
        context: Context,
        digest: FinancialDigest,
        privacyMode: Boolean
    ): Boolean {
        return try {
            ensureNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("destination_route", "digest")
                putExtra("digest_period", digest.period.name)
            }

            val notifId = if (digest.period == DigestPeriod.DAILY) NOTIFICATION_ID_DAILY else NOTIFICATION_ID_WEEKLY

            val pendingIntent = PendingIntent.getActivity(
                context,
                notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val isDaily = digest.period == DigestPeriod.DAILY
            val periodLabel = if (isDaily) context.getString(R.string.digest_period_daily) else context.getString(R.string.digest_period_weekly)

            val title: String
            val contentText: String
            val bigText: String

            if (privacyMode) {
                // Privacy mode: hide monetary amounts & specific merchant names on lockscreen
                title = context.getString(R.string.digest_notif_privacy_title, periodLabel)
                val billCount = digest.upcomingBills.size
                contentText = context.getString(
                    R.string.digest_notif_privacy_body,
                    digest.transactionCount,
                    billCount
                )
                bigText = context.getString(
                    R.string.digest_notif_privacy_expanded,
                    periodLabel,
                    digest.transactionCount,
                    billCount,
                    digest.unreviewedCount
                )
            } else {
                // Detailed mode: show total spent, change %, top highlights
                val changeStr = if (digest.spendChangePercentage >= 0.0) {
                    "+${"%.0f".format(digest.spendChangePercentage)}%"
                } else {
                    "${"%.0f".format(digest.spendChangePercentage)}%"
                }

                title = if (digest.previousPeriodSpend.amountMinor > 0L) {
                    context.getString(R.string.digest_notif_detailed_title_trend, periodLabel, digest.totalSpend.formatted(), changeStr)
                } else {
                    context.getString(R.string.digest_notif_detailed_title, periodLabel, digest.totalSpend.formatted())
                }

                val topHighlight = digest.topExpenses.firstOrNull()?.let {
                    context.getString(R.string.digest_notif_detailed_top_expense, it.title, it.amount.formatted())
                } ?: ""

                val billsText = if (digest.upcomingBills.isNotEmpty()) {
                    context.getString(R.string.digest_notif_detailed_bills, digest.upcomingBills.size)
                } else ""

                contentText = listOf(topHighlight, billsText)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                    .ifBlank { digest.headline }

                bigText = "${digest.narrativeSummary}\n\n${digest.headline}"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(
                    android.R.drawable.ic_menu_view,
                    context.getString(R.string.digest_notif_action_open),
                    pendingIntent
                )
                .build()

            NotificationManagerCompat.from(context).notify(notifId, notification)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}

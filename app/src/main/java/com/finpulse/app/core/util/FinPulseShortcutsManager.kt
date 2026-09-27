package com.finpulse.app.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.finpulse.app.MainActivity
import com.finpulse.app.R

object FinPulseShortcutsManager {

    fun initDynamicShortcuts(context: Context) {
        val expenseIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("finpulse://quick-add?type=expense")
            putExtra("quick_add_type", "EXPENSE")
            putExtra("open_quick_add", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val incomeIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("finpulse://quick-add?type=income")
            putExtra("quick_add_type", "INCOME")
            putExtra("open_quick_add", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val transferIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("finpulse://quick-add?type=transfer")
            putExtra("quick_add_type", "TRANSFER")
            putExtra("open_quick_add", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val reviewInboxIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("finpulse://review-inbox")
            putExtra("destination_route", "review_inbox")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val shortcuts = listOf(
            ShortcutInfoCompat.Builder(context, "shortcut_add_expense")
                .setShortLabel(context.getString(R.string.shortcut_add_expense_short))
                .setLongLabel(context.getString(R.string.shortcut_add_expense_long))
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_input_add))
                .setIntent(expenseIntent)
                .setRank(1)
                .build(),

            ShortcutInfoCompat.Builder(context, "shortcut_add_income")
                .setShortLabel(context.getString(R.string.shortcut_add_income_short))
                .setLongLabel(context.getString(R.string.shortcut_add_income_long))
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_input_add))
                .setIntent(incomeIntent)
                .setRank(2)
                .build(),

            ShortcutInfoCompat.Builder(context, "shortcut_add_transfer")
                .setShortLabel(context.getString(R.string.shortcut_add_transfer_short))
                .setLongLabel(context.getString(R.string.shortcut_add_transfer_long))
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_rotate))
                .setIntent(transferIntent)
                .setRank(3)
                .build(),

            ShortcutInfoCompat.Builder(context, "shortcut_review_inbox")
                .setShortLabel(context.getString(R.string.shortcut_review_inbox_short))
                .setLongLabel(context.getString(R.string.shortcut_review_inbox_long))
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_agenda))
                .setIntent(reviewInboxIntent)
                .setRank(4)
                .build()
        )

        try {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (_: Exception) {
            // Ignore if device doesn't support dynamic shortcuts
        }
    }
}

package com.finpulse.app.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.finpulse.app.FinPulseApplication
import com.finpulse.app.MainActivity
import com.finpulse.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object FinPulseWidgetUpdater {

    const val ACTION_TOGGLE_WIDGET_PRIVACY = "com.finpulse.app.action.TOGGLE_WIDGET_PRIVACY"
    const val ACTION_REFRESH_WIDGETS = "com.finpulse.app.action.REFRESH_WIDGETS"

    private val updaterScope = CoroutineScope(Dispatchers.IO)

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return

        updaterScope.launch {
            // 1. Balance widgets
            val balanceIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, CurrentBalanceWidgetProvider::class.java)
            )
            for (id in balanceIds) {
                val views = buildBalanceWidgetViews(context, id, appWidgetManager)
                appWidgetManager.updateAppWidget(id, views)
            }

            // 2. Spending widgets
            val spendingIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MonthlySpendingWidgetProvider::class.java)
            )
            for (id in spendingIds) {
                val views = buildSpendingWidgetViews(context, id, appWidgetManager)
                appWidgetManager.updateAppWidget(id, views)
            }

            // 3. Budget widgets
            val budgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, BudgetRemainingWidgetProvider::class.java)
            )
            for (id in budgetIds) {
                val views = buildBudgetWidgetViews(context, id, appWidgetManager)
                appWidgetManager.updateAppWidget(id, views)
            }

            // 4. Bills widgets
            val billsIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, UpcomingBillsWidgetProvider::class.java)
            )
            for (id in billsIds) {
                val views = buildBillsWidgetViews(context, id, appWidgetManager)
                appWidgetManager.updateAppWidget(id, views)
            }

            // 5. Quick Add widgets
            val quickAddIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, QuickAddWidgetProvider::class.java)
            )
            for (id in quickAddIds) {
                val views = buildQuickAddWidgetViews(context, id)
                appWidgetManager.updateAppWidget(id, views)
            }

            // 6. Dashboard Multi widgets
            val dashboardIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, FinPulseDashboardWidgetProvider::class.java)
            )
            for (id in dashboardIds) {
                val views = buildDashboardWidgetViews(context, id, appWidgetManager)
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }

    suspend fun buildBalanceWidgetViews(
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_current_balance)
        val app = context.applicationContext as? FinPulseApplication
        val container = app?.container ?: return views

        val data = FinPulseWidgetDataEngine.getBalanceData(container, context, appWidgetId, appWidgetManager)

        views.setTextViewText(R.id.widget_balance_amount, data.displayAmount)
        views.setTextViewText(R.id.widget_balance_subtitle, data.accountSubtitle)
        views.setTextViewText(R.id.widget_btn_privacy, if (data.isMasked) "👁" else "✕")

        // Root click -> Accounts screen
        val openIntent = createNavigationIntent(context, "accounts")
        views.setOnClickPendingIntent(R.id.widget_balance_root, openIntent)

        // Privacy click -> Toggle
        val privacyIntent = createPrivacyToggleIntent(context, appWidgetId, CurrentBalanceWidgetProvider::class.java)
        views.setOnClickPendingIntent(R.id.widget_btn_privacy, privacyIntent)

        return views
    }

    suspend fun buildSpendingWidgetViews(
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_monthly_spending)
        val app = context.applicationContext as? FinPulseApplication
        val container = app?.container ?: return views

        val data = FinPulseWidgetDataEngine.getSpendingData(container, context, appWidgetId, appWidgetManager)

        views.setTextViewText(R.id.widget_spending_amount, data.displayAmount)
        views.setTextViewText(R.id.widget_spending_subtitle, data.subtitle)
        views.setTextViewText(R.id.widget_btn_privacy, if (data.isMasked) "👁" else "✕")

        // Root click -> Analytics screen
        val openIntent = createNavigationIntent(context, "analytics")
        views.setOnClickPendingIntent(R.id.widget_spending_root, openIntent)

        // Privacy click -> Toggle
        val privacyIntent = createPrivacyToggleIntent(context, appWidgetId, MonthlySpendingWidgetProvider::class.java)
        views.setOnClickPendingIntent(R.id.widget_btn_privacy, privacyIntent)

        return views
    }

    suspend fun buildBudgetWidgetViews(
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_budget_remaining)
        val app = context.applicationContext as? FinPulseApplication
        val container = app?.container ?: return views

        val data = FinPulseWidgetDataEngine.getBudgetData(container, context, appWidgetId, appWidgetManager)

        views.setTextViewText(R.id.widget_budget_remaining, data.displayRemaining)
        views.setProgressBar(R.id.widget_budget_progress, 100, data.progressPercent, false)
        views.setTextViewText(R.id.widget_budget_subtitle, data.subtitle)
        views.setTextViewText(R.id.widget_btn_privacy, if (data.isMasked) "👁" else "✕")

        // Root click -> Budgets screen
        val openIntent = createNavigationIntent(context, "budgets")
        views.setOnClickPendingIntent(R.id.widget_budget_root, openIntent)

        // Privacy click -> Toggle
        val privacyIntent = createPrivacyToggleIntent(context, appWidgetId, BudgetRemainingWidgetProvider::class.java)
        views.setOnClickPendingIntent(R.id.widget_btn_privacy, privacyIntent)

        return views
    }

    suspend fun buildBillsWidgetViews(
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_upcoming_bills)
        val app = context.applicationContext as? FinPulseApplication
        val container = app?.container ?: return views

        val data = FinPulseWidgetDataEngine.getUpcomingBillsData(container, context, appWidgetId, appWidgetManager)

        views.setTextViewText(R.id.widget_bills_count_badge, data.totalUpcomingCount.toString())
        views.setTextViewText(R.id.widget_btn_privacy, if (data.isMasked) "👁" else "✕")

        if (data.bills.isEmpty()) {
            views.setViewVisibility(R.id.widget_bills_empty, View.VISIBLE)
            views.setViewVisibility(R.id.widget_bill1_row, View.GONE)
            views.setViewVisibility(R.id.widget_bill2_row, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_bills_empty, View.GONE)

            val bill1 = data.bills.getOrNull(0)
            if (bill1 != null) {
                views.setViewVisibility(R.id.widget_bill1_row, View.VISIBLE)
                views.setTextViewText(R.id.widget_bill1_title, bill1.title)
                views.setTextViewText(R.id.widget_bill1_date, bill1.dueDateText)
                views.setTextViewText(R.id.widget_bill1_amount, bill1.displayAmount)
            } else {
                views.setViewVisibility(R.id.widget_bill1_row, View.GONE)
            }

            val bill2 = data.bills.getOrNull(1)
            if (bill2 != null) {
                views.setViewVisibility(R.id.widget_bill2_row, View.VISIBLE)
                views.setTextViewText(R.id.widget_bill2_title, bill2.title)
                views.setTextViewText(R.id.widget_bill2_date, bill2.dueDateText)
                views.setTextViewText(R.id.widget_bill2_amount, bill2.displayAmount)
            } else {
                views.setViewVisibility(R.id.widget_bill2_row, View.GONE)
            }
        }

        // Root click -> Calendar screen
        val openIntent = createNavigationIntent(context, "calendar")
        views.setOnClickPendingIntent(R.id.widget_bills_root, openIntent)

        // Privacy click -> Toggle
        val privacyIntent = createPrivacyToggleIntent(context, appWidgetId, UpcomingBillsWidgetProvider::class.java)
        views.setOnClickPendingIntent(R.id.widget_btn_privacy, privacyIntent)

        return views
    }

    fun buildQuickAddWidgetViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_add)

        views.setOnClickPendingIntent(R.id.btn_quick_expense, createQuickAddIntent(context, "EXPENSE", 101))
        views.setOnClickPendingIntent(R.id.btn_quick_income, createQuickAddIntent(context, "INCOME", 102))
        views.setOnClickPendingIntent(R.id.btn_quick_transfer, createQuickAddIntent(context, "TRANSFER", 103))
        views.setOnClickPendingIntent(R.id.btn_quick_inbox, createNavigationIntent(context, "review_inbox", 104))

        return views
    }

    suspend fun buildDashboardWidgetViews(
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_dashboard_multi)
        val app = context.applicationContext as? FinPulseApplication
        val container = app?.container ?: return views

        val config = WidgetPreferences.getWidgetConfig(context, appWidgetId)
        val isMasked = FinPulseWidgetDataEngine.shouldMask(
            context,
            appWidgetId,
            container.userPreferencesDataStore.userPreferencesFlow.first(),
            appWidgetManager
        )

        when (config.displayType) {
            WidgetDisplayType.SPENDING -> {
                val data = FinPulseWidgetDataEngine.getSpendingData(container, context, appWidgetId, appWidgetManager)
                views.setTextViewText(R.id.widget_dash_type_label, "• Spending")
                views.setTextViewText(R.id.widget_dash_amount, data.displayAmount)
                views.setTextViewText(R.id.widget_dash_subtitle, data.subtitle)
                views.setOnClickPendingIntent(R.id.widget_dash_metric_container, createNavigationIntent(context, "analytics"))
            }
            WidgetDisplayType.BUDGET -> {
                val data = FinPulseWidgetDataEngine.getBudgetData(container, context, appWidgetId, appWidgetManager)
                views.setTextViewText(R.id.widget_dash_type_label, "• Budget")
                views.setTextViewText(R.id.widget_dash_amount, data.displayRemaining)
                views.setTextViewText(R.id.widget_dash_subtitle, data.subtitle)
                views.setOnClickPendingIntent(R.id.widget_dash_metric_container, createNavigationIntent(context, "budgets"))
            }
            else -> {
                val data = FinPulseWidgetDataEngine.getBalanceData(container, context, appWidgetId, appWidgetManager)
                views.setTextViewText(R.id.widget_dash_type_label, "• Liquid Cash")
                views.setTextViewText(R.id.widget_dash_amount, data.displayAmount)
                views.setTextViewText(R.id.widget_dash_subtitle, data.accountSubtitle)
                views.setOnClickPendingIntent(R.id.widget_dash_metric_container, createNavigationIntent(context, "accounts"))
            }
        }

        views.setTextViewText(R.id.widget_btn_privacy, if (isMasked) "👁" else "✕")

        // Privacy and Refresh buttons
        val privacyIntent = createPrivacyToggleIntent(context, appWidgetId, FinPulseDashboardWidgetProvider::class.java)
        views.setOnClickPendingIntent(R.id.widget_btn_privacy, privacyIntent)

        val refreshIntent = createRefreshIntent(context, appWidgetId, FinPulseDashboardWidgetProvider::class.java)
        views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshIntent)

        // Quick action buttons
        views.setOnClickPendingIntent(R.id.btn_quick_expense, createQuickAddIntent(context, "EXPENSE", 201))
        views.setOnClickPendingIntent(R.id.btn_quick_income, createQuickAddIntent(context, "INCOME", 202))
        views.setOnClickPendingIntent(R.id.btn_quick_transfer, createQuickAddIntent(context, "TRANSFER", 203))
        views.setOnClickPendingIntent(R.id.btn_quick_inbox, createNavigationIntent(context, "review_inbox", 204))

        return views
    }

    private fun createQuickAddIntent(context: Context, type: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("finpulse://quick-add?type=${type.lowercase()}")
            putExtra("quick_add_type", type)
            putExtra("open_quick_add", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNavigationIntent(context: Context, destination: String, requestCode: Int = 301): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("finpulse://$destination")
            putExtra("destination_route", destination)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createPrivacyToggleIntent(context: Context, appWidgetId: Int, receiverClass: Class<*>): PendingIntent {
        val intent = Intent(context, receiverClass).apply {
            action = ACTION_TOGGLE_WIDGET_PRIVACY
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createRefreshIntent(context: Context, appWidgetId: Int, receiverClass: Class<*>): PendingIntent {
        val intent = Intent(context, receiverClass).apply {
            action = ACTION_REFRESH_WIDGETS
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId + 2000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

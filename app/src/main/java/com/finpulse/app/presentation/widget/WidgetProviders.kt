package com.finpulse.app.presentation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

abstract class BaseFinPulseWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            FinPulseWidgetUpdater.ACTION_TOGGLE_WIDGET_PRIVACY -> {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    WidgetPreferences.toggleWidgetPrivacy(context, widgetId)
                }
                FinPulseWidgetUpdater.updateAllWidgets(context)
            }
            FinPulseWidgetUpdater.ACTION_REFRESH_WIDGETS -> {
                FinPulseWidgetUpdater.updateAllWidgets(context)
            }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            WidgetPreferences.deleteWidgetConfig(context, id)
        }
    }
}

class CurrentBalanceWidgetProvider : BaseFinPulseWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        FinPulseWidgetUpdater.updateAllWidgets(context)
    }
}

class MonthlySpendingWidgetProvider : BaseFinPulseWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        FinPulseWidgetUpdater.updateAllWidgets(context)
    }
}

class BudgetRemainingWidgetProvider : BaseFinPulseWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        FinPulseWidgetUpdater.updateAllWidgets(context)
    }
}

class UpcomingBillsWidgetProvider : BaseFinPulseWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        FinPulseWidgetUpdater.updateAllWidgets(context)
    }
}

class QuickAddWidgetProvider : BaseFinPulseWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            val views = FinPulseWidgetUpdater.buildQuickAddWidgetViews(context, id)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}

class FinPulseDashboardWidgetProvider : BaseFinPulseWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        FinPulseWidgetUpdater.updateAllWidgets(context)
    }
}

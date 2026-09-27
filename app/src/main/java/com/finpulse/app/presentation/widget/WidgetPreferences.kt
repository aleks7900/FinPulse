package com.finpulse.app.presentation.widget

import android.content.Context
import android.content.SharedPreferences

enum class WidgetPrivacySetting {
    FOLLOW_APP,
    ALWAYS_HIDE,
    ALWAYS_SHOW
}

enum class WidgetDisplayType {
    BALANCE,
    SPENDING,
    BUDGET,
    BILLS,
    QUICK_ADD,
    DASHBOARD
}

data class WidgetConfig(
    val appWidgetId: Int,
    val displayType: WidgetDisplayType = WidgetDisplayType.DASHBOARD,
    val privacySetting: WidgetPrivacySetting = WidgetPrivacySetting.FOLLOW_APP,
    val accountId: String? = null
)

object WidgetPreferences {

    private const val PREFS_NAME = "finpulse_widget_prefs"
    private const val KEY_DISPLAY_TYPE_PREFIX = "widget_type_"
    private const val KEY_PRIVACY_PREFIX = "widget_privacy_"
    private const val KEY_ACCOUNT_ID_PREFIX = "widget_account_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getWidgetConfig(context: Context, appWidgetId: Int, defaultType: WidgetDisplayType = WidgetDisplayType.DASHBOARD): WidgetConfig {
        val prefs = getPrefs(context)
        val typeStr = prefs.getString("$KEY_DISPLAY_TYPE_PREFIX$appWidgetId", defaultType.name)
        val displayType = try {
            WidgetDisplayType.valueOf(typeStr ?: defaultType.name)
        } catch (_: Exception) {
            defaultType
        }

        val privacyStr = prefs.getString("$KEY_PRIVACY_PREFIX$appWidgetId", WidgetPrivacySetting.FOLLOW_APP.name)
        val privacySetting = try {
            WidgetPrivacySetting.valueOf(privacyStr ?: WidgetPrivacySetting.FOLLOW_APP.name)
        } catch (_: Exception) {
            WidgetPrivacySetting.FOLLOW_APP
        }

        val accountId = prefs.getString("$KEY_ACCOUNT_ID_PREFIX$appWidgetId", null)

        return WidgetConfig(
            appWidgetId = appWidgetId,
            displayType = displayType,
            privacySetting = privacySetting,
            accountId = accountId
        )
    }

    fun saveWidgetConfig(context: Context, config: WidgetConfig) {
        getPrefs(context).edit().apply {
            putString("$KEY_DISPLAY_TYPE_PREFIX${config.appWidgetId}", config.displayType.name)
            putString("$KEY_PRIVACY_PREFIX${config.appWidgetId}", config.privacySetting.name)
            if (config.accountId != null) {
                putString("$KEY_ACCOUNT_ID_PREFIX${config.appWidgetId}", config.accountId)
            } else {
                remove("$KEY_ACCOUNT_ID_PREFIX${config.appWidgetId}")
            }
            apply()
        }
    }

    fun toggleWidgetPrivacy(context: Context, appWidgetId: Int): WidgetPrivacySetting {
        val current = getWidgetConfig(context, appWidgetId)
        val newSetting = when (current.privacySetting) {
            WidgetPrivacySetting.ALWAYS_HIDE -> WidgetPrivacySetting.ALWAYS_SHOW
            WidgetPrivacySetting.ALWAYS_SHOW -> WidgetPrivacySetting.FOLLOW_APP
            WidgetPrivacySetting.FOLLOW_APP -> WidgetPrivacySetting.ALWAYS_HIDE
        }
        saveWidgetConfig(context, current.copy(privacySetting = newSetting))
        return newSetting
    }

    fun deleteWidgetConfig(context: Context, appWidgetId: Int) {
        getPrefs(context).edit().apply {
            remove("$KEY_DISPLAY_TYPE_PREFIX$appWidgetId")
            remove("$KEY_PRIVACY_PREFIX$appWidgetId")
            remove("$KEY_ACCOUNT_ID_PREFIX$appWidgetId")
            apply()
        }
    }
}

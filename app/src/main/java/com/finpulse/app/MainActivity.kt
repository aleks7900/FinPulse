package com.finpulse.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.finpulse.app.core.designsystem.FinPulseTheme
import com.finpulse.app.core.locale.AppLanguage
import com.finpulse.app.core.locale.AppLocaleManager
import com.finpulse.app.core.security.LockTimeoutPolicy
import com.finpulse.app.core.util.FinPulseShortcutsManager
import com.finpulse.app.presentation.navigation.FinPulseApp
import com.finpulse.app.presentation.navigation.Screen
import com.finpulse.app.presentation.widget.FinPulseWidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val openQuickAddTypeFlow = MutableStateFlow<String?>(null)
    private val navigationFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FinPulseApplication
        val container = app.container

        handleIntent(intent)

        lifecycleScope.launch {
            val prefs = container.userPreferencesDataStore.userPreferencesFlow.first()
            if (prefs.selectedLanguage != "SYSTEM") {
                val lang = AppLanguage.fromCode(prefs.selectedLanguage)
                AppLocaleManager.setLocale(this@MainActivity, lang)
            }

            // On cold start, lock app if authentication is configured
            val isLockEnabled = container.appLockManager.isLockConfigured(
                isBiometricEnabled = prefs.isBiometricEnabled,
                isPinEnabled = prefs.isPinEnabled
            )
            if (isLockEnabled) {
                container.appLockManager.lockNow()
            }
        }

        setContent {
            val userPrefs by container.userPreferencesDataStore.userPreferencesFlow.collectAsState(
                initial = com.finpulse.app.core.datastore.UserPreferences()
            )
            val isAppLocked by container.appLockManager.isAppLocked.collectAsState()

            // Dynamic Screenshot & Recents Protection (FLAG_SECURE)
            // Enable if screenshot protection is on OR if the app is currently locked
            if (userPrefs.enableScreenshotProtection || isAppLocked) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            val darkTheme = userPrefs.isDarkMode ?: isSystemInDarkTheme()

            FinPulseTheme(darkTheme = darkTheme) {
                FinPulseApp(
                    container = container,
                    openQuickAddTypeTrigger = openQuickAddTypeFlow,
                    onQuickAddTypeHandled = { openQuickAddTypeFlow.value = null },
                    navigationTrigger = navigationFlow,
                    onNavigationHandled = { navigationFlow.value = null }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val app = application as FinPulseApplication
        val container = app.container
        lifecycleScope.launch {
            val prefs = container.userPreferencesDataStore.userPreferencesFlow.first()
            val isLockEnabled = container.appLockManager.isLockConfigured(
                isBiometricEnabled = prefs.isBiometricEnabled,
                isPinEnabled = prefs.isPinEnabled
            )
            val policy = LockTimeoutPolicy.fromName(prefs.lockTimeout)
            container.appLockManager.onAppForegrounded(policy, isLockEnabled)
        }
    }

    override fun onStop() {
        super.onStop()
        val app = application as FinPulseApplication
        val container = app.container
        lifecycleScope.launch {
            val prefs = container.userPreferencesDataStore.userPreferencesFlow.first()
            val isLockEnabled = container.appLockManager.isLockConfigured(
                isBiometricEnabled = prefs.isBiometricEnabled,
                isPinEnabled = prefs.isPinEnabled
            )
            val policy = LockTimeoutPolicy.fromName(prefs.lockTimeout)
            container.appLockManager.onAppBackgrounded(policy, isLockEnabled)
        }
    }

    override fun onResume() {
        super.onResume()
        FinPulseShortcutsManager.initDynamicShortcuts(this)
        FinPulseWidgetUpdater.updateAllWidgets(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        if (data?.scheme == "finpulse") {
            when (data.host) {
                "quick-add" -> {
                    val type = data.getQueryParameter("type")
                        ?: intent.getStringExtra("quick_add_type")
                        ?: "EXPENSE"
                    openQuickAddTypeFlow.value = type
                }
                "review-inbox" -> {
                    navigationFlow.value = Screen.ReviewInbox.route
                }
                "accounts" -> {
                    navigationFlow.value = Screen.Accounts.route
                }
                "budgets" -> {
                    navigationFlow.value = Screen.Budgets.route
                }
                "analytics" -> {
                    navigationFlow.value = Screen.Analytics.route
                }
                "calendar" -> {
                    navigationFlow.value = Screen.Calendar.route
                }
                "digest" -> {
                    navigationFlow.value = Screen.Digest.route
                }
            }
        } else {
            val destination = intent?.getStringExtra("destination_route")
            if (destination != null) {
                navigationFlow.value = when (destination) {
                    "review_inbox" -> Screen.ReviewInbox.route
                    "accounts" -> Screen.Accounts.route
                    "budgets" -> Screen.Budgets.route
                    "analytics" -> Screen.Analytics.route
                    "calendar" -> Screen.Calendar.route
                    "digest" -> Screen.Digest.route
                    else -> destination
                }
            } else if (intent?.getBooleanExtra("open_quick_add", false) == true) {
                val type = intent.getStringExtra("quick_add_type") ?: "EXPENSE"
                openQuickAddTypeFlow.value = type
            }
        }
    }
}

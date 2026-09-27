package com.finpulse.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val baseCurrencyCode: String = "USD",
    val selectedLanguage: String = "SYSTEM",
    val isBiometricEnabled: Boolean = false,
    val isPinEnabled: Boolean = false,
    val pinHash: String = "",
    val hideBalances: Boolean = false,
    val isDarkMode: Boolean? = null, // null = system default
    val isOnboardingCompleted: Boolean = false,
    val enableScreenshotProtection: Boolean = false,
    val lastUsedAccountId: String? = null,
    val lastUsedCategoryId: String? = null,
    val lastUsedTransactionType: String = "EXPENSE",
    val dismissedInboxItemIds: Set<String> = emptySet()
)

class UserPreferencesDataStore(private val context: Context) {

    private object PreferencesKeys {
        val BASE_CURRENCY = stringPreferencesKey("base_currency")
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
        val DARK_MODE = stringPreferencesKey("dark_mode") // "SYSTEM", "LIGHT", "DARK"
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        val LAST_USED_ACCOUNT_ID = stringPreferencesKey("last_used_account_id")
        val LAST_USED_CATEGORY_ID = stringPreferencesKey("last_used_category_id")
        val LAST_USED_TX_TYPE = stringPreferencesKey("last_used_tx_type")
        val DISMISSED_INBOX_ITEMS = stringSetPreferencesKey("dismissed_inbox_items")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val darkModeStr = preferences[PreferencesKeys.DARK_MODE] ?: "SYSTEM"
        val darkModeBool = when (darkModeStr) {
            "DARK" -> true
            "LIGHT" -> false
            else -> null
        }
        UserPreferences(
            baseCurrencyCode = preferences[PreferencesKeys.BASE_CURRENCY] ?: "USD",
            selectedLanguage = preferences[PreferencesKeys.SELECTED_LANGUAGE] ?: "SYSTEM",
            isBiometricEnabled = preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false,
            isPinEnabled = preferences[PreferencesKeys.PIN_ENABLED] ?: false,
            pinHash = preferences[PreferencesKeys.PIN_HASH] ?: "",
            hideBalances = preferences[PreferencesKeys.HIDE_BALANCES] ?: false,
            isDarkMode = darkModeBool,
            isOnboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
            enableScreenshotProtection = preferences[PreferencesKeys.SCREENSHOT_PROTECTION] ?: false,
            lastUsedAccountId = preferences[PreferencesKeys.LAST_USED_ACCOUNT_ID],
            lastUsedCategoryId = preferences[PreferencesKeys.LAST_USED_CATEGORY_ID],
            lastUsedTransactionType = preferences[PreferencesKeys.LAST_USED_TX_TYPE] ?: "EXPENSE",
            dismissedInboxItemIds = preferences[PreferencesKeys.DISMISSED_INBOX_ITEMS] ?: emptySet()
        )
    }

    suspend fun setSelectedLanguage(code: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_LANGUAGE] = code
        }
    }

    suspend fun setBaseCurrency(currencyCode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BASE_CURRENCY] = currencyCode
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setPin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PIN_ENABLED] = pin.isNotBlank()
            preferences[PreferencesKeys.PIN_HASH] = if (pin.isNotBlank()) hashPin(pin) else ""
        }
    }

    suspend fun setHideBalances(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_BALANCES] = hide
        }
    }

    suspend fun setDarkMode(mode: String) { // "SYSTEM", "LIGHT", "DARK"
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = mode
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setScreenshotProtection(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCREENSHOT_PROTECTION] = enabled
        }
    }

    suspend fun setLastUsedTransactionDefaults(accountId: String, categoryId: String, type: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_USED_ACCOUNT_ID] = accountId
            preferences[PreferencesKeys.LAST_USED_CATEGORY_ID] = categoryId
            preferences[PreferencesKeys.LAST_USED_TX_TYPE] = type
        }
    }

    suspend fun dismissInboxItem(id: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.DISMISSED_INBOX_ITEMS] ?: emptySet()
            preferences[PreferencesKeys.DISMISSED_INBOX_ITEMS] = current + id
        }
    }

    suspend fun dismissInboxItems(ids: Collection<String>) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.DISMISSED_INBOX_ITEMS] ?: emptySet()
            preferences[PreferencesKeys.DISMISSED_INBOX_ITEMS] = current + ids
        }
    }

    suspend fun clearDismissedInboxItems() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.DISMISSED_INBOX_ITEMS)
        }
    }

    companion object {
        fun hashPin(pin: String): String {
            val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}

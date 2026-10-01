package md.alexlab.finpulse.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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
    val pinSalt: String = "",
    val encryptedPinSecret: String = "",
    val lockTimeout: String = "MINUTE_1",
    val deviceCredentialFallbackEnabled: Boolean = true,
    val hideBalances: Boolean = false,
    val isDarkMode: Boolean? = null, // null = system default
    val isOnboardingCompleted: Boolean = false,
    val enableScreenshotProtection: Boolean = false,
    val lastUsedAccountId: String? = null,
    val lastUsedCategoryId: String? = null,
    val lastUsedTransactionType: String = "EXPENSE",
    val dismissedInboxItemIds: Set<String> = emptySet(),
    val widgetPrivacyEnabled: Boolean = false,
    val widgetMaskOnAppLock: Boolean = true,
    val lastBackupTimestamp: Long? = null,
    val backupReminderInterval: String = "OFF",
    val lastBackupReminderDismissedMillis: Long = 0L,
    val currentUserId: String? = null,
    val userEmail: String? = null,
    val userDisplayName: String? = null,
    val userPhotoUrl: String? = null,
    val lastSyncTimestamp: Long = 0L,
    val syncStatus: String = "IDLE",
    val syncErrorMessage: String? = null,
    val digestFrequency: String = "DAILY", // "DAILY", "WEEKLY", "OFF"
    val digestPrivacyEnabled: Boolean = false,
    val digestDeliveryHour: Int = 20, // 8 PM default
    val lastDigestSentMillis: Long = 0L,
    val settingsUpdatedAt: Long = 0L
)

class UserPreferencesDataStore(private val context: Context) {

    private object PreferencesKeys {
        val BASE_CURRENCY = stringPreferencesKey("base_currency")
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val ENCRYPTED_PIN_SECRET = stringPreferencesKey("encrypted_pin_secret")
        val LOCK_TIMEOUT = stringPreferencesKey("lock_timeout")
        val DEVICE_CREDENTIAL_FALLBACK = booleanPreferencesKey("device_credential_fallback")
        val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
        val DARK_MODE = stringPreferencesKey("dark_mode") // "SYSTEM", "LIGHT", "DARK"
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        val LAST_USED_ACCOUNT_ID = stringPreferencesKey("last_used_account_id")
        val LAST_USED_CATEGORY_ID = stringPreferencesKey("last_used_category_id")
        val LAST_USED_TX_TYPE = stringPreferencesKey("last_used_tx_type")
        val DISMISSED_INBOX_ITEMS = stringSetPreferencesKey("dismissed_inbox_items")
        val WIDGET_PRIVACY_ENABLED = booleanPreferencesKey("widget_privacy_enabled")
        val WIDGET_MASK_ON_APP_LOCK = booleanPreferencesKey("widget_mask_on_app_lock")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val BACKUP_REMINDER_INTERVAL = stringPreferencesKey("backup_reminder_interval")
        val LAST_BACKUP_REMINDER_DISMISSED = longPreferencesKey("last_backup_reminder_dismissed")
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        val USER_PHOTO_URL = stringPreferencesKey("user_photo_url")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val SYNC_STATUS = stringPreferencesKey("sync_status")
        val SYNC_ERROR_MESSAGE = stringPreferencesKey("sync_error_message")
        val DIGEST_FREQUENCY = stringPreferencesKey("digest_frequency")
        val DIGEST_PRIVACY_ENABLED = booleanPreferencesKey("digest_privacy_enabled")
        val DIGEST_DELIVERY_HOUR = androidx.datastore.preferences.core.intPreferencesKey("digest_delivery_hour")
        val LAST_DIGEST_SENT_MILLIS = longPreferencesKey("last_digest_sent_millis")
        val SETTINGS_UPDATED_AT = longPreferencesKey("settings_updated_at")
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
            pinSalt = preferences[PreferencesKeys.PIN_SALT] ?: "",
            encryptedPinSecret = preferences[PreferencesKeys.ENCRYPTED_PIN_SECRET] ?: "",
            lockTimeout = preferences[PreferencesKeys.LOCK_TIMEOUT] ?: "MINUTE_1",
            deviceCredentialFallbackEnabled = preferences[PreferencesKeys.DEVICE_CREDENTIAL_FALLBACK] ?: true,
            hideBalances = preferences[PreferencesKeys.HIDE_BALANCES] ?: false,
            isDarkMode = darkModeBool,
            isOnboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
            enableScreenshotProtection = preferences[PreferencesKeys.SCREENSHOT_PROTECTION] ?: false,
            lastUsedAccountId = preferences[PreferencesKeys.LAST_USED_ACCOUNT_ID],
            lastUsedCategoryId = preferences[PreferencesKeys.LAST_USED_CATEGORY_ID],
            lastUsedTransactionType = preferences[PreferencesKeys.LAST_USED_TX_TYPE] ?: "EXPENSE",
            dismissedInboxItemIds = preferences[PreferencesKeys.DISMISSED_INBOX_ITEMS] ?: emptySet(),
            widgetPrivacyEnabled = preferences[PreferencesKeys.WIDGET_PRIVACY_ENABLED] ?: false,
            widgetMaskOnAppLock = preferences[PreferencesKeys.WIDGET_MASK_ON_APP_LOCK] ?: true,
            lastBackupTimestamp = preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP],
            backupReminderInterval = preferences[PreferencesKeys.BACKUP_REMINDER_INTERVAL] ?: "OFF",
            lastBackupReminderDismissedMillis = preferences[PreferencesKeys.LAST_BACKUP_REMINDER_DISMISSED] ?: 0L,
            currentUserId = preferences[PreferencesKeys.CURRENT_USER_ID],
            userEmail = preferences[PreferencesKeys.USER_EMAIL],
            userDisplayName = preferences[PreferencesKeys.USER_DISPLAY_NAME],
            userPhotoUrl = preferences[PreferencesKeys.USER_PHOTO_URL],
            lastSyncTimestamp = preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: 0L,
            syncStatus = preferences[PreferencesKeys.SYNC_STATUS] ?: "IDLE",
            syncErrorMessage = preferences[PreferencesKeys.SYNC_ERROR_MESSAGE],
            digestFrequency = preferences[PreferencesKeys.DIGEST_FREQUENCY] ?: "DAILY",
            digestPrivacyEnabled = preferences[PreferencesKeys.DIGEST_PRIVACY_ENABLED] ?: false,
            digestDeliveryHour = preferences[PreferencesKeys.DIGEST_DELIVERY_HOUR] ?: 20,
            lastDigestSentMillis = preferences[PreferencesKeys.LAST_DIGEST_SENT_MILLIS] ?: 0L,
            settingsUpdatedAt = preferences[PreferencesKeys.SETTINGS_UPDATED_AT] ?: 0L
        )
    }

    suspend fun setDigestFrequency(frequency: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DIGEST_FREQUENCY] = frequency
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setDigestPrivacyEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DIGEST_PRIVACY_ENABLED] = enabled
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setDigestDeliveryHour(hour: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DIGEST_DELIVERY_HOUR] = hour
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setLastDigestSentMillis(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_DIGEST_SENT_MILLIS] = timestamp
        }
    }

    suspend fun setSelectedLanguage(code: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_LANGUAGE] = code
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setBaseCurrency(currencyCode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BASE_CURRENCY] = currencyCode
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setPin(pin: String) {
        context.dataStore.edit { preferences ->
            if (pin.isNotBlank()) {
                val salt = md.alexlab.finpulse.core.security.AppKeystoreManager.generateSalt()
                val hash = md.alexlab.finpulse.core.security.AppKeystoreManager.hashPinWithSalt(pin, salt)
                val encrypted = md.alexlab.finpulse.core.security.AppKeystoreManager.encryptSecret(pin)
                preferences[PreferencesKeys.PIN_ENABLED] = true
                preferences[PreferencesKeys.PIN_HASH] = hash
                preferences[PreferencesKeys.PIN_SALT] = salt
                preferences[PreferencesKeys.ENCRYPTED_PIN_SECRET] = encrypted
            } else {
                preferences[PreferencesKeys.PIN_ENABLED] = false
                preferences[PreferencesKeys.PIN_HASH] = ""
                preferences[PreferencesKeys.PIN_SALT] = ""
                preferences[PreferencesKeys.ENCRYPTED_PIN_SECRET] = ""
            }
        }
    }

    suspend fun setLockTimeout(timeout: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCK_TIMEOUT] = timeout
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setDeviceCredentialFallbackEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEVICE_CREDENTIAL_FALLBACK] = enabled
        }
    }

    suspend fun setHideBalances(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_BALANCES] = hide
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setDarkMode(mode: String) { // "SYSTEM", "LIGHT", "DARK"
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = mode
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
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
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun dismissInboxItems(ids: Collection<String>) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.DISMISSED_INBOX_ITEMS] ?: emptySet()
            preferences[PreferencesKeys.DISMISSED_INBOX_ITEMS] = current + ids
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun clearDismissedInboxItems() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.DISMISSED_INBOX_ITEMS)
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setWidgetPrivacyEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIDGET_PRIVACY_ENABLED] = enabled
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setWidgetMaskOnAppLock(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIDGET_MASK_ON_APP_LOCK] = enabled
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    suspend fun setBackupReminderInterval(interval: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_REMINDER_INTERVAL] = interval
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setLastBackupReminderDismissedMillis(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_REMINDER_DISMISSED] = timestamp
        }
    }

    suspend fun restoreCloudSettings(settings: md.alexlab.finpulse.domain.model.sync.CloudSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BASE_CURRENCY] = settings.baseCurrencyCode
            preferences[PreferencesKeys.SELECTED_LANGUAGE] = settings.selectedLanguage
            if (settings.darkMode != null) {
                preferences[PreferencesKeys.DARK_MODE] = settings.darkMode
            }
            preferences[PreferencesKeys.HIDE_BALANCES] = settings.hideBalances
            preferences[PreferencesKeys.WIDGET_PRIVACY_ENABLED] = settings.widgetPrivacyEnabled
            preferences[PreferencesKeys.WIDGET_MASK_ON_APP_LOCK] = settings.widgetMaskOnAppLock
            preferences[PreferencesKeys.LOCK_TIMEOUT] = settings.lockTimeout
            preferences[PreferencesKeys.BACKUP_REMINDER_INTERVAL] = settings.backupReminderInterval
            preferences[PreferencesKeys.DIGEST_FREQUENCY] = settings.digestFrequency
            preferences[PreferencesKeys.DIGEST_PRIVACY_ENABLED] = settings.digestPrivacyEnabled
            preferences[PreferencesKeys.DIGEST_DELIVERY_HOUR] = settings.digestDeliveryHour
            if (settings.dismissedInboxItemIds.isNotEmpty()) {
                preferences[PreferencesKeys.DISMISSED_INBOX_ITEMS] = settings.dismissedInboxItemIds.toSet()
            }
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = settings.updatedAt
        }
    }

    suspend fun resetSettingsUpdatedAt() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SETTINGS_UPDATED_AT] = 0L
        }
    }

    suspend fun setUserSession(uid: String, email: String?, displayName: String?, photoUrl: String?) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_USER_ID] = uid
            if (email != null) preferences[PreferencesKeys.USER_EMAIL] = email else preferences.remove(PreferencesKeys.USER_EMAIL)
            if (displayName != null) preferences[PreferencesKeys.USER_DISPLAY_NAME] = displayName else preferences.remove(PreferencesKeys.USER_DISPLAY_NAME)
            if (photoUrl != null) preferences[PreferencesKeys.USER_PHOTO_URL] = photoUrl else preferences.remove(PreferencesKeys.USER_PHOTO_URL)
        }
    }

    suspend fun clearUserSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.CURRENT_USER_ID)
            preferences.remove(PreferencesKeys.USER_EMAIL)
            preferences.remove(PreferencesKeys.USER_DISPLAY_NAME)
            preferences.remove(PreferencesKeys.USER_PHOTO_URL)
            preferences[PreferencesKeys.SYNC_STATUS] = "IDLE"
            preferences.remove(PreferencesKeys.SYNC_ERROR_MESSAGE)
        }
    }

    suspend fun setSyncState(status: String, timestamp: Long, errorMessage: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_STATUS] = status
            if (timestamp > 0L) {
                preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp
            }
            if (errorMessage != null) {
                preferences[PreferencesKeys.SYNC_ERROR_MESSAGE] = errorMessage
            } else {
                preferences.remove(PreferencesKeys.SYNC_ERROR_MESSAGE)
            }
        }
    }

    companion object {
        fun hashPin(pin: String): String {
            val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}

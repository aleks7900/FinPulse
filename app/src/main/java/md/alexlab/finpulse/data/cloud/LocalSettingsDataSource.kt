package md.alexlab.finpulse.data.cloud

import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.domain.model.sync.CloudSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface LocalSettingsDataSource {
    fun getSettingsFlow(): Flow<CloudSettings>
    suspend fun getSettings(): CloudSettings
    suspend fun restoreSettings(settings: CloudSettings)
    suspend fun resetSettingsTimestamp()
}

class DataStoreLocalSettingsDataSource(
    private val userPreferencesDataStore: UserPreferencesDataStore
) : LocalSettingsDataSource {

    override fun getSettingsFlow(): Flow<CloudSettings> =
        userPreferencesDataStore.userPreferencesFlow.map { prefs ->
            val darkModeStr = when (prefs.isDarkMode) {
                true -> "DARK"
                false -> "LIGHT"
                null -> "SYSTEM"
            }
            CloudSettings(
                schemaVersion = CloudSettings.CURRENT_SCHEMA_VERSION,
                baseCurrencyCode = prefs.baseCurrencyCode,
                selectedLanguage = prefs.selectedLanguage,
                darkMode = darkModeStr,
                hideBalances = prefs.hideBalances,
                widgetPrivacyEnabled = prefs.widgetPrivacyEnabled,
                widgetMaskOnAppLock = prefs.widgetMaskOnAppLock,
                lockTimeout = prefs.lockTimeout,
                backupReminderInterval = prefs.backupReminderInterval,
                digestFrequency = prefs.digestFrequency,
                digestPrivacyEnabled = prefs.digestPrivacyEnabled,
                digestDeliveryHour = prefs.digestDeliveryHour,
                dismissedInboxItemIds = prefs.dismissedInboxItemIds.toList(),
                updatedAt = prefs.settingsUpdatedAt
            )
        }

    override suspend fun getSettings(): CloudSettings = getSettingsFlow().first()

    override suspend fun restoreSettings(settings: CloudSettings) {
        userPreferencesDataStore.restoreCloudSettings(settings)
    }

    override suspend fun resetSettingsTimestamp() {
        userPreferencesDataStore.resetSettingsUpdatedAt()
    }
}

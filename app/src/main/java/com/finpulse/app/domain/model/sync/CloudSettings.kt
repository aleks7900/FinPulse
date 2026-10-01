package com.finpulse.app.domain.model.sync

import kotlinx.serialization.Serializable

@Serializable
data class CloudSettings(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val baseCurrencyCode: String = "USD",
    val selectedLanguage: String = "SYSTEM",
    val darkMode: String? = null, // "SYSTEM", "LIGHT", "DARK"
    val hideBalances: Boolean = false,
    val widgetPrivacyEnabled: Boolean = false,
    val widgetMaskOnAppLock: Boolean = true,
    val lockTimeout: String = "MINUTE_1",
    val backupReminderInterval: String = "OFF",
    val digestFrequency: String = "DAILY",
    val digestPrivacyEnabled: Boolean = false,
    val digestDeliveryHour: Int = 20,
    val dismissedInboxItemIds: List<String> = emptyList(),
    val updatedAt: Long = 0L,
    val deviceModel: String? = null
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val SETTINGS_COLLECTION = "settings"
        const val SETTINGS_DOCUMENT_ID = "app"
    }
}

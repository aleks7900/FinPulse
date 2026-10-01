package md.alexlab.finpulse.domain.model.sync

import kotlinx.serialization.Serializable

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    WAITING_FOR_NETWORK
}

@Serializable
data class SyncResult(
    val isSuccess: Boolean,
    val uploadedCount: Int = 0,
    val downloadedCount: Int = 0,
    val deletedCount: Int = 0,
    val conflictsResolvedCount: Int = 0,
    val errorMessage: String? = null,
    val syncedAt: Long = System.currentTimeMillis()
) {
    val totalProcessed: Int
        get() = uploadedCount + downloadedCount + deletedCount
}

enum class SyncEntityType {
    TRANSACTION,
    ACCOUNT,
    CATEGORY,
    BUDGET,
    RECURRING_RULE,
    GOAL,
    ASSET,
    DEBT,
    CATEGORIZATION_RULE,
    SAVED_FILTER,
    SETTINGS
}

@Serializable
data class CloudAccountSettings(
    val baseCurrencyCode: String = "USD",
    val selectedLanguage: String = "SYSTEM",
    val hideBalances: Boolean = false,
    val darkMode: String? = null,
    val widgetPrivacyEnabled: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

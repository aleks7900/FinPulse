package md.alexlab.finpulse.domain.model.backup

import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.Budget
import md.alexlab.finpulse.domain.model.CategorizationRule
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.DateRangePreset
import md.alexlab.finpulse.domain.model.Debt
import md.alexlab.finpulse.domain.model.FinancialGoal
import md.alexlab.finpulse.domain.model.InvestmentAsset
import md.alexlab.finpulse.domain.model.RecurringOccurrence
import md.alexlab.finpulse.domain.model.RecurringTransaction
import md.alexlab.finpulse.domain.model.SavedFilter
import md.alexlab.finpulse.domain.model.Transaction
import kotlinx.serialization.Serializable

const val CURRENT_BACKUP_VERSION = 1
const val CURRENT_DATABASE_SCHEMA_VERSION = 6
const val BACKUP_ENCRYPTED_FORMAT_ID = "FINPULSE_ENCRYPTED_BACKUP"

@Serializable
data class BackupEntityCounts(
    val accountsCount: Int = 0,
    val categoriesCount: Int = 0,
    val transactionsCount: Int = 0,
    val budgetsCount: Int = 0,
    val recurringRulesCount: Int = 0,
    val recurringOccurrencesCount: Int = 0,
    val financialGoalsCount: Int = 0,
    val assetsCount: Int = 0,
    val debtsCount: Int = 0,
    val categorizationRulesCount: Int = 0,
    val savedFiltersCount: Int = 0
) {
    val totalRecords: Int
        get() = accountsCount + categoriesCount + transactionsCount + budgetsCount +
                recurringRulesCount + recurringOccurrencesCount + financialGoalsCount +
                assetsCount + debtsCount + categorizationRulesCount + savedFiltersCount
}

@Serializable
data class BackupMetadata(
    val backupVersion: Int = CURRENT_BACKUP_VERSION,
    val appVersion: String = "1.0.0",
    val schemaVersion: Int = CURRENT_DATABASE_SCHEMA_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val checksum: String = "",
    val deviceModel: String = "",
    val counts: BackupEntityCounts = BackupEntityCounts()
)

@Serializable
data class FinPulseFullBackup(
    val metadata: BackupMetadata,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val recurringRules: List<RecurringTransaction> = emptyList(),
    val recurringOccurrences: List<RecurringOccurrence> = emptyList(),
    val financialGoals: List<FinancialGoal> = emptyList(),
    val assets: List<InvestmentAsset> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val categorizationRules: List<CategorizationRule> = emptyList(),
    val savedFilters: List<SavedFilter> = emptyList()
)

@Serializable
data class BackupExportPayload(
    val content: String,
    val isEncrypted: Boolean,
    val filename: String,
    val metadata: BackupMetadata
)

@Serializable
data class EncryptedBackupContainer(
    val format: String = BACKUP_ENCRYPTED_FORMAT_ID,
    val version: Int = 1,
    val algorithm: String = "AES-256-GCM",
    val kdf: String = "PBKDF2WithHmacSHA256",
    val iterations: Int = 65536,
    val saltBase64: String,
    val ivBase64: String,
    val ciphertextBase64: String
)

data class BackupValidationResult(
    val isValid: Boolean,
    val metadata: BackupMetadata? = null,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val backupData: FinPulseFullBackup? = null
)

@Serializable
data class RestoreSummary(
    val restoredAt: Long = System.currentTimeMillis(),
    val metadata: BackupMetadata,
    val totalRestoredRecords: Int
)

enum class BackupReminderInterval(val days: Int, val displayName: String) {
    OFF(0, "Off"),
    WEEKLY(7, "Weekly (7 days)"),
    MONTHLY(30, "Monthly (30 days)")
}

data class ExportFilterParams(
    val dateRangePreset: DateRangePreset = DateRangePreset.ALL,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val selectedAccountId: String? = null
)

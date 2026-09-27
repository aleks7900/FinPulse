package com.finpulse.app.data.repository

import androidx.room.withTransaction
import com.finpulse.app.core.database.FinPulseDatabase
import com.finpulse.app.core.database.util.TransactionQueryBuilder
import com.finpulse.app.core.security.BackupCrypto
import com.finpulse.app.data.mapper.toDomain
import com.finpulse.app.data.mapper.toEntity
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionFilterParams
import com.finpulse.app.domain.model.TransactionSort
import com.finpulse.app.domain.model.backup.BackupEntityCounts
import com.finpulse.app.domain.model.backup.BackupMetadata
import com.finpulse.app.domain.model.backup.CURRENT_BACKUP_VERSION
import com.finpulse.app.domain.model.backup.CURRENT_DATABASE_SCHEMA_VERSION
import com.finpulse.app.domain.model.backup.ExportFilterParams
import com.finpulse.app.domain.model.backup.FinPulseFullBackup
import com.finpulse.app.domain.model.backup.RestoreSummary
import com.finpulse.app.domain.repository.BackupRepository

class BackupRepositoryImpl(
    private val database: FinPulseDatabase
) : BackupRepository {

    override suspend fun createFullBackup(): FinPulseFullBackup {
        val accounts = database.accountDao().getAllAccounts().map { it.toDomain() }
        val categories = database.categoryDao().getAllCategories().map { it.toDomain() }
        val transactions = database.transactionDao().getAllTransactions().map { it.toDomain() }
        val budgets = database.budgetDao().getAllBudgets().map { it.toDomain() }
        val recurringRules = database.recurringTransactionDao().getAllRecurring().map { it.toDomain() }
        val rulesMap = recurringRules.associateBy { it.id }
        val occurrences = database.recurringTransactionDao().getAllOccurrences().mapNotNull { occEntity ->
            rulesMap[occEntity.ruleId]?.let { rule -> occEntity.toDomain(rule) }
        }
        val goals = database.financialGoalDao().getAllGoals().map { it.toDomain() }
        val assets = database.assetDao().getAllAssets().map { it.toDomain() }
        val debts = database.debtDao().getAllDebts().map { it.toDomain() }
        val rules = database.categorizationRuleDao().getAllRules().map { it.toDomain() }
        val savedFilters = database.savedFilterDao().getAllSavedFilters().map { it.toDomain() }

        val counts = BackupEntityCounts(
            accountsCount = accounts.size,
            categoriesCount = categories.size,
            transactionsCount = transactions.size,
            budgetsCount = budgets.size,
            recurringRulesCount = recurringRules.size,
            recurringOccurrencesCount = occurrences.size,
            financialGoalsCount = goals.size,
            assetsCount = assets.size,
            debtsCount = debts.size,
            categorizationRulesCount = rules.size,
            savedFiltersCount = savedFilters.size
        )

        val metadataWithoutChecksum = BackupMetadata(
            backupVersion = CURRENT_BACKUP_VERSION,
            appVersion = "1.0.0",
            schemaVersion = CURRENT_DATABASE_SCHEMA_VERSION,
            createdAt = System.currentTimeMillis(),
            deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim(),
            counts = counts
        )

        // Compute checksum of the data payload representation
        val manifestForChecksum = "${metadataWithoutChecksum.createdAt}:${counts.totalRecords}:${transactions.size}:${accounts.size}"
        val checksum = BackupCrypto.computeSha256(manifestForChecksum)

        val finalMetadata = metadataWithoutChecksum.copy(checksum = checksum)

        return FinPulseFullBackup(
            metadata = finalMetadata,
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            recurringRules = recurringRules,
            recurringOccurrences = occurrences,
            financialGoals = goals,
            assets = assets,
            debts = debts,
            categorizationRules = rules,
            savedFilters = savedFilters
        )
    }

    override suspend fun restoreFullBackup(backup: FinPulseFullBackup): RestoreSummary {
        // Execute inside an atomic transaction: if anything fails, all changes are automatically rolled back.
        database.withTransaction {
            // 1. Clear existing database tables
            database.transactionDao().deleteAllTransactions()
            database.recurringTransactionDao().deleteAllOccurrences()
            database.recurringTransactionDao().deleteAllRecurring()
            database.budgetDao().deleteAllBudgets()
            database.financialGoalDao().deleteAllGoals()
            database.assetDao().deleteAllAssets()
            database.debtDao().deleteAllDebts()
            database.categorizationRuleDao().deleteAllRules()
            database.savedFilterDao().deleteAllSavedFilters()
            database.accountDao().deleteAllAccounts()
            database.categoryDao().deleteAllCategories()

            // 2. Insert restored entities in order of foreign key relationships
            if (backup.categories.isNotEmpty()) {
                database.categoryDao().insertCategories(backup.categories.map { it.toEntity() })
            }
            if (backup.accounts.isNotEmpty()) {
                database.accountDao().insertAccounts(backup.accounts.map { it.toEntity() })
            }
            if (backup.transactions.isNotEmpty()) {
                database.transactionDao().insertTransactions(backup.transactions.map { it.toEntity() })
            }
            if (backup.budgets.isNotEmpty()) {
                database.budgetDao().insertBudgets(backup.budgets.map { it.toEntity() })
            }
            if (backup.recurringRules.isNotEmpty()) {
                database.recurringTransactionDao().insertRecurringList(backup.recurringRules.map { it.toEntity() })
            }
            if (backup.recurringOccurrences.isNotEmpty()) {
                database.recurringTransactionDao().insertOccurrences(backup.recurringOccurrences.map { it.toEntity() })
            }
            if (backup.financialGoals.isNotEmpty()) {
                database.financialGoalDao().insertGoals(backup.financialGoals.map { it.toEntity() })
            }
            if (backup.assets.isNotEmpty()) {
                database.assetDao().insertAssets(backup.assets.map { it.toEntity() })
            }
            if (backup.debts.isNotEmpty()) {
                database.debtDao().insertDebts(backup.debts.map { it.toEntity() })
            }
            if (backup.categorizationRules.isNotEmpty()) {
                database.categorizationRuleDao().insertRules(backup.categorizationRules.map { it.toEntity() })
            }
            if (backup.savedFilters.isNotEmpty()) {
                database.savedFilterDao().insertSavedFilters(backup.savedFilters.map { it.toEntity() })
            }
        }

        return RestoreSummary(
            restoredAt = System.currentTimeMillis(),
            metadata = backup.metadata,
            totalRestoredRecords = backup.metadata.counts.totalRecords
        )
    }

    override suspend fun getFilteredTransactions(params: ExportFilterParams): List<Transaction> {
        val filterParams = TransactionFilterParams(
            accountId = params.selectedAccountId,
            dateRangePreset = params.dateRangePreset,
            customStartDate = params.customStartDate,
            customEndDate = params.customEndDate,
            sortOrder = TransactionSort.DATE_DESC
        )
        val query = TransactionQueryBuilder.buildQuery(filterParams)
        return database.transactionDao().queryTransactions(query).map { it.toDomain() }
    }
}

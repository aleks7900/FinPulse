package com.finpulse.app.di

import android.content.Context
import com.finpulse.app.core.database.FinPulseDatabase
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.data.repository.AccountRepositoryImpl
import com.finpulse.app.data.repository.BudgetRepositoryImpl
import com.finpulse.app.data.repository.CategoryRepositoryImpl
import com.finpulse.app.data.repository.DebtRepositoryImpl
import com.finpulse.app.data.repository.GoalRepositoryImpl
import com.finpulse.app.data.repository.InvestmentRepositoryImpl
import com.finpulse.app.data.repository.RecurringRepositoryImpl
import com.finpulse.app.data.repository.TransactionRepositoryImpl
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.BudgetRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.DebtRepository
import com.finpulse.app.domain.repository.GoalRepository
import com.finpulse.app.domain.repository.InvestmentRepository
import com.finpulse.app.domain.repository.RecurringRepository
import com.finpulse.app.domain.repository.TransactionRepository
import com.finpulse.app.domain.usecase.transaction.CreateTransactionUseCase
import com.finpulse.app.domain.usecase.transaction.GetQuickAddSuggestionsUseCase

interface AppContainer {
    val database: FinPulseDatabase
    val userPreferencesDataStore: UserPreferencesDataStore
    val accountRepository: AccountRepository
    val transactionRepository: TransactionRepository
    val categoryRepository: CategoryRepository
    val budgetRepository: BudgetRepository
    val recurringRepository: RecurringRepository
    val goalRepository: GoalRepository
    val investmentRepository: InvestmentRepository
    val debtRepository: DebtRepository
    val createTransactionUseCase: CreateTransactionUseCase
    val getQuickAddSuggestionsUseCase: GetQuickAddSuggestionsUseCase
    val getUpcomingOccurrencesUseCase: com.finpulse.app.domain.usecase.recurring.GetUpcomingOccurrencesUseCase
    val markOccurrencePaidUseCase: com.finpulse.app.domain.usecase.recurring.MarkOccurrencePaidUseCase
    val skipOccurrenceUseCase: com.finpulse.app.domain.usecase.recurring.SkipOccurrenceUseCase
    val editOccurrenceUseCase: com.finpulse.app.domain.usecase.recurring.EditOccurrenceUseCase
    val manageRecurringRuleUseCase: com.finpulse.app.domain.usecase.recurring.ManageRecurringRuleUseCase
    val categorizationRuleRepository: com.finpulse.app.domain.repository.CategorizationRuleRepository
    val merchantSignalRepository: com.finpulse.app.domain.repository.MerchantSignalRepository
    val categorizeTransactionUseCase: com.finpulse.app.domain.usecase.categorization.CategorizeTransactionUseCase
    val recordCategoryCorrectionUseCase: com.finpulse.app.domain.usecase.categorization.RecordCategoryCorrectionUseCase
    val manageCategorizationRuleUseCase: com.finpulse.app.domain.usecase.categorization.ManageCategorizationRuleUseCase
    val findMatchingTransactionsForRuleUseCase: com.finpulse.app.domain.usecase.categorization.FindMatchingTransactionsForRuleUseCase
    val applyRuleToExistingTransactionsUseCase: com.finpulse.app.domain.usecase.categorization.ApplyRuleToExistingTransactionsUseCase
    val getReviewQueueUseCase: com.finpulse.app.domain.usecase.categorization.GetReviewQueueUseCase
    val importProfileRepository: com.finpulse.app.domain.repository.ImportProfileRepository
    val autoDetectCsvConfigUseCase: com.finpulse.app.domain.usecase.csv.AutoDetectCsvConfigUseCase
    val parseCsvStatementUseCase: com.finpulse.app.domain.usecase.csv.ParseCsvStatementUseCase
    val executeCsvImportUseCase: com.finpulse.app.domain.usecase.csv.ExecuteCsvImportUseCase
    val manageImportProfilesUseCase: com.finpulse.app.domain.usecase.csv.ManageImportProfilesUseCase
    val getReviewInboxUseCase: com.finpulse.app.domain.usecase.review.GetReviewInboxUseCase
    val resolveDuplicateTransactionUseCase: com.finpulse.app.domain.usecase.review.ResolveDuplicateTransactionUseCase
    val bulkCategorizeTransactionsUseCase: com.finpulse.app.domain.usecase.review.BulkCategorizeTransactionsUseCase
    val updateTransactionDetailsUseCase: com.finpulse.app.domain.usecase.review.UpdateTransactionDetailsUseCase
    val calculateSafeToSpendUseCase: com.finpulse.app.domain.usecase.budget.CalculateSafeToSpendUseCase
    val getFinancialCalendarUseCase: com.finpulse.app.domain.usecase.calendar.GetFinancialCalendarUseCase
    val savedFilterRepository: com.finpulse.app.domain.repository.SavedFilterRepository
    val backupRepository: com.finpulse.app.domain.repository.BackupRepository
    val createBackupUseCase: com.finpulse.app.domain.usecase.backup.CreateBackupUseCase
    val validateBackupUseCase: com.finpulse.app.domain.usecase.backup.ValidateBackupUseCase
    val restoreBackupUseCase: com.finpulse.app.domain.usecase.backup.RestoreBackupUseCase
    val exportTransactionsUseCase: com.finpulse.app.domain.usecase.backup.ExportTransactionsUseCase
    val appLockManager: com.finpulse.app.core.security.AppLockManager
    val googleAuthManager: com.finpulse.app.core.auth.GoogleAuthManager
    val authRepository: com.finpulse.app.domain.repository.AuthRepository
    val cloudStorageDataSource: com.finpulse.app.data.cloud.CloudStorageDataSource
    val localSettingsDataSource: com.finpulse.app.data.cloud.LocalSettingsDataSource
    val cloudSettingsDataSource: com.finpulse.app.data.cloud.CloudSettingsDataSource
    val settingsRepository: com.finpulse.app.domain.repository.SettingsRepository
    val cloudSyncRepository: com.finpulse.app.domain.repository.CloudSyncRepository
    val getFinancialDigestUseCase: com.finpulse.app.domain.usecase.digest.GetFinancialDigestUseCase
    val exchangeRateProvider: com.finpulse.app.domain.repository.ExchangeRateProvider
    val currencyConverter: com.finpulse.app.domain.engine.CurrencyConverter
    val getDashboardSummaryUseCase: com.finpulse.app.domain.usecase.GetDashboardSummaryUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: FinPulseDatabase by lazy {
        FinPulseDatabase.getInstance(context)
    }

    override val appLockManager: com.finpulse.app.core.security.AppLockManager by lazy {
        com.finpulse.app.core.security.AppLockManager()
    }

    override val savedFilterRepository: com.finpulse.app.domain.repository.SavedFilterRepository by lazy {
        com.finpulse.app.data.repository.SavedFilterRepositoryImpl(database)
    }

    override val userPreferencesDataStore: UserPreferencesDataStore by lazy {
        UserPreferencesDataStore(context)
    }

    override val accountRepository: AccountRepository by lazy {
        AccountRepositoryImpl(database)
    }

    override val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database)
    }

    override val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database)
    }

    override val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(database)
    }

    override val recurringRepository: RecurringRepository by lazy {
        RecurringRepositoryImpl(database, transactionRepository)
    }

    override val goalRepository: GoalRepository by lazy {
        GoalRepositoryImpl(database)
    }

    override val investmentRepository: InvestmentRepository by lazy {
        InvestmentRepositoryImpl(database)
    }

    override val debtRepository: DebtRepository by lazy {
        DebtRepositoryImpl(database)
    }

    override val categorizationRuleRepository: com.finpulse.app.domain.repository.CategorizationRuleRepository by lazy {
        com.finpulse.app.data.repository.CategorizationRuleRepositoryImpl(database)
    }

    override val merchantSignalRepository: com.finpulse.app.domain.repository.MerchantSignalRepository by lazy {
        com.finpulse.app.data.repository.MerchantSignalRepositoryImpl(database)
    }

    val onlineExchangeRateClient: com.finpulse.app.data.remote.currency.OnlineExchangeRateClient by lazy {
        com.finpulse.app.data.remote.currency.GlobalOnlineExchangeRateClient()
    }

    override val exchangeRateProvider: com.finpulse.app.domain.repository.ExchangeRateProvider by lazy {
        com.finpulse.app.data.repository.ExchangeRateProviderImpl(
            dao = database.exchangeRateDao(),
            onlineClient = onlineExchangeRateClient
        )
    }

    override val currencyConverter: com.finpulse.app.domain.engine.CurrencyConverter by lazy {
        com.finpulse.app.domain.engine.CurrencyConverter(exchangeRateProvider)
    }

    override val getDashboardSummaryUseCase: com.finpulse.app.domain.usecase.GetDashboardSummaryUseCase by lazy {
        com.finpulse.app.domain.usecase.GetDashboardSummaryUseCase(currencyConverter)
    }

    override val createTransactionUseCase: CreateTransactionUseCase by lazy {
        CreateTransactionUseCase(transactionRepository, accountRepository, exchangeRateProvider)
    }

    override val getQuickAddSuggestionsUseCase: GetQuickAddSuggestionsUseCase by lazy {
        GetQuickAddSuggestionsUseCase(transactionRepository, categoryRepository, accountRepository, userPreferencesDataStore)
    }

    override val getUpcomingOccurrencesUseCase: com.finpulse.app.domain.usecase.recurring.GetUpcomingOccurrencesUseCase by lazy {
        com.finpulse.app.domain.usecase.recurring.GetUpcomingOccurrencesUseCase(recurringRepository)
    }

    override val markOccurrencePaidUseCase: com.finpulse.app.domain.usecase.recurring.MarkOccurrencePaidUseCase by lazy {
        com.finpulse.app.domain.usecase.recurring.MarkOccurrencePaidUseCase(recurringRepository, transactionRepository)
    }

    override val skipOccurrenceUseCase: com.finpulse.app.domain.usecase.recurring.SkipOccurrenceUseCase by lazy {
        com.finpulse.app.domain.usecase.recurring.SkipOccurrenceUseCase(recurringRepository)
    }

    override val editOccurrenceUseCase: com.finpulse.app.domain.usecase.recurring.EditOccurrenceUseCase by lazy {
        com.finpulse.app.domain.usecase.recurring.EditOccurrenceUseCase(recurringRepository)
    }

    override val manageRecurringRuleUseCase: com.finpulse.app.domain.usecase.recurring.ManageRecurringRuleUseCase by lazy {
        com.finpulse.app.domain.usecase.recurring.ManageRecurringRuleUseCase(recurringRepository, accountRepository)
    }

    override val categorizeTransactionUseCase: com.finpulse.app.domain.usecase.categorization.CategorizeTransactionUseCase by lazy {
        com.finpulse.app.domain.usecase.categorization.CategorizeTransactionUseCase(categorizationRuleRepository, merchantSignalRepository, categoryRepository)
    }

    override val recordCategoryCorrectionUseCase: com.finpulse.app.domain.usecase.categorization.RecordCategoryCorrectionUseCase by lazy {
        com.finpulse.app.domain.usecase.categorization.RecordCategoryCorrectionUseCase(transactionRepository, merchantSignalRepository)
    }

    override val manageCategorizationRuleUseCase: com.finpulse.app.domain.usecase.categorization.ManageCategorizationRuleUseCase by lazy {
        com.finpulse.app.domain.usecase.categorization.ManageCategorizationRuleUseCase(categorizationRuleRepository)
    }

    override val findMatchingTransactionsForRuleUseCase: com.finpulse.app.domain.usecase.categorization.FindMatchingTransactionsForRuleUseCase by lazy {
        com.finpulse.app.domain.usecase.categorization.FindMatchingTransactionsForRuleUseCase(categorizationRuleRepository, transactionRepository)
    }

    override val applyRuleToExistingTransactionsUseCase: com.finpulse.app.domain.usecase.categorization.ApplyRuleToExistingTransactionsUseCase by lazy {
        com.finpulse.app.domain.usecase.categorization.ApplyRuleToExistingTransactionsUseCase(categorizationRuleRepository, transactionRepository, findMatchingTransactionsForRuleUseCase)
    }

    override val getReviewQueueUseCase: com.finpulse.app.domain.usecase.categorization.GetReviewQueueUseCase by lazy {
        com.finpulse.app.domain.usecase.categorization.GetReviewQueueUseCase(transactionRepository, categorizationRuleRepository, merchantSignalRepository)
    }

    override val importProfileRepository: com.finpulse.app.domain.repository.ImportProfileRepository by lazy {
        com.finpulse.app.data.repository.ImportProfileRepositoryImpl(database)
    }

    override val autoDetectCsvConfigUseCase: com.finpulse.app.domain.usecase.csv.AutoDetectCsvConfigUseCase by lazy {
        com.finpulse.app.domain.usecase.csv.AutoDetectCsvConfigUseCase()
    }

    override val parseCsvStatementUseCase: com.finpulse.app.domain.usecase.csv.ParseCsvStatementUseCase by lazy {
        com.finpulse.app.domain.usecase.csv.ParseCsvStatementUseCase(transactionRepository, categorizeTransactionUseCase)
    }

    override val executeCsvImportUseCase: com.finpulse.app.domain.usecase.csv.ExecuteCsvImportUseCase by lazy {
        com.finpulse.app.domain.usecase.csv.ExecuteCsvImportUseCase(transactionRepository, accountRepository, merchantSignalRepository, categoryRepository)
    }

    override val manageImportProfilesUseCase: com.finpulse.app.domain.usecase.csv.ManageImportProfilesUseCase by lazy {
        com.finpulse.app.domain.usecase.csv.ManageImportProfilesUseCase(importProfileRepository)
    }

    override val getReviewInboxUseCase: com.finpulse.app.domain.usecase.review.GetReviewInboxUseCase by lazy {
        com.finpulse.app.domain.usecase.review.GetReviewInboxUseCase(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
            ruleRepository = categorizationRuleRepository,
            signalRepository = merchantSignalRepository,
            recurringRepository = recurringRepository,
            getUpcomingOccurrencesUseCase = getUpcomingOccurrencesUseCase,
            userPreferencesDataStore = userPreferencesDataStore
        )
    }

    override val resolveDuplicateTransactionUseCase: com.finpulse.app.domain.usecase.review.ResolveDuplicateTransactionUseCase by lazy {
        com.finpulse.app.domain.usecase.review.ResolveDuplicateTransactionUseCase(transactionRepository, userPreferencesDataStore)
    }

    override val bulkCategorizeTransactionsUseCase: com.finpulse.app.domain.usecase.review.BulkCategorizeTransactionsUseCase by lazy {
        com.finpulse.app.domain.usecase.review.BulkCategorizeTransactionsUseCase(transactionRepository, merchantSignalRepository)
    }

    override val updateTransactionDetailsUseCase: com.finpulse.app.domain.usecase.review.UpdateTransactionDetailsUseCase by lazy {
        com.finpulse.app.domain.usecase.review.UpdateTransactionDetailsUseCase(transactionRepository, merchantSignalRepository)
    }

    override val calculateSafeToSpendUseCase: com.finpulse.app.domain.usecase.budget.CalculateSafeToSpendUseCase by lazy {
        com.finpulse.app.domain.usecase.budget.CalculateSafeToSpendUseCase()
    }

    override val getFinancialCalendarUseCase: com.finpulse.app.domain.usecase.calendar.GetFinancialCalendarUseCase by lazy {
        com.finpulse.app.domain.usecase.calendar.GetFinancialCalendarUseCase(
            accountRepository = accountRepository,
            recurringRepository = recurringRepository,
            debtRepository = debtRepository,
            goalRepository = goalRepository,
            categoryRepository = categoryRepository,
            transactionRepository = transactionRepository,
            userPreferencesDataStore = userPreferencesDataStore
        )
    }

    override val backupRepository: com.finpulse.app.domain.repository.BackupRepository by lazy {
        com.finpulse.app.data.repository.BackupRepositoryImpl(database)
    }

    override val createBackupUseCase: com.finpulse.app.domain.usecase.backup.CreateBackupUseCase by lazy {
        com.finpulse.app.domain.usecase.backup.CreateBackupUseCase(backupRepository, userPreferencesDataStore)
    }

    override val validateBackupUseCase: com.finpulse.app.domain.usecase.backup.ValidateBackupUseCase by lazy {
        com.finpulse.app.domain.usecase.backup.ValidateBackupUseCase()
    }

    override val restoreBackupUseCase: com.finpulse.app.domain.usecase.backup.RestoreBackupUseCase by lazy {
        com.finpulse.app.domain.usecase.backup.RestoreBackupUseCase(backupRepository)
    }

    override val exportTransactionsUseCase: com.finpulse.app.domain.usecase.backup.ExportTransactionsUseCase by lazy {
        com.finpulse.app.domain.usecase.backup.ExportTransactionsUseCase(backupRepository, accountRepository, categoryRepository)
    }

    override val googleAuthManager: com.finpulse.app.core.auth.GoogleAuthManager by lazy {
        com.finpulse.app.core.auth.GoogleAuthManager(context)
    }

    override val authRepository: com.finpulse.app.domain.repository.AuthRepository by lazy {
        com.finpulse.app.data.repository.FirebaseAuthRepositoryImpl(userPreferencesDataStore)
    }

    override val cloudStorageDataSource: com.finpulse.app.data.cloud.CloudStorageDataSource by lazy {
        com.finpulse.app.data.cloud.FirestoreCloudStorageDataSource()
    }

    override val localSettingsDataSource: com.finpulse.app.data.cloud.LocalSettingsDataSource by lazy {
        com.finpulse.app.data.cloud.DataStoreLocalSettingsDataSource(userPreferencesDataStore)
    }

    override val cloudSettingsDataSource: com.finpulse.app.data.cloud.CloudSettingsDataSource by lazy {
        com.finpulse.app.data.cloud.FirestoreCloudSettingsDataSource(cloudStorageDataSource)
    }

    override val settingsRepository: com.finpulse.app.domain.repository.SettingsRepository by lazy {
        com.finpulse.app.data.repository.SettingsRepositoryImpl(
            localDataSource = localSettingsDataSource,
            cloudDataSource = cloudSettingsDataSource
        )
    }

    override val cloudSyncRepository: com.finpulse.app.domain.repository.CloudSyncRepository by lazy {
        com.finpulse.app.data.sync.CloudSyncEngine(
            database = database,
            cloudStorage = cloudStorageDataSource,
            userPreferencesDataStore = userPreferencesDataStore,
            authRepository = authRepository,
            settingsRepository = settingsRepository
        )
    }

    override val getFinancialDigestUseCase: com.finpulse.app.domain.usecase.digest.GetFinancialDigestUseCase by lazy {
        com.finpulse.app.domain.usecase.digest.GetFinancialDigestUseCase(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            budgetRepository = budgetRepository,
            recurringRepository = recurringRepository,
            goalRepository = goalRepository,
            userPreferencesDataStore = userPreferencesDataStore,
            getReviewInboxUseCase = getReviewInboxUseCase
        )
    }
}


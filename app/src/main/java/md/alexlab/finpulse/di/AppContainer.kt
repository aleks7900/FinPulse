package md.alexlab.finpulse.di

import android.content.Context
import md.alexlab.finpulse.core.database.FinPulseDatabase
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.data.repository.AccountRepositoryImpl
import md.alexlab.finpulse.data.repository.BudgetRepositoryImpl
import md.alexlab.finpulse.data.repository.CategoryRepositoryImpl
import md.alexlab.finpulse.data.repository.DebtRepositoryImpl
import md.alexlab.finpulse.data.repository.GoalRepositoryImpl
import md.alexlab.finpulse.data.repository.InvestmentRepositoryImpl
import md.alexlab.finpulse.data.repository.RecurringRepositoryImpl
import md.alexlab.finpulse.data.repository.TransactionRepositoryImpl
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.BudgetRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import md.alexlab.finpulse.domain.repository.DebtRepository
import md.alexlab.finpulse.domain.repository.GoalRepository
import md.alexlab.finpulse.domain.repository.InvestmentRepository
import md.alexlab.finpulse.domain.repository.RecurringRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository
import md.alexlab.finpulse.domain.usecase.transaction.CreateTransactionUseCase
import md.alexlab.finpulse.domain.usecase.transaction.GetQuickAddSuggestionsUseCase

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
    val getUpcomingOccurrencesUseCase: md.alexlab.finpulse.domain.usecase.recurring.GetUpcomingOccurrencesUseCase
    val markOccurrencePaidUseCase: md.alexlab.finpulse.domain.usecase.recurring.MarkOccurrencePaidUseCase
    val skipOccurrenceUseCase: md.alexlab.finpulse.domain.usecase.recurring.SkipOccurrenceUseCase
    val editOccurrenceUseCase: md.alexlab.finpulse.domain.usecase.recurring.EditOccurrenceUseCase
    val manageRecurringRuleUseCase: md.alexlab.finpulse.domain.usecase.recurring.ManageRecurringRuleUseCase
    val categorizationRuleRepository: md.alexlab.finpulse.domain.repository.CategorizationRuleRepository
    val merchantSignalRepository: md.alexlab.finpulse.domain.repository.MerchantSignalRepository
    val categorizeTransactionUseCase: md.alexlab.finpulse.domain.usecase.categorization.CategorizeTransactionUseCase
    val recordCategoryCorrectionUseCase: md.alexlab.finpulse.domain.usecase.categorization.RecordCategoryCorrectionUseCase
    val manageCategorizationRuleUseCase: md.alexlab.finpulse.domain.usecase.categorization.ManageCategorizationRuleUseCase
    val findMatchingTransactionsForRuleUseCase: md.alexlab.finpulse.domain.usecase.categorization.FindMatchingTransactionsForRuleUseCase
    val applyRuleToExistingTransactionsUseCase: md.alexlab.finpulse.domain.usecase.categorization.ApplyRuleToExistingTransactionsUseCase
    val getReviewQueueUseCase: md.alexlab.finpulse.domain.usecase.categorization.GetReviewQueueUseCase
    val importProfileRepository: md.alexlab.finpulse.domain.repository.ImportProfileRepository
    val autoDetectCsvConfigUseCase: md.alexlab.finpulse.domain.usecase.csv.AutoDetectCsvConfigUseCase
    val parseCsvStatementUseCase: md.alexlab.finpulse.domain.usecase.csv.ParseCsvStatementUseCase
    val executeCsvImportUseCase: md.alexlab.finpulse.domain.usecase.csv.ExecuteCsvImportUseCase
    val manageImportProfilesUseCase: md.alexlab.finpulse.domain.usecase.csv.ManageImportProfilesUseCase
    val getReviewInboxUseCase: md.alexlab.finpulse.domain.usecase.review.GetReviewInboxUseCase
    val resolveDuplicateTransactionUseCase: md.alexlab.finpulse.domain.usecase.review.ResolveDuplicateTransactionUseCase
    val bulkCategorizeTransactionsUseCase: md.alexlab.finpulse.domain.usecase.review.BulkCategorizeTransactionsUseCase
    val updateTransactionDetailsUseCase: md.alexlab.finpulse.domain.usecase.review.UpdateTransactionDetailsUseCase
    val calculateSafeToSpendUseCase: md.alexlab.finpulse.domain.usecase.budget.CalculateSafeToSpendUseCase
    val getFinancialCalendarUseCase: md.alexlab.finpulse.domain.usecase.calendar.GetFinancialCalendarUseCase
    val savedFilterRepository: md.alexlab.finpulse.domain.repository.SavedFilterRepository
    val backupRepository: md.alexlab.finpulse.domain.repository.BackupRepository
    val createBackupUseCase: md.alexlab.finpulse.domain.usecase.backup.CreateBackupUseCase
    val validateBackupUseCase: md.alexlab.finpulse.domain.usecase.backup.ValidateBackupUseCase
    val restoreBackupUseCase: md.alexlab.finpulse.domain.usecase.backup.RestoreBackupUseCase
    val exportTransactionsUseCase: md.alexlab.finpulse.domain.usecase.backup.ExportTransactionsUseCase
    val appLockManager: md.alexlab.finpulse.core.security.AppLockManager
    val googleAuthManager: md.alexlab.finpulse.core.auth.GoogleAuthManager
    val authRepository: md.alexlab.finpulse.domain.repository.AuthRepository
    val cloudStorageDataSource: md.alexlab.finpulse.data.cloud.CloudStorageDataSource
    val localSettingsDataSource: md.alexlab.finpulse.data.cloud.LocalSettingsDataSource
    val cloudSettingsDataSource: md.alexlab.finpulse.data.cloud.CloudSettingsDataSource
    val settingsRepository: md.alexlab.finpulse.domain.repository.SettingsRepository
    val cloudSyncRepository: md.alexlab.finpulse.domain.repository.CloudSyncRepository
    val getFinancialDigestUseCase: md.alexlab.finpulse.domain.usecase.digest.GetFinancialDigestUseCase
    val exchangeRateProvider: md.alexlab.finpulse.domain.repository.ExchangeRateProvider
    val currencyConverter: md.alexlab.finpulse.domain.engine.CurrencyConverter
    val getDashboardSummaryUseCase: md.alexlab.finpulse.domain.usecase.GetDashboardSummaryUseCase
    val errorReporter: md.alexlab.finpulse.core.reporting.ErrorReporter
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val errorReporter: md.alexlab.finpulse.core.reporting.ErrorReporter by lazy {
        md.alexlab.finpulse.core.reporting.FirebaseCrashlyticsErrorReporter()
    }

    override val database: FinPulseDatabase by lazy {
        FinPulseDatabase.getInstance(context)
    }

    override val appLockManager: md.alexlab.finpulse.core.security.AppLockManager by lazy {
        md.alexlab.finpulse.core.security.AppLockManager()
    }

    override val savedFilterRepository: md.alexlab.finpulse.domain.repository.SavedFilterRepository by lazy {
        md.alexlab.finpulse.data.repository.SavedFilterRepositoryImpl(database)
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

    override val categorizationRuleRepository: md.alexlab.finpulse.domain.repository.CategorizationRuleRepository by lazy {
        md.alexlab.finpulse.data.repository.CategorizationRuleRepositoryImpl(database)
    }

    override val merchantSignalRepository: md.alexlab.finpulse.domain.repository.MerchantSignalRepository by lazy {
        md.alexlab.finpulse.data.repository.MerchantSignalRepositoryImpl(database)
    }

    val onlineExchangeRateClient: md.alexlab.finpulse.data.remote.currency.OnlineExchangeRateClient by lazy {
        md.alexlab.finpulse.data.remote.currency.GlobalOnlineExchangeRateClient()
    }

    override val exchangeRateProvider: md.alexlab.finpulse.domain.repository.ExchangeRateProvider by lazy {
        md.alexlab.finpulse.data.repository.ExchangeRateProviderImpl(
            dao = database.exchangeRateDao(),
            onlineClient = onlineExchangeRateClient
        )
    }

    override val currencyConverter: md.alexlab.finpulse.domain.engine.CurrencyConverter by lazy {
        md.alexlab.finpulse.domain.engine.CurrencyConverter(exchangeRateProvider)
    }

    override val getDashboardSummaryUseCase: md.alexlab.finpulse.domain.usecase.GetDashboardSummaryUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.GetDashboardSummaryUseCase(currencyConverter)
    }

    override val createTransactionUseCase: CreateTransactionUseCase by lazy {
        CreateTransactionUseCase(transactionRepository, accountRepository, exchangeRateProvider)
    }

    override val getQuickAddSuggestionsUseCase: GetQuickAddSuggestionsUseCase by lazy {
        GetQuickAddSuggestionsUseCase(transactionRepository, categoryRepository, accountRepository, userPreferencesDataStore)
    }

    override val getUpcomingOccurrencesUseCase: md.alexlab.finpulse.domain.usecase.recurring.GetUpcomingOccurrencesUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.recurring.GetUpcomingOccurrencesUseCase(recurringRepository)
    }

    override val markOccurrencePaidUseCase: md.alexlab.finpulse.domain.usecase.recurring.MarkOccurrencePaidUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.recurring.MarkOccurrencePaidUseCase(recurringRepository, transactionRepository)
    }

    override val skipOccurrenceUseCase: md.alexlab.finpulse.domain.usecase.recurring.SkipOccurrenceUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.recurring.SkipOccurrenceUseCase(recurringRepository)
    }

    override val editOccurrenceUseCase: md.alexlab.finpulse.domain.usecase.recurring.EditOccurrenceUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.recurring.EditOccurrenceUseCase(recurringRepository)
    }

    override val manageRecurringRuleUseCase: md.alexlab.finpulse.domain.usecase.recurring.ManageRecurringRuleUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.recurring.ManageRecurringRuleUseCase(recurringRepository, accountRepository)
    }

    override val categorizeTransactionUseCase: md.alexlab.finpulse.domain.usecase.categorization.CategorizeTransactionUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.categorization.CategorizeTransactionUseCase(categorizationRuleRepository, merchantSignalRepository, categoryRepository)
    }

    override val recordCategoryCorrectionUseCase: md.alexlab.finpulse.domain.usecase.categorization.RecordCategoryCorrectionUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.categorization.RecordCategoryCorrectionUseCase(transactionRepository, merchantSignalRepository)
    }

    override val manageCategorizationRuleUseCase: md.alexlab.finpulse.domain.usecase.categorization.ManageCategorizationRuleUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.categorization.ManageCategorizationRuleUseCase(categorizationRuleRepository)
    }

    override val findMatchingTransactionsForRuleUseCase: md.alexlab.finpulse.domain.usecase.categorization.FindMatchingTransactionsForRuleUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.categorization.FindMatchingTransactionsForRuleUseCase(categorizationRuleRepository, transactionRepository)
    }

    override val applyRuleToExistingTransactionsUseCase: md.alexlab.finpulse.domain.usecase.categorization.ApplyRuleToExistingTransactionsUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.categorization.ApplyRuleToExistingTransactionsUseCase(categorizationRuleRepository, transactionRepository, findMatchingTransactionsForRuleUseCase)
    }

    override val getReviewQueueUseCase: md.alexlab.finpulse.domain.usecase.categorization.GetReviewQueueUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.categorization.GetReviewQueueUseCase(transactionRepository, categorizationRuleRepository, merchantSignalRepository)
    }

    override val importProfileRepository: md.alexlab.finpulse.domain.repository.ImportProfileRepository by lazy {
        md.alexlab.finpulse.data.repository.ImportProfileRepositoryImpl(database)
    }

    override val autoDetectCsvConfigUseCase: md.alexlab.finpulse.domain.usecase.csv.AutoDetectCsvConfigUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.csv.AutoDetectCsvConfigUseCase()
    }

    override val parseCsvStatementUseCase: md.alexlab.finpulse.domain.usecase.csv.ParseCsvStatementUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.csv.ParseCsvStatementUseCase(transactionRepository, categorizeTransactionUseCase)
    }

    override val executeCsvImportUseCase: md.alexlab.finpulse.domain.usecase.csv.ExecuteCsvImportUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.csv.ExecuteCsvImportUseCase(transactionRepository, accountRepository, merchantSignalRepository, categoryRepository)
    }

    override val manageImportProfilesUseCase: md.alexlab.finpulse.domain.usecase.csv.ManageImportProfilesUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.csv.ManageImportProfilesUseCase(importProfileRepository)
    }

    override val getReviewInboxUseCase: md.alexlab.finpulse.domain.usecase.review.GetReviewInboxUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.review.GetReviewInboxUseCase(
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

    override val resolveDuplicateTransactionUseCase: md.alexlab.finpulse.domain.usecase.review.ResolveDuplicateTransactionUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.review.ResolveDuplicateTransactionUseCase(transactionRepository, userPreferencesDataStore)
    }

    override val bulkCategorizeTransactionsUseCase: md.alexlab.finpulse.domain.usecase.review.BulkCategorizeTransactionsUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.review.BulkCategorizeTransactionsUseCase(transactionRepository, merchantSignalRepository)
    }

    override val updateTransactionDetailsUseCase: md.alexlab.finpulse.domain.usecase.review.UpdateTransactionDetailsUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.review.UpdateTransactionDetailsUseCase(transactionRepository, merchantSignalRepository)
    }

    override val calculateSafeToSpendUseCase: md.alexlab.finpulse.domain.usecase.budget.CalculateSafeToSpendUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.budget.CalculateSafeToSpendUseCase()
    }

    override val getFinancialCalendarUseCase: md.alexlab.finpulse.domain.usecase.calendar.GetFinancialCalendarUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.calendar.GetFinancialCalendarUseCase(
            accountRepository = accountRepository,
            recurringRepository = recurringRepository,
            debtRepository = debtRepository,
            goalRepository = goalRepository,
            categoryRepository = categoryRepository,
            transactionRepository = transactionRepository,
            userPreferencesDataStore = userPreferencesDataStore
        )
    }

    override val backupRepository: md.alexlab.finpulse.domain.repository.BackupRepository by lazy {
        md.alexlab.finpulse.data.repository.BackupRepositoryImpl(database)
    }

    override val createBackupUseCase: md.alexlab.finpulse.domain.usecase.backup.CreateBackupUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.backup.CreateBackupUseCase(backupRepository, userPreferencesDataStore)
    }

    override val validateBackupUseCase: md.alexlab.finpulse.domain.usecase.backup.ValidateBackupUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.backup.ValidateBackupUseCase()
    }

    override val restoreBackupUseCase: md.alexlab.finpulse.domain.usecase.backup.RestoreBackupUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.backup.RestoreBackupUseCase(backupRepository)
    }

    override val exportTransactionsUseCase: md.alexlab.finpulse.domain.usecase.backup.ExportTransactionsUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.backup.ExportTransactionsUseCase(backupRepository, accountRepository, categoryRepository)
    }

    override val googleAuthManager: md.alexlab.finpulse.core.auth.GoogleAuthManager by lazy {
        md.alexlab.finpulse.core.auth.GoogleAuthManager(context)
    }

    override val authRepository: md.alexlab.finpulse.domain.repository.AuthRepository by lazy {
        md.alexlab.finpulse.data.repository.FirebaseAuthRepositoryImpl(
            userPreferencesDataStore = userPreferencesDataStore,
            errorReporter = errorReporter
        )
    }

    override val cloudStorageDataSource: md.alexlab.finpulse.data.cloud.CloudStorageDataSource by lazy {
        md.alexlab.finpulse.data.cloud.FirestoreCloudStorageDataSource(errorReporter = errorReporter)
    }

    override val localSettingsDataSource: md.alexlab.finpulse.data.cloud.LocalSettingsDataSource by lazy {
        md.alexlab.finpulse.data.cloud.DataStoreLocalSettingsDataSource(userPreferencesDataStore)
    }

    override val cloudSettingsDataSource: md.alexlab.finpulse.data.cloud.CloudSettingsDataSource by lazy {
        md.alexlab.finpulse.data.cloud.FirestoreCloudSettingsDataSource(cloudStorageDataSource)
    }

    override val settingsRepository: md.alexlab.finpulse.domain.repository.SettingsRepository by lazy {
        md.alexlab.finpulse.data.repository.SettingsRepositoryImpl(
            localDataSource = localSettingsDataSource,
            cloudDataSource = cloudSettingsDataSource
        )
    }

    override val cloudSyncRepository: md.alexlab.finpulse.domain.repository.CloudSyncRepository by lazy {
        md.alexlab.finpulse.data.sync.CloudSyncEngine(
            database = database,
            cloudStorage = cloudStorageDataSource,
            userPreferencesDataStore = userPreferencesDataStore,
            authRepository = authRepository,
            settingsRepository = settingsRepository,
            errorReporter = errorReporter
        )
    }

    override val getFinancialDigestUseCase: md.alexlab.finpulse.domain.usecase.digest.GetFinancialDigestUseCase by lazy {
        md.alexlab.finpulse.domain.usecase.digest.GetFinancialDigestUseCase(
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


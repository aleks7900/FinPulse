package com.finpulse.app.core.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.finpulse.app.core.database.dao.AccountDao;
import com.finpulse.app.core.database.dao.AccountDao_Impl;
import com.finpulse.app.core.database.dao.AssetDao;
import com.finpulse.app.core.database.dao.AssetDao_Impl;
import com.finpulse.app.core.database.dao.BudgetDao;
import com.finpulse.app.core.database.dao.BudgetDao_Impl;
import com.finpulse.app.core.database.dao.CategoryDao;
import com.finpulse.app.core.database.dao.CategoryDao_Impl;
import com.finpulse.app.core.database.dao.DebtDao;
import com.finpulse.app.core.database.dao.DebtDao_Impl;
import com.finpulse.app.core.database.dao.FinancialGoalDao;
import com.finpulse.app.core.database.dao.FinancialGoalDao_Impl;
import com.finpulse.app.core.database.dao.RecurringTransactionDao;
import com.finpulse.app.core.database.dao.RecurringTransactionDao_Impl;
import com.finpulse.app.core.database.dao.TransactionDao;
import com.finpulse.app.core.database.dao.TransactionDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class FinPulseDatabase_Impl extends FinPulseDatabase {
  private volatile AccountDao _accountDao;

  private volatile TransactionDao _transactionDao;

  private volatile CategoryDao _categoryDao;

  private volatile BudgetDao _budgetDao;

  private volatile RecurringTransactionDao _recurringTransactionDao;

  private volatile FinancialGoalDao _financialGoalDao;

  private volatile AssetDao _assetDao;

  private volatile DebtDao _debtDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `balanceMinor` INTEGER NOT NULL, `availableBalanceMinor` INTEGER NOT NULL, `creditLimitMinor` INTEGER, `currencyCode` TEXT NOT NULL, `institution` TEXT, `icon` TEXT NOT NULL, `colorHex` INTEGER NOT NULL, `notes` TEXT, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL, `type` TEXT NOT NULL, `sourceAccountId` TEXT NOT NULL, `destinationAccountId` TEXT, `categoryId` TEXT NOT NULL, `merchant` TEXT, `timestamp` INTEGER NOT NULL, `description` TEXT NOT NULL, `tags` TEXT NOT NULL, `notes` TEXT, `recurringRuleId` TEXT, `isExcludedFromBudget` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_sourceAccountId` ON `transactions` (`sourceAccountId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_destinationAccountId` ON `transactions` (`destinationAccountId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `parentCategoryId` TEXT, `icon` TEXT NOT NULL, `colorHex` INTEGER NOT NULL, `isDefault` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (`id` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `name` TEXT NOT NULL, `limitAmountMinor` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL, `periodType` TEXT NOT NULL, `startDate` INTEGER NOT NULL, `endDate` INTEGER NOT NULL, `notifyAt70` INTEGER NOT NULL, `notifyAt90` INTEGER NOT NULL, `notifyAt100` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `recurring_transactions` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL, `accountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `frequency` TEXT NOT NULL, `nextDueDate` INTEGER NOT NULL, `lastProcessedDate` INTEGER, `isActive` INTEGER NOT NULL, `isSubscription` INTEGER NOT NULL, `notes` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_accountId` ON `recurring_transactions` (`accountId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_categoryId` ON `recurring_transactions` (`categoryId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_nextDueDate` ON `recurring_transactions` (`nextDueDate`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `financial_goals` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `targetAmountMinor` INTEGER NOT NULL, `currentAmountMinor` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL, `targetDate` INTEGER NOT NULL, `linkedAccountId` TEXT, `icon` TEXT NOT NULL, `colorHex` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_goals_linkedAccountId` ON `financial_goals` (`linkedAccountId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `assets` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `symbol` TEXT NOT NULL, `assetClass` TEXT NOT NULL, `quantity` REAL NOT NULL, `purchasePriceMinor` INTEGER NOT NULL, `currentPriceMinor` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL, `lastUpdated` INTEGER NOT NULL, `notes` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `debts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `totalPrincipalMinor` INTEGER NOT NULL, `remainingBalanceMinor` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL, `interestRatePercent` REAL NOT NULL, `minimumPaymentMinor` INTEGER NOT NULL, `nextPaymentDate` INTEGER NOT NULL, `linkedAccountId` TEXT, `notes` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_linkedAccountId` ON `debts` (`linkedAccountId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f7b58aadbed176c1ae41c66f49fbd257')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `accounts`");
        db.execSQL("DROP TABLE IF EXISTS `transactions`");
        db.execSQL("DROP TABLE IF EXISTS `categories`");
        db.execSQL("DROP TABLE IF EXISTS `budgets`");
        db.execSQL("DROP TABLE IF EXISTS `recurring_transactions`");
        db.execSQL("DROP TABLE IF EXISTS `financial_goals`");
        db.execSQL("DROP TABLE IF EXISTS `assets`");
        db.execSQL("DROP TABLE IF EXISTS `debts`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsAccounts = new HashMap<String, TableInfo.Column>(14);
        _columnsAccounts.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("balanceMinor", new TableInfo.Column("balanceMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("availableBalanceMinor", new TableInfo.Column("availableBalanceMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("creditLimitMinor", new TableInfo.Column("creditLimitMinor", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("currencyCode", new TableInfo.Column("currencyCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("institution", new TableInfo.Column("institution", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("icon", new TableInfo.Column("icon", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("colorHex", new TableInfo.Column("colorHex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("isArchived", new TableInfo.Column("isArchived", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAccounts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAccounts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAccounts = new TableInfo("accounts", _columnsAccounts, _foreignKeysAccounts, _indicesAccounts);
        final TableInfo _existingAccounts = TableInfo.read(db, "accounts");
        if (!_infoAccounts.equals(_existingAccounts)) {
          return new RoomOpenHelper.ValidationResult(false, "accounts(com.finpulse.app.core.database.entity.AccountEntity).\n"
                  + " Expected:\n" + _infoAccounts + "\n"
                  + " Found:\n" + _existingAccounts);
        }
        final HashMap<String, TableInfo.Column> _columnsTransactions = new HashMap<String, TableInfo.Column>(15);
        _columnsTransactions.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("amountMinor", new TableInfo.Column("amountMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("currencyCode", new TableInfo.Column("currencyCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("sourceAccountId", new TableInfo.Column("sourceAccountId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("destinationAccountId", new TableInfo.Column("destinationAccountId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("categoryId", new TableInfo.Column("categoryId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("merchant", new TableInfo.Column("merchant", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("tags", new TableInfo.Column("tags", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("recurringRuleId", new TableInfo.Column("recurringRuleId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("isExcludedFromBudget", new TableInfo.Column("isExcludedFromBudget", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTransactions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTransactions = new HashSet<TableInfo.Index>(5);
        _indicesTransactions.add(new TableInfo.Index("index_transactions_sourceAccountId", false, Arrays.asList("sourceAccountId"), Arrays.asList("ASC")));
        _indicesTransactions.add(new TableInfo.Index("index_transactions_destinationAccountId", false, Arrays.asList("destinationAccountId"), Arrays.asList("ASC")));
        _indicesTransactions.add(new TableInfo.Index("index_transactions_categoryId", false, Arrays.asList("categoryId"), Arrays.asList("ASC")));
        _indicesTransactions.add(new TableInfo.Index("index_transactions_timestamp", false, Arrays.asList("timestamp"), Arrays.asList("ASC")));
        _indicesTransactions.add(new TableInfo.Index("index_transactions_type", false, Arrays.asList("type"), Arrays.asList("ASC")));
        final TableInfo _infoTransactions = new TableInfo("transactions", _columnsTransactions, _foreignKeysTransactions, _indicesTransactions);
        final TableInfo _existingTransactions = TableInfo.read(db, "transactions");
        if (!_infoTransactions.equals(_existingTransactions)) {
          return new RoomOpenHelper.ValidationResult(false, "transactions(com.finpulse.app.core.database.entity.TransactionEntity).\n"
                  + " Expected:\n" + _infoTransactions + "\n"
                  + " Found:\n" + _existingTransactions);
        }
        final HashMap<String, TableInfo.Column> _columnsCategories = new HashMap<String, TableInfo.Column>(8);
        _columnsCategories.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("parentCategoryId", new TableInfo.Column("parentCategoryId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("icon", new TableInfo.Column("icon", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("colorHex", new TableInfo.Column("colorHex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("isDefault", new TableInfo.Column("isDefault", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCategories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCategories = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCategories = new TableInfo("categories", _columnsCategories, _foreignKeysCategories, _indicesCategories);
        final TableInfo _existingCategories = TableInfo.read(db, "categories");
        if (!_infoCategories.equals(_existingCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "categories(com.finpulse.app.core.database.entity.CategoryEntity).\n"
                  + " Expected:\n" + _infoCategories + "\n"
                  + " Found:\n" + _existingCategories);
        }
        final HashMap<String, TableInfo.Column> _columnsBudgets = new HashMap<String, TableInfo.Column>(12);
        _columnsBudgets.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBudgets.put("categoryId", new TableInfo.Column("categoryId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBudgets.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBudgets.put("limitAmountMinor", new TableInfo.Column("limitAmountMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBudgets.put("currencyCode", new TableInfo.Column("currencyCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBudgets.put("periodType", new TableInfo.Column("periodType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBudgets.put("startDate", new TableInfo.Column("startDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBudgets.put("endDate", new TableInfo.Column("endDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBudgets.put("notifyAt70", new TableInfo.Column("notifyAt70", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBudgets.put("notifyAt90", new TableInfo.Column("notifyAt90", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBudgets.put("notifyAt100", new TableInfo.Column("notifyAt100", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBudgets.put("isArchived", new TableInfo.Column("isArchived", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBudgets = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBudgets = new HashSet<TableInfo.Index>(1);
        _indicesBudgets.add(new TableInfo.Index("index_budgets_categoryId", false, Arrays.asList("categoryId"), Arrays.asList("ASC")));
        final TableInfo _infoBudgets = new TableInfo("budgets", _columnsBudgets, _foreignKeysBudgets, _indicesBudgets);
        final TableInfo _existingBudgets = TableInfo.read(db, "budgets");
        if (!_infoBudgets.equals(_existingBudgets)) {
          return new RoomOpenHelper.ValidationResult(false, "budgets(com.finpulse.app.core.database.entity.BudgetEntity).\n"
                  + " Expected:\n" + _infoBudgets + "\n"
                  + " Found:\n" + _existingBudgets);
        }
        final HashMap<String, TableInfo.Column> _columnsRecurringTransactions = new HashMap<String, TableInfo.Column>(12);
        _columnsRecurringTransactions.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecurringTransactions.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecurringTransactions.put("amountMinor", new TableInfo.Column("amountMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecurringTransactions.put("currencyCode", new TableInfo.Column("currencyCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecurringTransactions.put("accountId", new TableInfo.Column("accountId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecurringTransactions.put("categoryId", new TableInfo.Column("categoryId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecurringTransactions.put("frequency", new TableInfo.Column("frequency", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecurringTransactions.put("nextDueDate", new TableInfo.Column("nextDueDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecurringTransactions.put("lastProcessedDate", new TableInfo.Column("lastProcessedDate", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecurringTransactions.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecurringTransactions.put("isSubscription", new TableInfo.Column("isSubscription", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecurringTransactions.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRecurringTransactions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRecurringTransactions = new HashSet<TableInfo.Index>(3);
        _indicesRecurringTransactions.add(new TableInfo.Index("index_recurring_transactions_accountId", false, Arrays.asList("accountId"), Arrays.asList("ASC")));
        _indicesRecurringTransactions.add(new TableInfo.Index("index_recurring_transactions_categoryId", false, Arrays.asList("categoryId"), Arrays.asList("ASC")));
        _indicesRecurringTransactions.add(new TableInfo.Index("index_recurring_transactions_nextDueDate", false, Arrays.asList("nextDueDate"), Arrays.asList("ASC")));
        final TableInfo _infoRecurringTransactions = new TableInfo("recurring_transactions", _columnsRecurringTransactions, _foreignKeysRecurringTransactions, _indicesRecurringTransactions);
        final TableInfo _existingRecurringTransactions = TableInfo.read(db, "recurring_transactions");
        if (!_infoRecurringTransactions.equals(_existingRecurringTransactions)) {
          return new RoomOpenHelper.ValidationResult(false, "recurring_transactions(com.finpulse.app.core.database.entity.RecurringTransactionEntity).\n"
                  + " Expected:\n" + _infoRecurringTransactions + "\n"
                  + " Found:\n" + _existingRecurringTransactions);
        }
        final HashMap<String, TableInfo.Column> _columnsFinancialGoals = new HashMap<String, TableInfo.Column>(11);
        _columnsFinancialGoals.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFinancialGoals.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFinancialGoals.put("targetAmountMinor", new TableInfo.Column("targetAmountMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFinancialGoals.put("currentAmountMinor", new TableInfo.Column("currentAmountMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFinancialGoals.put("currencyCode", new TableInfo.Column("currencyCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFinancialGoals.put("targetDate", new TableInfo.Column("targetDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFinancialGoals.put("linkedAccountId", new TableInfo.Column("linkedAccountId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFinancialGoals.put("icon", new TableInfo.Column("icon", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFinancialGoals.put("colorHex", new TableInfo.Column("colorHex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFinancialGoals.put("isCompleted", new TableInfo.Column("isCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFinancialGoals.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFinancialGoals = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFinancialGoals = new HashSet<TableInfo.Index>(1);
        _indicesFinancialGoals.add(new TableInfo.Index("index_financial_goals_linkedAccountId", false, Arrays.asList("linkedAccountId"), Arrays.asList("ASC")));
        final TableInfo _infoFinancialGoals = new TableInfo("financial_goals", _columnsFinancialGoals, _foreignKeysFinancialGoals, _indicesFinancialGoals);
        final TableInfo _existingFinancialGoals = TableInfo.read(db, "financial_goals");
        if (!_infoFinancialGoals.equals(_existingFinancialGoals)) {
          return new RoomOpenHelper.ValidationResult(false, "financial_goals(com.finpulse.app.core.database.entity.FinancialGoalEntity).\n"
                  + " Expected:\n" + _infoFinancialGoals + "\n"
                  + " Found:\n" + _existingFinancialGoals);
        }
        final HashMap<String, TableInfo.Column> _columnsAssets = new HashMap<String, TableInfo.Column>(10);
        _columnsAssets.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssets.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssets.put("symbol", new TableInfo.Column("symbol", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssets.put("assetClass", new TableInfo.Column("assetClass", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssets.put("quantity", new TableInfo.Column("quantity", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssets.put("purchasePriceMinor", new TableInfo.Column("purchasePriceMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssets.put("currentPriceMinor", new TableInfo.Column("currentPriceMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssets.put("currencyCode", new TableInfo.Column("currencyCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssets.put("lastUpdated", new TableInfo.Column("lastUpdated", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssets.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAssets = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAssets = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAssets = new TableInfo("assets", _columnsAssets, _foreignKeysAssets, _indicesAssets);
        final TableInfo _existingAssets = TableInfo.read(db, "assets");
        if (!_infoAssets.equals(_existingAssets)) {
          return new RoomOpenHelper.ValidationResult(false, "assets(com.finpulse.app.core.database.entity.AssetEntity).\n"
                  + " Expected:\n" + _infoAssets + "\n"
                  + " Found:\n" + _existingAssets);
        }
        final HashMap<String, TableInfo.Column> _columnsDebts = new HashMap<String, TableInfo.Column>(11);
        _columnsDebts.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDebts.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDebts.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDebts.put("totalPrincipalMinor", new TableInfo.Column("totalPrincipalMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDebts.put("remainingBalanceMinor", new TableInfo.Column("remainingBalanceMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDebts.put("currencyCode", new TableInfo.Column("currencyCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDebts.put("interestRatePercent", new TableInfo.Column("interestRatePercent", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDebts.put("minimumPaymentMinor", new TableInfo.Column("minimumPaymentMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDebts.put("nextPaymentDate", new TableInfo.Column("nextPaymentDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDebts.put("linkedAccountId", new TableInfo.Column("linkedAccountId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDebts.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDebts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDebts = new HashSet<TableInfo.Index>(1);
        _indicesDebts.add(new TableInfo.Index("index_debts_linkedAccountId", false, Arrays.asList("linkedAccountId"), Arrays.asList("ASC")));
        final TableInfo _infoDebts = new TableInfo("debts", _columnsDebts, _foreignKeysDebts, _indicesDebts);
        final TableInfo _existingDebts = TableInfo.read(db, "debts");
        if (!_infoDebts.equals(_existingDebts)) {
          return new RoomOpenHelper.ValidationResult(false, "debts(com.finpulse.app.core.database.entity.DebtEntity).\n"
                  + " Expected:\n" + _infoDebts + "\n"
                  + " Found:\n" + _existingDebts);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "f7b58aadbed176c1ae41c66f49fbd257", "fd769c7df001f7e511d5be376bb9fdbf");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "accounts","transactions","categories","budgets","recurring_transactions","financial_goals","assets","debts");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `accounts`");
      _db.execSQL("DELETE FROM `transactions`");
      _db.execSQL("DELETE FROM `categories`");
      _db.execSQL("DELETE FROM `budgets`");
      _db.execSQL("DELETE FROM `recurring_transactions`");
      _db.execSQL("DELETE FROM `financial_goals`");
      _db.execSQL("DELETE FROM `assets`");
      _db.execSQL("DELETE FROM `debts`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(AccountDao.class, AccountDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TransactionDao.class, TransactionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CategoryDao.class, CategoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BudgetDao.class, BudgetDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RecurringTransactionDao.class, RecurringTransactionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FinancialGoalDao.class, FinancialGoalDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AssetDao.class, AssetDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DebtDao.class, DebtDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public AccountDao accountDao() {
    if (_accountDao != null) {
      return _accountDao;
    } else {
      synchronized(this) {
        if(_accountDao == null) {
          _accountDao = new AccountDao_Impl(this);
        }
        return _accountDao;
      }
    }
  }

  @Override
  public TransactionDao transactionDao() {
    if (_transactionDao != null) {
      return _transactionDao;
    } else {
      synchronized(this) {
        if(_transactionDao == null) {
          _transactionDao = new TransactionDao_Impl(this);
        }
        return _transactionDao;
      }
    }
  }

  @Override
  public CategoryDao categoryDao() {
    if (_categoryDao != null) {
      return _categoryDao;
    } else {
      synchronized(this) {
        if(_categoryDao == null) {
          _categoryDao = new CategoryDao_Impl(this);
        }
        return _categoryDao;
      }
    }
  }

  @Override
  public BudgetDao budgetDao() {
    if (_budgetDao != null) {
      return _budgetDao;
    } else {
      synchronized(this) {
        if(_budgetDao == null) {
          _budgetDao = new BudgetDao_Impl(this);
        }
        return _budgetDao;
      }
    }
  }

  @Override
  public RecurringTransactionDao recurringTransactionDao() {
    if (_recurringTransactionDao != null) {
      return _recurringTransactionDao;
    } else {
      synchronized(this) {
        if(_recurringTransactionDao == null) {
          _recurringTransactionDao = new RecurringTransactionDao_Impl(this);
        }
        return _recurringTransactionDao;
      }
    }
  }

  @Override
  public FinancialGoalDao financialGoalDao() {
    if (_financialGoalDao != null) {
      return _financialGoalDao;
    } else {
      synchronized(this) {
        if(_financialGoalDao == null) {
          _financialGoalDao = new FinancialGoalDao_Impl(this);
        }
        return _financialGoalDao;
      }
    }
  }

  @Override
  public AssetDao assetDao() {
    if (_assetDao != null) {
      return _assetDao;
    } else {
      synchronized(this) {
        if(_assetDao == null) {
          _assetDao = new AssetDao_Impl(this);
        }
        return _assetDao;
      }
    }
  }

  @Override
  public DebtDao debtDao() {
    if (_debtDao != null) {
      return _debtDao;
    } else {
      synchronized(this) {
        if(_debtDao == null) {
          _debtDao = new DebtDao_Impl(this);
        }
        return _debtDao;
      }
    }
  }
}

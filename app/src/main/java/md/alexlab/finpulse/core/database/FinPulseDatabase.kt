package md.alexlab.finpulse.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import md.alexlab.finpulse.core.database.dao.AccountDao
import md.alexlab.finpulse.core.database.dao.AssetDao
import md.alexlab.finpulse.core.database.dao.BudgetDao
import md.alexlab.finpulse.core.database.dao.CategoryDao
import md.alexlab.finpulse.core.database.dao.CategorizationRuleDao
import md.alexlab.finpulse.core.database.dao.DebtDao
import md.alexlab.finpulse.core.database.dao.FinancialGoalDao
import md.alexlab.finpulse.core.database.dao.ImportProfileDao
import md.alexlab.finpulse.core.database.dao.MerchantSignalDao
import md.alexlab.finpulse.core.database.dao.RecurringTransactionDao
import md.alexlab.finpulse.core.database.dao.TransactionDao
import md.alexlab.finpulse.core.database.entity.AccountEntity
import md.alexlab.finpulse.core.database.entity.AssetEntity
import md.alexlab.finpulse.core.database.entity.BudgetEntity
import md.alexlab.finpulse.core.database.entity.CategorizationRuleEntity
import md.alexlab.finpulse.core.database.entity.CategoryEntity
import md.alexlab.finpulse.core.database.entity.DebtEntity
import md.alexlab.finpulse.core.database.entity.FinancialGoalEntity
import md.alexlab.finpulse.core.database.entity.ImportProfileEntity
import md.alexlab.finpulse.core.database.entity.MerchantSignalEntity
import md.alexlab.finpulse.core.database.entity.RecurringOccurrenceEntity
import md.alexlab.finpulse.core.database.entity.RecurringTransactionEntity
import md.alexlab.finpulse.core.database.entity.TransactionEntity

import md.alexlab.finpulse.core.database.dao.SavedFilterDao
import md.alexlab.finpulse.core.database.dao.SyncRecordDao
import md.alexlab.finpulse.core.database.dao.ExchangeRateDao
import md.alexlab.finpulse.core.database.entity.ExchangeRateEntity
import md.alexlab.finpulse.core.database.entity.SavedFilterEntity
import md.alexlab.finpulse.core.database.entity.SyncRecordEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        RecurringOccurrenceEntity::class,
        FinancialGoalEntity::class,
        AssetEntity::class,
        DebtEntity::class,
        CategorizationRuleEntity::class,
        MerchantSignalEntity::class,
        ImportProfileEntity::class,
        SavedFilterEntity::class,
        SyncRecordEntity::class,
        ExchangeRateEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class FinPulseDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun financialGoalDao(): FinancialGoalDao
    abstract fun assetDao(): AssetDao
    abstract fun debtDao(): DebtDao
    abstract fun categorizationRuleDao(): CategorizationRuleDao
    abstract fun merchantSignalDao(): MerchantSignalDao
    abstract fun importProfileDao(): ImportProfileDao
    abstract fun savedFilterDao(): SavedFilterDao
    abstract fun syncRecordDao(): SyncRecordDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        @Volatile
        private var INSTANCE: FinPulseDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_records` (
                        `entityType` TEXT NOT NULL,
                        `entityId` TEXT NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `localUpdatedAt` INTEGER NOT NULL,
                        `cloudUpdatedAt` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        `errorMessage` TEXT,
                        PRIMARY KEY(`entityType`, `entityId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_records_syncStatus` ON `sync_records` (`syncStatus`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_records_entityType` ON `sync_records` (`entityType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_records_localUpdatedAt` ON `sync_records` (`localUpdatedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_records_isDeleted` ON `sync_records` (`isDeleted`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exchange_rates` (
                        `fromCurrency` TEXT NOT NULL,
                        `toCurrency` TEXT NOT NULL,
                        `rate` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `isManual` INTEGER NOT NULL,
                        PRIMARY KEY(`fromCurrency`, `toCurrency`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exchange_rates_fromCurrency` ON `exchange_rates` (`fromCurrency`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exchange_rates_toCurrency` ON `exchange_rates` (`toCurrency`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exchange_rates_timestamp` ON `exchange_rates` (`timestamp`)")

                // Add multi-currency columns to transactions
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `exchangeRate` REAL")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `exchangeRateDate` INTEGER")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `destinationAmountMinor` INTEGER")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `destinationCurrencyCode` TEXT")
            }
        }

        fun getInstance(context: Context): FinPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinPulseDatabase::class.java,
                    "finpulse.db"
                ).addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

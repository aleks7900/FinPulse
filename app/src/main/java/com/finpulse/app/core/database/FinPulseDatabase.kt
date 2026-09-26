package com.finpulse.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.finpulse.app.core.database.dao.AccountDao
import com.finpulse.app.core.database.dao.AssetDao
import com.finpulse.app.core.database.dao.BudgetDao
import com.finpulse.app.core.database.dao.CategoryDao
import com.finpulse.app.core.database.dao.DebtDao
import com.finpulse.app.core.database.dao.FinancialGoalDao
import com.finpulse.app.core.database.dao.RecurringTransactionDao
import com.finpulse.app.core.database.dao.TransactionDao
import com.finpulse.app.core.database.entity.AccountEntity
import com.finpulse.app.core.database.entity.AssetEntity
import com.finpulse.app.core.database.entity.BudgetEntity
import com.finpulse.app.core.database.entity.CategoryEntity
import com.finpulse.app.core.database.entity.DebtEntity
import com.finpulse.app.core.database.entity.FinancialGoalEntity
import com.finpulse.app.core.database.entity.RecurringTransactionEntity
import com.finpulse.app.core.database.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        FinancialGoalEntity::class,
        AssetEntity::class,
        DebtEntity::class
    ],
    version = 1,
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

    companion object {
        @Volatile
        private var INSTANCE: FinPulseDatabase? = null

        fun getInstance(context: Context): FinPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinPulseDatabase::class.java,
                    "finpulse.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

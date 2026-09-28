package com.finpulse.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.finpulse.app.core.database.entity.AccountEntity
import com.finpulse.app.core.database.entity.CategoryEntity
import com.finpulse.app.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionByIdFlow(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE sourceAccountId = :accountId OR destinationAccountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsByAccountFlow(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE sourceAccountId = :accountId OR destinationAccountId = :accountId")
    suspend fun getTransactionCountForAccount(accountId: String): Int

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getTransactionsByCategoryFlow(categoryId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp DESC")
    fun getTransactionsByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startDate AND timestamp <= :endDate")
    suspend fun getTransactionsByDateRange(startDate: Long, endDate: Long): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions 
        WHERE (description LIKE '%' || :query || '%' 
           OR merchant LIKE '%' || :query || '%' 
           OR notes LIKE '%' || :query || '%' 
           OR tags LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchTransactionsFlow(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE type = :type AND timestamp >= :startDate AND timestamp <= :endDate AND isExcludedFromBudget = 0")
    fun getSumByTypeAndDateRangeFlow(type: String, startDate: Long, endDate: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE categoryId = :categoryId AND type = 'EXPENSE' AND timestamp >= :startDate AND timestamp <= :endDate AND isExcludedFromBudget = 0")
    fun getExpenseSumByCategoryAndDateRangeFlow(categoryId: String, startDate: Long, endDate: Long): Flow<Long>

    @Query("""
        SELECT categoryId FROM transactions 
        WHERE type = :type 
        GROUP BY categoryId 
        ORDER BY COUNT(*) DESC, MAX(timestamp) DESC 
        LIMIT :limit
    """)
    fun getFrequentCategoryIdsFlow(type: String, limit: Int = 6): Flow<List<String>>

    @Query("""
        SELECT merchant FROM transactions 
        WHERE merchant IS NOT NULL AND merchant != '' 
        GROUP BY merchant 
        ORDER BY COUNT(*) DESC, MAX(timestamp) DESC 
        LIMIT :limit
    """)
    fun getFrequentMerchantsFlow(limit: Int = 8): Flow<List<String>>

    @Query("""
        SELECT categoryId FROM transactions 
        WHERE merchant = :merchant 
        GROUP BY categoryId 
        ORDER BY COUNT(*) DESC, MAX(timestamp) DESC 
        LIMIT 1
    """)
    suspend fun getSuggestedCategoryForMerchant(merchant: String): String?

    @Query("""
        SELECT * FROM transactions 
        WHERE isCategoryConfirmed = 0 OR categoryId = 'cat_uncategorized'
        ORDER BY timestamp DESC
    """)
    fun getUnreviewedTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE isCategoryConfirmed = 0 OR categoryId = 'cat_uncategorized'
        ORDER BY timestamp DESC
    """)
    suspend fun getUnreviewedTransactions(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE isCategoryConfirmed = 0 OR categoryId = 'cat_uncategorized'")
    fun getUnreviewedCountFlow(): Flow<Int>

    @Query("""
        UPDATE transactions 
        SET categoryId = :categoryId, isCategoryConfirmed = :isConfirmed, matchedRuleId = :matchedRuleId, categorizationConfidence = :confidence
        WHERE id = :id
    """)
    suspend fun updateTransactionCategory(
        id: String,
        categoryId: String,
        isConfirmed: Boolean = true,
        matchedRuleId: String? = null,
        confidence: Float = 1.0f
    )

    @Query("""
        UPDATE transactions 
        SET categoryId = :categoryId, isCategoryConfirmed = :isConfirmed, matchedRuleId = :matchedRuleId, categorizationConfidence = 1.0
        WHERE id IN (:ids)
    """)
    suspend fun bulkUpdateCategory(
        ids: List<String>,
        categoryId: String,
        isConfirmed: Boolean = true,
        matchedRuleId: String? = null
    )

    @RawQuery(observedEntities = [TransactionEntity::class, CategoryEntity::class, AccountEntity::class])
    fun queryTransactionsFlow(query: SupportSQLiteQuery): Flow<List<TransactionEntity>>

    @RawQuery
    suspend fun queryTransactions(query: SupportSQLiteQuery): List<TransactionEntity>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}

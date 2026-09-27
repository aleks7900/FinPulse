package com.finpulse.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finpulse.app.core.database.entity.RecurringOccurrenceEntity
import com.finpulse.app.core.database.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    // --- Recurring Rules ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(recurring: RecurringTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringList(list: List<RecurringTransactionEntity>)

    @Update
    suspend fun updateRecurring(recurring: RecurringTransactionEntity)

    @Delete
    suspend fun deleteRecurring(recurring: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringById(id: String)

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getRecurringById(id: String): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    fun getRecurringByIdFlow(id: String): Flow<RecurringTransactionEntity?>

    @Query("SELECT * FROM recurring_transactions WHERE isCancelled = 0 ORDER BY nextDueDate ASC")
    fun getAllRecurringFlow(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND isCancelled = 0 ORDER BY nextDueDate ASC")
    fun getActiveRecurringFlow(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND isCancelled = 0 AND isSubscription = 1 ORDER BY nextDueDate ASC")
    fun getActiveSubscriptionsFlow(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND isCancelled = 0 AND nextDueDate <= :timestamp ORDER BY nextDueDate ASC")
    suspend fun getDueRecurring(timestamp: Long): List<RecurringTransactionEntity>

    @Query("UPDATE recurring_transactions SET nextDueDate = :nextDueDate, lastProcessedDate = :lastProcessedDate WHERE id = :id")
    suspend fun updateProcessedDate(id: String, nextDueDate: Long, lastProcessedDate: Long)

    @Query("UPDATE recurring_transactions SET isActive = :isActive WHERE id = :id")
    suspend fun setRuleActive(id: String, isActive: Boolean)

    @Query("UPDATE recurring_transactions SET isCancelled = 1, isActive = 0 WHERE id = :id")
    suspend fun cancelRule(id: String)

    // --- Occurrences ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrence(occurrence: RecurringOccurrenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrences(occurrences: List<RecurringOccurrenceEntity>)

    @Update
    suspend fun updateOccurrence(occurrence: RecurringOccurrenceEntity)

    @Query("SELECT * FROM recurring_occurrences WHERE id = :id")
    suspend fun getOccurrenceById(id: String): RecurringOccurrenceEntity?

    @Query("SELECT * FROM recurring_occurrences WHERE ruleId = :ruleId ORDER BY dueDate DESC")
    fun getOccurrencesByRuleFlow(ruleId: String): Flow<List<RecurringOccurrenceEntity>>

    @Query("SELECT * FROM recurring_occurrences WHERE dueDate BETWEEN :startDate AND :endDate ORDER BY dueDate ASC")
    fun getOccurrencesInRangeFlow(startDate: Long, endDate: Long): Flow<List<RecurringOccurrenceEntity>>

    @Query("SELECT * FROM recurring_occurrences WHERE dueDate BETWEEN :startDate AND :endDate ORDER BY dueDate ASC")
    suspend fun getOccurrencesInRange(startDate: Long, endDate: Long): List<RecurringOccurrenceEntity>

    @Query("UPDATE recurring_occurrences SET status = :status, paidDate = :paidDate, transactionId = :transactionId WHERE id = :id")
    suspend fun markOccurrenceStatus(id: String, status: String, paidDate: Long?, transactionId: String?)

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAllRecurring(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_occurrences")
    suspend fun getAllOccurrences(): List<RecurringOccurrenceEntity>

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAllRecurring()

    @Query("DELETE FROM recurring_occurrences")
    suspend fun deleteAllOccurrences()
}

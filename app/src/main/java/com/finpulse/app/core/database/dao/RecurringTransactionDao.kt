package com.finpulse.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finpulse.app.core.database.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

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

    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueDate ASC")
    fun getAllRecurringFlow(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getActiveRecurringFlow(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND isSubscription = 1 ORDER BY nextDueDate ASC")
    fun getActiveSubscriptionsFlow(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextDueDate <= :timestamp ORDER BY nextDueDate ASC")
    suspend fun getDueRecurring(timestamp: Long): List<RecurringTransactionEntity>

    @Query("UPDATE recurring_transactions SET nextDueDate = :nextDueDate, lastProcessedDate = :lastProcessedDate WHERE id = :id")
    suspend fun updateProcessedDate(id: String, nextDueDate: Long, lastProcessedDate: Long)
}

package com.finpulse.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finpulse.app.core.database.entity.FinancialGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: FinancialGoalEntity)

    @Update
    suspend fun updateGoal(goal: FinancialGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: FinancialGoalEntity)

    @Query("DELETE FROM financial_goals WHERE id = :id")
    suspend fun deleteGoalById(id: String)

    @Query("SELECT * FROM financial_goals WHERE id = :id")
    suspend fun getGoalById(id: String): FinancialGoalEntity?

    @Query("SELECT * FROM financial_goals WHERE id = :id")
    fun getGoalByIdFlow(id: String): Flow<FinancialGoalEntity?>

    @Query("SELECT * FROM financial_goals ORDER BY isCompleted ASC, targetDate ASC")
    fun getAllGoalsFlow(): Flow<List<FinancialGoalEntity>>

    @Query("UPDATE financial_goals SET currentAmountMinor = :currentAmountMinor, isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateProgress(id: String, currentAmountMinor: Long, isCompleted: Boolean)
}

package com.finpulse.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finpulse.app.core.database.entity.DebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: String)

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: String): DebtEntity?

    @Query("SELECT * FROM debts WHERE id = :id")
    fun getDebtByIdFlow(id: String): Flow<DebtEntity?>

    @Query("SELECT * FROM debts ORDER BY remainingBalanceMinor DESC")
    fun getAllDebtsFlow(): Flow<List<DebtEntity>>

    @Query("UPDATE debts SET remainingBalanceMinor = :remainingMinor WHERE id = :id")
    suspend fun updateBalance(id: String, remainingMinor: Long)

    @Query("SELECT * FROM debts")
    suspend fun getAllDebts(): List<DebtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<DebtEntity>)

    @Query("DELETE FROM debts")
    suspend fun deleteAllDebts()
}

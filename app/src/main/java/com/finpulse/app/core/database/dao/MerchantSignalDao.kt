package com.finpulse.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.finpulse.app.core.database.entity.MerchantSignalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantSignalDao {

    @Query("SELECT * FROM merchant_signals ORDER BY lastUsedAt DESC")
    fun getAllSignalsFlow(): Flow<List<MerchantSignalEntity>>

    @Query("SELECT * FROM merchant_signals ORDER BY lastUsedAt DESC")
    suspend fun getAllSignals(): List<MerchantSignalEntity>

    @Query("SELECT * FROM merchant_signals WHERE normalizedMerchant = :normalizedMerchant LIMIT 1")
    suspend fun getSignal(normalizedMerchant: String): MerchantSignalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSignal(signal: MerchantSignalEntity)

    @Query("DELETE FROM merchant_signals WHERE normalizedMerchant = :normalizedMerchant")
    suspend fun deleteSignal(normalizedMerchant: String)

    @Query("DELETE FROM merchant_signals")
    suspend fun clearAllSignals()
}

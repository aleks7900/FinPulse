package md.alexlab.finpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import md.alexlab.finpulse.core.database.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {

    @Query("SELECT * FROM exchange_rates WHERE fromCurrency = :from AND toCurrency = :to LIMIT 1")
    fun getRateFlow(from: String, to: String): Flow<ExchangeRateEntity?>

    @Query("SELECT * FROM exchange_rates WHERE fromCurrency = :from AND toCurrency = :to LIMIT 1")
    suspend fun getRate(from: String, to: String): ExchangeRateEntity?

    @Query("SELECT * FROM exchange_rates ORDER BY fromCurrency ASC, toCurrency ASC")
    fun getAllRatesFlow(): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates ORDER BY fromCurrency ASC, toCurrency ASC")
    suspend fun getAllRates(): List<ExchangeRateEntity>

    @Query("SELECT MAX(timestamp) FROM exchange_rates WHERE isManual = 0")
    fun getLastMarketUpdateTimestampFlow(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: ExchangeRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRateEntity>)

    @Query("DELETE FROM exchange_rates WHERE fromCurrency = :from AND toCurrency = :to")
    suspend fun deleteRate(from: String, to: String)

    @Query("DELETE FROM exchange_rates")
    suspend fun clearAllRates()
}

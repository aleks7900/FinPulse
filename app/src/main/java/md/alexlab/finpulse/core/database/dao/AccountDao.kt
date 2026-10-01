package md.alexlab.finpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import md.alexlab.finpulse.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: String)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getAccountByIdFlow(id: String): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, createdAt DESC")
    fun getAllAccountsFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getActiveAccountsFlow(): Flow<List<AccountEntity>>

    @Query("UPDATE accounts SET balanceMinor = :balanceMinor, availableBalanceMinor = :availableBalanceMinor, updatedAt = :updatedAt WHERE id = :accountId")
    suspend fun updateBalances(accountId: String, balanceMinor: Long, availableBalanceMinor: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE accounts SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :accountId")
    suspend fun setArchived(accountId: String, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccounts(): List<AccountEntity>

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}

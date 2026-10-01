package md.alexlab.finpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import md.alexlab.finpulse.core.database.entity.CategorizationRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategorizationRuleDao {

    @Query("SELECT * FROM categorization_rules ORDER BY priority DESC, createdAt DESC")
    fun getAllRulesFlow(): Flow<List<CategorizationRuleEntity>>

    @Query("SELECT * FROM categorization_rules WHERE isActive = 1 ORDER BY priority DESC, createdAt DESC")
    fun getActiveRulesFlow(): Flow<List<CategorizationRuleEntity>>

    @Query("SELECT * FROM categorization_rules WHERE isActive = 1 ORDER BY priority DESC, createdAt DESC")
    suspend fun getActiveRules(): List<CategorizationRuleEntity>

    @Query("SELECT * FROM categorization_rules WHERE id = :id")
    suspend fun getRuleById(id: String): CategorizationRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CategorizationRuleEntity)

    @Update
    suspend fun updateRule(rule: CategorizationRuleEntity)

    @Query("DELETE FROM categorization_rules WHERE id = :id")
    suspend fun deleteRuleById(id: String)

    @Query("UPDATE categorization_rules SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setRuleActive(id: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM categorization_rules")
    suspend fun getRuleCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<CategorizationRuleEntity>)

    @Query("UPDATE categorization_rules SET priority = :priority, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRulePriority(id: String, priority: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM categorization_rules")
    suspend fun getAllRules(): List<CategorizationRuleEntity>

    @Query("DELETE FROM categorization_rules")
    suspend fun deleteAllRules()
}

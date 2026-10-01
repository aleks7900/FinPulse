package md.alexlab.finpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import md.alexlab.finpulse.core.database.entity.ImportProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportProfileDao {

    @Query("SELECT * FROM import_profiles ORDER BY isSystemPreset DESC, updatedAt DESC")
    fun getAllProfilesFlow(): Flow<List<ImportProfileEntity>>

    @Query("SELECT * FROM import_profiles ORDER BY isSystemPreset DESC, updatedAt DESC")
    suspend fun getAllProfiles(): List<ImportProfileEntity>

    @Query("SELECT * FROM import_profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ImportProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ImportProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<ImportProfileEntity>)

    @Update
    suspend fun updateProfile(profile: ImportProfileEntity)

    @Query("DELETE FROM import_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: String)

    @Query("SELECT COUNT(*) FROM import_profiles")
    suspend fun getProfileCount(): Int
}

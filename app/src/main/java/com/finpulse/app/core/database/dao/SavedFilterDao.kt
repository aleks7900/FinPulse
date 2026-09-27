package com.finpulse.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finpulse.app.core.database.entity.SavedFilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedFilterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedFilter(savedFilter: SavedFilterEntity)

    @Update
    suspend fun updateSavedFilter(savedFilter: SavedFilterEntity)

    @Delete
    suspend fun deleteSavedFilter(savedFilter: SavedFilterEntity)

    @Query("DELETE FROM saved_filters WHERE id = :id")
    suspend fun deleteSavedFilterById(id: String)

    @Query("SELECT * FROM saved_filters WHERE id = :id")
    suspend fun getSavedFilterById(id: String): SavedFilterEntity?

    @Query("SELECT * FROM saved_filters ORDER BY createdAt DESC")
    fun getAllSavedFiltersFlow(): Flow<List<SavedFilterEntity>>

    @Query("SELECT * FROM saved_filters ORDER BY createdAt DESC")
    suspend fun getAllSavedFilters(): List<SavedFilterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedFilters(savedFilters: List<SavedFilterEntity>)

    @Query("DELETE FROM saved_filters")
    suspend fun deleteAllSavedFilters()
}

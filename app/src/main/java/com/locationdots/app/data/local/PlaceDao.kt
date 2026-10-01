package com.locationdots.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE name LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY updatedAtEpochMillis DESC")
    suspend fun searchByName(query: String): List<PlaceEntity>

    @Query("SELECT * FROM places")
    suspend fun getAll(): List<PlaceEntity>

    @Query("SELECT * FROM places WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PlaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: PlaceEntity)

    @Query("DELETE FROM places")
    suspend fun deleteAll()
}

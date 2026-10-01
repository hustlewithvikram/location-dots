package com.locationdots.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM location_points ORDER BY timestampEpochMillis ASC")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM location_points ORDER BY timestampEpochMillis ASC")
    suspend fun getAll(): List<LocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: LocationEntity)
}

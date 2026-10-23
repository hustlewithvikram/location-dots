package com.vikram.locationdots.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM location_points ORDER BY timestampEpochMillis ASC")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM location_points WHERE timestampEpochMillis >= :fromEpochMillis AND timestampEpochMillis <= :toEpochMillis ORDER BY timestampEpochMillis ASC")
    suspend fun getRange(fromEpochMillis: Long, toEpochMillis: Long): List<LocationEntity>

    // Stable IDs are derived from timestamp + coordinates. Ignore exact duplicate fixes so
    // they do not replace the same row and trigger unnecessary Room Flow emissions.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(point: LocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<LocationEntity>)

    @Query("DELETE FROM location_points")
    suspend fun deleteAll()
}

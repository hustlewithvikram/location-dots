package com.locationdots.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TimelineEventDao {
    @Query("""
        SELECT * FROM timeline_events
        ORDER BY timestampEpochMillis DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getPage(limit: Int, offset: Int): List<TimelineEventEntity>

    @Query("""
        SELECT * FROM timeline_events
        WHERE type = 'visit' AND placeId = :placeId
        ORDER BY timestampEpochMillis DESC
    """)
    suspend fun getVisitsForPlace(placeId: String): List<TimelineEventEntity>

    @Query("SELECT * FROM timeline_events ORDER BY timestampEpochMillis DESC")
    suspend fun getAll(): List<TimelineEventEntity>

    @Query("DELETE FROM timeline_events")
    suspend fun deleteAll()

    @Query("""
        DELETE FROM timeline_events
        WHERE timestampEpochMillis >= :fromEpochMillis
        AND timestampEpochMillis <= :toEpochMillis
    """)
    suspend fun deleteRange(fromEpochMillis: Long, toEpochMillis: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<TimelineEventEntity>)

    @Transaction
    suspend fun replaceRange(
        fromEpochMillis: Long,
        toEpochMillis: Long,
        events: List<TimelineEventEntity>
    ) {
        deleteRange(fromEpochMillis, toEpochMillis)
        insertAll(events)
    }
}

package com.vikram.locationdots.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineEventDao {
    @Query("""
        SELECT * FROM timeline_events
        ORDER BY timestampEpochMillis DESC, id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getPage(limit: Int, offset: Int): List<TimelineEventEntity>

    @Query("""
        SELECT * FROM timeline_events
        ORDER BY timestampEpochMillis DESC, id DESC
        LIMIT :limit
    """)
    fun observeRecent(limit: Int): Flow<List<TimelineEventEntity>>

    @Query("""
        SELECT * FROM timeline_events
        WHERE type = 'visit' AND placeId = :placeId
        ORDER BY timestampEpochMillis DESC
    """)
    suspend fun getVisitsForPlace(placeId: String): List<TimelineEventEntity>

    @Query("""
        SELECT * FROM timeline_events
        WHERE type = 'visit' AND departureEpochMillis IS NULL
        ORDER BY arrivalEpochMillis DESC
        LIMIT 1
    """)
    suspend fun getActiveVisit(): TimelineEventEntity?

    @Query("SELECT * FROM timeline_events ORDER BY timestampEpochMillis DESC")
    suspend fun getAll(): List<TimelineEventEntity>

    @Query("DELETE FROM timeline_events WHERE type = 'visit' AND placeId = :placeId")
    suspend fun deleteVisitsForPlace(placeId: String): Int

    @Query("UPDATE timeline_events SET startPlaceId = NULL WHERE startPlaceId = :placeId")
    suspend fun clearJourneyStartPlace(placeId: String): Int

    @Query("UPDATE timeline_events SET endPlaceId = NULL WHERE endPlaceId = :placeId")
    suspend fun clearJourneyEndPlace(placeId: String): Int

    @Query("DELETE FROM timeline_events")
    suspend fun deleteAll()

    @Query("""
        DELETE FROM timeline_events
        WHERE (
            type = 'visit'
            AND timestampEpochMillis <= :toEpochMillis
            AND COALESCE(departureEpochMillis, timestampEpochMillis) >= :fromEpochMillis
        )
        OR (
            type = 'journey'
            AND COALESCE(startedAtEpochMillis, timestampEpochMillis) <= :toEpochMillis
            AND COALESCE(endedAtEpochMillis, timestampEpochMillis) >= :fromEpochMillis
        )
    """)
    suspend fun deleteRange(fromEpochMillis: Long, toEpochMillis: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<TimelineEventEntity>)

    @Transaction
    suspend fun replaceAll(events: List<TimelineEventEntity>) {
        deleteAll()
        if (events.isNotEmpty()) insertAll(events)
    }

    @Transaction
    suspend fun replaceRange(
        fromEpochMillis: Long,
        toEpochMillis: Long,
        events: List<TimelineEventEntity>
    ) {
        deleteRange(fromEpochMillis, toEpochMillis)
        if (events.isNotEmpty()) insertAll(events)
    }
}

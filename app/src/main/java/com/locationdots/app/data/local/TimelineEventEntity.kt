package com.locationdots.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "timeline_events",
    indices = []
)
data class TimelineEventEntity(
    @PrimaryKey val id: String,
    val type: String,
    val timestampEpochMillis: Long,
    val placeId: String?,
    val arrivalEpochMillis: Long?,
    val departureEpochMillis: Long?,
    val startPlaceId: String?,
    val endPlaceId: String?,
    val startedAtEpochMillis: Long?,
    val endedAtEpochMillis: Long?,
    val distanceMeters: Double?
)

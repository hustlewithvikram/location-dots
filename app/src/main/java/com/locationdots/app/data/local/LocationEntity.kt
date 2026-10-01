package com.locationdots.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_points")
data class LocationEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val timestampEpochMillis: Long
)

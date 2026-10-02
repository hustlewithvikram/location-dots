package com.locationdots.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "location_points", indices = [Index(value = ["timestampEpochMillis"])])
data class LocationEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val timestampEpochMillis: Long
)

package com.locationdots.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

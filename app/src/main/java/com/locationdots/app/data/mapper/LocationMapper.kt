package com.vikram.locationdots.data.mapper

import com.vikram.locationdots.data.local.LocationEntity
import com.vikram.locationdots.domain.model.LocationPoint
import java.time.Instant

fun LocationEntity.toDomain() = LocationPoint(
    latitude, longitude, accuracyMeters, Instant.ofEpochMilli(timestampEpochMillis)
)

fun LocationPoint.toEntity(id: String) = LocationEntity(
    id, latitude, longitude, accuracyMeters, timestamp.toEpochMilli()
)

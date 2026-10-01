package com.locationdots.app.data.mapper

import com.locationdots.app.data.local.LocationEntity
import com.locationdots.app.domain.model.LocationPoint
import java.time.Instant

fun LocationEntity.toDomain() = LocationPoint(
    latitude, longitude, accuracyMeters, Instant.ofEpochMilli(timestampEpochMillis)
)

fun LocationPoint.toEntity(id: String) = LocationEntity(
    id, latitude, longitude, accuracyMeters, timestamp.toEpochMilli()
)

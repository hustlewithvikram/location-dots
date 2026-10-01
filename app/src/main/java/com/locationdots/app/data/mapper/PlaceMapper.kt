package com.locationdots.app.data.mapper

import com.locationdots.app.data.local.PlaceEntity
import com.locationdots.app.domain.model.Place
import java.time.Instant

fun PlaceEntity.toDomain() = Place(
    id, name, latitude, longitude,
    Instant.ofEpochMilli(createdAtEpochMillis),
    Instant.ofEpochMilli(updatedAtEpochMillis)
)

fun Place.toEntity() = PlaceEntity(
    id, name, latitude, longitude,
    createdAt.toEpochMilli(), updatedAt.toEpochMilli()
)

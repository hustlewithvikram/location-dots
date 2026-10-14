package com.vikram.locationdots.data.mapper

import com.vikram.locationdots.data.local.PlaceEntity
import com.vikram.locationdots.domain.model.Place
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

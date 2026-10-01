package com.locationdots.app.domain.model

import java.time.Instant

data class Place(
    val id: String,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Instant,
    val updatedAt: Instant
)

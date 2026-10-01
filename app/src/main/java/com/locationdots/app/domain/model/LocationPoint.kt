package com.locationdots.app.domain.model

import java.time.Instant

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val timestamp: Instant
)

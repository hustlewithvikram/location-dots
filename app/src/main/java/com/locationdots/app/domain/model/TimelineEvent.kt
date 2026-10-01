package com.locationdots.app.domain.model

import java.time.Instant

sealed interface TimelineEvent {
    val id: String
    val timestamp: Instant

    data class Visit(
        override val id: String,
        override val timestamp: Instant,
        val place: Place,
        val arrival: Instant,
        val departure: Instant?
    ) : TimelineEvent

    data class Journey(
        override val id: String,
        override val timestamp: Instant,
        val startPlace: Place?,
        val endPlace: Place?,
        val startedAt: Instant,
        val endedAt: Instant?,
        val distanceMeters: Double?,
        val mode: JourneyMode
    ) : TimelineEvent
}

enum class JourneyMode {
    WALKING,
    CYCLING,
    VEHICLE,
    UNKNOWN
}

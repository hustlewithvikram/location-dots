package com.locationdots.app.domain.journey

import com.locationdots.app.domain.model.LocationPoint
import com.locationdots.app.domain.model.TimelineEvent

interface JourneyProcessor {
    suspend fun process(points: List<LocationPoint>): List<TimelineEvent>
}

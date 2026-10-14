package com.vikram.locationdots.domain.journey

import com.vikram.locationdots.domain.model.LocationPoint
import com.vikram.locationdots.domain.model.TimelineEvent

interface JourneyProcessor {
    suspend fun process(points: List<LocationPoint>): List<TimelineEvent>
}

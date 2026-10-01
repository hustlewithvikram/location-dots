package com.locationdots.app.data.repository

import com.locationdots.app.domain.insights.InsightsRepository
import com.locationdots.app.domain.journey.JourneyProcessor
import com.locationdots.app.domain.model.TimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomInsightsRepository(
    private val locationRepository: RoomLocationRepository,
    private val journeyProcessor: JourneyProcessor
) : InsightsRepository {
    override fun observeTimeline(): Flow<List<TimelineEvent>> =
        locationRepository.observeLocationPoints().map { points ->
            journeyProcessor.process(points)
        }
}

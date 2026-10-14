package com.vikram.locationdots.domain.timeline

import com.vikram.locationdots.domain.model.TimelineEvent
import java.time.Instant

interface TimelineRepository {
    suspend fun getAll(): List<TimelineEvent>
    suspend fun getPage(limit: Int, offset: Int): List<TimelineEvent>
    suspend fun replaceAll(events: List<TimelineEvent>)
    suspend fun getVisitsForPlace(placeId: String): List<TimelineEvent.Visit>
    suspend fun getActiveVisit(): TimelineEvent.Visit?
    suspend fun replaceRange(from: Instant, to: Instant, events: List<TimelineEvent>)
}

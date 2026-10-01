package com.locationdots.app.domain.timeline

import com.locationdots.app.domain.model.TimelineEvent
import java.time.Instant

interface TimelineRepository {
    suspend fun getPage(limit: Int, offset: Int): List<TimelineEvent>
    suspend fun replaceAll(events: List<TimelineEvent>)
    suspend fun replaceRange(from: Instant, to: Instant, events: List<TimelineEvent>)
}

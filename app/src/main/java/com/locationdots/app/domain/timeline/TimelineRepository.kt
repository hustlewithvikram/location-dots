package com.locationdots.app.domain.timeline

import com.locationdots.app.domain.model.TimelineEvent

interface TimelineRepository {
    suspend fun getPage(limit: Int, offset: Int): List<TimelineEvent>
    suspend fun replaceAll(events: List<TimelineEvent>)
}

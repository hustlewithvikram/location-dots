package com.locationdots.app.domain.insights

import com.locationdots.app.domain.model.TimelineEvent
import kotlinx.coroutines.flow.Flow

interface InsightsRepository {
    fun observeTimeline(): Flow<List<TimelineEvent>>
}

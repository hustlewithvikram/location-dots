package com.locationdots.app.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationdots.app.domain.insights.InsightsRepository
import com.locationdots.app.domain.model.TimelineEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TimelineViewModel(repository: InsightsRepository) : ViewModel() {
    val timeline: StateFlow<List<TimelineEvent>> =
        repository.observeTimeline().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )
}

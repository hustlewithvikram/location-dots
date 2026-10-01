package com.locationdots.app.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.locationdots.app.domain.insights.InsightsRepository

class TimelineViewModelFactory(
    private val repository: InsightsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TimelineViewModel::class.java))
        return TimelineViewModel(repository) as T
    }
}

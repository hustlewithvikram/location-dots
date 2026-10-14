package com.vikram.locationdots.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vikram.locationdots.domain.timeline.TimelineRepository

class TimelineViewModelFactory(
    private val repository: TimelineRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TimelineViewModel::class.java))
        return TimelineViewModel(repository) as T
    }
}

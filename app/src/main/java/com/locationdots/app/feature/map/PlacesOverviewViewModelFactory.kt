package com.vikram.locationdots.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vikram.locationdots.domain.places.PlaceRepository
import com.vikram.locationdots.domain.timeline.TimelineRepository

class PlacesOverviewViewModelFactory(
    private val repository: PlaceRepository,
    private val timelineRepository: TimelineRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PlacesOverviewViewModel(repository, timelineRepository) as T
}

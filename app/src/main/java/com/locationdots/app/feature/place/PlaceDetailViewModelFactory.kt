package com.vikram.locationdots.feature.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vikram.locationdots.domain.places.PlaceRepository
import com.vikram.locationdots.domain.timeline.TimelineRepository

class PlaceDetailViewModelFactory(
    private val repository: PlaceRepository,
    private val timelineRepository: TimelineRepository,
    private val placeId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PlaceDetailViewModel(repository, timelineRepository, placeId) as T
}

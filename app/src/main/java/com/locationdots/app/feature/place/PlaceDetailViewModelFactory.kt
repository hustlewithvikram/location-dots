package com.locationdots.app.feature.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.locationdots.app.domain.places.PlaceRepository
import com.locationdots.app.domain.timeline.TimelineRepository

class PlaceDetailViewModelFactory(
    private val repository: PlaceRepository,
    private val timelineRepository: TimelineRepository,
    private val placeId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PlaceDetailViewModel(repository, timelineRepository, placeId) as T
}

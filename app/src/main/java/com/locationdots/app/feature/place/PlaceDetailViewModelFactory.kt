package com.locationdots.app.feature.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.locationdots.app.domain.places.PlaceRepository

class PlaceDetailViewModelFactory(
    private val repository: PlaceRepository,
    private val placeId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PlaceDetailViewModel(repository, placeId) as T
}

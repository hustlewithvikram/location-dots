package com.locationdots.app.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationdots.app.domain.model.Place
import com.locationdots.app.domain.places.PlaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PlacesOverviewViewModel(repository: PlaceRepository) : ViewModel() {
    val places: StateFlow<List<Place>> = repository.observePlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

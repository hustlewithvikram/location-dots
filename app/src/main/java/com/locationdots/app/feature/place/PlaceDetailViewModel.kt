package com.locationdots.app.feature.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationdots.app.domain.model.Place
import com.locationdots.app.domain.places.PlaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaceDetailViewModel(private val repository: PlaceRepository, private val placeId: String) : ViewModel() {
    private val _place = MutableStateFlow<Place?>(null)
    val place: StateFlow<Place?> = _place.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { _place.value = repository.getPlace(placeId) }
    }

    fun updateName(name: String?) {
        viewModelScope.launch {
            repository.updatePlaceName(placeId, name?.trim()?.takeIf { it.isNotEmpty() })
            refresh()
        }
    }
}

package com.locationdots.app.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationdots.app.domain.model.Place
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.domain.places.PlaceRepository
import com.locationdots.app.domain.timeline.TimelineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration

data class PlaceSummary(
    val visitCount: Int,
    val totalStayMinutes: Long,
    val averageStayMinutes: Long,
    val lastVisit: TimelineEvent.Visit?
)

class PlacesOverviewViewModel(
    repository: PlaceRepository,
    private val timelineRepository: TimelineRepository
) : ViewModel() {
    val places: StateFlow<List<Place>> = repository.observePlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _summaries = MutableStateFlow<Map<String, PlaceSummary>>(emptyMap())
    val summaries: StateFlow<Map<String, PlaceSummary>> = _summaries

    fun refresh() {
        viewModelScope.launch { refreshSummaries(places.value) }
    }

    private suspend fun refreshSummaries(currentPlaces: List<Place>) {
        _summaries.value = currentPlaces.associate { place ->
            val visits = timelineRepository.getVisitsForPlace(place.id)
            val completed = visits.mapNotNull { visit ->
                visit.departure?.let { departure ->
                    Duration.between(visit.arrival, departure).toMinutes().coerceAtLeast(0)
                }
            }
            place.id to PlaceSummary(
                visitCount = visits.size,
                totalStayMinutes = completed.sum(),
                averageStayMinutes = if (completed.isEmpty()) 0 else completed.sum() / completed.size,
                lastVisit = visits.maxByOrNull { it.arrival }
            )
        }
    }

    init {
        viewModelScope.launch {
            places.collectLatest { currentPlaces ->
                refreshSummaries(currentPlaces)
            }
        }
    }
}

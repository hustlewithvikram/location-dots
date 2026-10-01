package com.locationdots.app.data.repository

import com.locationdots.app.domain.model.JourneyMode
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.domain.places.PlaceRepository
import com.locationdots.app.domain.search.SearchRepository
import com.locationdots.app.domain.search.SearchResult
import com.locationdots.app.domain.timeline.TimelineRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.Locale

class RoomSearchRepository(
    private val placeRepository: PlaceRepository,
    private val timelineRepository: TimelineRepository
) : SearchRepository {

    override suspend fun search(query: String): List<SearchResult> = coroutineScope {
        val normalized = query.trim()
        if (normalized.length < 2) return@coroutineScope emptyList()

        val places = async { placeRepository.searchByName(normalized) }
        val events = async { timelineRepository.getAll() }

        val results = mutableListOf<SearchResult>()
        results += places.await().map { SearchResult.PlaceResult(it) }

        val lower = normalized.lowercase(Locale.getDefault())
        events.await()
            .asSequence()
            .filter { event ->
                when (event) {
                    is TimelineEvent.Visit ->
                        event.place.name?.contains(lower, ignoreCase = true) == true
                    is TimelineEvent.Journey ->
                        event.startPlace?.name?.contains(lower, ignoreCase = true) == true ||
                            event.endPlace?.name?.contains(lower, ignoreCase = true) == true ||
                            event.mode.searchLabel().contains(lower, ignoreCase = true)
                }
            }
            .take(40)
            .forEach { event ->
                results += when (event) {
                    is TimelineEvent.Visit -> SearchResult.VisitResult(event)
                    is TimelineEvent.Journey -> SearchResult.JourneyResult(event)
                }
            }

        results.distinctBy { result ->
            when (result) {
                is SearchResult.PlaceResult -> result.place.id
                is SearchResult.VisitResult -> result.event.id
                is SearchResult.JourneyResult -> result.event.id
            }
        }.take(50)
    }

    private fun JourneyMode.searchLabel(): String = when (this) {
        JourneyMode.WALKING -> "walking"
        JourneyMode.CYCLING -> "cycling"
        JourneyMode.VEHICLE -> "vehicle"
        JourneyMode.UNKNOWN -> "movement"
    }
}

package com.vikram.locationdots.domain.search

import com.vikram.locationdots.domain.model.Place
import com.vikram.locationdots.domain.model.TimelineEvent

sealed interface SearchResult {
    data class PlaceResult(val place: Place) : SearchResult
    data class VisitResult(val event: TimelineEvent.Visit) : SearchResult
    data class JourneyResult(val event: TimelineEvent.Journey) : SearchResult
}

interface SearchRepository {
    suspend fun search(query: String): List<SearchResult>
}

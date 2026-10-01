package com.locationdots.app.domain.places

import com.locationdots.app.domain.model.Place
import kotlinx.coroutines.flow.Flow

interface PlaceRepository {
    fun observePlaces(): Flow<List<Place>>
    suspend fun getPlace(id: String): Place?
    suspend fun savePlace(place: Place)
    suspend fun updatePlaceName(id: String, name: String?)
}

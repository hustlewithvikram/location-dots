package com.vikram.locationdots.domain.places

import com.vikram.locationdots.domain.model.Place
import kotlinx.coroutines.flow.Flow

interface PlaceRepository {
    fun observePlaces(): Flow<List<Place>>
    suspend fun searchByName(query: String): List<Place>
    suspend fun getPlace(id: String): Place?
    suspend fun findNearby(latitude: Double, longitude: Double, radiusMeters: Double): Place?
    suspend fun savePlace(place: Place)
    suspend fun updatePlaceName(id: String, name: String?)
    suspend fun saveNamedPlace(name: String, latitude: Double, longitude: Double): Place
}

package com.locationdots.app.data.repository

import com.locationdots.app.data.local.PlaceDao
import com.locationdots.app.data.mapper.toDomain
import com.locationdots.app.data.mapper.toEntity
import com.locationdots.app.domain.model.Place
import com.locationdots.app.domain.places.PlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.*

class RoomPlaceRepository(private val dao: PlaceDao) : PlaceRepository {
    override fun observePlaces(): Flow<List<Place>> =
        dao.observeAll().map { it.map { entity -> entity.toDomain() } }

    override suspend fun getPlace(id: String): Place? =
        dao.findById(id)?.toDomain()

    override suspend fun findNearby(latitude: Double, longitude: Double, radiusMeters: Double): Place? =
        dao.getAll()
            .asSequence()
            .map { it.toDomain() }
            .map { place ->
                place to distanceMeters(latitude, longitude, place.latitude, place.longitude)
            }
            .filter { (_, distance) -> distance <= radiusMeters }
            .minByOrNull { (_, distance) -> distance }
            ?.first

    override suspend fun savePlace(place: Place) =
        dao.insert(place.toEntity())

    override suspend fun updatePlaceName(id: String, name: String?) {
        val existing = dao.findById(id) ?: return
        dao.insert(existing.copy(
            name = name,
            updatedAtEpochMillis = System.currentTimeMillis()
        ))
    }

    private fun distanceMeters(latitude1: Double, longitude1: Double, latitude2: Double, longitude2: Double): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(latitude2 - latitude1)
        val dLon = Math.toRadians(longitude2 - longitude1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(latitude1)) * cos(Math.toRadians(latitude2)) * sin(dLon / 2).pow(2)
        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

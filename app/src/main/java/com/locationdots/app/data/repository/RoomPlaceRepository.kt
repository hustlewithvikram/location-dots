package com.locationdots.app.data.repository

import com.locationdots.app.data.local.PlaceDao
import com.locationdots.app.data.mapper.toDomain
import com.locationdots.app.data.mapper.toEntity
import com.locationdots.app.domain.model.Place
import com.locationdots.app.domain.places.PlaceRepository
import kotlinx.coroutines.flow.Flow

class RoomPlaceRepository(private val dao: PlaceDao) : PlaceRepository {
    override fun observePlaces(): Flow<List<Place>> =
        dao.observeAll().map { it.map { entity -> entity.toDomain() } }

    override suspend fun getPlace(id: String): Place? =
        dao.findById(id)?.toDomain()

    override suspend fun savePlace(place: Place) = dao.insert(place.toEntity())

    override suspend fun updatePlaceName(id: String, name: String?) {
        val existing = dao.findById(id) ?: return
        dao.insert(existing.copy(
            name = name,
            updatedAtEpochMillis = System.currentTimeMillis()
        ))
    }
}

package com.locationdots.app.data.repository

import com.locationdots.app.data.local.LocationDao
import com.locationdots.app.data.mapper.toDomain
import com.locationdots.app.data.mapper.toEntity
import com.locationdots.app.domain.location.LocationRepository
import com.locationdots.app.domain.model.LocationPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomLocationRepository(private val dao: LocationDao) : LocationRepository {
    override fun observeLocationPoints(): Flow<List<LocationPoint>> =
        dao.observeAll().map { it.map { entity -> entity.toDomain() } }

    override suspend fun saveLocationPoint(point: LocationPoint) {
        dao.insert(point.toEntity(UUID.randomUUID().toString()))
    }
}

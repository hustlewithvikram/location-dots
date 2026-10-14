package com.vikram.locationdots.data.repository

import com.vikram.locationdots.data.local.LocationDao
import com.vikram.locationdots.data.mapper.toDomain
import com.vikram.locationdots.data.mapper.toEntity
import com.vikram.locationdots.domain.location.LocationRepository
import com.vikram.locationdots.domain.model.LocationPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomLocationRepository(private val dao: LocationDao) : LocationRepository {
    override fun observeLocationPoints(): Flow<List<LocationPoint>> =
        dao.observeAll().map { it.map { entity -> entity.toDomain() } }

    override suspend fun getLocationPoints(from: Instant, to: Instant): List<LocationPoint> =
        dao.getRange(from.toEpochMilli(), to.toEpochMilli()).map { it.toDomain() }

    override suspend fun saveLocationPoint(point: LocationPoint) {
        val stableId = buildString {
            append("location:")
            append(point.timestamp.toEpochMilli())
            append(':')
            append(point.latitude)
            append(':')
            append(point.longitude)
        }
        dao.insert(point.toEntity(stableId))
    }
}

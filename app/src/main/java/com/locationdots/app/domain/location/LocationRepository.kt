package com.locationdots.app.domain.location

import com.locationdots.app.domain.model.LocationPoint
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeLocationPoints(): Flow<List<LocationPoint>>
    suspend fun saveLocationPoint(point: LocationPoint)
}

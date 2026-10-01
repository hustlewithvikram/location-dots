package com.locationdots.app.domain.location

import com.locationdots.app.domain.model.LocationPoint
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface LocationRepository {
    fun observeLocationPoints(): Flow<List<LocationPoint>>
    suspend fun getLocationPoints(from: Instant, to: Instant): List<LocationPoint>
    suspend fun saveLocationPoint(point: LocationPoint)
}

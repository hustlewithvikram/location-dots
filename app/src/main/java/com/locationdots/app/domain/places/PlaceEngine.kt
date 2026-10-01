package com.locationdots.app.domain.places

import com.locationdots.app.domain.model.LocationPoint
import com.locationdots.app.domain.model.Place

class PlaceEngine(private val repository: PlaceRepository) {
    suspend fun resolve(points: List<LocationPoint>): Place {
        require(points.isNotEmpty())
        val latitude = points.map { it.latitude }.average()
        val longitude = points.map { it.longitude }.average()
        val timestamp = points.last().timestamp

        val existing = repository.findNearby(latitude, longitude, MERGE_RADIUS_METERS)

        val place = existing?.copy(updatedAt = maxOf(existing.updatedAt, timestamp))
            ?: Place(
                id = "place:${timestamp.toEpochMilli()}:$latitude:$longitude",
                name = null,
                latitude = latitude,
                longitude = longitude,
                createdAt = points.first().timestamp,
                updatedAt = timestamp
            )

        repository.savePlace(place)
        return place
    }

    private companion object {
        const val MERGE_RADIUS_METERS = 150.0
    }
}

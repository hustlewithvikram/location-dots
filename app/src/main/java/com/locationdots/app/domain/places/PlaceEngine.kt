package com.locationdots.app.domain.places

import com.locationdots.app.domain.model.LocationPoint
import com.locationdots.app.domain.model.Place

class PlaceEngine(private val repository: PlaceRepository) {
    suspend fun resolve(points: List<LocationPoint>): Place {
        require(points.isNotEmpty())

        val center = robustCenter(points)
        val timestamp = points.last().timestamp
        val mergeRadius = mergeRadiusFor(points)

        val existing = repository.findNearby(center.first, center.second, mergeRadius)

        val place = existing?.copy(updatedAt = maxOf(existing.updatedAt, timestamp))
            ?: Place(
                id = "place:${timestamp.toEpochMilli()}:${center.first}:${center.second}",
                name = null,
                latitude = center.first,
                longitude = center.second,
                createdAt = points.first().timestamp,
                updatedAt = timestamp
            )

        repository.savePlace(place)
        return place
    }

    private fun robustCenter(points: List<LocationPoint>): Pair<Double, Double> {
        val latitudes = points.map { it.latitude }.sorted()
        val longitudes = points.map { it.longitude }.sorted()
        return median(latitudes) to median(longitudes)
    }

    private fun median(values: List<Double>): Double {
        val middle = values.size / 2
        return if (values.size % 2 == 0) {
            (values[middle - 1] + values[middle]) / 2.0
        } else {
            values[middle]
        }
    }

    private fun mergeRadiusFor(points: List<LocationPoint>): Double {
        val accuracies = points.mapNotNull { it.accuracyMeters?.toDouble() }
        if (accuracies.isEmpty()) return DEFAULT_MERGE_RADIUS_METERS

        val medianAccuracy = median(accuracies)
        return (medianAccuracy * ACCURACY_RADIUS_MULTIPLIER)
            .coerceIn(MIN_MERGE_RADIUS_METERS, MAX_MERGE_RADIUS_METERS)
    }

    private companion object {
        const val MIN_MERGE_RADIUS_METERS = 75.0
        const val DEFAULT_MERGE_RADIUS_METERS = 100.0
        const val MAX_MERGE_RADIUS_METERS = 125.0
        const val ACCURACY_RADIUS_MULTIPLIER = 1.5
    }
}
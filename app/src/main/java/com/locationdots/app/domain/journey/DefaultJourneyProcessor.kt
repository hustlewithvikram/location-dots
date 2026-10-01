package com.locationdots.app.domain.journey

import com.locationdots.app.domain.model.LocationPoint
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.domain.places.PlaceEngine
import java.time.Duration
import kotlin.math.*

class DefaultJourneyProcessor(private val placeEngine: PlaceEngine) : JourneyProcessor {
    override suspend fun process(points: List<LocationPoint>): List<TimelineEvent> {
        val sorted = points.sortedBy { it.timestamp }
        if (sorted.size < 2) return emptyList()

        val clusters = buildClusters(sorted).filter {
            Duration.between(it.first().timestamp, it.last().timestamp) >= MIN_DWELL
        }
        if (clusters.isEmpty()) return emptyList()

        val visits = clusters.map { cluster ->
            val place = placeEngine.resolve(cluster)
            TimelineEvent.Visit(
                id = "visit:${place.id}:${cluster.first().timestamp.toEpochMilli()}",
                timestamp = cluster.first().timestamp,
                place = place,
                arrival = cluster.first().timestamp,
                departure = cluster.last().timestamp
            )
        }

        return buildList {
            visits.forEachIndexed { index, visit ->
                add(visit)
                val next = visits.getOrNull(index + 1) ?: return@forEachIndexed
                val start = visit.departure ?: visit.arrival
                if (next.arrival.isAfter(start)) {
                    add(TimelineEvent.Journey(
                        id = "journey:${visit.place.id}:${next.place.id}:$start",
                        timestamp = start,
                        startPlace = visit.place,
                        endPlace = next.place,
                        startedAt = start,
                        endedAt = next.arrival,
                        distanceMeters = distanceMeters(
                            visit.place.latitude, visit.place.longitude,
                            next.place.latitude, next.place.longitude
                        )
                    ))
                }
            }
        }.sortedByDescending { it.timestamp }
    }

    private fun buildClusters(points: List<LocationPoint>): List<List<LocationPoint>> {
        val clusters = mutableListOf<MutableList<LocationPoint>>()
        var current = mutableListOf<LocationPoint>()
        for (point in points) {
            if (current.isEmpty()) {
                current.add(point)
                continue
            }
            val center = centroid(current)
            if (distanceMeters(center.first, center.second, point.latitude, point.longitude) <= PLACE_RADIUS_METERS) {
                current.add(point)
            } else {
                clusters += current
                current = mutableListOf(point)
            }
        }
        if (current.isNotEmpty()) clusters += current
        return clusters
    }

    private fun centroid(points: List<LocationPoint>): Pair<Double, Double> =
        points.map { it.latitude }.average() to points.map { it.longitude }.average()

    private fun distanceMeters(latitude1: Double, longitude1: Double, latitude2: Double, longitude2: Double): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(latitude2 - latitude1)
        val dLon = Math.toRadians(longitude2 - longitude1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(latitude1)) * cos(Math.toRadians(latitude2)) * sin(dLon / 2).pow(2)
        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private companion object {
        val MIN_DWELL: Duration = Duration.ofMinutes(5)
        const val PLACE_RADIUS_METERS = 150.0
    }
}

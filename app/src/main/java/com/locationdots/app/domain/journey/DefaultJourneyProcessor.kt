package com.locationdots.app.domain.journey

import com.locationdots.app.domain.model.LocationPoint
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.domain.places.PlaceEngine
import java.time.Duration
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class DefaultJourneyProcessor(private val placeEngine: PlaceEngine) : JourneyProcessor {

    override suspend fun process(points: List<LocationPoint>): List<TimelineEvent> {
        val sorted = points
            .asSequence()
            .filter(::isUsablePoint)
            .sortedBy { it.timestamp }
            .toList()

        if (sorted.size < MIN_POINTS_PER_VISIT) return emptyList()

        val clusters = buildClusters(sorted)
            .filter { cluster ->
                cluster.size >= MIN_POINTS_PER_VISIT &&
                    Duration.between(cluster.first().timestamp, cluster.last().timestamp) >= MIN_DWELL
            }

        if (clusters.isEmpty()) return emptyList()

        val lastPoint = sorted.last()
        val visits = clusters.mapIndexed { index, cluster ->
            val isActive = index == clusters.lastIndex &&
                Duration.between(cluster.last().timestamp, lastPoint.timestamp) <= ACTIVE_VISIT_WINDOW

            val place = placeEngine.resolve(cluster)

            TimelineEvent.Visit(
                id = "visit:" + place.id + ":" + cluster.first().timestamp.toEpochMilli(),
                timestamp = cluster.first().timestamp,
                place = place,
                arrival = cluster.first().timestamp,
                departure = if (isActive) null else cluster.last().timestamp
            )
        }

        return buildList {
            visits.forEachIndexed { index, visit ->
                add(visit)

                val next = visits.getOrNull(index + 1) ?: return@forEachIndexed
                val journeyStart = visit.departure ?: visit.arrival

                if (next.arrival.isAfter(journeyStart)) {
                    add(
                        TimelineEvent.Journey(
                            id = "journey:" + visit.id + ":" + next.id,
                            timestamp = journeyStart,
                            startPlace = visit.place,
                            endPlace = next.place,
                            startedAt = journeyStart,
                            endedAt = next.arrival,
                            distanceMeters = distanceMeters(
                                visit.place.latitude,
                                visit.place.longitude,
                                next.place.latitude,
                                next.place.longitude
                            )
                        )
                    )
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
            val distance = distanceMeters(
                center.first,
                center.second,
                point.latitude,
                point.longitude
            )

            if (distance <= PLACE_RADIUS_METERS) {
                current.add(point)
            } else {
                clusters += current
                current = mutableListOf(point)
            }
        }

        if (current.isNotEmpty()) {
            clusters += current
        }

        return clusters
    }

    private fun isUsablePoint(point: LocationPoint): Boolean {
        if (point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0) {
            return false
        }

        val accuracy = point.accuracyMeters
        return accuracy == null || accuracy <= MAX_ACCURACY_METERS
    }

    private fun centroid(points: List<LocationPoint>): Pair<Double, Double> =
        points.map { it.latitude }.average() to points.map { it.longitude }.average()

    private fun distanceMeters(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(latitude2 - latitude1)
        val dLon = Math.toRadians(longitude2 - longitude1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latitude1)) *
            cos(Math.toRadians(latitude2)) *
            sin(dLon / 2) * sin(dLon / 2)

        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private companion object {
        val MIN_DWELL: Duration = Duration.ofMinutes(5)
        val ACTIVE_VISIT_WINDOW: Duration = Duration.ofMinutes(15)

        const val PLACE_RADIUS_METERS = 150.0
        const val MAX_ACCURACY_METERS = 100.0
        const val MIN_POINTS_PER_VISIT = 3
    }
}

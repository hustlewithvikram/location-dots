package com.locationdots.app.domain.journey

import com.locationdots.app.domain.model.JourneyMode
import com.locationdots.app.domain.model.LocationPoint
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.domain.places.PlaceEngine
import java.time.Duration
import java.time.Instant
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
            .filter { isConfirmedVisit(it.points) }

        if (clusters.isEmpty()) return emptyList()

        val visits = clusters.map { cluster ->
            val place = placeEngine.resolve(cluster.points)

            TimelineEvent.Visit(
                id = "visit:" + place.id + ":" + cluster.points.first().timestamp.toEpochMilli(),
                timestamp = cluster.points.first().timestamp,
                place = place,
                arrival = cluster.points.first().timestamp,
                departure = cluster.departureTimestamp
            )
        }

        return buildList {
            visits.forEachIndexed { index, visit ->
                add(visit)

                val next = visits.getOrNull(index + 1) ?: return@forEachIndexed
                val journeyStart = visit.departure ?: return@forEachIndexed

                if (next.arrival.isAfter(journeyStart)) {
                    val path = sorted.filter {
                        !it.timestamp.isBefore(journeyStart) &&
                            !it.timestamp.isAfter(next.arrival)
                    }

                    val distance = pathDistance(path)
                    if (distance < MIN_JOURNEY_DISTANCE_METERS) return@forEachIndexed

                    val durationSeconds = Duration.between(journeyStart, next.arrival).seconds
                    val mode = classifyMode(distance, durationSeconds)

                    add(
                        TimelineEvent.Journey(
                            id = "journey:" + visit.id + ":" + next.id,
                            timestamp = journeyStart,
                            startPlace = visit.place,
                            endPlace = next.place,
                            startedAt = journeyStart,
                            endedAt = next.arrival,
                            distanceMeters = distance,
                            mode = mode,
                            path = path
                        )
                    )
                }
            }
        }.sortedByDescending { it.timestamp }
    }

    private fun buildClusters(points: List<LocationPoint>): List<VisitCluster> {
        val clusters = mutableListOf<VisitCluster>()
        var current = mutableListOf<LocationPoint>()
        var departureCandidates = mutableListOf<LocationPoint>()
        var departureTimestamp: Instant? = null

        fun finishCurrent() {
            if (current.isNotEmpty()) {
                clusters += VisitCluster(
                    points = current,
                    departureTimestamp = departureTimestamp
                )
            }
            current = mutableListOf()
            departureCandidates = mutableListOf()
            departureTimestamp = null
        }

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

            when {
                distance <= STAY_RADIUS_METERS -> {
                    departureCandidates.clear()
                    departureTimestamp = null
                    current.add(point)
                }

                distance <= DEPARTURE_RADIUS_METERS -> {
                    departureCandidates.clear()
                }

                else -> {
                    if (!isConfirmedVisit(current)) {
                        // We are still in transit after a confirmed departure. Keep
                        // replacing the transit seed until points begin clustering.
                        current = mutableListOf(point)
                        departureCandidates.clear()
                        departureTimestamp = null
                    } else {
                        departureCandidates.add(point)

                        if (departureCandidates.size >= DEPARTURE_CONFIRMATION_POINTS) {
                            departureTimestamp = departureCandidates.first().timestamp
                            val newClusterStart = departureCandidates.last()
                            finishCurrent()
                            current.add(newClusterStart)
                        }
                    }
                }
            }
        }

        if (current.isNotEmpty()) {
            clusters += VisitCluster(
                points = current,
                departureTimestamp = departureTimestamp
            )
        }

        return clusters
    }

    private fun isConfirmedVisit(cluster: List<LocationPoint>): Boolean {
        if (cluster.size < MIN_POINTS_PER_VISIT) return false

        val dwell = Duration.between(cluster.first().timestamp, cluster.last().timestamp)
        if (dwell < MIN_DWELL) return false

        val transitions = cluster.zipWithNext()
        if (transitions.isEmpty()) return false

        val stationaryTransitions = transitions.count { (from, to) ->
            speedKmh(from, to) <= STATIONARY_MAX_KMH
        }

        return stationaryTransitions.toDouble() / transitions.size >= STATIONARY_RATIO
    }

    private fun speedKmh(from: LocationPoint, to: LocationPoint): Double {
        val seconds = Duration.between(from.timestamp, to.timestamp).toMillis() / 1000.0
        if (seconds <= 0.0) return Double.POSITIVE_INFINITY

        return distanceMeters(
            from.latitude,
            from.longitude,
            to.latitude,
            to.longitude
        ) / seconds * 3.6
    }

    private fun pathDistance(points: List<LocationPoint>): Double {
        if (points.size < 2) return 0.0

        return points.zipWithNext().sumOf { (from, to) ->
            distanceMeters(
                from.latitude,
                from.longitude,
                to.latitude,
                to.longitude
            )
        }
    }

    private fun classifyMode(distanceMeters: Double?, durationSeconds: Long): JourneyMode {
        if (distanceMeters == null || durationSeconds <= 0) return JourneyMode.UNKNOWN

        val speedKmh = distanceMeters / durationSeconds * 3.6

        return when {
            speedKmh <= WALKING_MAX_KMH -> JourneyMode.WALKING
            speedKmh <= CYCLING_MAX_KMH -> JourneyMode.CYCLING
            speedKmh >= VEHICLE_MIN_KMH -> JourneyMode.VEHICLE
            else -> JourneyMode.UNKNOWN
        }
    }

    private fun isUsablePoint(point: LocationPoint): Boolean {
        if (point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0) {
            return false
        }

        return point.accuracyMeters == null || point.accuracyMeters <= MAX_ACCURACY_METERS
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

    private data class VisitCluster(
        val points: List<LocationPoint>,
        val departureTimestamp: Instant?
    )

    private companion object {
        val MIN_DWELL: Duration = Duration.ofMinutes(8)

        const val STAY_RADIUS_METERS = 100.0
        const val DEPARTURE_RADIUS_METERS = 150.0
        const val DEPARTURE_CONFIRMATION_POINTS = 2
        const val MAX_ACCURACY_METERS = 100.0
        const val MIN_POINTS_PER_VISIT = 5
        const val STATIONARY_MAX_KMH = 3.0
        const val STATIONARY_RATIO = 0.70

        const val MIN_JOURNEY_DISTANCE_METERS = 100.0
        const val WALKING_MAX_KMH = 7.0
        const val CYCLING_MAX_KMH = 25.0
        const val VEHICLE_MIN_KMH = 35.0
    }
}

package com.locationdots.app.data.repository

import com.locationdots.app.domain.insights.DailyInsight
import com.locationdots.app.domain.insights.InsightsRepository
import com.locationdots.app.domain.insights.InsightsSnapshot
import com.locationdots.app.domain.insights.PlaceInsight
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.domain.timeline.TimelineRepository
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RoomInsightsRepository(
    private val timelineRepository: TimelineRepository
) : InsightsRepository {
    override suspend fun getSnapshot(): InsightsSnapshot {
        val events = timelineRepository.getAll()
        val visits = events.filterIsInstance<TimelineEvent.Visit>()
        val journeys = events.filterIsInstance<TimelineEvent.Journey>()

        val visitDurations = visits.mapNotNull { visit ->
            visit.departure?.let {
                Duration.between(visit.arrival, it).toMinutes().coerceAtLeast(0)
            }
        }
        val totalTime = visitDurations.sum()
        val totalDistance = journeys.sumOf { it.distanceMeters ?: 0.0 }

        val topPlaces = visits
            .groupBy { it.place.id }
            .map { (id, placeVisits) ->
                PlaceInsight(
                    placeId = id,
                    name = placeVisits.first().place.name ?: "Unnamed place",
                    visits = placeVisits.size,
                    timeMinutes = placeVisits.sumOf {
                        it.departure?.let { departure ->
                            Duration.between(it.arrival, departure).toMinutes().coerceAtLeast(0)
                        } ?: 0
                    }
                )
            }
            .sortedWith(compareByDescending<PlaceInsight> { it.visits }.thenByDescending { it.timeMinutes })
            .take(8)

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val formatter = DateTimeFormatter.ofPattern("EEE")
        val daily = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DailyInsight(
                label = date.format(formatter),
                visits = visits.count { it.timestamp.atZone(zone).toLocalDate() == date },
                journeys = journeys.count { it.timestamp.atZone(zone).toLocalDate() == date }
            )
        }
        val busiest = daily.maxByOrNull { it.visits }

        return InsightsSnapshot(
            totalPlaces = visits.map { it.place.id }.distinct().size,
            totalVisits = visits.size,
            totalTimeMinutes = totalTime,
            totalDistanceMeters = totalDistance,
            journeyCount = journeys.size,
            averageVisitMinutes = if (visitDurations.isEmpty()) 0 else totalTime / visitDurations.size,
            longestVisitMinutes = visitDurations.maxOrNull() ?: 0,
            busiestDayLabel = busiest?.takeIf { it.visits > 0 }?.label,
            busiestDayVisits = busiest?.visits ?: 0,
            modeBreakdown = journeys.groupingBy { it.mode }.eachCount(),
            topPlaces = topPlaces,
            dailyVisits = daily
        )
    }
}

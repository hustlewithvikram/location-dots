package com.locationdots.app.data.repository

import com.locationdots.app.data.local.TimelineEventDao
import com.locationdots.app.data.local.TimelineEventEntity
import com.locationdots.app.domain.model.JourneyMode
import com.locationdots.app.domain.model.LocationPoint
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.domain.places.PlaceRepository
import com.locationdots.app.domain.timeline.TimelineRepository
import java.time.Instant

class RoomTimelineRepository(
    private val dao: TimelineEventDao,
    private val placeRepository: PlaceRepository
) : TimelineRepository {

    override suspend fun getAll(): List<TimelineEvent> =
        dao.getAll().mapNotNull { toDomain(it) }

    override suspend fun getPage(limit: Int, offset: Int): List<TimelineEvent> =
        dao.getPage(limit, offset).map { toDomain(it) }.filterNotNull()

    override suspend fun getVisitsForPlace(placeId: String): List<TimelineEvent.Visit> =
        dao.getVisitsForPlace(placeId).mapNotNull { toDomain(it) as? TimelineEvent.Visit }

    override suspend fun replaceAll(events: List<TimelineEvent>) {
        dao.deleteAll()
        dao.insertAll(events.map(::toEntity))
    }

    override suspend fun replaceRange(from: Instant, to: Instant, events: List<TimelineEvent>) {
        dao.replaceRange(
            fromEpochMillis = from.toEpochMilli(),
            toEpochMillis = to.toEpochMilli(),
            events = events.map(::toEntity)
        )
    }

    private suspend fun toDomain(entity: TimelineEventEntity): TimelineEvent? =
        when (entity.type) {
            TYPE_VISIT -> {
                val place = entity.placeId?.let { placeRepository.getPlace(it) } ?: return null
                TimelineEvent.Visit(
                    id = entity.id,
                    timestamp = Instant.ofEpochMilli(entity.timestampEpochMillis),
                    place = place,
                    arrival = Instant.ofEpochMilli(entity.arrivalEpochMillis ?: entity.timestampEpochMillis),
                    departure = entity.departureEpochMillis?.let(Instant::ofEpochMilli)
                )
            }
            TYPE_JOURNEY -> TimelineEvent.Journey(
                id = entity.id,
                timestamp = Instant.ofEpochMilli(entity.timestampEpochMillis),
                startPlace = entity.startPlaceId?.let { placeRepository.getPlace(it) },
                endPlace = entity.endPlaceId?.let { placeRepository.getPlace(it) },
                startedAt = Instant.ofEpochMilli(entity.startedAtEpochMillis ?: entity.timestampEpochMillis),
                endedAt = entity.endedAtEpochMillis?.let(Instant::ofEpochMilli),
                distanceMeters = entity.distanceMeters,
                path = decodePath(entity.pathEncoded),
                mode = entity.journeyMode?.let {
                    runCatching { JourneyMode.valueOf(it) }.getOrDefault(JourneyMode.UNKNOWN)
                } ?: JourneyMode.UNKNOWN
            )
            else -> null
        }

    private fun toEntity(event: TimelineEvent): TimelineEventEntity =
        when (event) {
            is TimelineEvent.Visit -> TimelineEventEntity(
                id = event.id,
                type = TYPE_VISIT,
                timestampEpochMillis = event.timestamp.toEpochMilli(),
                placeId = event.place.id,
                arrivalEpochMillis = event.arrival.toEpochMilli(),
                departureEpochMillis = event.departure?.toEpochMilli(),
                startPlaceId = null,
                endPlaceId = null,
                startedAtEpochMillis = null,
                endedAtEpochMillis = null,
                distanceMeters = null,
                journeyMode = null,
                pathEncoded = null
            )
            is TimelineEvent.Journey -> TimelineEventEntity(
                id = event.id,
                type = TYPE_JOURNEY,
                timestampEpochMillis = event.timestamp.toEpochMilli(),
                placeId = null,
                arrivalEpochMillis = null,
                departureEpochMillis = null,
                startPlaceId = event.startPlace?.id,
                endPlaceId = event.endPlace?.id,
                startedAtEpochMillis = event.startedAt.toEpochMilli(),
                endedAtEpochMillis = event.endedAt?.toEpochMilli(),
                distanceMeters = event.distanceMeters,
                journeyMode = event.mode.name,
                pathEncoded = encodePath(event.path)
            )
        }

    private fun encodePath(path: List<LocationPoint>): String =
        path.joinToString(";") { point ->
            listOf(point.latitude, point.longitude, point.accuracyMeters ?: -1f, point.timestamp.toEpochMilli()).joinToString(",")
        }

    private fun decodePath(encoded: String?): List<LocationPoint> =
        encoded?.split(";")?.mapNotNull { value ->
            val parts = value.split(",")
            if (parts.size != 4) return@mapNotNull null
            runCatching {
                LocationPoint(
                    latitude = parts[0].toDouble(),
                    longitude = parts[1].toDouble(),
                    accuracyMeters = parts[2].toFloat().takeUnless { it < 0f },
                    timestamp = Instant.ofEpochMilli(parts[3].toLong())
                )
            }.getOrNull()
        }.orEmpty()
    private companion object {
        const val TYPE_VISIT = "visit"
        const val TYPE_JOURNEY = "journey"
    }
}

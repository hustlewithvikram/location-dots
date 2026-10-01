package com.locationdots.app.data.repository

import com.locationdots.app.data.local.TimelineEventDao
import com.locationdots.app.data.local.TimelineEventEntity
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.domain.model.Place
import com.locationdots.app.domain.places.PlaceRepository
import com.locationdots.app.domain.timeline.TimelineRepository
import java.time.Instant

class RoomTimelineRepository(
    private val dao: TimelineEventDao,
    private val placeRepository: PlaceRepository
) : TimelineRepository {

    override suspend fun getPage(limit: Int, offset: Int): List<TimelineEvent> {
        return dao.getPage(limit, offset).mapNotNull { entity ->
            when (entity.type) {
                TYPE_VISIT -> {
                    val place = entity.placeId?.let { placeRepository.getPlace(it) } ?: return@mapNotNull null
                    TimelineEvent.Visit(
                        id = entity.id,
                        timestamp = Instant.ofEpochMilli(entity.timestampEpochMillis),
                        place = place,
                        arrival = Instant.ofEpochMilli(entity.arrivalEpochMillis ?: entity.timestampEpochMillis),
                        departure = entity.departureEpochMillis?.let(Instant::ofEpochMilli)
                    )
                }
                TYPE_JOURNEY -> {
                    val startPlace = entity.startPlaceId?.let { placeRepository.getPlace(it) }
                    val endPlace = entity.endPlaceId?.let { placeRepository.getPlace(it) }
                    TimelineEvent.Journey(
                        id = entity.id,
                        timestamp = Instant.ofEpochMilli(entity.timestampEpochMillis),
                        startPlace = startPlace,
                        endPlace = endPlace,
                        startedAt = Instant.ofEpochMilli(entity.startedAtEpochMillis ?: entity.timestampEpochMillis),
                        endedAt = entity.endedAtEpochMillis?.let(Instant::ofEpochMilli),
                        distanceMeters = entity.distanceMeters
                    )
                }
                else -> null
            }
        }
    }

    override suspend fun replaceAll(events: List<TimelineEvent>) {
        dao.deleteAll()
        dao.insertAll(events.map(::toEntity))
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
                distanceMeters = null
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
                distanceMeters = event.distanceMeters
            )
        }

    private companion object {
        const val TYPE_VISIT = "visit"
        const val TYPE_JOURNEY = "journey"
    }
}

package com.locationdots.app.domain.journey

import com.locationdots.app.domain.model.LocationPoint
import com.locationdots.app.domain.model.Place
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.domain.places.PlaceRepository
import com.locationdots.app.domain.places.PlaceEngine
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultJourneyProcessorTest {

    private val placeRepository = object : PlaceRepository {
        private var place: Place? = null

        override fun observePlaces(): Flow<List<Place>> = emptyFlow()
        override suspend fun searchByName(query: String): List<Place> = emptyList()
        override suspend fun getPlace(id: String): Place? = place
        override suspend fun findNearby(latitude: Double, longitude: Double, radiusMeters: Double): Place? = place
        override suspend fun savePlace(place: Place) {
            this.place = place
        }
        override suspend fun updatePlaceName(id: String, name: String?) = Unit
        override suspend fun saveNamedPlace(name: String, latitude: Double, longitude: Double): Place =
            error("Not used by this test")
    }

    private val processor = DefaultJourneyProcessor(PlaceEngine(placeRepository))

    @Test
    fun stationaryStayMustReachEightMinutes() = runBlocking {
        val start = Instant.parse("2026-10-02T10:00:00Z")
        val points = points(start, count = 17, secondsBetween = 30, latitudeStep = 0.00002)

        val events = processor.process(points)

        val visit = events.filterIsInstance<TimelineEvent.Visit>().singleOrNull()
        assertTrue(visit != null)
        assertEquals(start, visit.arrival)
    }

    @Test
    fun stayShorterThanEightMinutesIsNotAVisit() = runBlocking {
        val start = Instant.parse("2026-10-02T10:00:00Z")
        val points = points(start, count = 15, secondsBetween = 30, latitudeStep = 0.00002)

        val events = processor.process(points)

        assertTrue(events.none { it is TimelineEvent.Visit })
    }

    @Test
    fun movingVehicleStopDoesNotBecomeVisit() = runBlocking {
        val start = Instant.parse("2026-10-02T10:00:00Z")
        val points = buildList {
            repeat(8) { index ->
                add(
                    LocationPoint(
                        latitude = 18.5200 + index * 0.0010,
                        longitude = 73.8567,
                        accuracyMeters = 20f,
                        timestamp = start.plusSeconds(index * 60L)
                    )
                )
            }
            repeat(9) { index ->
                add(
                    LocationPoint(
                        latitude = 18.5280,
                        longitude = 73.8567,
                        accuracyMeters = 20f,
                        timestamp = start.plusSeconds((8 + index) * 60L)
                    )
                )
            }
        }

        val events = processor.process(points)

        assertTrue(events.none { it is TimelineEvent.Visit })
    }

    @Test
    fun poorAccuracyPointsAreIgnoredForVisitConfirmation() = runBlocking {
        val start = Instant.parse("2026-10-02T10:00:00Z")
        val points = points(start, count = 17, secondsBetween = 30, latitudeStep = 0.00002)
            .mapIndexed { index, point ->
                if (index >= 14) point.copy(accuracyMeters = 150f) else point
            }

        val events = processor.process(points)

        assertTrue(events.none { it is TimelineEvent.Visit })
    }

    private fun points(
        start: Instant,
        count: Int,
        secondsBetween: Long,
        latitudeStep: Double
    ): List<LocationPoint> =
        (0 until count).map { index ->
            LocationPoint(
                latitude = 18.5200 + index * latitudeStep,
                longitude = 73.8567,
                accuracyMeters = 20f,
                timestamp = start.plusSeconds(index * secondsBetween)
            )
        }
}

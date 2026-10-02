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
            repeat(7) { index ->
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
    fun stableGpsNoiseStillCreatesVisit() = runBlocking {
        val start = Instant.parse("2026-10-02T10:00:00Z")
        val offsets = listOf(0.00000, 0.00020, -0.00015, 0.00018, -0.00010, 0.00012, -0.00008, 0.00010, -0.00005, 0.00006, 0.00000, 0.00004, -0.00003, 0.00002, 0.00000, 0.00001, 0.00000)
        val points = offsets.mapIndexed { index, offset ->
            LocationPoint(
                latitude = 18.5200 + offset,
                longitude = 73.8567,
                accuracyMeters = 25f,
                timestamp = start.plusSeconds(index * 30L)
            )
        }

        val events = processor.process(points)

        assertTrue(events.any { it is TimelineEvent.Visit })
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

    @Test
    fun singleGpsOutlierDoesNotCloseVisit() = runBlocking {
        val start = Instant.parse("2026-10-02T10:00:00Z")
        val stable = points(start, count = 17, secondsBetween = 30, latitudeStep = 0.00002)
        val outlier = LocationPoint(
            latitude = 18.5230,
            longitude = 73.8567,
            accuracyMeters = 20f,
            timestamp = start.plusSeconds(17 * 30L)
        )
        val resumed = LocationPoint(
            latitude = 18.5201,
            longitude = 73.8567,
            accuracyMeters = 20f,
            timestamp = start.plusSeconds(18 * 30L)
        )

        val events = processor.process(stable + outlier + resumed)

        val visit = events.filterIsInstance<TimelineEvent.Visit>().single()
        assertEquals(null, visit.departure)
    }

    @Test
    fun sustainedDepartureClosesVisitAtFirstConfirmedOutsidePoint() = runBlocking {
        val start = Instant.parse("2026-10-02T10:00:00Z")
        val firstStay = points(start, count = 17, secondsBetween = 30, latitudeStep = 0.00002)
        val departureOne = LocationPoint(
            latitude = 18.5230,
            longitude = 73.8567,
            accuracyMeters = 20f,
            timestamp = start.plusSeconds(17 * 30L)
        )
        val departureTwo = LocationPoint(
            latitude = 18.5240,
            longitude = 73.8567,
            accuracyMeters = 20f,
            timestamp = start.plusSeconds(18 * 30L)
        )
        val secondStay = points(
            departureTwo.timestamp.plusSeconds(30),
            count = 17,
            secondsBetween = 30,
            latitudeStep = 0.00002
        ).map { it.copy(latitude = 18.5400 + (it.latitude - 18.5200)) }

        val events = processor.process(firstStay + departureOne + departureTwo + secondStay)

        val visits = events.filterIsInstance<TimelineEvent.Visit>()
        val journey = events.filterIsInstance<TimelineEvent.Journey>().single()

        assertEquals(2, visits.size)
        assertEquals(departureOne.timestamp, visits.first { it.arrival == start }.departure)
        assertEquals(departureOne.timestamp, journey.startedAt)
        assertEquals(secondStay.first().timestamp, journey.endedAt)
    }

    @Test
    fun departureIsCancelledWhenUserReturnsBeforeConfirmation() = runBlocking {
        val start = Instant.parse("2026-10-02T10:00:00Z")
        val stable = points(start, count = 17, secondsBetween = 30, latitudeStep = 0.00002)
        val outside = LocationPoint(
            latitude = 18.5230,
            longitude = 73.8567,
            accuracyMeters = 20f,
            timestamp = start.plusSeconds(17 * 30L)
        )
        val returned = LocationPoint(
            latitude = 18.5201,
            longitude = 73.8567,
            accuracyMeters = 20f,
            timestamp = start.plusSeconds(18 * 30L)
        )

        val visit = processor.process(stable + outside + returned)
            .filterIsInstance<TimelineEvent.Visit>()
            .single()

        assertEquals(null, visit.departure)
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

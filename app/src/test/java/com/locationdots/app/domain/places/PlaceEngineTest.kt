package com.locationdots.app.domain.places

import com.locationdots.app.domain.model.LocationPoint
import com.locationdots.app.domain.model.Place
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking

class PlaceEngineTest {

    private class FakeRepository : PlaceRepository {
        var place: Place? = null
        var lastRadiusMeters: Double? = null

        override fun observePlaces(): Flow<List<Place>> = emptyFlow()
        override suspend fun searchByName(query: String): List<Place> = emptyList()
        override suspend fun getPlace(id: String): Place? = place

        override suspend fun findNearby(
            latitude: Double,
            longitude: Double,
            radiusMeters: Double
        ): Place? {
            lastRadiusMeters = radiusMeters
            val existing = place ?: return null
            return existing.takeIf {
                distanceMeters(latitude, longitude, it.latitude, it.longitude) <= radiusMeters
            }
        }

        override suspend fun savePlace(place: Place) {
            this.place = place
        }

        override suspend fun updatePlaceName(id: String, name: String?) = Unit

        override suspend fun saveNamedPlace(
            name: String,
            latitude: Double,
            longitude: Double
        ): Place = error("Not used by this test")
    }

    private fun distanceMeters(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(latitude2 - latitude1)
        val dLon = Math.toRadians(longitude2 - longitude1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(latitude1)) *
            kotlin.math.cos(Math.toRadians(latitude2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        return earthRadius * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }

    @Test
    fun spreadOfVisitCanExpandMergeRadiusWithoutExceedingSafetyCap() = runBlocking {
        val repository = FakeRepository()
        repository.place = Place(
            id = "place:existing",
            name = "Home",
            latitude = 18.5200,
            longitude = 73.8567,
            createdAt = Instant.parse("2026-10-01T10:00:00Z"),
            updatedAt = Instant.parse("2026-10-01T10:00:00Z")
        )

        val engine = PlaceEngine(repository)
        val start = Instant.parse("2026-10-02T10:00:00Z")
        val points = listOf(
            point(18.5200, 73.8567, start),
            point(18.5206, 73.8567, start.plusSeconds(30)),
            point(18.5194, 73.8567, start.plusSeconds(60))
        )

        val resolved = engine.resolve(points)

        assertEquals("place:existing", resolved.id)
        assertTrue(repository.lastRadiusMeters!! in 96.6..96.8)
    }

    @Test
    fun mergeRadiusIsCappedForPoorAccuracy() = runBlocking {
        val repository = FakeRepository()
        val engine = PlaceEngine(repository)
        val start = Instant.parse("2026-10-02T10:00:00Z")
        val points = listOf(
            point(18.5200, 73.8567, start, accuracy = 100f),
            point(18.5200, 73.8567, start.plusSeconds(30), accuracy = 100f),
            point(18.5200, 73.8567, start.plusSeconds(60), accuracy = 100f)
        )

        engine.resolve(points)

        assertEquals(125.0, repository.lastRadiusMeters)
    }

    @Test
    fun distinctPlaceOutsideConfidenceRadiusIsNotMerged() = runBlocking {
        val repository = FakeRepository()
        repository.place = Place(
            id = "place:existing",
            name = "Existing",
            latitude = 18.52117,
            longitude = 73.8567,
            createdAt = Instant.parse("2026-10-01T10:00:00Z"),
            updatedAt = Instant.parse("2026-10-01T10:00:00Z")
        )

        val engine = PlaceEngine(repository)
        val start = Instant.parse("2026-10-02T10:00:00Z")
        val resolved = engine.resolve(
            listOf(
                point(18.5200, 73.8567, start),
                point(18.5200, 73.8567, start.plusSeconds(30)),
                point(18.5200, 73.8567, start.plusSeconds(60))
            )
        )

        assertTrue(resolved.id != "place:existing")
        assertEquals(125.0, repository.lastRadiusMeters)
    }

    @Test
    fun existingPlaceIdentityAndNameArePreservedWhenMatched() = runBlocking {
        val repository = FakeRepository()
        repository.place = Place(
            id = "place:existing",
            name = "Home",
            latitude = 18.5200,
            longitude = 73.8567,
            createdAt = Instant.parse("2026-10-01T10:00:00Z"),
            updatedAt = Instant.parse("2026-10-01T10:00:00Z")
        )

        val engine = PlaceEngine(repository)
        val start = Instant.parse("2026-10-02T10:00:00Z")
        val resolved = engine.resolve(
            listOf(
                point(18.5201, 73.8567, start),
                point(18.5201, 73.8567, start.plusSeconds(30)),
                point(18.5201, 73.8567, start.plusSeconds(60))
            )
        )

        assertEquals("place:existing", resolved.id)
        assertEquals("Home", resolved.name)
    }

    private fun point(
        latitude: Double,
        longitude: Double,
        timestamp: Instant,
        accuracy: Float = 20f
    ) = LocationPoint(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        timestamp = timestamp
    )
}

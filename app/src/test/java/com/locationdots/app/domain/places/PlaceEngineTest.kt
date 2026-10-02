package com.locationdots.app.domain.places

import com.locationdots.app.domain.model.LocationPoint
import com.locationdots.app.domain.model.Place
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
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
            return place
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
        assertEquals(96.0, repository.lastRadiusMeters)
    }

    @Test
    fun mergeRadiusIsCappedForVeryWideVisitCluster() = runBlocking {
        val repository = FakeRepository()
        val engine = PlaceEngine(repository)
        val start = Instant.parse("2026-10-02T10:00:00Z")
        val points = listOf(
            point(18.5200, 73.8567, start),
            point(18.5220, 73.8567, start.plusSeconds(30)),
            point(18.5180, 73.8567, start.plusSeconds(60))
        )

        engine.resolve(points)

        assertEquals(125.0, repository.lastRadiusMeters)
    }

    private fun point(
        latitude: Double,
        longitude: Double,
        timestamp: Instant
    ) = LocationPoint(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = 20f,
        timestamp = timestamp
    )
}

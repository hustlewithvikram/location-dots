package com.locationdots.app

import android.app.Application
import com.google.android.gms.location.LocationServices
import com.locationdots.app.core.location.FusedLocationProvider
import com.locationdots.app.data.local.LocationDotsDatabase
import com.locationdots.app.data.repository.RoomInsightsRepository
import com.locationdots.app.data.repository.RoomLocationRepository
import com.locationdots.app.data.repository.RoomPlaceRepository
import com.locationdots.app.data.repository.RoomTimelineRepository
import com.locationdots.app.data.repository.RoomSearchRepository
import com.locationdots.app.domain.search.SearchRepository
import com.locationdots.app.domain.insights.InsightsRepository
import com.locationdots.app.domain.journey.DefaultJourneyProcessor
import com.locationdots.app.domain.journey.JourneyProcessor
import com.locationdots.app.domain.location.LocationRepository
import com.locationdots.app.domain.places.PlaceEngine
import com.locationdots.app.domain.places.PlaceRepository
import com.locationdots.app.domain.timeline.TimelineRepository

class LocationDotsApplication : Application() {
    val database: LocationDotsDatabase by lazy { LocationDotsDatabase.create(this) }

    val locationRepository: LocationRepository by lazy {
        RoomLocationRepository(database.locationDao())
    }

    val placeRepository: PlaceRepository by lazy {
        RoomPlaceRepository(database.placeDao())
    }

    val locationProvider by lazy {
        FusedLocationProvider(
            context = this,
            client = LocationServices.getFusedLocationProviderClient(this)
        )
    }

    val placeEngine: PlaceEngine by lazy { PlaceEngine(placeRepository) }

    val journeyProcessor: JourneyProcessor by lazy {
        DefaultJourneyProcessor(placeEngine)
    }

    val timelineRepository: TimelineRepository by lazy {
        RoomTimelineRepository(database.timelineEventDao(), placeRepository)
    }

    val searchRepository: SearchRepository by lazy {
        RoomSearchRepository(placeRepository, timelineRepository)
    }

    val insightsRepository: InsightsRepository by lazy {
        RoomInsightsRepository(
            timelineRepository = timelineRepository
        )
    }
}
package com.vikram.locationdots

import android.app.Application
import com.google.android.gms.location.LocationServices
import com.vikram.locationdots.core.location.FusedLocationProvider
import com.vikram.locationdots.data.local.LocationDotsDatabase
import com.vikram.locationdots.data.repository.RoomInsightsRepository
import com.vikram.locationdots.data.repository.RoomLocationRepository
import com.vikram.locationdots.data.repository.RoomPlaceRepository
import com.vikram.locationdots.data.repository.RoomSearchRepository
import com.vikram.locationdots.data.repository.RoomTimelineRepository
import com.vikram.locationdots.domain.insights.InsightsRepository
import com.vikram.locationdots.domain.journey.DefaultJourneyProcessor
import com.vikram.locationdots.domain.journey.JourneyProcessor
import com.vikram.locationdots.domain.location.LocationRepository
import com.vikram.locationdots.domain.places.PlaceEngine
import com.vikram.locationdots.domain.places.PlaceRepository
import com.vikram.locationdots.domain.search.SearchRepository
import com.vikram.locationdots.domain.timeline.TimelineRepository
import com.vikram.locationdots.logging.CrashLogger

class LocationDotsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }

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
        DefaultJourneyProcessor(
            placeEngine = placeEngine,
            preferences = getSharedPreferences("location_dots_ui", MODE_PRIVATE)
        )
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

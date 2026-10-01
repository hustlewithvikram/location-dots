package com.locationdots.app

import android.app.Application
import com.google.android.gms.location.LocationServices
import com.locationdots.app.core.location.FusedLocationProvider
import com.locationdots.app.data.local.LocationDotsDatabase
import com.locationdots.app.data.repository.RoomInsightsRepository
import com.locationdots.app.data.repository.RoomLocationRepository
import com.locationdots.app.domain.insights.InsightsRepository
import com.locationdots.app.domain.journey.DefaultJourneyProcessor
import com.locationdots.app.domain.journey.JourneyProcessor
import com.locationdots.app.domain.location.LocationRepository

class LocationDotsApplication : Application() {
    val database: LocationDotsDatabase by lazy { LocationDotsDatabase.create(this) }

    val locationRepository: LocationRepository by lazy {
        RoomLocationRepository(database.locationDao())
    }

    val locationProvider by lazy {
        FusedLocationProvider(
            context = this,
            client = LocationServices.getFusedLocationProviderClient(this)
        )
    }

    val journeyProcessor: JourneyProcessor by lazy { DefaultJourneyProcessor() }

    val insightsRepository: InsightsRepository by lazy {
        RoomInsightsRepository(
            locationRepository = locationRepository as RoomLocationRepository,
            journeyProcessor = journeyProcessor
        )
    }
}

package com.locationdots.app

import android.app.Application
import com.google.android.gms.location.LocationServices
import com.locationdots.app.core.location.FusedLocationProvider
import com.locationdots.app.data.local.LocationDotsDatabase
import com.locationdots.app.data.repository.RoomLocationRepository
import com.locationdots.app.domain.location.LocationRepository

class LocationDotsApplication : Application() {

    val database: LocationDotsDatabase by lazy {
        LocationDotsDatabase.create(this)
    }

    val locationRepository: LocationRepository by lazy {
        RoomLocationRepository(database.locationDao())
    }

    val locationProvider by lazy {
        FusedLocationProvider(
            context = this,
            client = LocationServices.getFusedLocationProviderClient(this)
        )
    }
}

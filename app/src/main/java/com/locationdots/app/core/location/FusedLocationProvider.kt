package com.locationdots.app.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.locationdots.app.domain.model.LocationPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.Instant

class FusedLocationProvider(
    private val context: Context,
    private val client: FusedLocationProviderClient
) : LocationProvider {

    private val _locations = MutableSharedFlow<LocationPoint>(
        replay = 0,
        extraBufferCapacity = 32
    )

    override val locations: Flow<LocationPoint> = _locations.asSharedFlow()

    private val request = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        UPDATE_INTERVAL_MILLIS
    )
        .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MILLIS)
        .setWaitForAccurateLocation(false)
        .build()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                _locations.tryEmit(
                    LocationPoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyMeters = location.accuracy,
                        timestamp = Instant.ofEpochMilli(location.time)
                    )
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun start() {
        if (!hasLocationPermission()) return

        client.requestLocationUpdates(request, callback, context.mainLooper)
    }

    override fun stop() {
        client.removeLocationUpdates(callback)
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val UPDATE_INTERVAL_MILLIS = 30_000L
        const val MIN_UPDATE_INTERVAL_MILLIS = 10_000L
    }
}

package com.vikram.locationdots.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.vikram.locationdots.domain.model.LocationPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.Instant

class FusedLocationProvider(
    private val context: Context,
    private val client: FusedLocationProviderClient
) : LocationProvider {

    // Location points are persisted and used to reconstruct visits and journeys.
    // An unbounded channel avoids silently discarding older fixes while the database
    // and journey processor are temporarily busy. Processing remains serialized by
    // the service collector.
    private val locationChannel = Channel<LocationPoint>(Channel.UNLIMITED)

    override val locations: Flow<LocationPoint> = locationChannel.receiveAsFlow()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                locationChannel.trySend(
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
        client.removeLocationUpdates(callback)

        val preferences = context.getSharedPreferences("location_dots_ui", Context.MODE_PRIVATE)
        val interval = preferences.getLong("tracking_interval_millis", DEFAULT_UPDATE_INTERVAL_MILLIS)
            .coerceIn(MIN_UPDATE_INTERVAL_MILLIS, MAX_UPDATE_INTERVAL_MILLIS)
        val accuracy = preferences.getString("tracking_accuracy", "HIGH")
        val priority = if (accuracy == "BALANCED") {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        } else {
            Priority.PRIORITY_HIGH_ACCURACY
        }

        val request = LocationRequest.Builder(priority, interval)
            .setMinUpdateIntervalMillis((interval / 2).coerceAtLeast(MIN_UPDATE_INTERVAL_MILLIS))
            .setMaxUpdateDelayMillis(interval)
            .setWaitForAccurateLocation(accuracy != "BALANCED")
            .build()

        client.requestLocationUpdates(request, callback, context.mainLooper)
            .addOnSuccessListener {
                preferences.edit()
                    .remove(KEY_LOCATION_ERROR)
                    .putLong(KEY_LOCATION_REQUESTED_AT, System.currentTimeMillis())
                    .apply()
            }
            .addOnFailureListener { error ->
                preferences.edit()
                    .putString(KEY_LOCATION_ERROR, error.localizedMessage ?: "Unable to start location updates")
                    .apply()
                android.util.Log.e(TAG, "Unable to start location updates", error)
            }

        // Request a one-shot fix as soon as tracking starts instead of waiting for the
        // periodic callback. Fall back to the cached fix while GPS warms up.
        val cancellation = CancellationTokenSource()
        client.getCurrentLocation(
            CurrentLocationRequest.Builder()
                .setPriority(priority)
                .setMaxUpdateAgeMillis(MAX_CACHED_LOCATION_AGE_MILLIS)
                .build(),
            cancellation.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                emitLocation(location)
                preferences.edit().remove(KEY_LOCATION_ERROR).apply()
            } else {
                client.lastLocation.addOnSuccessListener { cached ->
                    if (cached != null) emitLocation(cached)
                }
            }
        }.addOnFailureListener { error ->
            preferences.edit()
                .putString(KEY_LOCATION_ERROR, error.localizedMessage ?: "Current location request failed")
                .apply()
            android.util.Log.w(TAG, "Current location request failed; waiting for regular updates", error)
            client.lastLocation.addOnSuccessListener { cached ->
                if (cached != null) emitLocation(cached)
            }
        }
    }

    private fun emitLocation(location: android.location.Location) {
        locationChannel.trySend(
            LocationPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy,
                timestamp = Instant.ofEpochMilli(location.time)
            )
        )
    }

    override fun stop() {
        client.removeLocationUpdates(callback)
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val TAG = "FusedLocationProvider"
        const val KEY_LOCATION_ERROR = "tracking_location_error"
        const val KEY_LOCATION_REQUESTED_AT = "tracking_location_requested_at"
        const val KEY_LAST_LOCATION_FIX_AT = "tracking_last_fix_at"
        const val KEY_LAST_LOCATION_ACCURACY = "tracking_last_fix_accuracy"
        const val MAX_CACHED_LOCATION_AGE_MILLIS = 120_000L
        const val DEFAULT_UPDATE_INTERVAL_MILLIS = 30_000L
        const val MIN_UPDATE_INTERVAL_MILLIS = 5_000L
        const val MAX_UPDATE_INTERVAL_MILLIS = 15 * 60_000L
    }
}

package com.vikram.locationdots.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.vikram.locationdots.domain.model.LocationPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.Instant

class FusedLocationProvider(
    private val context: Context,
    private val client: FusedLocationProviderClient
) : LocationProvider {

    // A bounded queue prevents an extended processing backlog from growing without limit.
    // Location callbacks are frequent enough that the newest samples matter most when the
    // consumer is temporarily behind; capacity is deliberately generous for short bursts.
    private val locationChannel = Channel<LocationPoint>(LOCATION_QUEUE_CAPACITY)
    override val locations: Flow<LocationPoint> = locationChannel.receiveAsFlow()

    private var baseIntervalMillis = DEFAULT_UPDATE_INTERVAL_MILLIS
    private var configuredAccuracy = "HIGH"
    private var moving = true
    private var stationarySamples = 0
    private var previousLocation: Location? = null
    private var requestConfigured = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                updateMovementState(location)
                locationChannel.trySend(location.toPoint())
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun start() {
        if (!hasLocationPermission()) return
        client.removeLocationUpdates(callback)

        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        baseIntervalMillis = preferences.getLong(KEY_TRACKING_INTERVAL, DEFAULT_UPDATE_INTERVAL_MILLIS)
            .coerceIn(MIN_UPDATE_INTERVAL_MILLIS, MAX_UPDATE_INTERVAL_MILLIS)
        configuredAccuracy = preferences.getString(KEY_TRACKING_ACCURACY, "HIGH") ?: "HIGH"
        moving = true
        stationarySamples = 0
        previousLocation = null
        requestConfigured = false
        configureLocationUpdates()

        // Get an initial fix immediately rather than waiting for the periodic stream.
        val priority = if (configuredAccuracy == "BALANCED") {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        } else {
            Priority.PRIORITY_HIGH_ACCURACY
        }
        val cancellation = CancellationTokenSource()
        client.getCurrentLocation(
            CurrentLocationRequest.Builder()
                .setPriority(priority)
                .setMaxUpdateAgeMillis(MAX_CACHED_LOCATION_AGE_MILLIS)
                .build(),
            cancellation.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                updateMovementState(location)
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

    private fun updateMovementState(location: Location) {
        val previous = previousLocation
        previousLocation = location
        if (previous == null || location.accuracy > MAX_MOVEMENT_CLASSIFICATION_ACCURACY_METERS) return

        val distanceMeters = previous.distanceTo(location)
        val speedMetersPerSecond = if (location.hasSpeed()) location.speed else 0f
        val looksMoving = distanceMeters >= MOVEMENT_DISTANCE_METERS ||
            speedMetersPerSecond >= MOVEMENT_SPEED_METERS_PER_SECOND

        val stateChanged = if (looksMoving) {
            stationarySamples = 0
            val changed = !moving
            moving = true
            changed
        } else if (distanceMeters <= STATIONARY_DISTANCE_METERS &&
            speedMetersPerSecond <= STATIONARY_SPEED_METERS_PER_SECOND
        ) {
            stationarySamples++
            if (moving && stationarySamples >= REQUIRED_STATIONARY_SAMPLES) {
                moving = false
                true
            } else {
                false
            }
        } else {
            stationarySamples = 0
            false
        }

        if (stateChanged && requestConfigured) configureLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun configureLocationUpdates() {
        if (!hasLocationPermission()) return
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val balancedOnly = configuredAccuracy == "BALANCED"
        val interval = if (moving) {
            minOf(baseIntervalMillis, MOVING_UPDATE_INTERVAL_MILLIS)
        } else {
            maxOf(baseIntervalMillis, STATIONARY_UPDATE_INTERVAL_MILLIS)
        }.coerceIn(MIN_UPDATE_INTERVAL_MILLIS, MAX_UPDATE_INTERVAL_MILLIS)
        val priority = if (balancedOnly || !moving) {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        } else {
            Priority.PRIORITY_HIGH_ACCURACY
        }
        val request = LocationRequest.Builder(priority, interval)
            .setMinUpdateIntervalMillis((interval / 2).coerceAtLeast(MIN_UPDATE_INTERVAL_MILLIS))
            .setMaxUpdateDelayMillis(interval)
            .setWaitForAccurateLocation(moving && !balancedOnly)
            .build()

        client.removeLocationUpdates(callback)
        client.requestLocationUpdates(request, callback, context.mainLooper)
            .addOnSuccessListener {
                requestConfigured = true
                preferences.edit()
                    .remove(KEY_LOCATION_ERROR)
                    .putLong(KEY_LOCATION_REQUESTED_AT, System.currentTimeMillis())
                    .putString(KEY_TRACKING_STATE, if (moving) STATE_MOVING else STATE_STATIONARY)
                    .putLong(KEY_EFFECTIVE_INTERVAL, interval)
                    .apply()
            }
            .addOnFailureListener { error ->
                requestConfigured = false
                preferences.edit()
                    .putString(KEY_LOCATION_ERROR, error.localizedMessage ?: "Unable to start location updates")
                    .apply()
                android.util.Log.e(TAG, "Unable to start location updates", error)
            }
    }

    private fun emitLocation(location: Location) {
        locationChannel.trySend(location.toPoint())
    }

    private fun Location.toPoint() = LocationPoint(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        timestamp = Instant.ofEpochMilli(time)
    )

    override fun stop() {
        requestConfigured = false
        previousLocation = null
        stationarySamples = 0
        client.removeLocationUpdates(callback)
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val TAG = "FusedLocationProvider"
        const val PREFERENCES_NAME = "location_dots_ui"
        const val KEY_LOCATION_ERROR = "tracking_location_error"
        const val KEY_LOCATION_REQUESTED_AT = "tracking_location_requested_at"
        const val KEY_TRACKING_INTERVAL = "tracking_interval_millis"
        const val KEY_TRACKING_ACCURACY = "tracking_accuracy"
        const val KEY_TRACKING_STATE = "tracking_adaptive_state"
        const val KEY_EFFECTIVE_INTERVAL = "tracking_effective_interval_millis"
        const val MAX_CACHED_LOCATION_AGE_MILLIS = 120_000L
        const val DEFAULT_UPDATE_INTERVAL_MILLIS = 30_000L
        const val MIN_UPDATE_INTERVAL_MILLIS = 5_000L
        const val MAX_UPDATE_INTERVAL_MILLIS = 15 * 60_000L
        const val MOVING_UPDATE_INTERVAL_MILLIS = 20_000L
        const val STATIONARY_UPDATE_INTERVAL_MILLIS = 120_000L
        const val MOVEMENT_DISTANCE_METERS = 75f
        const val STATIONARY_DISTANCE_METERS = 25f
        const val MOVEMENT_SPEED_METERS_PER_SECOND = 2.0f
        const val STATIONARY_SPEED_METERS_PER_SECOND = 0.8f
        const val MAX_MOVEMENT_CLASSIFICATION_ACCURACY_METERS = 60f
        const val REQUIRED_STATIONARY_SAMPLES = 3
        const val LOCATION_QUEUE_CAPACITY = 128
        const val STATE_MOVING = "MOVING"
        const val STATE_STATIONARY = "STATIONARY"
    }
}

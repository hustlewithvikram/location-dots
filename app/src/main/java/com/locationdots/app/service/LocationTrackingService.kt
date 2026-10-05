package com.locationdots.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.location.LocationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.locationdots.app.LocationDotsApplication
import com.locationdots.app.R
import com.locationdots.app.domain.model.TimelineEvent
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LocationTrackingService : LifecycleService() {

    private var collectionJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        if (!hasLocationPermission()) {
            markTrackingStopped()
            stopSelf()
            return
        }

        val app = application as LocationDotsApplication

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
        markTrackingStarted()

        collectionJob = lifecycleScope.launch {
            app.locationProvider.locations.collect { point ->
                runCatching {
                    app.locationRepository.saveLocationPoint(point)
                    getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit()
                        .putLong("last_location_timestamp", point.timestamp.toEpochMilli())
                        .apply()
                    point.accuracyMeters?.let { accuracy ->
                        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit()
                            .putFloat("last_location_accuracy", accuracy.toFloat())
                            .apply()
                    }

                    val to = point.timestamp
                    val from = to.minus(PROCESSING_WINDOW)
                    val activeVisit = app.timelineRepository.getActiveVisit()
                    val processingFrom = minOf(from, activeVisit?.arrival ?: from)
                    val contextFrom = if (activeVisit != null) {
                        processingFrom
                    } else {
                        processingFrom.minus(PROCESSING_CONTEXT)
                    }
                    val points = app.locationRepository.getLocationPoints(contextFrom, to)
                    val events = app.journeyProcessor.process(points)
                        .filter { it.overlaps(processingFrom, to) }

                    app.timelineRepository.replaceRange(processingFrom, to, events)
                }.onFailure { error ->
                    Log.e(TAG, "Failed to process location update; continuing tracking.", error)
                }
            }
        }

        app.locationProvider.start()
    }

    private fun TimelineEvent.overlaps(from: Instant, to: Instant): Boolean =
        when (this) {
            is TimelineEvent.Visit ->
                timestamp <= to && (departure ?: timestamp) >= from
            is TimelineEvent.Journey ->
                startedAt <= to && (endedAt ?: startedAt) >= from
        }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!hasLocationPermission() || !isLocationEnabled()) {
            markTrackingStopped()
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        collectionJob?.cancel()
        (application as LocationDotsApplication).locationProvider.stop()
        markTrackingStopped()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_location_dots)
            .setContentTitle("Location Dots")
            .setContentText("Location tracking is active")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Location tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Location Dots is recording your location."
            }
        )
    }

    private fun isLocationEnabled(): Boolean =
        getSystemService(LocationManager::class.java)?.let { manager ->
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } ?: false

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun markTrackingStarted() {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit()
            .putBoolean(com.locationdots.app.core.location.LocationTrackingController.KEY_TRACKING_ACTIVE, true)
            .apply()
    }

    private fun markTrackingStopped() {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit()
            .putBoolean(com.locationdots.app.core.location.LocationTrackingController.KEY_TRACKING_ACTIVE, false)
            .apply()
    }

    private companion object {
        const val TAG = "LocationTrackingService"
        const val CHANNEL_ID = "location_tracking"
        const val PREFERENCES_NAME = "location_dots_ui"
        const val NOTIFICATION_ID = 1001
        val PROCESSING_WINDOW: Duration = Duration.ofHours(48)
        val PROCESSING_CONTEXT: Duration = Duration.ofHours(2)
    }
}

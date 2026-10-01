package com.locationdots.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.locationdots.app.LocationDotsApplication
import com.locationdots.app.R
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
            stopSelf()
            return
        }

        val app = application as LocationDotsApplication

        collectionJob = lifecycleScope.launch {
            app.locationProvider.locations.collect { point ->
                app.locationRepository.saveLocationPoint(point)

                val to = point.timestamp
                val from = to.minus(PROCESSING_WINDOW)
                val points = app.locationRepository.getLocationPoints(from, to)
                val events = app.journeyProcessor.process(points)

                app.timelineRepository.replaceRange(from, to, events)
            }
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        app.locationProvider.start()
    }

    override fun onDestroy() {
        collectionJob?.cancel()
        (application as LocationDotsApplication).locationProvider.stop()
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

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val CHANNEL_ID = "location_tracking"
        const val NOTIFICATION_ID = 1001
        val PROCESSING_WINDOW: Duration = Duration.ofHours(48)
    }
}

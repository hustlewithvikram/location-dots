package com.locationdots.app.core.location

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.locationdots.app.service.LocationTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationTrackingController(
    private val context: Context
) {
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    fun start() {
        val intent = Intent(context, LocationTrackingService::class.java)
        ContextCompat.startForegroundService(context, intent)
        _isTracking.value = true
    }

    fun stop() {
        context.stopService(
            Intent(context, LocationTrackingService::class.java)
        )
        _isTracking.value = false
    }

    fun markStopped() {
        _isTracking.value = false
    }
}

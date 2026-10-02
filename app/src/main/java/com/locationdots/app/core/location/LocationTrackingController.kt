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
    private val preferences = context.getSharedPreferences("location_dots_ui", Context.MODE_PRIVATE)
    private val _isTracking = MutableStateFlow(preferences.getBoolean(KEY_TRACKING_ACTIVE, false))
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    fun start() {
        val intent = Intent(context, LocationTrackingService::class.java)
        runCatching {
            ContextCompat.startForegroundService(context, intent)
        }.onSuccess {
            _isTracking.value = true
            preferences.edit().putBoolean(KEY_TRACKING_ACTIVE, true).apply()
        }.onFailure {
            markStopped()
        }
    }

    fun stop() {
        context.stopService(Intent(context, LocationTrackingService::class.java))
        markStopped()
    }

    fun markStopped() {
        _isTracking.value = false
        preferences.edit().putBoolean(KEY_TRACKING_ACTIVE, false).apply()
    }

    companion object {
        const val KEY_TRACKING_ACTIVE = "tracking_active"
    }
}

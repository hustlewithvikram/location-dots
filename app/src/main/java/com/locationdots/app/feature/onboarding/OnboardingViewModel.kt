package com.vikram.locationdots.feature.onboarding

import androidx.lifecycle.ViewModel
import com.vikram.locationdots.core.location.LocationTrackingController
import kotlinx.coroutines.flow.StateFlow

class OnboardingViewModel(
    private val trackingController: LocationTrackingController
) : ViewModel() {

    val isTracking: StateFlow<Boolean> = trackingController.isTracking

    fun startTracking() {
        trackingController.start()
    }

    fun stopTracking() {
        trackingController.stop()
    }
}

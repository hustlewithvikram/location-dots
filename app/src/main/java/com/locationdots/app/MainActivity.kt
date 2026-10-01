package com.locationdots.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.locationdots.app.core.location.LocationTrackingController
import com.locationdots.app.core.permissions.LocationPermissionManager
import com.locationdots.app.feature.onboarding.OnboardingScreen
import com.locationdots.app.feature.place.PlaceDetailScreen
import com.locationdots.app.feature.place.PlaceDetailViewModel
import com.locationdots.app.feature.place.PlaceDetailViewModelFactory
import com.locationdots.app.feature.timeline.TimelineScreen
import com.locationdots.app.feature.timeline.TimelineViewModel
import com.locationdots.app.feature.timeline.TimelineViewModelFactory
import com.locationdots.app.ui.theme.LocationDotsTheme

class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: LocationPermissionManager
    private lateinit var trackingController: LocationTrackingController
    private lateinit var timelineViewModel: TimelineViewModel

    private var hasLocationPermission by mutableStateOf(false)
    private var isTracking by mutableStateOf(false)
    private var selectedPlaceId by mutableStateOf<String?>(null)

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            hasLocationPermission =
                result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (hasLocationPermission) startTracking()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as LocationDotsApplication
        permissionManager = LocationPermissionManager(this)
        trackingController = LocationTrackingController(this)
        timelineViewModel = ViewModelProvider(
            this,
            TimelineViewModelFactory(app.timelineRepository)
        )[TimelineViewModel::class.java]

        refreshState()

        setContent {
            LocationDotsTheme {
                val events by timelineViewModel.timeline.collectAsStateWithLifecycle()

                if (!hasLocationPermission) {
                    OnboardingScreen(
                        hasLocationPermission = false,
                        isTracking = isTracking,
                        onRequestLocationPermission = ::requestLocationPermission,
                        onStartTracking = ::startTracking,
                        onStopTracking = ::stopTracking
                    )
                } else {
                    val placeId = selectedPlaceId
                    if (placeId == null) {
                        TimelineScreen(
                            events = events,
                            isTracking = isTracking,
                            onPlaceClick = { selectedPlaceId = it },
                            onLoadMore = timelineViewModel::loadMore
                        )
                    } else {
                        val placeViewModel = ViewModelProvider(
                            this,
                            PlaceDetailViewModelFactory(app.placeRepository, placeId)
                        )[PlaceDetailViewModel::class.java]
                        val place by placeViewModel.place.collectAsStateWithLifecycle()

                        PlaceDetailScreen(
                            place = place,
                            onBack = { selectedPlaceId = null },
                            onRename = placeViewModel::updateName
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun startTracking() {
        if (!permissionManager.hasForegroundLocationPermission()) {
            requestLocationPermission()
            return
        }

        if (!isLocationEnabled()) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        trackingController.start()
        isTracking = true
    }

    private fun stopTracking() {
        trackingController.stop()
        isTracking = false
    }

    private fun refreshState() {
        hasLocationPermission = permissionManager.hasForegroundLocationPermission()
        isTracking = trackingController.isTracking.value
    }

    private fun isLocationEnabled(): Boolean =
        ContextCompat.getSystemService(this, android.location.LocationManager::class.java)
            ?.let { manager ->
                manager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                    manager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
            } ?: false
}

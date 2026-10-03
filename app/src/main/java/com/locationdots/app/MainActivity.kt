package com.locationdots.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.*
import com.google.android.gms.location.LocationServices
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.core.view.WindowCompat
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.locationdots.app.core.location.LocationTrackingController
import com.locationdots.app.data.local.LocationEntity
import com.locationdots.app.data.local.PlaceEntity
import com.locationdots.app.data.local.TimelineEventEntity
import com.locationdots.app.core.permissions.LocationPermissionManager
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.feature.about.AboutScreen
import com.locationdots.app.feature.insights.*
import com.locationdots.app.feature.journey.JourneyDetailScreen
import com.locationdots.app.feature.map.*
import com.locationdots.app.feature.onboarding.OnboardingScreen
import com.locationdots.app.feature.place.*
import com.locationdots.app.feature.search.*
import com.locationdots.app.feature.settings.*
import com.locationdots.app.feature.splash.SplashScreen
import com.locationdots.app.feature.timeline.*
import com.locationdots.app.ui.components.AppBottomBar
import com.locationdots.app.ui.components.AppTab
import com.locationdots.app.ui.theme.LocationDotsTheme
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: LocationPermissionManager
    private lateinit var trackingController: LocationTrackingController
    private lateinit var timelineViewModel: TimelineViewModel
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var placesViewModel: PlacesOverviewViewModel
    private lateinit var insightsViewModel: InsightsViewModel

    private var hasLocationPermission by mutableStateOf(false)
    private var isTracking by mutableStateOf(false)
    private var selectedPlaceId by mutableStateOf<String?>(null)
    private var selectedJourneyId by mutableStateOf<String?>(null)
    private var currentTab by mutableStateOf(AppTab.TIMELINE)
    private var isSearchOpen by mutableStateOf(false)
    private var isAboutOpen by mutableStateOf(false)
    private var showSplash by mutableStateOf(true)
    private var themeChoice by mutableStateOf(ThemeChoice.SYSTEM)
    private var animationsEnabled by mutableStateOf(true)
    private var mapStyle by mutableStateOf("liberty")
    private var showRouteLines by mutableStateOf(true)
    private var showPlaceMarkers by mutableStateOf(true)
    private var trackingAccuracy by mutableStateOf(TrackingAccuracy.HIGH)
    private var trackingInterval by mutableStateOf(TrackingInterval.THIRTY_SECONDS)
    private var importPreview by mutableStateOf<ImportPreview?>(null)
    private var importError by mutableStateOf<String?>(null)
    private var pendingExportJson: String? = null

    private enum class NavigationDirection { FORWARD, BACK }
    private data class AppDestination(
        val key: Int,
        val direction: NavigationDirection
    )
    private var pendingNavigationDirection by mutableStateOf(NavigationDirection.FORWARD)

    private val exportBackupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            val json = pendingExportJson
            pendingExportJson = null
            if (uri != null && json != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                            ?: error("Couldn't open the selected location.")
                    }.onFailure {
                        runOnUiThread { importError = "Couldn't save the backup file." }
                    }
                }
            }
        }

    private val importBackupLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch(Dispatchers.IO) {
                val preview = runCatching { readImportPreview(uri) }.getOrElse {
                    null
                }
                withContext(Dispatchers.Main) {
                    if (preview != null) {
                        importError = null
                        importPreview = preview
                    } else {
                        importError = "This isn't a valid Location Dots backup."
                    }
                }
            }
        }

    private val preferences by lazy { getSharedPreferences("location_dots_ui", MODE_PRIVATE) }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            hasLocationPermission =
                result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (hasLocationPermission) {
                preferences.edit().putBoolean("onboarding_complete", true).apply()
                startTracking()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        // Keep the Android navigation surface transparent. The Compose root below owns
        // the background so uncovered transition/system-bar areas always use the theme.
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        permissionManager = LocationPermissionManager(this)
        trackingController = LocationTrackingController(this)
        themeChoice = runCatching { ThemeChoice.valueOf(preferences.getString("theme", ThemeChoice.SYSTEM.name)!!) }.getOrDefault(ThemeChoice.SYSTEM)
        animationsEnabled = preferences.getBoolean("animations", true)
        mapStyle = preferences.getString("map_style", "liberty") ?: "liberty"
        showRouteLines = preferences.getBoolean("show_route_lines", true)
        showPlaceMarkers = preferences.getBoolean("show_place_markers", true)
        trackingAccuracy = runCatching {
            TrackingAccuracy.valueOf(
                preferences.getString("tracking_accuracy", TrackingAccuracy.HIGH.name) ?: TrackingAccuracy.HIGH.name
            )
        }.getOrDefault(TrackingAccuracy.HIGH)
        trackingInterval = TrackingInterval.entries.firstOrNull {
            it.millis == preferences.getLong("tracking_interval_millis", TrackingInterval.THIRTY_SECONDS.millis)
        } ?: TrackingInterval.THIRTY_SECONDS
        showSplash = !preferences.getBoolean("splash_seen", false)

        val app = application as LocationDotsApplication
        timelineViewModel = ViewModelProvider(this, TimelineViewModelFactory(app.timelineRepository))[TimelineViewModel::class.java]
        placesViewModel = ViewModelProvider(this, PlacesOverviewViewModelFactory(app.placeRepository))[PlacesOverviewViewModel::class.java]
        searchViewModel = ViewModelProvider(this, SearchViewModelFactory(app.searchRepository))[SearchViewModel::class.java]
        insightsViewModel = ViewModelProvider(this, InsightsViewModelFactory(app.insightsRepository))[InsightsViewModel::class.java]
        refreshState()

        setContent {
            val darkTheme = when (themeChoice) {
                ThemeChoice.DARK -> true
                ThemeChoice.LIGHT -> false
                ThemeChoice.SYSTEM -> isSystemInDarkTheme()
            }
            SideEffect { WindowCompat.getInsetsController(window, window.decorView).apply { isAppearanceLightStatusBars = !darkTheme; isAppearanceLightNavigationBars = !darkTheme } }
            LocationDotsTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val timelineState by timelineViewModel.uiState.collectAsStateWithLifecycle()
                val places by placesViewModel.places.collectAsStateWithLifecycle()
                val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
                val insights by insightsViewModel.snapshot.collectAsStateWithLifecycle()
                val insightsLoading by insightsViewModel.isLoading.collectAsStateWithLifecycle()
                val insightsError by insightsViewModel.error.collectAsStateWithLifecycle()

                val destinationKey = when {
                    showSplash -> -100
                    !hasLocationPermission -> -90
                    isAboutOpen -> -80
                    selectedJourneyId != null -> -60
                    selectedPlaceId != null -> -50
                    isSearchOpen -> -70
                    else -> when (currentTab) {
                        AppTab.TIMELINE -> 0
                        AppTab.MAP -> 1
                        AppTab.INSIGHTS -> 2
                        AppTab.SETTINGS -> 3
                    }
                }

                val destination = remember(destinationKey, pendingNavigationDirection) {
                    AppDestination(destinationKey, pendingNavigationDirection)
                }
                // One app-level back handler keeps every manually-managed destination
                // consistent with its visible navigation hierarchy.
                val canNavigateBack = isAboutOpen ||
                    selectedJourneyId != null ||
                    selectedPlaceId != null ||
                    isSearchOpen ||
                    currentTab != AppTab.TIMELINE

                BackHandler(enabled = canNavigateBack) {
                    when {
                        isAboutOpen -> {
                            pendingNavigationDirection = NavigationDirection.BACK
                            isAboutOpen = false
                        }
                        selectedJourneyId != null -> {
                            pendingNavigationDirection = NavigationDirection.BACK
                            selectedJourneyId = null
                        }
                        selectedPlaceId != null -> {
                            pendingNavigationDirection = NavigationDirection.BACK
                            selectedPlaceId = null
                        }
                        isSearchOpen -> {
                            searchViewModel.clear()
                            pendingNavigationDirection = NavigationDirection.BACK
                            isSearchOpen = false
                        }
                        currentTab != AppTab.TIMELINE -> {
                            pendingNavigationDirection = NavigationDirection.BACK
                            currentTab = AppTab.TIMELINE
                        }
                    }
                }

                AnimatedContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (destination.key in 0..3) 88.dp else 0.dp),
                    targetState = destination,
                    transitionSpec = {
                        if (!animationsEnabled) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val direction = if (targetState.direction == NavigationDirection.FORWARD) {
                                AnimatedContentTransitionScope.SlideDirection.Left
                            } else {
                                AnimatedContentTransitionScope.SlideDirection.Right
                            }
                            (
                                slideIntoContainer(
                                    direction,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeIn(tween(90))
                            ) togetherWith (
                                slideOutOfContainer(
                                    direction,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeOut(tween(90))
                            )
                        }
                    },
                    label = "page-navigation"
                ) { destination ->
                    when {
                        destination.key == -100 -> SplashScreen(
                        onGetStarted = {
                            pendingNavigationDirection = NavigationDirection.FORWARD
                            preferences.edit().putBoolean("splash_seen", true).apply()
                            if (hasLocationPermission) {
                                preferences.edit().putBoolean("onboarding_complete", true).apply()
                            }
                            showSplash = false
                        }
                    )
                        destination.key == -90 -> OnboardingScreen(
                        hasLocationPermission = hasLocationPermission,
                        isTracking = isTracking,
                        onRequestLocationPermission = ::requestLocationPermission,
                        onStartTracking = ::startTracking,
                        onStopTracking = ::stopTracking
                    )
                        destination.key == -80 -> AboutScreen {
                            pendingNavigationDirection = NavigationDirection.BACK
                            isAboutOpen = false
                        }
                        destination.key == -70 -> SearchScreen(
                        query = searchState.query,
                        results = searchState.results,
                        isSearching = searchState.isSearching,
                        onQueryChange = searchViewModel::search,
                        onBack = {
                            pendingNavigationDirection = NavigationDirection.BACK
                            searchViewModel.clear()
                            isSearchOpen = false
                        },
                        // Keep Search in the navigation hierarchy while opening details.
                        // Back from the detail screen will reveal the existing search state.
                        onPlaceClick = {
                            pendingNavigationDirection = NavigationDirection.FORWARD
                            selectedPlaceId = it
                        },
                        onJourneyClick = {
                            pendingNavigationDirection = NavigationDirection.FORWARD
                            selectedJourneyId = it
                        }
                    )
                    destination.key == -60 -> {
                        val journey = timelineState.events.firstOrNull { it.id == selectedJourneyId } as? TimelineEvent.Journey
                        if (journey == null) {
                            LaunchedEffect(selectedJourneyId) { timelineViewModel.refresh() }
                            selectedJourneyId = null
                        } else JourneyDetailScreen(journey) {
                            pendingNavigationDirection = NavigationDirection.BACK
                            selectedJourneyId = null
                        }
                    }
                    destination.key == -50 -> {
                        val placeId = selectedPlaceId
                        if (placeId != null) {
                            val placeVm = ViewModelProvider(
                                this@MainActivity,
                                PlaceDetailViewModelFactory(
                                    app.placeRepository,
                                    app.timelineRepository,
                                    placeId
                                )
                            )[PlaceDetailViewModel::class.java]
                            val place by placeVm.place.collectAsStateWithLifecycle()
                            val visits by placeVm.visits.collectAsStateWithLifecycle()
                            PlaceDetailScreen(
                                place,
                                visits,
                                {
                                    pendingNavigationDirection = NavigationDirection.BACK
                                    selectedPlaceId = null
                                },
                                placeVm::updateName
                            )
                        }
                    }
                    destination.key == 1 -> PlacesOverviewScreen(
                        places = places,
                        onBack = {
                            pendingNavigationDirection = NavigationDirection.BACK
                            currentTab = AppTab.TIMELINE
                        },
                        onPlaceClick = {
                            pendingNavigationDirection = NavigationDirection.FORWARD
                            selectedPlaceId = it
                        },
                        onTabSelected = ::selectTab
                    )
                    destination.key == 2 -> InsightsScreen(
                        snapshot = insights,
                        isLoading = insightsLoading,
                        error = insightsError,
                        onBack = {
                            pendingNavigationDirection = NavigationDirection.BACK
                            currentTab = AppTab.TIMELINE
                        },
                        onRefresh = insightsViewModel::refresh,
                        onTabSelected = ::selectTab
                    )
                    destination.key == 3 -> SettingsScreen(
                        themeChoice = themeChoice,
                        isTracking = isTracking,
                        animationsEnabled = animationsEnabled,
                        mapStyle = mapStyle,
                        showRouteLines = showRouteLines,
                        showPlaceMarkers = showPlaceMarkers,
                        places = places,
                        locationPermissionGranted = hasLocationPermission,
                        trackingAccuracy = trackingAccuracy,
                        trackingInterval = trackingInterval,
                        onThemeChange = {
                            themeChoice = it
                            preferences.edit().putString("theme", it.name).apply()
                        },
                        onTrackingChange = { enabled -> if (enabled) startTracking() else stopTracking() },
                        onTrackingAccuracyChange = { accuracy ->
                            trackingAccuracy = accuracy
                            preferences.edit().putString("tracking_accuracy", accuracy.name).apply()
                            restartTrackingIfNeeded()
                        },
                        onTrackingIntervalChange = { interval ->
                            trackingInterval = interval
                            preferences.edit().putLong("tracking_interval_millis", interval.millis).apply()
                            restartTrackingIfNeeded()
                        },
                        onAnimationsChange = {
                            animationsEnabled = it
                            preferences.edit().putBoolean("animations", it).apply()
                        },
                        onMapStyleChange = {
                            mapStyle = it
                            preferences.edit().putString("map_style", it).apply()
                        },
                        onRouteLinesChange = {
                            showRouteLines = it
                            preferences.edit().putBoolean("show_route_lines", it).apply()
                        },
                        onPlaceMarkersChange = {
                            showPlaceMarkers = it
                            preferences.edit().putBoolean("show_place_markers", it).apply()
                        },
                        onExport = { exportSummary(insights) },
                        onExportData = ::exportLocalData,
                        onImportData = { importBackupLauncher.launch(arrayOf("application/json", "text/plain", "text/*")) },
                        importPreview = importPreview,
                        importError = importError,
                        onDismissImport = { importPreview = null; importError = null },
                        onConfirmImport = { preview, replaceExisting ->
                            lifecycleScope.launch { importBackup(preview, replaceExisting) }
                        },
                        onClearHistory = ::clearHistory,
                        onResetSettings = ::resetAppSettings,
                        onAbout = {
                            pendingNavigationDirection = NavigationDirection.FORWARD
                            isAboutOpen = true
                        },
                        onOpenLocationSettings = {
                            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        },
                        batteryOptimizationIgnored = isBatteryOptimizationIgnored(),
                        onOpenBatterySettings = {
                            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        },
                        onShareDiagnostics = ::shareDiagnostics,
                        onRequestLocationPermission = ::requestLocationPermission,
                        onSaveNamedPlace = ::saveNamedPlace,
                        onRequestCurrentLocation = ::requestCurrentLocation
                    )
                    else -> TimelineScreen(
                        events = timelineState.events,
                        isTracking = isTracking,
                        isLoadingMore = timelineState.isLoading,
                        isRefreshing = timelineState.isRefreshing,
                        hasMore = timelineState.hasMore,
                        errorMessage = timelineState.errorMessage,
                        onPlaceClick = {
                            pendingNavigationDirection = NavigationDirection.FORWARD
                            selectedPlaceId = it
                        },
                        onJourneyClick = {
                            pendingNavigationDirection = NavigationDirection.FORWARD
                            selectedJourneyId = it
                        },
                        onSearchClick = {
                            pendingNavigationDirection = NavigationDirection.FORWARD
                            isSearchOpen = true
                        },
                        onPlacesClick = {
                            pendingNavigationDirection = NavigationDirection.FORWARD
                            currentTab = AppTab.MAP
                        },
                        onInsightsClick = {
                            pendingNavigationDirection = NavigationDirection.FORWARD
                            currentTab = AppTab.INSIGHTS
                        },
                        onSettingsClick = {
                            pendingNavigationDirection = NavigationDirection.FORWARD
                            currentTab = AppTab.SETTINGS
                        },
                        onLoadMore = timelineViewModel::loadMore,
                        onRetry = timelineViewModel::retry,
                        onClearError = timelineViewModel::clearError
                    )
                    }
                }

                if (destinationKey in 0..3) {
                    // Keep the navigation shell spatially anchored to the bottom.
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                    ) {
                        AppBottomBar(currentTab, ::selectTab)
                    }
                }
            }
        }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::permissionManager.isInitialized) refreshState()
        if (::timelineViewModel.isInitialized) timelineViewModel.refresh()
        if (::insightsViewModel.isInitialized) insightsViewModel.refresh()
    }

    private fun selectTab(tab: AppTab) {
        if (tab == currentTab) return
        pendingNavigationDirection = if (tab.ordinal >= currentTab.ordinal) {
            NavigationDirection.FORWARD
        } else {
            NavigationDirection.BACK
        }
        selectedPlaceId = null
        selectedJourneyId = null
        currentTab = tab
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
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
        isTracking = trackingController.isTracking.value
    }

    private fun stopTracking() {
        trackingController.stop()
        isTracking = false
    }

    private fun restartTrackingIfNeeded() {
        if (!isTracking) return
        trackingController.stop()
        trackingController.start()
        isTracking = true
    }

    private fun saveNamedPlace(name: String, latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            application.let { (it as LocationDotsApplication).placeRepository.saveNamedPlace(name, latitude, longitude) }
            timelineViewModel.refresh()
            insightsViewModel.refresh()
        }
    }

    private fun requestCurrentLocation(onLocation: (Double, Double) -> Unit) {
        if (!permissionManager.hasForegroundLocationPermission()) {
            requestLocationPermission()
            return
        }
        try {
            LocationServices.getFusedLocationProviderClient(this)
                .lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        onLocation(location.latitude, location.longitude)
                    }
                }
        } catch (_: SecurityException) {
            // Permission can be revoked between the explicit check and this API call.
            hasLocationPermission = false
        }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = getSystemService(PowerManager::class.java)
        return powerManager?.isIgnoringBatteryOptimizations(packageName) == true
    }

    private fun shareDiagnostics() {
        val report = buildString {
            appendLine("Location Dots diagnostic report")
            appendLine()
            appendLine("Android: " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")")
            appendLine("Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL)
            appendLine("Tracking: " + if (isTracking) "Running" else "Stopped")
            appendLine("Location permission: " + if (permissionManager.hasForegroundLocationPermission()) "Granted" else "Not granted")
            appendLine("Location services: " + if (isLocationEnabled()) "Enabled" else "Disabled")
            appendLine("Battery optimization: " + if (isBatteryOptimizationIgnored()) "Unrestricted" else "Optimized")
            appendLine("Tracking accuracy: " + trackingAccuracy.name)
            appendLine("Tracking interval: " + trackingInterval.label)
            appendLine("Map style: " + mapStyle)
            appendLine("Route lines: " + showRouteLines)
            appendLine("Place markers: " + showPlaceMarkers)
            appendLine("Saved places: " + placesViewModel.places.value.size)
            appendLine("Maps: MapLibre + OpenFreeMap")
            appendLine()
            appendLine("Generated locally by Location Dots.")
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Location Dots diagnostic report")
            putExtra(Intent.EXTRA_TEXT, report)
        }, "Share diagnostic report"))
    }

    private fun clearHistory() {
        val app = application as LocationDotsApplication
        lifecycleScope.launch {
            app.database.clearAllData()
            timelineViewModel.refresh()
            insightsViewModel.refresh()
        }
    }

    private fun exportLocalData() {
        val app = application as LocationDotsApplication
        lifecycleScope.launch {
            val json = withContext(Dispatchers.Default) {
                val locations = app.database.locationDao().getRange(Long.MIN_VALUE, Long.MAX_VALUE)
                val places = app.database.placeDao().getAll()
                val events = app.database.timelineEventDao().getAll()
                JSONObject().apply {
                    put("format", "location-dots-local-export")
                    put("version", 1)
                    put("generatedAtEpochMillis", System.currentTimeMillis())
                    put("locations", JSONArray().apply {
                        locations.forEach {
                            put(JSONObject().apply {
                                put("id", it.id)
                                put("latitude", it.latitude)
                                put("longitude", it.longitude)
                                put("accuracyMeters", it.accuracyMeters)
                                put("timestampEpochMillis", it.timestampEpochMillis)
                            })
                        }
                    })
                    put("places", JSONArray().apply {
                        places.forEach {
                            put(JSONObject().apply {
                                put("id", it.id)
                                put("name", it.name)
                                put("latitude", it.latitude)
                                put("longitude", it.longitude)
                                put("createdAtEpochMillis", it.createdAtEpochMillis)
                                put("updatedAtEpochMillis", it.updatedAtEpochMillis)
                            })
                        }
                    })
                    put("timelineEvents", JSONArray().apply {
                        events.forEach {
                            put(JSONObject().apply {
                                put("id", it.id)
                                put("type", it.type)
                                put("timestampEpochMillis", it.timestampEpochMillis)
                                put("placeId", it.placeId)
                                put("arrivalEpochMillis", it.arrivalEpochMillis)
                                put("departureEpochMillis", it.departureEpochMillis)
                                put("startPlaceId", it.startPlaceId)
                                put("endPlaceId", it.endPlaceId)
                                put("startedAtEpochMillis", it.startedAtEpochMillis)
                                put("endedAtEpochMillis", it.endedAtEpochMillis)
                                put("distanceMeters", it.distanceMeters)
                                put("journeyMode", it.journeyMode)
                                put("pathEncoded", it.pathEncoded)
                            })
                        }
                    })
                }.toString(2)
            }
            pendingExportJson = json
            exportBackupLauncher.launch(
                "location-dots-backup-" +
                    SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date()) +
                    ".json"
            )
        }
    }

    private fun readImportPreview(uri: Uri): ImportPreview {
        val text = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("Empty backup file.")
        val root = JSONObject(text)
        require(root.optString("format") == "location-dots-local-export") { "Unsupported backup format." }
        require(root.optInt("version", -1) == 1) { "Unsupported backup version." }
        return ImportPreview(
            uri = uri,
            locations = root.optJSONArray("locations")?.length() ?: 0,
            places = root.optJSONArray("places")?.length() ?: 0,
            timelineEvents = root.optJSONArray("timelineEvents")?.length() ?: 0,
            generatedAtEpochMillis = root.optLong("generatedAtEpochMillis", 0L)
        )
    }

    private suspend fun importBackup(preview: ImportPreview, replaceExisting: Boolean) {
        val app = application as LocationDotsApplication
        val uri = preview.uri
        runCatching {
            val text = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("Couldn't read the backup file.")
            val root = JSONObject(text)
            require(root.optString("format") == "location-dots-local-export")
            require(root.optInt("version", -1) == 1)

            val locations = parseLocations(root.optJSONArray("locations"))
            val places = parsePlaces(root.optJSONArray("places"))
            val events = parseTimelineEvents(root.optJSONArray("timelineEvents"))

            app.database.withTransaction {
                if (replaceExisting) {
                    app.database.locationDao().deleteAll()
                    app.database.placeDao().deleteAll()
                    app.database.timelineEventDao().deleteAll()
                }
                app.database.locationDao().insertAll(locations)
                app.database.placeDao().insertAll(places)
                app.database.timelineEventDao().insertAll(events)
            }
        }.onSuccess {
            importPreview = null
            importError = null
            timelineViewModel.refresh()
            insightsViewModel.refresh()
        }.onFailure {
            importPreview = null
            importError = "Couldn't import this backup. No changes were made."
        }
    }

    private fun parseLocations(array: JSONArray?): List<LocationEntity> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val id = o.getString("id")
                val lat = o.getDouble("latitude")
                val lon = o.getDouble("longitude")
                require(lat in -90.0..90.0 && lon in -180.0..180.0)
                add(
                    LocationEntity(
                        id = id,
                        latitude = lat,
                        longitude = lon,
                        accuracyMeters = if (o.isNull("accuracyMeters")) null else o.getDouble("accuracyMeters").toFloat(),
                        timestampEpochMillis = o.getLong("timestampEpochMillis")
                    )
                )
            }
        }
    }

    private fun parsePlaces(array: JSONArray?): List<PlaceEntity> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val lat = o.getDouble("latitude")
                val lon = o.getDouble("longitude")
                require(lat in -90.0..90.0 && lon in -180.0..180.0)
                add(
                    PlaceEntity(
                        id = o.getString("id"),
                        name = if (o.isNull("name")) null else o.getString("name"),
                        latitude = lat,
                        longitude = lon,
                        createdAtEpochMillis = o.getLong("createdAtEpochMillis"),
                        updatedAtEpochMillis = o.getLong("updatedAtEpochMillis")
                    )
                )
            }
        }
    }

    private fun parseTimelineEvents(array: JSONArray?): List<TimelineEventEntity> {
        if (array == null) return emptyList()
        fun stringOrNull(o: JSONObject, key: String) = if (o.isNull(key)) null else o.getString(key)
        fun longOrNull(o: JSONObject, key: String) = if (o.isNull(key)) null else o.getLong(key)
        fun doubleOrNull(o: JSONObject, key: String) = if (o.isNull(key)) null else o.getDouble(key)
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    TimelineEventEntity(
                        id = o.getString("id"),
                        type = o.getString("type"),
                        timestampEpochMillis = o.getLong("timestampEpochMillis"),
                        placeId = stringOrNull(o, "placeId"),
                        arrivalEpochMillis = longOrNull(o, "arrivalEpochMillis"),
                        departureEpochMillis = longOrNull(o, "departureEpochMillis"),
                        startPlaceId = stringOrNull(o, "startPlaceId"),
                        endPlaceId = stringOrNull(o, "endPlaceId"),
                        startedAtEpochMillis = longOrNull(o, "startedAtEpochMillis"),
                        endedAtEpochMillis = longOrNull(o, "endedAtEpochMillis"),
                        distanceMeters = doubleOrNull(o, "distanceMeters"),
                        journeyMode = stringOrNull(o, "journeyMode"),
                        pathEncoded = stringOrNull(o, "pathEncoded")
                    )
                )
            }
        }
    }

    private fun resetAppSettings() {
        preferences.edit()
            .putString("theme", ThemeChoice.SYSTEM.name)
            .putBoolean("animations", true)
            .putString("map_style", "liberty")
            .putBoolean("show_route_lines", true)
            .putBoolean("show_place_markers", true)
            .putString("tracking_accuracy", TrackingAccuracy.HIGH.name)
            .putLong("tracking_interval_millis", TrackingInterval.THIRTY_SECONDS.millis)
            .apply()
        themeChoice = ThemeChoice.SYSTEM
        animationsEnabled = true
        mapStyle = "liberty"
        showRouteLines = true
        showPlaceMarkers = true
        trackingAccuracy = TrackingAccuracy.HIGH
        trackingInterval = TrackingInterval.THIRTY_SECONDS
    }

    private fun exportSummary(snapshot: com.locationdots.app.domain.insights.InsightsSnapshot) {
        val text = buildString {
            appendLine("Location Dots — activity summary")
            appendLine()
            appendLine("Places: " + snapshot.totalPlaces)
            appendLine("Visits: " + snapshot.totalVisits)
            appendLine("Journeys: " + snapshot.journeyCount)
            appendLine("Distance: " + snapshot.totalDistanceMeters.toInt() + " m")
            appendLine("Time: " + snapshot.totalTimeMinutes + " min")
            appendLine()
            appendLine("Generated locally on this device.")
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Share summary"))
    }

    private fun refreshState() {
        hasLocationPermission = permissionManager.hasForegroundLocationPermission()
        isTracking = trackingController.isTracking.value
    }

    private fun isLocationEnabled(): Boolean =
        ContextCompat.getSystemService(this, android.location.LocationManager::class.java)?.let { manager ->
            manager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        } ?: false
}

package com.locationdots.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.locationdots.app.core.location.LocationTrackingController
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
import com.locationdots.app.ui.components.AppTab
import com.locationdots.app.ui.theme.LocationDotsTheme
import kotlinx.coroutines.launch

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
        enableEdgeToEdge()
        permissionManager = LocationPermissionManager(this)
        trackingController = LocationTrackingController(this)
        themeChoice = runCatching { ThemeChoice.valueOf(preferences.getString("theme", ThemeChoice.SYSTEM.name)!!) }.getOrDefault(ThemeChoice.SYSTEM)
        animationsEnabled = preferences.getBoolean("animations", true)
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
                    isSearchOpen -> -70
                    selectedJourneyId != null -> -60
                    selectedPlaceId != null -> -50
                    else -> when (currentTab) {
                        AppTab.TIMELINE -> 0
                        AppTab.MAP -> 1
                        AppTab.INSIGHTS -> 2
                        AppTab.SETTINGS -> 3
                    }
                }

                AnimatedContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (destinationKey in 0..3) 88.dp else 0.dp),
                    targetState = destinationKey,
                    transitionSpec = {
                        if (!animationsEnabled) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val forward = targetState > initialState
                            val direction = if (forward) AnimatedContentTransitionScope.SlideDirection.Left
                            else AnimatedContentTransitionScope.SlideDirection.Right
                            (slideIntoContainer(
                                    direction,
                                    spring(dampingRatio = 0.9f, stiffness = 520f)
                                ) + fadeIn(tween(110))) togetherWith
                                (slideOutOfContainer(
                                    direction,
                                    spring(dampingRatio = 1f, stiffness = 520f)
                                ) + fadeOut(tween(90)))
                        }.using(SizeTransform(clip = false))
                    },
                    label = "page-navigation"
                ) { destination ->
                    when {
                        destination == -100 -> SplashScreen(
                        onGetStarted = {
                            preferences.edit().putBoolean("splash_seen", true).apply()
                            if (hasLocationPermission) {
                                preferences.edit().putBoolean("onboarding_complete", true).apply()
                            }
                            showSplash = false
                        }
                    )
                        destination == -90 -> OnboardingScreen(
                        hasLocationPermission = false,
                        isTracking = isTracking,
                        onRequestLocationPermission = ::requestLocationPermission,
                        onStartTracking = ::startTracking,
                        onStopTracking = ::stopTracking
                    )
                        destination == -80 -> AboutScreen { isAboutOpen = false }
                        destination == -70 -> SearchScreen(
                        query = searchState.query,
                        results = searchState.results,
                        isSearching = searchState.isSearching,
                        onQueryChange = searchViewModel::search,
                        onBack = { searchViewModel.clear(); isSearchOpen = false },
                        onPlaceClick = { searchViewModel.clear(); isSearchOpen = false; selectedPlaceId = it },
                        onJourneyClick = { searchViewModel.clear(); isSearchOpen = false; selectedJourneyId = it }
                    )
                    destination == -60 -> {
                        val journey = timelineState.events.firstOrNull { it.id == selectedJourneyId } as? TimelineEvent.Journey
                        if (journey == null) {
                            LaunchedEffect(selectedJourneyId) { timelineViewModel.refresh() }
                            selectedJourneyId = null
                        } else JourneyDetailScreen(journey) { selectedJourneyId = null }
                    }
                    destination == -50 -> {
                        val placeVm = ViewModelProvider(this@MainActivity, PlaceDetailViewModelFactory(app.placeRepository, app.timelineRepository, selectedPlaceId!!))[PlaceDetailViewModel::class.java]
                        val place by placeVm.place.collectAsStateWithLifecycle()
                        val visits by placeVm.visits.collectAsStateWithLifecycle()
                        PlaceDetailScreen(place, visits, { selectedPlaceId = null }, placeVm::updateName)
                    }
                    destination == 1 -> PlacesOverviewScreen(
                        places = places,
                        onBack = { currentTab = AppTab.TIMELINE },
                        onPlaceClick = { selectedPlaceId = it },
                        onTabSelected = ::selectTab
                    )
                    destination == 2 -> InsightsScreen(
                        snapshot = insights,
                        isLoading = insightsLoading,
                        error = insightsError,
                        onBack = { currentTab = AppTab.TIMELINE },
                        onRefresh = insightsViewModel::refresh,
                        onTabSelected = ::selectTab
                    )
                    destination == 3 -> SettingsScreen(
                        selectedTab = currentTab,
                        themeChoice = themeChoice,
                        isTracking = isTracking,
                        animationsEnabled = animationsEnabled,
                        onTabSelected = ::selectTab,
                        onThemeChange = {
                            themeChoice = it
                            preferences.edit().putString("theme", it.name).apply()
                        },
                        onTrackingChange = { enabled -> if (enabled) startTracking() else stopTracking() },
                        onAnimationsChange = {
                            animationsEnabled = it
                            preferences.edit().putBoolean("animations", it).apply()
                        },
                        onExport = { exportSummary(insights) },
                        onClearHistory = ::clearHistory,
                        onAbout = { isAboutOpen = true }
                    )
                    else -> TimelineScreen(
                        events = timelineState.events,
                        isTracking = isTracking,
                        isLoadingMore = timelineState.isLoading,
                        isRefreshing = timelineState.isRefreshing,
                        hasMore = timelineState.hasMore,
                        errorMessage = timelineState.errorMessage,
                        onPlaceClick = { selectedPlaceId = it },
                        onJourneyClick = { selectedJourneyId = it },
                        onSearchClick = { isSearchOpen = true },
                        onPlacesClick = { currentTab = AppTab.MAP },
                        onInsightsClick = { currentTab = AppTab.INSIGHTS },
                        onSettingsClick = { currentTab = AppTab.SETTINGS },
                        onLoadMore = timelineViewModel::loadMore,
                        onRetry = timelineViewModel::retry,
                        onClearError = timelineViewModel::clearError
                    )
                    }
                }

                if (destinationKey in 0..3) {
                    AppBottomBar(currentTab, ::selectTab)
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
        isTracking = true
    }

    private fun stopTracking() {
        trackingController.stop()
        isTracking = false
    }

    private fun clearHistory() {
        val app = application as LocationDotsApplication
        lifecycleScope.launch {
            app.database.locationDao().deleteAll()
            app.database.placeDao().deleteAll()
            app.database.timelineEventDao().deleteAll()
            timelineViewModel.refresh()
            insightsViewModel.refresh()
        }
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

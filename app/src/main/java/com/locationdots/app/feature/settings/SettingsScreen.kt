package com.locationdots.app.feature.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.Place
import com.locationdots.app.feature.place.PlaceEditorDialog
import com.locationdots.app.ui.components.*

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

enum class TrackingAccuracy { HIGH, BALANCED }
enum class TrackingInterval(val millis: Long, val label: String) {
    THIRTY_SECONDS(30_000L, "30 seconds"),
    ONE_MINUTE(60_000L, "1 minute"),
    FIVE_MINUTES(300_000L, "5 minutes")
}
enum class VisitDuration(val minutes: Long, val label: String) {
    FIVE(5, "5 min"),
    EIGHT(8, "8 min"),
    TEN(10, "10 min"),
    FIFTEEN(15, "15 min")
}
enum class VisitRadius(val meters: Double, val label: String) {
    FIFTY(50.0, "50m"),
    HUNDRED(100.0, "100m"),
    ONE_FIFTY(150.0, "150m"),
    TWO_FIFTY(250.0, "250m")
}
enum class TrackingMaxAccuracy(val meters: Double, val label: String) {
    FIFTY(50.0, "50m"),
    HUNDRED(100.0, "100m"),
    ONE_FIFTY(150.0, "150m")
}
enum class MovementThreshold(val meters: Double, val label: String) {
    TEN(10.0, "10m"),
    TWENTY_FIVE(25.0, "25m"),
    FIFTY(50.0, "50m")
}

data class ImportPreview(
    val uri: Uri,
    val locations: Int,
    val places: Int,
    val timelineEvents: Int,
    val generatedAtEpochMillis: Long
)

private enum class SettingsPage {
    HOME, SAVED_PLACES, TRACKING, MAP_APPEARANCE, INTERFACE, PRIVACY_DATA, EXPORT_BACKUP, DIAGNOSTICS
}

private enum class NavigationDirection { FORWARD, BACK }

enum class ActionButton {
    THEME, REFRESH, TRACKING, TODAY, EXPORT;

    fun label(): String = when (this) {
        THEME -> "Theme"
        REFRESH -> "Refresh timeline"
        TRACKING -> "Tracking"
        TODAY -> "Today"
        EXPORT -> "Export data"
    }
}

private enum class MapStyleChoice(val key: String, val label: String) {
    LIBERTY("liberty", "Liberty"),
    BRIGHT("bright", "Bright"),
    POSITRON("positron", "Positron"),
    DARK("dark", "Dark"),
    FIORD("fiord", "Fiord");

    companion object {
        fun fromKey(key: String): MapStyleChoice =
            entries.firstOrNull { it.key == key } ?: LIBERTY
    }
}

@Composable
fun SettingsScreen(
    themeChoice: ThemeChoice,
    isTracking: Boolean,
    animationsEnabled: Boolean,
    mapStyle: String,
    showRouteLines: Boolean,
    showPlaceMarkers: Boolean,
    places: List<Place>,
    locationPermissionGranted: Boolean,
    trackingAccuracy: TrackingAccuracy,
    trackingInterval: TrackingInterval,
    automaticJourneyDetection: Boolean,
    stationaryDetection: Boolean,
    visitDuration: VisitDuration,
    visitRadius: VisitRadius,
    maxTrackingAccuracy: TrackingMaxAccuracy,
    movementThreshold: MovementThreshold,
    actionButton: ActionButton,
    onActionButtonChange: (ActionButton) -> Unit,
    onThemeChange: (ThemeChoice) -> Unit,
    onTrackingChange: (Boolean) -> Unit,
    onTrackingAccuracyChange: (TrackingAccuracy) -> Unit,
    onTrackingIntervalChange: (TrackingInterval) -> Unit,
    onAutomaticJourneyDetectionChange: (Boolean) -> Unit,
    onStationaryDetectionChange: (Boolean) -> Unit,
    onVisitDurationChange: (VisitDuration) -> Unit,
    onVisitRadiusChange: (VisitRadius) -> Unit,
    onMaxTrackingAccuracyChange: (TrackingMaxAccuracy) -> Unit,
    onMovementThresholdChange: (MovementThreshold) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onMapStyleChange: (String) -> Unit,
    onRouteLinesChange: (Boolean) -> Unit,
    onPlaceMarkersChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    importPreview: ImportPreview?,
    importLoading: Boolean,
    importError: String?,
    onDismissImport: () -> Unit,
    onConfirmImport: (ImportPreview, Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onResetSettings: () -> Unit,
    onAbout: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    batteryOptimizationIgnored: Boolean,
    onOpenBatterySettings: () -> Unit,
    onShareDiagnostics: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onSaveNamedPlace: (String, Double, Double) -> Unit,
    onRequestCurrentLocation: ((onLocation: (Double, Double) -> Unit) -> Unit)
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.HOME) }
    var settingsNavigationDirection by remember { mutableStateOf(NavigationDirection.FORWARD) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showPlaceEditor by remember { mutableStateOf(false) }
    var showMapStyleDialog by remember { mutableStateOf(false) }
    var previewMapStyle by remember { mutableStateOf(mapStyle) }
    var showAttributionDialog by remember { mutableStateOf(false) }
    var editingPlace by remember { mutableStateOf<Place?>(null) }

    fun openPlaceEditor(place: Place?) {
        editingPlace = place
        showPlaceEditor = true
    }

    fun openSettingsPage(target: SettingsPage) {
        if (target == page) return
        settingsNavigationDirection = NavigationDirection.FORWARD
        page = target
    }

    fun closeSettingsPage() {
        settingsNavigationDirection = NavigationDirection.BACK
        page = SettingsPage.HOME
    }

    BackHandler(enabled = page != SettingsPage.HOME) {
        closeSettingsPage()
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var lastLocationTimestamp by remember { mutableLongStateOf(0L) }
    var lastLocationAccuracy by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(page, isTracking) {
        if (page != SettingsPage.TRACKING) return@LaunchedEffect
        while (page == SettingsPage.TRACKING) {
            val prefs = context.getSharedPreferences(
                "location_dots_ui",
                android.content.Context.MODE_PRIVATE
            )
            lastLocationTimestamp = prefs.getLong("last_location_timestamp", 0L)
            lastLocationAccuracy = if (prefs.contains("last_location_accuracy")) {
                prefs.getFloat("last_location_accuracy", 0f)
            } else {
                null
            }
            kotlinx.coroutines.delay(2_000L)
        }
    }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (!animationsEnabled) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val direction = if (settingsNavigationDirection == NavigationDirection.FORWARD) {
                    AnimatedContentTransitionScope.SlideDirection.Left
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Right
                }
                slideIntoContainer(
                    direction,
                    animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) togetherWith slideOutOfContainer(
                    direction,
                    animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                )
            }
        },
        label = "settings-page-navigation"
    ) { currentPage ->
        when (currentPage) {
            SettingsPage.HOME -> SettingsHome(
                Modifier,
                places.size,
                isTracking,
                actionButton,
                onOpen = ::openSettingsPage,
                onAbout = onAbout,
                onTrackingChange = onTrackingChange
            )
            SettingsPage.SAVED_PLACES -> SettingsSubPage(
                Modifier, "Saved places",
                onBack = ::closeSettingsPage
            ) {
                item {
                    ExpressiveCard(emphasized = true) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExpressiveSectionHeader("Your places", "${places.size} saved ${if (places.size == 1) "place" else "places"}")
                            ExpressiveButton("Add place", { openPlaceEditor(null) }, Modifier.fillMaxWidth())
                        }
                    }
                }
                if (places.isEmpty()) {
                    item {
                        EmptySettingsCard(Icons.Default.Place, "No saved places", "Add Home, Office, Gym or any place you want Location Dots to recognize.")
                    }
                } else {
                    item {
                        ExpressiveCard {
                            Column(Modifier.padding(8.dp)) {
                                places.forEachIndexed { index, place ->
                                    ExpressiveListRow(
                                        title = place.name ?: "Unnamed place",
                                        subtitle = String.format(java.util.Locale.US, "%.5f, %.5f", place.latitude, place.longitude),
                                        icon = {
                                            Icon(
                                                when {
                                                    place.name.equals("Home", true) -> Icons.Default.Home
                                                    place.name.equals("Office", true) || place.name.equals("Work", true) -> Icons.Default.Business
                                                    else -> Icons.Default.Place
                                                }, null
                                            )
                                        },
                                        trailing = {
                                            IconButton(onClick = { openPlaceEditor(place) }) {
                                                Icon(Icons.Default.Edit, "Edit place")
                                            }
                                        }
                                    )
                                    if (index != places.lastIndex) {
                                        HorizontalDivider(
                                            Modifier.padding(horizontal = 14.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            SettingsPage.TRACKING -> SettingsSubPage(
                Modifier, "Tracking",
                onBack = ::closeSettingsPage
            ) {
                item { SettingsGroupTitle("Tracking status", "See whether Location Dots is currently recording your location.") }

                item {
                    ExpressiveCard(emphasized = isTracking) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ExpressiveIconBadge(
                                    icon = {
                                        Icon(
                                            if (isTracking) Icons.Default.LocationOn else Icons.Default.LocationOff,
                                            null
                                        )
                                    },
                                    containerColor = if (isTracking) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    }
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (isTracking) "Tracking active" else "Tracking paused",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        if (isTracking) "Location Dots is recording your location."
                                        else "Location recording is currently paused.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider()
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Last update", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        if (lastLocationTimestamp > 0L) {
                                            java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
                                                .format(java.util.Date(lastLocationTimestamp))
                                        } else "Waiting for location",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Accuracy", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        lastLocationAccuracy?.let { "%.0f m".format(java.util.Locale.US, it) } ?: "—",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            Button(
                                onClick = { onTrackingChange(!isTracking) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isTracking) "Pause tracking" else "Start tracking")
                            }
                        }
                    }
                }

                item { SettingsGroupTitle("Required", "Permissions and Android settings needed for reliable tracking.") }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(8.dp)) {
                            ExpressiveListRow(
                                "Location permission",
                                if (locationPermissionGranted) "Allowed" else "Not allowed",
                                { Icon(Icons.Default.LocationOn, null) },
                                trailing = {
                                    if (locationPermissionGranted) {
                                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                    } else {
                                        TextButton(onClick = onRequestLocationPermission) { Text("Allow") }
                                    }
                                },
                                onClick = if (locationPermissionGranted) null else onRequestLocationPermission
                            )
                            ExpressiveListRow(
                                "Location services",
                                "Android system setting",
                                { Icon(Icons.Default.GpsFixed, null) },
                                trailing = { Icon(Icons.Default.ChevronRight, null) },
                                onClick = onOpenLocationSettings
                            )
                            ExpressiveListRow(
                                "Battery optimization",
                                if (batteryOptimizationIgnored) "Unrestricted" else "Optimized",
                                { Icon(Icons.Default.BatteryChargingFull, null) },
                                trailing = {
                                    if (batteryOptimizationIgnored) {
                                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Icon(Icons.Default.ChevronRight, null)
                                    }
                                },
                                onClick = if (batteryOptimizationIgnored) null else onOpenBatterySettings
                            )
                        }
                    }
                }

                item { SettingsGroupTitle("Location collection", "Choose how accurately and how often Location Dots records your location.") }

                item {
                    ExpressiveCard {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Accuracy", style = MaterialTheme.typography.titleMedium)
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                val options = TrackingAccuracy.entries
                                options.forEachIndexed { index, option ->
                                    SegmentedButton(
                                        selected = trackingAccuracy == option,
                                        onClick = { onTrackingAccuracyChange(option) },
                                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                                        icon = {}
                                    ) { Text(if (option == TrackingAccuracy.HIGH) "High" else "Balanced") }
                                }
                            }
                            Text("Update frequency", style = MaterialTheme.typography.titleMedium)
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                val options = TrackingInterval.entries
                                options.forEachIndexed { index, option ->
                                    SegmentedButton(
                                        selected = trackingInterval == option,
                                        onClick = { onTrackingIntervalChange(option) },
                                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                                        icon = {}
                                    ) {
                                        Text(
                                            when (option) {
                                                TrackingInterval.THIRTY_SECONDS -> "30s"
                                                TrackingInterval.ONE_MINUTE -> "1m"
                                                TrackingInterval.FIVE_MINUTES -> "5m"
                                            }
                                        )
                                    }
                                }
                            }
                            Text(
                                "Shorter intervals use more battery.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item { SettingsGroupTitle("Tracking behavior", "Let Location Dots adapt tracking while you move and stay.") }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(8.dp)) {
                            ExpressiveListRow(
                                "Automatic journey detection",
                                "Detect movement between places automatically",
                                { Icon(Icons.Default.Route, null) },
                                trailing = { Switch(automaticJourneyDetection, onAutomaticJourneyDetectionChange) }
                            )
                            ExpressiveListRow(
                                "Stationary detection",
                                "Reduce strict movement checks while you're still",
                                { Icon(Icons.Default.Accessibility, null) },
                                trailing = { Switch(stationaryDetection, onStationaryDetectionChange) }
                            )
                            ExpressiveListRow(
                                "Background tracking",
                                "Continues while the app is closed",
                                { Icon(Icons.Default.Sync, null) },
                                trailing = {
                                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }
                    }
                }

                item { SettingsGroupTitle("Visit detection", "Decide when a stay becomes a visit on your timeline.") }

                item {
                    ExpressiveCard {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Minimum stay", style = MaterialTheme.typography.titleMedium)
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                val options = VisitDuration.entries
                                options.forEachIndexed { index, option ->
                                    SegmentedButton(
                                        selected = visitDuration == option,
                                        onClick = { onVisitDurationChange(option) },
                                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                                        icon = {}
                                    ) { Text(option.label) }
                                }
                            }
                            Text("Detection radius", style = MaterialTheme.typography.titleMedium)
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                val options = VisitRadius.entries
                                options.forEachIndexed { index, option ->
                                    SegmentedButton(
                                        selected = visitRadius == option,
                                        onClick = { onVisitRadiusChange(option) },
                                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                                        icon = {}
                                    ) { Text(option.label) }
                                }
                            }
                            Text(
                                "A visit is created after you remain within the selected area for the minimum stay.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item { SettingsGroupTitle("Advanced tracking", "Fine-tune location quality and stationary detection.") }

                item {
                    ExpressiveCard {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Maximum GPS accuracy", style = MaterialTheme.typography.titleMedium)
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                val options = TrackingMaxAccuracy.entries
                                options.forEachIndexed { index, option ->
                                    SegmentedButton(
                                        selected = maxTrackingAccuracy == option,
                                        onClick = { onMaxTrackingAccuracyChange(option) },
                                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                                        icon = {}
                                    ) { Text(option.label) }
                                }
                            }
                            Text("Movement threshold", style = MaterialTheme.typography.titleMedium)
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                val options = MovementThreshold.entries
                                options.forEachIndexed { index, option ->
                                    SegmentedButton(
                                        selected = movementThreshold == option,
                                        onClick = { onMovementThresholdChange(option) },
                                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                                        icon = {}
                                    ) { Text(option.label) }
                                }
                            }
                        }
                    }
                }
            }
            SettingsPage.MAP_APPEARANCE -> SettingsSubPage(
                Modifier,
                "Map & appearance",
                "Customize the app theme and how maps are displayed.",
                onBack = ::closeSettingsPage
            ) {
                item {
                    SettingsGroupTitle(
                        "Appearance",
                        "Control the visual style and motion used throughout Location Dots."
                    )
                }

                item {
                    ExpressiveCard(emphasized = true) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ExpressiveIconBadge(
                                    icon = {
                                        Icon(
                                            when (themeChoice) {
                                                ThemeChoice.SYSTEM -> Icons.Default.BrightnessAuto
                                                ThemeChoice.LIGHT -> Icons.Default.LightMode
                                                ThemeChoice.DARK -> Icons.Default.DarkMode
                                            },
                                            null
                                        )
                                    }
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        when (themeChoice) {
                                            ThemeChoice.SYSTEM -> "Follow your device"
                                            ThemeChoice.LIGHT -> "Always use light mode"
                                            ThemeChoice.DARK -> "Always use dark mode"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                ThemeChoice.entries.forEachIndexed { index, choice ->
                                    SegmentedButton(
                                        selected = themeChoice == choice,
                                        onClick = { onThemeChange(choice) },
                                        shape = SegmentedButtonDefaults.itemShape(index, ThemeChoice.entries.size),
                                        icon = {}
                                    ) { Text(choice.label()) }
                                }
                            }

                            ExpressiveListRow(
                                title = "Motion & transitions",
                                subtitle = "Use animations when moving between settings and screens.",
                                icon = { Icon(Icons.Default.Animation, null) },
                                trailing = { Switch(animationsEnabled, onAnimationsChange) }
                            )
                        }
                    }
                }

                item {
                    SettingsGroupTitle(
                        "Map display",
                        "Choose the map style and which visual elements are drawn on the map."
                    )
                }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(8.dp)) {
                            ExpressiveListRow(
                                title = "Map style",
                                subtitle = MapStyleChoice.fromKey(mapStyle).label + " · Tap to preview",
                                icon = { Icon(Icons.Default.Map, null) },
                                trailing = { Icon(Icons.Default.ChevronRight, null) },
                                onClick = {
                                    previewMapStyle = mapStyle
                                    showMapStyleDialog = true
                                }
                            )
                            HorizontalDivider(
                                Modifier.padding(horizontal = 14.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            ExpressiveListRow(
                                title = "Route lines",
                                subtitle = "Draw journey paths between places",
                                icon = { Icon(Icons.Default.Timeline, null) },
                                trailing = { Switch(showRouteLines, onRouteLinesChange) }
                            )
                            ExpressiveListRow(
                                title = "Place markers",
                                subtitle = "Show saved places on the map",
                                icon = { Icon(Icons.Default.Place, null) },
                                trailing = { Switch(showPlaceMarkers, onPlaceMarkersChange) }
                            )
                        }
                    }
                }

                item {
                    SettingsGroupTitle(
                        "Map information",
                        "See the source information required by the map providers."
                    )
                }

                item {
                    ExpressiveCard {
                        ExpressiveListRow(
                            title = "Map credits",
                            subtitle = "OpenFreeMap · OpenStreetMap",
                            icon = { Icon(Icons.Default.Public, null) },
                            trailing = { Icon(Icons.Default.Info, null) },
                            onClick = { showAttributionDialog = true }
                        )
                    }
                }
            }
            SettingsPage.INTERFACE -> SettingsSubPage(
                Modifier,
                "Interface",
                "Customize quick controls used around your timeline.",
                onBack = ::closeSettingsPage
            ) {
                item {
                    SettingsGroupTitle(
                        "Timeline",
                        "Choose the quick action shown in the empty space at the top-left of your timeline."
                    )
                }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(8.dp)) {
                            ActionButton.entries.forEachIndexed { index, option ->
                                val selected = actionButton == option
                                ExpressiveListRow(
                                    title = option.label(),
                                    icon = {
                                        Icon(
                                            when (option) {
                                                ActionButton.THEME -> when (themeChoice) {
                                                    ThemeChoice.SYSTEM -> Icons.Default.BrightnessAuto
                                                    ThemeChoice.LIGHT -> Icons.Default.LightMode
                                                    ThemeChoice.DARK -> Icons.Default.DarkMode
                                                }
                                                ActionButton.REFRESH -> Icons.Default.Refresh
                                                ActionButton.TRACKING -> Icons.Default.MyLocation
                                                ActionButton.TODAY -> Icons.Default.Today
                                                ActionButton.EXPORT -> Icons.Default.FileDownload
                                            },
                                            null
                                        )
                                    },
                                    trailing = {
                                        RadioButton(
                                            selected = selected,
                                            onClick = { onActionButtonChange(option) }
                                        )
                                    },
                                    onClick = { onActionButtonChange(option) }
                                )
                                if (index != ActionButton.entries.lastIndex) {
                                    HorizontalDivider(
                                        Modifier.padding(horizontal = 14.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            SettingsPage.PRIVACY_DATA -> SettingsSubPage(
                Modifier, "Privacy & data",
                onBack = ::closeSettingsPage
            ) {
                item {
                    ExpressiveCard(emphasized = true) {
                        Row(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExpressiveIconBadge(
                                icon = { Icon(Icons.Default.Lock, null) },
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Stored on this device", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "No account or cloud history is required.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.VerifiedUser,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item { SettingsGroupTitle("Data", "Manage your local location history, saved places, and exports.") }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(8.dp)) {
                            ExpressiveListRow(
                                "Location history",
                                icon = { Icon(Icons.Default.History, null) },
                                onClick = { showClearDialog = true }
                            )
                            ExpressiveListRow(
                                "Saved places",
                                icon = { Icon(Icons.Default.Place, null) },
                                onClick = { openSettingsPage(SettingsPage.SAVED_PLACES) }
                            )
                            ExpressiveListRow(
                                "Export your data",
                                icon = { Icon(Icons.Default.FileUpload, null) },
                                onClick = onExportData
                            )
                        }
                    }
                }

                item { SettingsGroupTitle("Permissions", "Review the Android permissions and system settings Location Dots depends on.") }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(8.dp)) {
                            ExpressiveListRow(
                                "Location permission",
                                icon = { Icon(Icons.Default.LocationOn, null) },
                                trailing = {
                                    if (locationPermissionGranted) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        TextButton(onClick = onRequestLocationPermission) {
                                            Text("Allow")
                                        }
                                    }
                                },
                                onClick = if (locationPermissionGranted) null else onRequestLocationPermission
                            )
                            ExpressiveListRow(
                                "Android location settings",
                                icon = { Icon(Icons.Default.GpsFixed, null) },
                                onClick = onOpenLocationSettings
                            )
                        }
                    }
                }

                item { SettingsGroupTitle("Danger zone", "Permanent actions that remove data from this device.") }

                item {
                    ExpressiveCard {
                        ExpressiveListRow(
                            "Delete all local data",
                            icon = { Icon(Icons.Default.DeleteForever, null) },
                            trailing = {
                                Text(
                                    "Delete",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = { showClearDialog = true }
                        )
                    }
                }
            }
            SettingsPage.EXPORT_BACKUP -> SettingsSubPage(
                Modifier, "Export & backup",
                onBack = ::closeSettingsPage
            ) {
                item {
                    ExpressiveCard(emphasized = true) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ExpressiveIconBadge(
                                    icon = { Icon(Icons.Default.Security, null) },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Your backup", style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "JSON file · stored wherever you choose",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            ExpressiveButton(
                                "Export backup",
                                onExportData,
                                Modifier.fillMaxWidth()
                            )
                            OutlinedButton(
                                onClick = onImportData,
                                enabled = !importLoading,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = MaterialTheme.shapes.large
                            ) {
                                if (importLoading) {
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Reading backup…")
                                } else {
                                    Icon(Icons.Default.FileDownload, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Import backup")
                                }
                            }
                        }
                    }
                }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(8.dp)) {
                            ExpressiveListRow(
                                "Activity summary",
                                icon = { Icon(Icons.Default.Share, null) },
                                onClick = onExport
                            )
                            ExpressiveListRow(
                                "What is included",
                                icon = { Icon(Icons.Default.DataObject, null) },
                                trailing = {
                                    Text(
                                        "Places · locations · timeline",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }

                if (importError != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    importError,
                                    Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                IconButton(onClick = onDismissImport) {
                                    Icon(Icons.Default.Close, "Dismiss")
                                }
                            }
                        }
                    }
                }
            }
            SettingsPage.DIAGNOSTICS -> SettingsSubPage(
                Modifier, "Diagnostics",
                onBack = ::closeSettingsPage
            ) {
                item {
                    ExpressiveCard(emphasized = isTracking && locationPermissionGranted) {
                        Row(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExpressiveIconBadge(
                                icon = {
                                    Icon(
                                        if (isTracking) Icons.Default.CheckCircle else Icons.Default.PauseCircleOutline,
                                        null
                                    )
                                },
                                containerColor = if (isTracking) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (isTracking) "Tracking is running" else "Tracking is stopped",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    if (!locationPermissionGranted) "Location permission is required"
                                    else if (isTracking) "Location updates can be collected"
                                    else "No location updates are being collected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item { SettingsGroupTitle("System checks", "Check the permissions and system conditions that affect background tracking.") }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(8.dp)) {
                            DiagnosticRow(
                                "Location permission",
                                if (locationPermissionGranted) "Ready" else "Needs permission",
                                if (locationPermissionGranted) Icons.Default.CheckCircle else Icons.Default.Warning
                            )
                            DiagnosticRow(
                                "Location services",
                                "Android setting",
                                Icons.Default.GpsFixed
                            )
                            DiagnosticRow(
                                "Battery optimization",
                                if (batteryOptimizationIgnored) "Unrestricted" else "May limit background tracking",
                                if (batteryOptimizationIgnored) Icons.Default.BatteryFull else Icons.Default.BatteryAlert
                            )
                        }
                    }
                }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(8.dp)) {
                            ExpressiveListRow(
                                "Location settings",
                                icon = { Icon(Icons.Default.GpsFixed, null) },
                                trailing = { Icon(Icons.Default.ChevronRight, null) },
                                onClick = onOpenLocationSettings
                            )
                            ExpressiveListRow(
                                "Battery optimization",
                                icon = { Icon(Icons.Default.BatterySaver, null) },
                                trailing = { Icon(Icons.Default.ChevronRight, null) },
                                onClick = onOpenBatterySettings
                            )
                            ExpressiveListRow(
                                "Share diagnostic report",
                                icon = { Icon(Icons.Default.Share, null) },
                                trailing = { Icon(Icons.Default.ChevronRight, null) },
                                onClick = onShareDiagnostics
                            )
                        }
                    }
                }

                item { SettingsGroupTitle("App status", "See the storage, map, places, and tracking configuration currently in use.") }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(8.dp)) {
                            DiagnosticRow("Storage", "Local Room database", Icons.Default.Storage)
                            DiagnosticRow("Maps", "MapLibre + OpenFreeMap", Icons.Default.Map)
                            DiagnosticRow("Saved places", places.size.toString(), Icons.Default.Place)
                            DiagnosticRow(
                                "Tracking profile",
                                trackingAccuracy.name.lowercase().replaceFirstChar { it.uppercase() } + " · " + trackingInterval.label,
                                Icons.Default.Tune
                            )
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onResetSettings,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.Restore, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reset app settings")
                    }
                }
            }
        }
    }

    if (showPlaceEditor) {
        PlaceEditorDialog(
            initialPlace = editingPlace,
            onDismiss = { showPlaceEditor = false; editingPlace = null },
            onSave = { name, latitude, longitude ->
                onSaveNamedPlace(name, latitude, longitude)
                showPlaceEditor = false
                editingPlace = null
            },
            onRequestCurrentLocation = onRequestCurrentLocation
        )
    }

    if (importPreview != null) {
        AlertDialog(
            onDismissRequest = onDismissImport,
            icon = { Icon(Icons.Default.FileDownload, null) },
            title = { Text("Import backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Location Dots found the following data in this backup.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ImportStatRow("Places", importPreview.places)
                            ImportStatRow("Locations", importPreview.locations)
                            ImportStatRow("Timeline events", importPreview.timelineEvents)
                        }
                    }
                    Text(
                        "Replace all removes the current local history before importing. Merge keeps existing data and adds the backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirmImport(importPreview, true) },
                    enabled = !importLoading
                ) {
                    Text("Replace all")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onConfirmImport(importPreview, false) },
                    enabled = !importLoading
                ) {
                    Text("Merge")
                }
            }
        )
    }

    if (showMapStyleDialog) {
        AlertDialog(
            onDismissRequest = { showMapStyleDialog = false },
            title = { Text("Map style") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        LocationMap(
                            points = listOf(
                                org.maplibre.android.geometry.LatLng(18.5204, 73.8567),
                                org.maplibre.android.geometry.LatLng(18.5314, 73.8446),
                                org.maplibre.android.geometry.LatLng(18.5089, 73.8077)
                            ),
                            modifier = Modifier.fillMaxWidth().height(190.dp),
                            interactive = false,
                            fitRequest = previewMapStyle,
                            mapStyle = previewMapStyle,
                            showRouteLines = true,
                            showPlaceMarkers = true
                        )
                    }
                    MapStyleChoice.entries.forEach { style ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = previewMapStyle == style.key,
                                onClick = { previewMapStyle = style.key }
                            )
                            Text(style.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onMapStyleChange(previewMapStyle)
                    showMapStyleDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showMapStyleDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAttributionDialog) {
        AlertDialog(
            onDismissRequest = { showAttributionDialog = false },
            icon = { Icon(Icons.Default.Public, null) },
            title = { Text("Map credits") },
            text = {
                Text("Location Dots uses OpenFreeMap map tiles with OpenStreetMap data. These credits are required by the map providers.")
            },
            confirmButton = {
                TextButton(onClick = { showAttributionDialog = false }) { Text("Done") }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, null) },
            title = { Text("Delete all local data?") },
            text = { Text("This permanently removes recorded locations, saved places and timeline events from this device. This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    showClearDialog = false
                    onClearHistory()
                    page = SettingsPage.HOME
                }) { Text("Delete all data") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsHome(
    modifier: Modifier,
    placesCount: Int,
    isTracking: Boolean,
    actionButton: ActionButton,
    onOpen: (SettingsPage) -> Unit,
    onAbout: () -> Unit,
    onTrackingChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(18.dp, 10.dp, 18.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 2.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineLarge)
            }
        }

        // Most-used control stays at the top of Settings.
        item {
            ExpressiveCard(emphasized = isTracking) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveIconBadge(
                        icon = {
                            Icon(
                                if (isTracking) Icons.Default.MyLocation else Icons.Default.LocationOff,
                                null
                            )
                        },
                        containerColor = if (isTracking) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Location tracking",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            if (isTracking) {
                                "Recording your location in the background."
                            } else {
                                "Tracking is paused. No new location points are recorded."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = isTracking,
                        onCheckedChange = onTrackingChange
                    )
                }
            }
        }

        item {
            SettingsGroupTitle(
                "Tracking",
            )
        }
        item {
            ExpressiveCard {
                ExpressiveListRow(
                    "Tracking settings",
                    "Accuracy, frequency, and permissions",
                    { Icon(Icons.Default.LocationOn, null) },
                    trailing = { Icon(Icons.Default.ChevronRight, null) },
                    onClick = { onOpen(SettingsPage.TRACKING) }
                )
            }
        }

        item {
            SettingsGroupTitle(
                "Appearance",
            )
        }
        item {
            ExpressiveCard {
                ExpressiveListRow(
                    "Map & appearance",
                    "Theme, map style, routes, and place markers",
                    { Icon(Icons.Default.Palette, null) },
                    trailing = { Icon(Icons.Default.ChevronRight, null) },
                    onClick = { onOpen(SettingsPage.MAP_APPEARANCE) }
                )
            }
        }

        item {
            SettingsGroupTitle(
                "Interface",
            )
        }
        item {
            ExpressiveCard {
                ExpressiveListRow(
                    "Timeline action button",
                    actionButton.label(),
                    { Icon(Icons.Default.Tune, null) },
                    trailing = { Icon(Icons.Default.ChevronRight, null) },
                    onClick = { onOpen(SettingsPage.INTERFACE) }
                )
            }
        }

        item {
            SettingsGroupTitle(
                "Places & data",
            )
        }
        item {
            ExpressiveCard {
                Column(Modifier.padding(8.dp)) {
                    ExpressiveListRow(
                        "Saved places",
                        "$placesCount ${if (placesCount == 1) "place" else "places"} saved",
                        { Icon(Icons.Default.Place, null) },
                        trailing = { Icon(Icons.Default.ChevronRight, null) },
                        onClick = { onOpen(SettingsPage.SAVED_PLACES) }
                    )
                    ExpressiveListRow(
                        "Privacy & data",
                        "Local history, permissions, and data controls",
                        { Icon(Icons.Default.Lock, null) },
                        trailing = { Icon(Icons.Default.ChevronRight, null) },
                        onClick = { onOpen(SettingsPage.PRIVACY_DATA) }
                    )
                }
            }
        }

        item {
            SettingsGroupTitle(
                "Backup",
            )
        }
        item {
            ExpressiveCard {
                ExpressiveListRow(
                    "Export & backup",
                    "Import or export your local data",
                    { Icon(Icons.Default.FileUpload, null) },
                    trailing = { Icon(Icons.Default.ChevronRight, null) },
                    onClick = { onOpen(SettingsPage.EXPORT_BACKUP) }
                )
            }
        }

        item {
            SettingsGroupTitle(
                "Support",
            )
        }
        item {
            ExpressiveCard {
                Column(Modifier.padding(8.dp)) {
                    ExpressiveListRow(
                        "Diagnostics",
                        "Permissions, battery, maps, and app status",
                        { Icon(Icons.Default.Build, null) },
                        trailing = { Icon(Icons.Default.ChevronRight, null) },
                        onClick = { onOpen(SettingsPage.DIAGNOSTICS) }
                    )
                    ExpressiveListRow(
                        title = "About Location Dots",
                        subtitle = "All about the Location dots App",
                        icon = { Icon(Icons.Default.Info, null) },
                        trailing = { Icon(Icons.Default.ChevronRight, null) },
                        onClick = onAbout
                    )
                }
            }
        }
    }
}


@Composable
private fun SettingsSubPage(
    modifier: Modifier,
    title: String,
    subtitle: String = "",
    onBack: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(18.dp, 10.dp, 18.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Spacer(Modifier.width(12.dp))
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        content()
    }
}

@Composable
private fun ImportStatRow(label: String, value: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SettingsGroupTitle(
    text: String,
    description: String = ""
) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium
        )
        if(description.isNotEmpty()) {
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptySettingsCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    ExpressiveCard {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ExpressiveIconBadge(icon = { Icon(icon, null) })
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    ExpressiveCard {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            ExpressiveIconBadge(icon = { Icon(icon, null) })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DiagnosticRow(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ExpressiveListRow(title, icon = { Icon(icon, null) }, trailing = {
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    })
}

private fun ThemeChoice.label(): String = when (this) {
    ThemeChoice.SYSTEM -> "System"
    ThemeChoice.LIGHT -> "Light"
    ThemeChoice.DARK -> "Dark"
}
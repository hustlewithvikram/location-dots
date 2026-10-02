package com.locationdots.app.feature.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
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

data class ImportPreview(
    val uri: Uri,
    val locations: Int,
    val places: Int,
    val timelineEvents: Int,
    val generatedAtEpochMillis: Long
)

private enum class SettingsPage {
    HOME, SAVED_PLACES, TRACKING, MAP_APPEARANCE, PRIVACY_DATA, EXPORT_BACKUP, DIAGNOSTICS
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
    onThemeChange: (ThemeChoice) -> Unit,
    onTrackingChange: (Boolean) -> Unit,
    onTrackingAccuracyChange: (TrackingAccuracy) -> Unit,
    onTrackingIntervalChange: (TrackingInterval) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onMapStyleChange: (String) -> Unit,
    onRouteLinesChange: (Boolean) -> Unit,
    onPlaceMarkersChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    importPreview: ImportPreview?,
    importError: String?,
    onDismissImport: () -> Unit,
    onConfirmImport: (ImportPreview, Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onResetSettings: () -> Unit,
    onAbout: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onSaveNamedPlace: (String, Double, Double) -> Unit,
    onRequestCurrentLocation: ((onLocation: (Double, Double) -> Unit) -> Unit)
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.HOME) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showPlaceEditor by remember { mutableStateOf(false) }
    var showMapStyleDialog by remember { mutableStateOf(false) }
    var editingPlace by remember { mutableStateOf<Place?>(null) }

    BackHandler(enabled = page != SettingsPage.HOME) { page = SettingsPage.HOME }

    fun openPlaceEditor(place: Place?) {
        editingPlace = place
        showPlaceEditor = true
    }

    Scaffold { padding ->
        when (page) {
            SettingsPage.HOME -> SettingsHome(
                Modifier.padding(padding), places.size, isTracking,
                onOpen = { page = it }, onAbout = onAbout
            )
            SettingsPage.SAVED_PLACES -> SettingsSubPage(
                Modifier.padding(padding), "Saved places",
                "Places Location Dots should recognize automatically.",
                onBack = { page = SettingsPage.HOME }
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
                Modifier.padding(padding), "Tracking",
                "",
                onBack = { page = SettingsPage.HOME }
            ) {
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
                                    if (isTracking) "Tracking active" else "Tracking paused",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    if (isTracking) "Recording location in the background"
                                    else "No new location points are being recorded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isTracking,
                                onCheckedChange = onTrackingChange
                            )
                        }
                    }
                }

                item { SettingsGroupTitle("Required") }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(8.dp)) {
                            ExpressiveListRow(
                                "Location permission",
                                if (locationPermissionGranted) "Allowed" else "Not allowed",
                                { Icon(Icons.Default.LocationOn, null) },
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
                                "Location services",
                                "Android system setting",
                                { Icon(Icons.Default.GpsFixed, null) },
                                trailing = {
                                    Icon(Icons.Default.ChevronRight, null)
                                },
                                onClick = onOpenLocationSettings
                            )
                        }
                    }
                }

                item { SettingsGroupTitle("Location collection") }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(8.dp)) {
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
                                    ) {
                                        Text(if (option == TrackingAccuracy.HIGH) "High" else "Balanced")
                                    }
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
                }
            }
            SettingsPage.MAP_APPEARANCE -> SettingsSubPage(
                Modifier.padding(padding), "Map & appearance",
                "Tune the visual experience without changing your data.",
                onBack = { page = SettingsPage.HOME }
            ) {
                item {
                    ExpressiveCard {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExpressiveSectionHeader("Theme", "Choose how Location Dots follows your device.")
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
                                "Motion & transitions",
                                if (animationsEnabled) "Expressive animations are enabled." else "Animations are reduced.",
                                { Icon(Icons.Default.Animation, null) },
                                trailing = { Switch(animationsEnabled, onAnimationsChange) }
                            )
                        }
                    }
                }
                item {
                    ExpressiveCard {
                        Column(Modifier.padding(10.dp)) {
                            ExpressiveListRow(
                                "Map style",
                                "${MapStyleChoice.fromKey(mapStyle).label} · OpenFreeMap",
                                { Icon(Icons.Default.Map, null) },
                                onClick = { showMapStyleDialog = true }
                            )
                            ExpressiveListRow(
                                "Route lines",
                                "Show movement paths on timeline and journey maps.",
                                { Icon(Icons.Default.Timeline, null) },
                                trailing = { Switch(showRouteLines, onRouteLinesChange) }
                            )
                            ExpressiveListRow(
                                "Place markers",
                                "Show locations as markers on maps.",
                                { Icon(Icons.Default.Place, null) },
                                trailing = { Switch(showPlaceMarkers, onPlaceMarkersChange) }
                            )
                        }
                    }
                }
                item {
                    InfoCard(Icons.Default.Public, "Map attribution", "MapLibre displays the required attribution for OpenFreeMap, OpenMapTiles and OpenStreetMap.")
                }
            }
            SettingsPage.PRIVACY_DATA -> SettingsSubPage(
                Modifier.padding(padding), "Privacy & data",
                "",
                onBack = { page = SettingsPage.HOME }
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

                item { SettingsGroupTitle("Data") }

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
                                onClick = { page = SettingsPage.SAVED_PLACES }
                            )
                            ExpressiveListRow(
                                "Export your data",
                                icon = { Icon(Icons.Default.FileUpload, null) },
                                onClick = onExportData
                            )
                        }
                    }
                }

                item { SettingsGroupTitle("Permissions") }

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

                item { SettingsGroupTitle("Danger zone") }

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
                Modifier.padding(padding), "Export & backup",
                "",
                onBack = { page = SettingsPage.HOME }
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
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(Icons.Default.FileDownload, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Import backup")
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
                Modifier.padding(padding), "Diagnostics",
                "Useful runtime information for troubleshooting.",
                onBack = { page = SettingsPage.HOME }
            ) {
                item {
                    ExpressiveCard {
                        Column(Modifier.padding(10.dp)) {
                            DiagnosticRow("Tracking service", if (isTracking) "Running" else "Stopped", Icons.Default.LocationOn)
                            DiagnosticRow("Location permission", if (locationPermissionGranted) "Granted" else "Not granted", Icons.Default.Security)
                            DiagnosticRow("Storage", "Room · on device", Icons.Default.Storage)
                            DiagnosticRow("Maps", "MapLibre · OpenFreeMap", Icons.Default.Map)
                            DiagnosticRow("Saved places", places.size.toString(), Icons.Default.Place)
                        }
                    }
                }
                item {
                    InfoCard(Icons.Default.Build, "Troubleshooting", "If tracking is not recording, verify location permission, Android location services, and that tracking is enabled.")
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

    if (showMapStyleDialog) {
        val selectedStyle = MapStyleChoice.fromKey(mapStyle)
        AlertDialog(
            onDismissRequest = { showMapStyleDialog = false },
            title = { Text("Map style") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MapStyleChoice.entries.forEach { style ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedStyle == style,
                                onClick = {
                                    onMapStyleChange(style.key)
                                    showMapStyleDialog = false
                                }
                            )
                            Text(
                                style.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMapStyleDialog = false }) { Text("Done") }
            }
        )
    }

    if (importPreview != null) {
        AlertDialog(
            onDismissRequest = onDismissImport,
            icon = { Icon(Icons.Default.FileDownload, null) },
            title = { Text("Import backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "This backup contains:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    ImportStatRow("Places", importPreview.places)
                    ImportStatRow("Locations", importPreview.locations)
                    ImportStatRow("Timeline events", importPreview.timelineEvents)
                    HorizontalDivider()
                    Text(
                        "Choose how to apply it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmImport(importPreview, false) }
                ) {
                    Text("Merge")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = { onConfirmImport(importPreview, true) }
                    ) {
                        Text(
                            "Replace all",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = onDismissImport) {
                        Text("Cancel")
                    }
                }
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
    onOpen: (SettingsPage) -> Unit,
    onAbout: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 2.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(4.dp))
                Text("Make Location Dots work the way you want.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            ExpressiveCard(emphasized = true) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExpressiveIconBadge(
                        icon = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.onPrimary) },
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Location Dots", style = MaterialTheme.typography.titleLarge)
                        Text(if (isTracking) "Tracking is active" else "Tracking is paused", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { SettingsGroupTitle("Your data") }
        item {
            ExpressiveCard {
                Column(Modifier.padding(8.dp)) {
                    ExpressiveListRow("Saved places", icon = { Icon(Icons.Default.Place, null) }, onClick = { onOpen(SettingsPage.SAVED_PLACES) })
                    ExpressiveListRow("Privacy & data", icon = { Icon(Icons.Default.Lock, null) }, onClick = { onOpen(SettingsPage.PRIVACY_DATA) })
                    ExpressiveListRow("Export & backup", icon = { Icon(Icons.Default.FileUpload, null) }, onClick = { onOpen(SettingsPage.EXPORT_BACKUP) })
                }
            }
        }
        item { SettingsGroupTitle("Experience") }
        item {
            ExpressiveCard {
                Column(Modifier.padding(8.dp)) {
                    ExpressiveListRow("Tracking", icon = { Icon(Icons.Default.LocationOn, null) }, onClick = { onOpen(SettingsPage.TRACKING) })
                    ExpressiveListRow("Map & appearance", icon = { Icon(Icons.Default.Palette, null) }, onClick = { onOpen(SettingsPage.MAP_APPEARANCE) })
                }
            }
        }
        item { SettingsGroupTitle("Support") }
        item {
            ExpressiveCard {
                Column(Modifier.padding(8.dp)) {
                    ExpressiveListRow("Diagnostics", icon = { Icon(Icons.Default.Build, null) }, onClick = { onOpen(SettingsPage.DIAGNOSTICS) })
                    ExpressiveListRow("About Location Dots", icon = { Icon(Icons.Default.Info, null) }, onClick = onAbout)
                }
            }
        }
    }
}

@Composable
private fun SettingsSubPage(
    modifier: Modifier,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 10.dp, 18.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    // Keep sub-page headers intentionally compact; the page title carries the context.
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
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsGroupTitle(text: String) {
    Text(text, Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    ExpressiveListRow(title, null, { Icon(icon, null) }, trailing = {
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    })
}

private fun ThemeChoice.label(): String = when (this) {
    ThemeChoice.SYSTEM -> "System"
    ThemeChoice.LIGHT -> "Light"
    ThemeChoice.DARK -> "Dark"
}

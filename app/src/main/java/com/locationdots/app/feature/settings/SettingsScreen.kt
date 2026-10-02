package com.locationdots.app.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.Place
import com.locationdots.app.feature.place.PlaceEditorDialog
import com.locationdots.app.ui.components.*

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedTab: AppTab,
    themeChoice: ThemeChoice,
    isTracking: Boolean,
    animationsEnabled: Boolean,
    places: List<Place>,
    onTabSelected: (AppTab) -> Unit,
    onThemeChange: (ThemeChoice) -> Unit,
    onTrackingChange: (Boolean) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onClearHistory: () -> Unit,
    onAbout: () -> Unit,
    onSaveNamedPlace: (String, Double, Double) -> Unit,
    onRequestCurrentLocation: ((onLocation: (Double, Double) -> Unit) -> Unit)
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showPlaceEditor by remember { mutableStateOf(false) }
    var editingPlace by remember { mutableStateOf<Place?>(null) }

    fun openPlaceEditor(place: Place?) {
        editingPlace = place
        showPlaceEditor = true
    }

    Scaffold { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column {
                    Text("Settings", style = MaterialTheme.typography.headlineLarge)
                    Text("Shape how Location Dots works for you.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                ExpressiveCard(emphasized = true) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpressiveSectionHeader("Automatic tracking", "Capture places and journeys in the background.")
                        ExpressiveListRow(
                            "Location tracking",
                            if (isTracking) "Active now" else "Paused",
                            { Icon(Icons.Default.LocationOn, null) },
                            trailing = { Switch(checked = isTracking, onCheckedChange = onTrackingChange) }
                        )
                    }
                }
            }
            item {
                ExpressiveCard(emphasized = true) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExpressiveSectionHeader(
                            "Saved locations",
                            "Name Home, Office and other places so matching visits stay recognizable."
                        )
                        Button(
                            onClick = { openPlaceEditor(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add a location")
                        }
                        if (places.isEmpty()) {
                            Text(
                                "No saved locations yet. Add one manually or name an existing place from its detail screen.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                places.forEach { place ->
                                    ExpressiveListRow(
                                        title = place.name ?: "Unnamed place",
                                        subtitle = String.format(java.util.Locale.US, "%.5f, %.5f", place.latitude, place.longitude),
                                        icon = {
                                            Icon(
                                                when {
                                                    place.name.equals("Home", ignoreCase = true) -> Icons.Default.Home
                                                    place.name.equals("Office", ignoreCase = true) ||
                                                        place.name.equals("Work", ignoreCase = true) -> Icons.Default.Business
                                                    else -> Icons.Default.Place
                                                },
                                                null
                                            )
                                        },
                                        trailing = {
                                            IconButton(onClick = { openPlaceEditor(place) }) {
                                                Icon(Icons.Default.Edit, "Edit location")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                ExpressiveCard {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExpressiveSectionHeader("Appearance", "Expressive Material design with your choice of theme.")
                        Text("Theme", style = MaterialTheme.typography.labelLarge)
                        SingleChoiceSegmentedButtonRow {
                            ThemeChoice.entries.forEachIndexed { index, choice ->
                                SegmentedButton(
                                    selected = themeChoice == choice,
                                    onClick = { onThemeChange(choice) },
                                    shape = SegmentedButtonDefaults.itemShape(index, ThemeChoice.entries.size),
                                    icon = {}
                                ) { Text(choice.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            }
                        }
                        ExpressiveListRow(
                            "Motion & transitions",
                            "Use expressive screen transitions and animated surfaces.",
                            { Icon(Icons.Default.Speed, null) },
                            trailing = { Switch(checked = animationsEnabled, onCheckedChange = onAnimationsChange) }
                        )
                    }
                }
            }
            item {
                ExpressiveCard {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        ExpressiveSectionHeader("Privacy & data")
                        ExpressiveListRow("Private by design", "Your location history stays on this device.", { Icon(Icons.Default.PrivacyTip, null) }, trailing = { Icon(Icons.Default.VerifiedUser, null) })
                        ExpressiveListRow("Export summary", "Share a readable summary of your activity.", { Icon(Icons.Default.Share, null) }, onClick = onExport)
                        ExpressiveListRow("Clear local history", "Delete recorded locations, places and timeline events.", { Icon(Icons.Default.DeleteOutline, null) }, onClick = { showClearDialog = true })
                    }
                }
            }
            item {
                ExpressiveCard {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        ExpressiveSectionHeader("More")
                        ExpressiveListRow("Storage", "Local database and cached timeline data.", { Icon(Icons.Default.Storage, null) })
                        ExpressiveListRow("About Location Dots", "Version, architecture and open-source information.", { Icon(Icons.Default.Info, null) }, onClick = onAbout)
                    }
                }
            }
        }
    }

    if (showPlaceEditor) {
        PlaceEditorDialog(
            initialPlace = editingPlace,
            onDismiss = {
                showPlaceEditor = false
                editingPlace = null
            },
            onSave = { name, latitude, longitude ->
                onSaveNamedPlace(name, latitude, longitude)
                showPlaceEditor = false
                editingPlace = null
            },
            onRequestCurrentLocation = onRequestCurrentLocation
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear local history?") },
            text = { Text("This removes recorded locations, places and timeline events from this device.") },
            confirmButton = { TextButton(onClick = { showClearDialog = false; onClearHistory() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }
}

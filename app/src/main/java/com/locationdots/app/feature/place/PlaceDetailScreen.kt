package com.vikram.locationdots.feature.place

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalConfiguration
import org.maplibre.android.geometry.LatLng
import androidx.compose.ui.unit.dp
import com.vikram.locationdots.domain.model.Place
import com.vikram.locationdots.domain.model.TimelineEvent
import com.vikram.locationdots.ui.components.*
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    place: Place?,
    visits: List<TimelineEvent.Visit>,
    onBack: () -> Unit,
    onRename: (String?) -> Unit,
    onDeletePlace: (String) -> Unit = {}
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameText by remember(place?.id) { mutableStateOf(place?.name.orEmpty()) }
    var showMapFullscreen by remember { mutableStateOf(false) }
    val currentLocale = LocalConfiguration.current.locales[0]

    Scaffold { padding ->
        if (place == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val zone = ZoneId.systemDefault()
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", currentLocale)
            val sortedVisits = remember(visits) { visits.sortedByDescending { it.arrival } }
            val completedVisits = sortedVisits.filter { it.departure != null }
            val completedStayMinutes = completedVisits.sumOf {
                Duration.between(it.arrival, it.departure!!).toMinutes().coerceAtLeast(0)
            }
            val activeVisit = sortedVisits.firstOrNull { it.departure == null }
            var now by remember(activeVisit?.id) { mutableStateOf(Instant.now()) }

            LaunchedEffect(activeVisit?.id) {
                if (activeVisit == null) return@LaunchedEffect
                while (true) {
                    now = Instant.now()
                    delay(30_000)
                }
            }

            val activeMinutes = activeVisit?.let {
                Duration.between(it.arrival, now).toMinutes().coerceAtLeast(0)
            }
            val totalStayMinutes = completedStayMinutes + (activeMinutes ?: 0L)
            val completedDurations = completedVisits.map {
                Duration.between(it.arrival, it.departure!!).toMinutes().coerceAtLeast(0)
            }.sorted()
            val typicalStayMinutes = if (completedDurations.size >= 2) {
                val middle = completedDurations.size / 2
                if (completedDurations.size % 2 == 0) {
                    (completedDurations[middle - 1] + completedDurations[middle]) / 2
                } else {
                    completedDurations[middle]
                }
            } else null
            val lastVisit = sortedVisits.firstOrNull()
            val points = listOf(LatLng(place.latitude, place.longitude))

            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                place.name ?: "Unnamed place",
                                style = MaterialTheme.typography.headlineMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                DateTimeFormatter.ofPattern("EEE, d MMM yyyy", currentLocale)
                                    .format(place.createdAt.atZone(zone)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalIconButton(onClick = {
                            renameText = place.name.orEmpty()
                            showRenameDialog = true
                        }) {
                            Icon(Icons.Default.Edit, "Rename place")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete place", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                item {
                    ExpressiveCard(emphasized = true) {
                        Box(Modifier.fillMaxWidth().height(190.dp)) {
                            LocationMap(
                                points = points,
                                modifier = Modifier.fillMaxSize(),
                                interactive = true,
                                drawRoute = false
                            )
                            FilledTonalIconButton(
                                onClick = { showMapFullscreen = true },
                                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                            ) {
                                Icon(Icons.Default.Fullscreen, "Open map fullscreen")
                            }
                        }
                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(
                            Triple(Icons.Default.Repeat, sortedVisits.size.toString(), "Total visits"),
                            Triple(Icons.Default.Schedule, formatMinutes(totalStayMinutes), "Time spent here")
                        ).forEach { (icon, value, label) ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        value,
                                        style = MaterialTheme.typography.headlineSmall,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (activeVisit != null && label == "Time spent here") {
                                        Text(
                                            "Includes current stay",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (lastVisit != null || typicalStayMinutes != null) {
                    item {
                        ExpressiveCard {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text("At a glance", style = MaterialTheme.typography.titleMedium)
                                if (lastVisit != null) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ExpressiveIconBadge(
                                            icon = { Icon(Icons.Default.CalendarMonth, null) }
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(
                                            Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text("Last visit", style = MaterialTheme.typography.bodyLarge)
                                            Text(
                                                DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", currentLocale)
                                                    .format(lastVisit.arrival.atZone(zone)),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (typicalStayMinutes != null) {
                                    if (lastVisit != null) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ExpressiveIconBadge(
                                            icon = { Icon(Icons.Default.QueryStats, null) }
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(
                                            Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text("Typical visit", style = MaterialTheme.typography.bodyLarge)
                                            Text(
                                                formatMinutes(typicalStayMinutes),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                "Median of completed visits",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    ExpressiveSectionHeader(
                        "Visit history",
                        if (lastVisit == null) "No visits recorded yet." else "Most recent first."
                    )
                }

                if (sortedVisits.isEmpty()) {
                    item {
                        ExpressiveCard {
                            Column(
                                Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ExpressiveIconBadge(icon = { Icon(Icons.Default.History, null) })
                                Text("No visits yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Visits will appear here.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(sortedVisits, key = { it.id }) { visit ->
                        val end = visit.departure ?: now
                        val duration = formatMinutes(
                            Duration.between(visit.arrival, end).toMinutes().coerceAtLeast(0)
                        )
                        val timeRange = timeFormatter.format(visit.arrival.atZone(zone)) + " – " +
                            if (visit.departure == null) "Now" else timeFormatter.format(end.atZone(zone))
                        ExpressiveCard {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ExpressiveIconBadge(
                                    icon = {
                                        Icon(
                                            if (visit.departure == null) Icons.Default.MyLocation else Icons.Default.History,
                                            null
                                        )
                                    }
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(
                                    Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            DateTimeFormatter.ofPattern("EEE, d MMM yyyy", currentLocale)
                                                .format(visit.arrival.atZone(zone)),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        if (visit.departure == null) {
                                            Text(
                                                "ONGOING",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Text(
                                        timeRange,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        duration,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        "duration",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showMapFullscreen) {
                Dialog(
                    onDismissRequest = { showMapFullscreen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            LocationMap(
                                points = points,
                                modifier = Modifier.fillMaxSize(),
                                interactive = true,
                                drawRoute = false
                            )
                            FilledTonalIconButton(
                                onClick = { showMapFullscreen = false },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                            ) {
                                Icon(Icons.Default.Close, "Close fullscreen map")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && place != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete ${place.name ?: "this place"}?") },
            text = {
                Text("The saved place and its recorded visits will be removed. Location history and unrelated journeys will remain on this device. If you visit this area again, Location Dots may discover it again.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePlace(place.id)
                    showDeleteDialog = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRenameDialog && place != null) {
        RenamePlaceDialog(
            name = renameText,
            onNameChange = { renameText = it },
            onDismiss = { showRenameDialog = false },
            onSave = {
                onRename(renameText.trim().ifEmpty { null })
                showRenameDialog = false
            }
        )
    }
}
@Composable
private fun RenamePlaceDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name this place") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Location name") },
                placeholder = { Text("e.g. Home, Office, Gym") }
            )
        },
        confirmButton = {
            Button(onClick = onSave, enabled = name.trim().isNotEmpty()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatMinutes(minutes: Long): String = if (minutes < 60) minutes.toString() + " min" else (minutes / 60).toString() + "h " + (minutes % 60).toString() + "m"

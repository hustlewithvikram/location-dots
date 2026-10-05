package com.locationdots.app.feature.place

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
import com.locationdots.app.domain.model.Place
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.ui.components.*
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
    onRename: (String?) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
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
            val sortedVisits = remember(visits) { visits.sortedByDescending { it.arrival } }
            val completedVisits = sortedVisits.filter { it.departure != null }
            val totalMinutes = completedVisits.sumOf {
                Duration.between(it.arrival, it.departure!!).toMinutes().coerceAtLeast(0)
            }
            val average = if (completedVisits.isNotEmpty()) totalMinutes / completedVisits.size else 0
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
                                "Place details",
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
                    }
                }

                item {
                    ExpressiveCard(emphasized = true) {
                        Box(Modifier.fillMaxWidth().height(240.dp)) {
                            LocationMap(
                                points = points,
                                modifier = Modifier.fillMaxSize(),
                                interactive = true,
                                drawRoute = false
                            )
                            Surface(
                                Modifier.align(Alignment.TopStart).padding(12.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = .92f)
                            ) {
                                Text(
                                    "Saved location",
                                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
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
                    ExpressiveCard {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                place.name ?: "Unnamed place",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                String.format(Locale.US, "%.5f, %.5f", place.latitude, place.longitude),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Saved " + DateTimeFormatter.ofPattern("d MMM yyyy", currentLocale)
                                    .format(place.createdAt.atZone(zone)),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExpressiveMetric(
                            sortedVisits.size.toString(),
                            "visits",
                            Modifier.weight(1f),
                            { Icon(Icons.Default.Repeat, null) }
                        )
                        ExpressiveMetric(
                            formatMinutes(totalMinutes),
                            "total stay",
                            Modifier.weight(1f),
                            { Icon(Icons.Default.Schedule, null) }
                        )
                        ExpressiveMetric(
                            formatMinutes(average),
                            "avg stay",
                            Modifier.weight(1f),
                            { Icon(Icons.Default.Timelapse, null) }
                        )
                    }
                    if (completedVisits.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (activeVisit != null)
                                "Stay totals and average are based on " + completedVisits.size + " completed visits. Your current stay is shown separately below."
                            else
                                "Stay totals and average are based on " + completedVisits.size + " completed visits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (activeVisit != null && activeMinutes != null) {
                        Spacer(Modifier.height(10.dp))
                        ExpressiveCard {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ExpressiveIconBadge(
                                    icon = { Icon(Icons.Default.LocationOn, null) }
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Currently here",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "Started " + DateTimeFormatter.ofPattern("HH:mm", currentLocale)
                                            .format(activeVisit.arrival.atZone(zone)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    formatMinutes(activeMinutes),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }

                item {
                    ExpressiveSectionHeader(
                        "Visit history",
                        when {
                            lastVisit == null -> "No visits recorded yet."
                            activeVisit != null -> sortedVisits.size.toString() + " sessions · " +
                                completedVisits.size + " completed · 1 current"
                            else -> completedVisits.size.toString() + " completed sessions · Most recent first."
                        }
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
                                    "Visits to this saved place will appear here as tracking records them.",
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
                        val timeRange = DateTimeFormatter.ofPattern(
                            "HH:mm",
                            currentLocale
                        ).format(visit.arrival.atZone(zone)) + " – " +
                            if (visit.departure == null) "Now" else
                                DateTimeFormatter.ofPattern("HH:mm", currentLocale)
                                    .format(end.atZone(zone))
                        ExpressiveListRow(
                            title = DateTimeFormatter.ofPattern(
                                "EEE, d MMM",
                                currentLocale
                            ).format(visit.arrival.atZone(zone)),
                            subtitle = "$timeRange · $duration",
                            icon = { Icon(Icons.Default.Place, null) }
                        )
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

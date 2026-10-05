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
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", currentLocale)
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
                    }
                }

                item {
                    ExpressiveCard(emphasized = true) {
                        Box(Modifier.fillMaxWidth().height(210.dp)) {
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
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExpressiveMetric(
                            sortedVisits.size.toString(),
                            "visits",
                            Modifier.weight(1f).fillMaxHeight(),
                            { Icon(Icons.Default.Repeat, null) }
                        )
                        ExpressiveMetric(
                            formatMinutes(totalMinutes),
                            "total stay",
                            Modifier.weight(1f).fillMaxHeight(),
                            { Icon(Icons.Default.Schedule, null) }
                        )
                        ExpressiveMetric(
                            formatMinutes(average),
                            "avg stay",
                            Modifier.weight(1f).fillMaxHeight(),
                            { Icon(Icons.Default.Timelapse, null) }
                        )
                    }
                }

                item {
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
                                    String.format(Locale.US, "%.5f, %.5f", place.latitude, place.longitude),
                                    style = MaterialTheme.typography.bodyMedium
                                )

                            }
                            if (activeVisit != null && activeMinutes != null) {
                                Spacer(Modifier.width(16.dp))
                                VerticalDivider(
                                    modifier = Modifier.height(42.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Now",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(50.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                "LIVE",
                                                Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Text(
                                        timeFormatter.format(activeVisit.arrival.atZone(zone)) +
                                            " – Now | " + formatMinutes(activeMinutes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                    item {
                        Column(Modifier.fillMaxWidth()) {
                            sortedVisits.forEachIndexed { index, visit ->
                                val end = visit.departure ?: now
                                val duration = formatMinutes(
                                    Duration.between(visit.arrival, end).toMinutes().coerceAtLeast(0)
                                )
                                val timeRange = timeFormatter.format(visit.arrival.atZone(zone)) + " – " +
                                    if (visit.departure == null) "Now" else timeFormatter.format(end.atZone(zone))
                                val shape = when {
                                    sortedVisits.size == 1 -> RoundedCornerShape(22.dp)
                                    index == 0 -> RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                                    index == sortedVisits.lastIndex -> RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
                                    else -> RoundedCornerShape(2.dp)
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = shape,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    ExpressiveListRow(
                                        title = DateTimeFormatter.ofPattern(
                                            "EEE, d MMM",
                                            currentLocale
                                        ).format(visit.arrival.atZone(zone)),
                                        subtitle = "$timeRange · $duration",
                                        icon = { Icon(Icons.Default.Place, null) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (index < sortedVisits.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        thickness = 1.dp
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

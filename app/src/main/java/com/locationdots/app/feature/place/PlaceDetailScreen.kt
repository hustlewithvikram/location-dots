package com.locationdots.app.feature.place

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.Place
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.ui.components.*
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(place?.name ?: "Place") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    if (place != null) {
                        IconButton(onClick = {
                            renameText = place.name.orEmpty()
                            showRenameDialog = true
                        }) {
                            Icon(Icons.Default.Edit, "Rename place")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (place == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        } else {
            val totalMinutes = visits.sumOf { it.departure?.let { end -> Duration.between(it.arrival, end).toMinutes().coerceAtLeast(0) } ?: 0 }
            val average = if (visits.isNotEmpty()) totalMinutes / visits.size else 0
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    ExpressiveCard(emphasized = true) {
                        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(place.name ?: "Unnamed place", style = MaterialTheme.typography.headlineMedium)
                            Text(String.format(Locale.US, "%.5f, %.5f", place.latitude, place.longitude), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(onClick = {}, label = { Text(visits.size.toString() + " visits") }, leadingIcon = { Icon(Icons.Default.LocationOn, null) })
                                AssistChip(onClick = {}, label = { Text(formatMinutes(totalMinutes)) }, leadingIcon = { Icon(Icons.Default.Schedule, null) })
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExpressiveMetric(visits.size.toString(), "visits", Modifier.weight(1f), { Icon(Icons.Default.Repeat, null) })
                        ExpressiveMetric(formatMinutes(average), "average stay", Modifier.weight(1f), { Icon(Icons.Default.Schedule, null) })
                    }
                }
                item { ExpressiveSectionHeader("Visit history", "Every recorded visit to this place.") }
                items(visits, key = { it.id }) { visit ->
                    ExpressiveListRow(
                        title = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()).format(visit.arrival.atZone(ZoneId.systemDefault())),
                        subtitle = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(visit.arrival.atZone(ZoneId.systemDefault())) + " · " + (visit.departure?.let { formatMinutes(Duration.between(visit.arrival, it).toMinutes()) } ?: "Still here"),
                        icon = { Icon(Icons.Default.Place, null) }
                    )
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
                onRename(renameText)
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

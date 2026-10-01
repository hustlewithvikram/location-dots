package com.locationdots.app.feature.place

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.Place
import com.locationdots.app.domain.model.TimelineEvent
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
    var name by remember(place?.id, place?.name) { mutableStateOf(place?.name.orEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(place?.name ?: "Place") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (place == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Place not found")
            }
            return@Scaffold
        }

        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
        val timeFormatter = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
        val totalDuration = visits.sumOf { visit ->
            Duration.between(visit.arrival, visit.departure ?: visit.arrival).seconds.coerceAtLeast(0)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Column {
                            Text(
                                place.name ?: "Unnamed place",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                "%.5f, %.5f".format(Locale.US, place.latitude, place.longitude),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlaceStat(
                        value = visits.size.toString(),
                        label = "Visits",
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                    PlaceStat(
                        value = formatDuration(totalDuration),
                        label = "Time here",
                        icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("History", style = MaterialTheme.typography.titleMedium)
                        Text("First recorded · \${formatter.format(place.createdAt)}")
                        Text("Last visited · \${formatter.format(place.updatedAt)}")
                    }
                }
            }

            item {
                Text("Recent visits", style = MaterialTheme.typography.titleMedium)
            }

            if (visits.isEmpty()) {
                item {
                    Text(
                        "No visits recorded yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(visits.take(20), key = { it.id }) { visit ->
                    VisitHistoryRow(visit, timeFormatter)
                }
            }

            item {
                Text("Name this place", style = MaterialTheme.typography.titleMedium)
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. Home, Office, Gym") }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onRename(name) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                    if (place.name != null) {
                        OutlinedButton(
                            onClick = { name = ""; onRename(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceStat(
    value: String,
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon()
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VisitHistoryRow(
    visit: TimelineEvent.Visit,
    formatter: DateTimeFormatter
) {
    val duration = visit.departure?.let {
        Duration.between(visit.arrival, it).seconds.coerceAtLeast(0)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(formatter.format(visit.arrival), style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        duration != null -> formatDuration(duration)
                        else -> "Currently here"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "\${hours}h \${minutes}m"
        hours > 0 -> "\${hours}h"
        minutes > 0 -> "\${minutes}m"
        else -> "<1m"
    }
}

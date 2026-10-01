package com.locationdots.app.feature.timeline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.TimelineEvent
import java.time.Duration
import java.time.format.DateTimeFormatter

@Composable
fun TimelineScreen(
    events: List<TimelineEvent>,
    isTracking: Boolean,
    onPlaceClick: (String) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm")

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Timeline", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(24.dp))
        Text(
            if (isTracking) "Tracking location" else "Tracking paused",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        if (events.isEmpty()) {
            Text(
                "Your timeline will appear here as Location Dots learns your movements.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(24.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(events, key = { it.id }) { event ->
                    TimelineEventRow(event, formatter.format(event.timestamp), onPlaceClick)
                }
            }
        }
    }
}

@Composable
private fun TimelineEventRow(
    event: TimelineEvent,
    timestamp: String,
    onPlaceClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .then(
                if (event is TimelineEvent.Visit) Modifier.clickable { onPlaceClick(event.place.id) }
                else Modifier
            )
    ) {
        Text(
            when (event) {
                is TimelineEvent.Visit -> event.place.name ?: "Unnamed place"
                is TimelineEvent.Journey -> "Journey"
            },
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            when (event) {
                is TimelineEvent.Visit -> timestamp + " · " + durationLabel(event.arrival, event.departure)
                is TimelineEvent.Journey -> timestamp + " · " + distanceLabel(event.distanceMeters)
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun durationLabel(arrival: java.time.Instant, departure: java.time.Instant?): String {
    if (departure == null) return "Still there"
    val minutes = Duration.between(arrival, departure).toMinutes()
    return if (minutes < 60) "\$minutes min" else "\${minutes / 60}h \${minutes % 60}m"
}

private fun distanceLabel(distanceMeters: Double?): String {
    if (distanceMeters == null) return "Distance unavailable"
    return if (distanceMeters < 1000) "\${distanceMeters.toInt()} m"
    else "%.1f km".format(distanceMeters / 1000.0)
}

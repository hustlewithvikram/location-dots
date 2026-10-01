package com.locationdots.app.feature.timeline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.JourneyMode
import com.locationdots.app.domain.model.TimelineEvent
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private sealed interface TimelineItem {
    data class DayHeader(val date: LocalDate) : TimelineItem
    data class Event(val value: TimelineEvent) : TimelineItem
}

@Composable
fun TimelineScreen(
    events: List<TimelineEvent>,
    isTracking: Boolean,
    onPlaceClick: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    val items = buildTimelineItems(events)
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= items.size - 5) onLoadMore()
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Timeline", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp))
        Text(if (isTracking) "Tracking location" else "Tracking paused",
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 24.dp))

        if (items.isEmpty()) {
            Text("Your timeline will appear here as Location Dots learns your movements.",
                style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(24.dp))
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = {
                    when (it) {
                        is TimelineItem.DayHeader -> "day:${it.date}"
                        is TimelineItem.Event -> it.value.id
                    }
                }) { item ->
                    when (item) {
                        is TimelineItem.DayHeader -> Text(
                            dateFormatter.format(item.date),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 4.dp)
                        )
                        is TimelineItem.Event -> TimelineEventCard(item.value, timeFormatter, onPlaceClick)
                    }
                }
            }
        }
    }
}

private fun buildTimelineItems(events: List<TimelineEvent>): List<TimelineItem> {
    val zone = ZoneId.systemDefault()
    val result = mutableListOf<TimelineItem>()
    var lastDate: LocalDate? = null

    events.sortedByDescending { it.timestamp }.forEach { event ->
        val date = event.timestamp.atZone(zone).toLocalDate()
        if (date != lastDate) {
            result += TimelineItem.DayHeader(date)
            lastDate = date
        }
        result += TimelineItem.Event(event)
    }
    return result
}

@Composable
private fun TimelineEventCard(
    event: TimelineEvent,
    timeFormatter: DateTimeFormatter,
    onPlaceClick: (String) -> Unit
) {
    val clickable = if (event is TimelineEvent.Visit) {
        Modifier.clickable { onPlaceClick(event.place.id) }
    } else Modifier

    Card(modifier = clickable.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(timeFormatter.format(event.timestamp.atZone(ZoneId.systemDefault())),
                style = MaterialTheme.typography.labelLarge)

            Column(modifier = Modifier.weight(1f)) {
                when (event) {
                    is TimelineEvent.Visit -> {
                        Text(event.place.name ?: "Unnamed place", style = MaterialTheme.typography.titleMedium)
                        Text(durationLabel(event.arrival, event.departure), style = MaterialTheme.typography.bodyMedium)
                    }
                    is TimelineEvent.Journey -> {
                        Text("Journey", style = MaterialTheme.typography.titleMedium)
                        Text(journeyLabel(event), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun durationLabel(arrival: java.time.Instant, departure: java.time.Instant?): String {
    if (departure == null) return "Still there"
    val minutes = Duration.between(arrival, departure).toMinutes()
    return if (minutes < 60) "$minutes min" else "${minutes / 60}h ${minutes % 60}m"
}

private fun journeyLabel(event: TimelineEvent.Journey): String {
    val distance = event.distanceMeters?.let {
        if (it < 1000) "${it.toInt()} m" else "%.1f km".format(it / 1000.0)
    } ?: "Distance unavailable"

    val destination = event.endPlace?.name ?: "Unnamed place"
    val mode = when (event.mode) {
        JourneyMode.WALKING -> "Walking"
        JourneyMode.CYCLING -> "Cycling"
        JourneyMode.VEHICLE -> "Vehicle"
        JourneyMode.UNKNOWN -> "Movement"
    }

    return "$destination · $distance · $mode"
}

package com.locationdots.app.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
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
    isLoadingMore: Boolean,
    isRefreshing: Boolean,
    hasMore: Boolean,
    errorMessage: String?,
    onPlaceClick: (String) -> Unit,
    onJourneyClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onPlacesClick: () -> Unit,
    onInsightsClick: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit
) {
    val items = remember(events) { buildTimelineItems(events) }
    val listState = rememberLazyListState()
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    }

    LoadMoreOnScroll(listState, items.size, hasMore, isLoadingMore, onLoadMore)

    Column(Modifier.fillMaxSize()) {
        TimelineHeader(isTracking, onSearchClick, onPlacesClick, onInsightsClick)

        if (items.isEmpty()) {
            TimelineEmptyState(isTracking)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "daily-summary") {
                    DailySummaryCard(events)
                }

                items(items = items, key = {
                    when (it) {
                        is TimelineItem.DayHeader -> "day:" + it.date
                        is TimelineItem.Event -> it.value.id
                    }
                }) { item ->
                    when (item) {
                        is TimelineItem.DayHeader -> DayHeader(item.date, dateFormatter)
                        is TimelineItem.Event -> TimelineEventCard(
                            item.value,
                            onPlaceClick,
                            onJourneyClick
                        )
                    }
                }

                if (isLoadingMore) {
                    item(key = "loading") {
                        TimelineLoadingIndicator()
                    }
                } else if (!hasMore) {
                    item(key = "end") {
                        TimelineEndIndicator()
                    }
                }

                if (errorMessage != null) {
                    item(key = "error") {
                        TimelineError(errorMessage, onRetry, onClearError)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailySummaryCard(events: List<TimelineEvent>) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val todayEvents = events.filter { it.timestamp.atZone(zone).toLocalDate() == today }
    val visits = todayEvents.filterIsInstance<TimelineEvent.Visit>()
    val journeys = todayEvents.filterIsInstance<TimelineEvent.Journey>()
    val minutes = visits.sumOf { visit ->
        visit.departure?.let {
            Duration.between(visit.arrival, it).toMinutes().coerceAtLeast(0)
        } ?: 0
    }
    val distance = journeys.sumOf { it.distanceMeters ?: 0.0 }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Today", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (todayEvents.isEmpty()) "No activity recorded yet" else "Your day so far",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(Icons.Default.Route, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryStat("Visits", visits.size.toString(), Modifier.weight(1f))
                SummaryStat("Journeys", journeys.size.toString(), Modifier.weight(1f))
                SummaryStat("Distance", formatDistance(distance), Modifier.weight(1f))
            }
            if (minutes > 0) {
                Text(formatDuration(minutes) + " spent at places", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatDuration(minutes: Long): String =
    if (minutes < 60) minutes.toString() + " min" else (minutes / 60).toString() + "h " + (minutes % 60).toString() + "m"

@Composable
private fun TimelineLoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.5.dp
        )
    }
}

@Composable
private fun TimelineEndIndicator() {
    Text(
        "You're all caught up",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun TimelineError(
    message: String,
    onRetry: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRetry) { Text("Retry") }
                TextButton(onClick = onClear) { Text("Dismiss") }
            }
        }
    }
}

@Composable
private fun TimelineHeader(isTracking: Boolean, onSearchClick: () -> Unit, onPlacesClick: () -> Unit, onInsightsClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Timeline",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onPlacesClick) {
                Icon(Icons.Default.Map, contentDescription = "Places")
            }
            IconButton(onClick = onInsightsClick) {
                Icon(Icons.Default.Insights, contentDescription = "Insights")
            }
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = if (isTracking) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTracking) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                )
                Text(
                    if (isTracking) "Tracking location" else "Tracking paused",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, formatter: DateTimeFormatter) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(formatter.format(date), style = MaterialTheme.typography.titleMedium)
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun TimelineEventCard(
    event: TimelineEvent,
    onPlaceClick: (String) -> Unit,
    onJourneyClick: (String) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val isVisit = event is TimelineEvent.Visit
    val accent = if (isVisit) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when (event) {
                    is TimelineEvent.Visit -> onPlaceClick(event.place.id)
                    is TimelineEvent.Journey -> onJourneyClick(event.id)
                }
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimelineIcon(event, accent)

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    timeFormatter.format(event.timestamp.atZone(zone)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (event) {
                    is TimelineEvent.Visit -> VisitContent(event)
                    is TimelineEvent.Journey -> JourneyContent(event)
                }
            }
        }
    }
}

@Composable
private fun TimelineIcon(
    event: TimelineEvent,
    accent: androidx.compose.ui.graphics.Color
) {
    val icon = when (event) {
        is TimelineEvent.Visit -> Icons.Default.Place
        is TimelineEvent.Journey -> when (event.mode) {
            JourneyMode.WALKING -> Icons.Default.DirectionsWalk
            JourneyMode.CYCLING -> Icons.Default.PedalBike
            JourneyMode.VEHICLE -> Icons.Default.DirectionsCar
            JourneyMode.UNKNOWN -> Icons.Default.MyLocation
        }
    }

    Surface(
        modifier = Modifier.size(46.dp),
        shape = CircleShape,
        color = accent.copy(alpha = 0.12f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@Composable
private fun VisitContent(event: TimelineEvent.Visit) {
    Text(
        event.place.name ?: "Unnamed place",
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        durationLabel(event.arrival, event.departure),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun JourneyContent(event: TimelineEvent.Journey) {
    val destination = event.endPlace?.name ?: "Unnamed place"

    Text(
        "Journey to " + destination,
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        buildJourneyMeta(event),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun TimelineEmptyState(isTracking: Boolean) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Text("Your timeline is empty", style = MaterialTheme.typography.titleLarge)
            Text(
                if (isTracking) {
                    "Location Dots will build your timeline as you move."
                } else {
                    "Start tracking to begin building your timeline."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadMoreOnScroll(
    listState: LazyListState,
    itemCount: Int,
    hasMore: Boolean,
    isLoading: Boolean,
    onLoadMore: () -> Unit
) {
    LaunchedEffect(listState, itemCount, hasMore, isLoading) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastIndex ->
            if (
                hasMore &&
                !isLoading &&
                itemCount > 0 &&
                lastIndex >= itemCount - 5
            ) {
                onLoadMore()
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

private fun durationLabel(
    arrival: java.time.Instant,
    departure: java.time.Instant?
): String {
    if (departure == null) return "Still here · ongoing visit"
    val minutes = Duration.between(arrival, departure).toMinutes().coerceAtLeast(0)
    return if (minutes < 60) {
        "${minutes} min stay"
    } else {
        "${minutes / 60}h ${minutes % 60}m stay"
    }
}

private fun buildJourneyMeta(event: TimelineEvent.Journey): String {
    val parts = mutableListOf(event.mode.label(), formatDistance(event.distanceMeters))
    event.endedAt?.let {
        val minutes = Duration.between(event.startedAt, it).toMinutes().coerceAtLeast(0)
        parts += if (minutes < 60) {
            "${minutes} min"
        } else {
            "${minutes / 60}h ${minutes % 60}m"
        }
    } ?: parts.add("In progress")

    event.startPlace?.name?.let { start ->
        parts.add(0, "From ${start}")
    }

    return parts.joinToString(" · ")
}

private fun JourneyMode.label(): String = when (this) {
    JourneyMode.WALKING -> "Walking"
    JourneyMode.CYCLING -> "Cycling"
    JourneyMode.VEHICLE -> "Vehicle"
    JourneyMode.UNKNOWN -> "Movement"
}

private fun formatDistance(distanceMeters: Double?): String = distanceMeters?.let {
    if (it < 1000) it.toInt().toString() + " m"
    else "%.1f km".format(it / 1000.0)
} ?: "Distance unavailable"

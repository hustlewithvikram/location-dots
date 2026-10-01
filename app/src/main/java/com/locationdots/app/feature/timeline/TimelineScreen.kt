package com.locationdots.app.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.JourneyMode
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.ui.components.*
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private sealed interface TimelineItem { data class Day(val date: LocalDate) : TimelineItem; data class Event(val event: TimelineEvent) : TimelineItem }

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
    onSettingsClick: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit
) {
    val listState = rememberLazyListState()
    val items = remember(events) { buildItems(events) }

    LaunchedEffect(listState, items.size, hasMore, isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (hasMore && !isLoadingMore && last >= items.size - 4) onLoadMore()
        }
    }

    Scaffold(bottomBar = {
        AppBottomBar(AppTab.TIMELINE) { tab ->
            when (tab) { AppTab.TIMELINE -> Unit; AppTab.MAP -> onPlacesClick(); AppTab.INSIGHTS -> onInsightsClick(); AppTab.SETTINGS -> onSettingsClick() }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TimelineTopBar(isTracking, onSearchClick)
            if (items.isEmpty() && !isRefreshing) EmptyTimeline(isTracking)
            else LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp, 4.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item("summary") { TodayCard(events, isTracking) }
                items(items, key = { when (it) { is TimelineItem.Day -> "day-" + it.date; is TimelineItem.Event -> it.event.id } }) { item ->
                    when (item) {
                        is TimelineItem.Day -> Text(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()).format(item.date), Modifier.padding(top = 8.dp, start = 4.dp), style = MaterialTheme.typography.titleMedium)
                        is TimelineItem.Event -> EventCard(item.event, onPlaceClick, onJourneyClick)
                    }
                }
                if (isRefreshing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                if (isLoadingMore) item { Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(26.dp)) } }
                if (!hasMore && items.isNotEmpty()) item { Text("You’re all caught up.", Modifier.fillMaxWidth().padding(18.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (errorMessage != null) item {
                    ExpressiveCard(onClick = onRetry) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, null)
                            Column(Modifier.weight(1f)) { Text(errorMessage); Text("Tap to retry", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium) }
                            IconButton(onClick = onClearError) { Icon(Icons.Default.Close, "Dismiss") }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun TimelineTopBar(isTracking: Boolean, onSearchClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Your timeline", style = MaterialTheme.typography.headlineMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).background(if (isTracking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(50)))
                Text(if (isTracking) "Tracking automatically" else "Tracking paused", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        FilledTonalIconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, "Search") }
    }
}
@Composable private fun TodayCard(events: List<TimelineEvent>, isTracking: Boolean) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val todayEvents = events.filter { it.timestamp.atZone(zone).toLocalDate() == today }
    val visits = todayEvents.filterIsInstance<TimelineEvent.Visit>()
    val journeys = todayEvents.filterIsInstance<TimelineEvent.Journey>()
    val minutes = visits.sumOf { it.departure?.let { end -> Duration.between(it.arrival, end).toMinutes().coerceAtLeast(0) } ?: 0 }
    val distance = journeys.sumOf { it.distanceMeters ?: 0.0 }
    ExpressiveCard(emphasized = true) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Today", style = MaterialTheme.typography.titleLarge)
                    Text(if (todayEvents.isEmpty()) "Your timeline is ready." else "A quick view of your day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ExpressiveIconBadge(icon = { Icon(if (isTracking) Icons.Default.MyLocation else Icons.Default.PauseCircleOutline, null) })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(visits.size.toString(), "places", Modifier.weight(1f))
                MiniStat(formatMinutes(minutes), "time", Modifier.weight(1f))
                MiniStat(formatDistance(distance), "distance", Modifier.weight(1f))
            }
        }
    }
}
@Composable private fun MiniStat(value: String, label: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .55f)) {
        Column(Modifier.padding(12.dp)) { Text(value, style = MaterialTheme.typography.titleMedium); Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
@Composable private fun EventCard(event: TimelineEvent, onPlaceClick: (String) -> Unit, onJourneyClick: (String) -> Unit) {
    when (event) {
        is TimelineEvent.Visit -> ExpressiveListRow(event.place.name ?: "Unnamed place", "Arrived " + time(event.arrival) + " · " + (event.departure?.let { formatMinutes(Duration.between(event.arrival, it).toMinutes()) } ?: "Still here"), { Icon(Icons.Default.Place, null) }, onClick = { onPlaceClick(event.place.id) })
        is TimelineEvent.Journey -> ExpressiveListRow((event.startPlace?.name ?: "Unknown") + " → " + (event.endPlace?.name ?: "Unknown"), event.mode.label() + " · " + formatDistance(event.distanceMeters ?: 0.0) + " · " + time(event.startedAt), { Icon(modeIcon(event.mode), null) }, onClick = { onJourneyClick(event.id) })
    }
}
@Composable private fun EmptyTimeline(isTracking: Boolean) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        ExpressiveIconBadge(Modifier.size(72.dp), { Icon(Icons.Default.Timeline, null, Modifier.size(34.dp)) })
        Spacer(Modifier.height(18.dp))
        Text("Your timeline starts here", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(if (isTracking) "Keep moving. Location Dots will build your first places and journeys automatically." else "Turn on tracking to start building your personal map.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
private fun buildItems(events: List<TimelineEvent>): List<TimelineItem> {
    val result = mutableListOf<TimelineItem>()
    var last: LocalDate? = null
    events.sortedByDescending { it.timestamp }.forEach { event ->
        val date = event.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
        if (date != last) { result += TimelineItem.Day(date); last = date }
        result += TimelineItem.Event(event)
    }
    return result
}
private fun time(i: java.time.Instant) = DateTimeFormatter.ofPattern("HH:mm").format(i.atZone(ZoneId.systemDefault()))
private fun formatMinutes(m: Long) = if (m < 60) m.toString() + "m" else (m / 60).toString() + "h " + (m % 60).toString() + "m"
private fun formatDistance(m: Double) = if (m < 1000) m.toInt().toString() + " m" else "%.1f km".format(m / 1000.0)
private fun JourneyMode.label() = when (this) { JourneyMode.WALKING -> "Walking"; JourneyMode.CYCLING -> "Cycling"; JourneyMode.VEHICLE -> "Driving"; JourneyMode.UNKNOWN -> "Journey" }
private fun modeIcon(m: JourneyMode) = when (m) { JourneyMode.WALKING -> Icons.Default.DirectionsWalk; JourneyMode.CYCLING -> Icons.Default.PedalBike; JourneyMode.VEHICLE -> Icons.Default.DirectionsCar; JourneyMode.UNKNOWN -> Icons.Default.Route }

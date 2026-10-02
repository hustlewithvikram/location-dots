package com.locationdots.app.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.maplibre.android.geometry.LatLng
import com.locationdots.app.ui.components.LocationMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (items.isEmpty() && !isRefreshing) EmptyTimeline(isTracking)
            else LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 108.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item("summary") {
                    TodayCard(events, isTracking, onSearchClick)
                }
                item("activity-header") {
                    ExpressiveSectionHeader(
                        title = "Activity"
                    )
                }
                items(items, key = { when (it) { is TimelineItem.Day -> "day-" + it.date; is TimelineItem.Event -> it.event.id } }) { item ->
                    when (item) {
                        is TimelineItem.Day -> DayHeader(item.date)
                        is TimelineItem.Event -> TimelineEventRow(item.event, onPlaceClick, onJourneyClick)
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

@Composable
private fun DayHeader(date: LocalDate) {
    val today = LocalDate.now(ZoneId.systemDefault())
    val formatted = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()).format(date)
    val label = when (date) {
        today -> "Today · " + formatted
        today.minusDays(1) -> "Yesterday · " + formatted
        else -> formatted
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(100.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                label,
                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(
            Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable private fun TimelineEventRow(
    event: TimelineEvent,
    onPlaceClick: (String) -> Unit,
    onJourneyClick: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        TimelineRail(event = event, modifier = Modifier.width(36.dp))
        Spacer(Modifier.width(8.dp))
        EventCard(event, onPlaceClick, onJourneyClick, Modifier.weight(1f))
    }
}


@Composable
private fun TimelineRail(event: TimelineEvent, modifier: Modifier = Modifier) {
    val isJourney = event is TimelineEvent.Journey
    val container = if (isJourney) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
    val content = if (isJourney) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = container,
            tonalElevation = 1.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isJourney) Icons.Default.Route else Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = content
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.width(3.dp).height(74.dp).clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        )
    }
}

@Composable
private fun TodayCard(
    events: List<TimelineEvent>,
    isTracking: Boolean,
    onSearchClick: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val todayEvents = events.filter { it.timestamp.atZone(zone).toLocalDate() == today }
    val visits = todayEvents.filterIsInstance<TimelineEvent.Visit>()
    val journeys = todayEvents.filterIsInstance<TimelineEvent.Journey>()
    val minutes = visits.sumOf {
        it.departure?.let { end ->
            Duration.between(it.arrival, end).toMinutes().coerceAtLeast(0)
        } ?: 0
    }
    val distance = journeys.sumOf { it.distanceMeters ?: 0.0 }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Today", style = MaterialTheme.typography.headlineLarge)
                Text(
                    if (todayEvents.isEmpty()) "Your timeline is ready."
                    else "A quick view of your day.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            ExpressiveIconButton(
                onClick = onSearchClick,
                icon = { Icon(Icons.Default.Search, "Search your timeline") },
                emphasized = true
            )
        }

        ExpressiveCard(emphasized = true) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExpressiveIconBadge(
                        modifier = Modifier.size(44.dp),
                        containerColor = if (isTracking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        icon = {
                            Icon(
                                if (isTracking) Icons.Default.LocationOn else Icons.Default.LocationDisabled,
                                contentDescription = null,
                                tint = if (isTracking) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isTracking) "Tracking active" else "Tracking paused",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (isTracking) "Location updates are being recorded."
                            else "Turn tracking on to build your timeline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniStat(visits.size.toString(), "stops", Modifier.weight(1f))
                    MiniStat(journeys.size.toString(), "trips", Modifier.weight(1f))
                    MiniStat(formatMinutes(minutes), "stayed", Modifier.weight(1f))
                    MiniStat(formatDistance(distance), "distance", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniStat(
    value: String,
    label: String,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EventCard(
    event: TimelineEvent,
    onPlaceClick: (String) -> Unit,
    onJourneyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (event) {
        is TimelineEvent.Visit -> VisitCard(event, onPlaceClick, modifier)
        is TimelineEvent.Journey -> JourneyCard(event, onJourneyClick, modifier)
    }
}

@Composable
private fun VisitCard(
    event: TimelineEvent.Visit,
    onPlaceClick: (String) -> Unit,
    modifier: Modifier
) {
    val duration = event.departure?.let {
        formatMinutes(Duration.between(event.arrival, it).toMinutes().coerceAtLeast(0))
    } ?: "Still here"

    ExpressiveCard(
        modifier = modifier,
        onClick = { onPlaceClick(event.place.id) }
    ) {
        Column {
            Row(
                Modifier.padding(start = 14.dp, top = 14.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveIconBadge(
                    modifier = Modifier.size(46.dp),
                    icon = { Icon(Icons.Default.Place, null) }
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        event.place.name ?: "Unnamed place",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Arrived " + time(event.arrival) + " · " + duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            MiniMap(
                points = listOf(LatLng(event.place.latitude, event.place.longitude)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun JourneyCard(
    event: TimelineEvent.Journey,
    onJourneyClick: (String) -> Unit,
    modifier: Modifier
) {
    val points = event.path.map { LatLng(it.latitude, it.longitude) }.let { path ->
        when {
            path.isNotEmpty() -> path
            event.startPlace != null && event.endPlace != null -> listOf(
                LatLng(event.startPlace.latitude, event.startPlace.longitude),
                LatLng(event.endPlace.latitude, event.endPlace.longitude)
            )
            event.startPlace != null -> listOf(
                LatLng(event.startPlace.latitude, event.startPlace.longitude)
            )
            else -> emptyList()
        }
    }

    ExpressiveCard(
        modifier = modifier,
        onClick = { onJourneyClick(event.id) }
    ) {
        Column {
            Row(
                Modifier.padding(start = 14.dp, top = 14.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveIconBadge(
                    modifier = Modifier.size(46.dp),
                    icon = { Icon(modeIcon(event.mode), null) }
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        (event.startPlace?.name ?: "Unknown") + " → " +
                            (event.endPlace?.name ?: "Unknown"),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        event.mode.label() + " · " +
                            (event.endedAt?.let {
                                formatMinutes(
                                    Duration.between(event.startedAt, it).toMinutes().coerceAtLeast(0)
                                )
                            } ?: "In progress") +
                            " · " + formatDistance(event.distanceMeters ?: 0.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (points.isNotEmpty()) {
                MiniMap(
                    points = points,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniMap(
    points: List<LatLng>,
    modifier: Modifier
) {
    LocationMap(
        points = points,
        modifier = modifier,
        interactive = false
    )
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

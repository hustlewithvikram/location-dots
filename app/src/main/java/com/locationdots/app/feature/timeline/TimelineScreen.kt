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

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (items.isEmpty() && !isRefreshing) EmptyTimeline(isTracking)
            else LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp, 10.dp, 18.dp, 108.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item("summary") {
                    TodayCard(events, isTracking, onSearchClick)
                }
                item("quick-actions") {
                    QuickActionsCard(
                        onSearchClick = onSearchClick,
                        onPlacesClick = onPlacesClick,
                        onInsightsClick = onInsightsClick,
                        onSettingsClick = onSettingsClick
                    )
                }
                item("activity-header") {
                    ExpressiveSectionHeader(
                        title = "Activity",
                        subtitle = "Your visits and journeys, newest first."
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
private fun QuickActionsCard(
    onSearchClick: () -> Unit,
    onPlacesClick: () -> Unit,
    onInsightsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    ExpressiveCard {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Quick actions", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Search,
                    label = "Search",
                    onClick = onSearchClick
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Place,
                    label = "Places",
                    onClick = onPlacesClick
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.BarChart,
                    label = "Insights",
                    onClick = onInsightsClick
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    onClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(icon, contentDescription = label)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable private fun DayHeader(date: LocalDate) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(100.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                when (date) {
                    LocalDate.now(ZoneId.systemDefault()) -> "Today"
                    LocalDate.now(ZoneId.systemDefault()).minusDays(1) -> "Yesterday"
                    else -> DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()).format(date)
                },
                Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable private fun TimelineEventRow(
    event: TimelineEvent,
    onPlaceClick: (String) -> Unit,
    onJourneyClick: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.Top) {
        Box(Modifier.width(30.dp).fillMaxHeight()) {
            Box(
                Modifier.width(2.dp).fillMaxHeight()
                    .align(Alignment.Center)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Box(
                Modifier.size(12.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.TopCenter)
            )
        }
        Spacer(Modifier.width(6.dp))
        EventCard(
            event,
            onPlaceClick,
            onJourneyClick,
            Modifier.weight(1f)
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

    ExpressiveCard(emphasized = true) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Today", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (todayEvents.isEmpty()) "Your timeline is ready."
                        else "Your movement at a glance.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ExpressiveIconBadge(
                    modifier = Modifier.size(48.dp),
                    containerColor = if (isTracking) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    icon = {
                        Icon(
                            if (isTracking) Icons.Default.MyLocation
                            else Icons.Default.PauseCircleOutline,
                            contentDescription = if (isTracking) "Tracking on" else "Tracking off",
                            tint = if (isTracking) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniStat(visits.size.toString(), "stops", Modifier.weight(1f))
                MiniStat(journeys.size.toString(), "journeys", Modifier.weight(1f))
                MiniStat(formatMinutes(minutes), "stayed", Modifier.weight(1f))
                MiniStat(formatDistance(distance), "distance", Modifier.weight(1f))
            }

            OutlinedButton(
                onClick = onSearchClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Search, null)
                Spacer(Modifier.width(8.dp))
                Text("Search your timeline")
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
        modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = .55f)
    ) {
        Column(
            Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val title: String
    val subtitle: String
    val icon: androidx.compose.ui.graphics.vector.ImageVector
    val action: () -> Unit

    when (event) {
        is TimelineEvent.Visit -> {
            title = event.place.name ?: "Unnamed place"
            subtitle = if (event.departure != null) {
                "Visited for " + formatMinutes(
                    Duration.between(event.arrival, event.departure).toMinutes().coerceAtLeast(0)
                )
            } else {
                "Still here"
            }
            icon = Icons.Default.Place
            action = { onPlaceClick(event.place.id) }
        }

        is TimelineEvent.Journey -> {
            title = (event.startPlace?.name ?: "Unknown") + " → " +
                (event.endPlace?.name ?: "Unknown")
            subtitle = event.mode.label() + " · " +
                formatDistance(event.distanceMeters ?: 0.0) +
                (event.endedAt?.let {
                    " · " + formatMinutes(
                        Duration.between(event.startedAt, it).toMinutes().coerceAtLeast(0)
                    )
                } ?: "")
            icon = modeIcon(event.mode)
            action = { onJourneyClick(event.id) }
        }
    }

    ExpressiveCard(
        modifier = modifier,
        onClick = action
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ExpressiveIconBadge(
                modifier = Modifier.size(48.dp),
                icon = { Icon(icon, contentDescription = null) }
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    time(event.timestamp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

package com.locationdots.app.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.maplibre.android.geometry.LatLng
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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            if (items.isEmpty() && !isRefreshing && !isLoadingMore) EmptyTimeline(isTracking)
            else LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item("summary") {
                    TodayCard(events, isTracking, onSearchClick)
                }
                item("activity-header") {
                    ExpressiveSectionHeader(
                        title = "Activity"
                    )
                }
                items.forEach { item ->
                    when (item) {
                        is TimelineItem.Day -> stickyHeader(key = "day-" + item.date) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 16.dp)
                            ) {
                                DayHeader(item.date)
                            }
                        }
                        is TimelineItem.Event -> item(key = item.event.id) {
                            TimelineEventRow(item.event, onPlaceClick, onJourneyClick)
                        }
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
    val currentLocale = LocalConfiguration.current.locales[0]
    val formatted = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", currentLocale).format(date)
    val isToday = date == today
    val isYesterday = date == today.minusDays(1)
    val title = when {
        isToday -> "Today"
        isYesterday -> "Yesterday"
        else -> DateTimeFormatter.ofPattern("EEEE", currentLocale).format(date)
    }
    val containerColor = when {
        isToday -> MaterialTheme.colorScheme.primaryContainer
        isYesterday -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        isYesterday -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        shape = RoundedCornerShape(100.dp),
        color = containerColor,
        tonalElevation = if (isToday) 2.dp else 0.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isToday) Icons.Default.Today else Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = contentColor
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor
                )
                Text(
                    formatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.78f)
                )
            }
            Text(
                if (isToday) "LIVE" else "DAY",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.72f)
            )
        }
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
    val dotColor = if (event is TimelineEvent.Journey) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .width(6.dp)
                .height(74.dp)
                .clip(MaterialTheme.shapes.extraLarge)
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
    val dayStart = today.atStartOfDay(zone).toInstant()
    val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant()
    val todayEvents = events.filter { event ->
        when (event) {
            is TimelineEvent.Visit ->
                event.arrival < dayEnd && (event.departure ?: dayEnd) >= dayStart
            is TimelineEvent.Journey ->
                event.startedAt < dayEnd && (event.endedAt ?: dayEnd) >= dayStart
        }
    }
    val visits = todayEvents.filterIsInstance<TimelineEvent.Visit>()
    val journeys = todayEvents.filterIsInstance<TimelineEvent.Journey>()
    val now = java.time.Instant.now().coerceIn(dayStart, dayEnd)
    val effectiveDayEnd = now
    val minutes = visits.sumOf {
        val start = maxOf(it.arrival, dayStart)
        val end = minOf(it.departure ?: effectiveDayEnd, effectiveDayEnd)
        Duration.between(start, end).toMinutes().coerceAtLeast(0)
    }
    val distance = journeys.sumOf { it.distanceMeters ?: 0.0 }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Today", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (todayEvents.isEmpty()) "Your timeline is ready."
                    else "A quick view of your day.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            ExpressiveIconButton(
                onClick = onSearchClick,
                icon = { Icon(Icons.Default.Search, "Search your timeline") },
                emphasized = true,
                modifier = Modifier.size(50.dp)
            )
        }

        ExpressiveCard(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExpressiveIconBadge(
                        modifier = Modifier.size(44.dp),
                        containerColor = if (isTracking) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                        icon = {
                            Icon(
                                if (isTracking) Icons.Default.LocationOn else Icons.Default.LocationDisabled,
                                contentDescription = null,
                                tint = if (isTracking) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
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
                            if (isTracking) "Recording your location in the background."
                            else "Turn tracking on to build your timeline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                ResponsiveStats(
                    stats = listOf(
                        "stops" to visits.size.toString(),
                        "trips" to journeys.size.toString(),
                        "stayed" to formatDuration(minutes),
                        "distance" to formatDistance(distance)
                    )
                )
            }
        }
    }
}

@Composable
private fun ResponsiveStats(
    stats: List<Pair<String, String>>
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsColumn(stats.take(2), Modifier.weight(1f))
        StatsColumn(stats.drop(2), Modifier.weight(1f))
    }
}

@Composable
private fun StatsColumn(
    stats: List<Pair<String, String>>,
    modifier: Modifier
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stats.forEach { (label, value) ->
            MiniStat(value, label, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MiniStat(
    value: String,
    label: String,
    modifier: Modifier
) {
    val icon = when (label) {
        "stops" -> Icons.Default.Place
        "trips" -> Icons.Default.Route
        "stayed" -> Icons.Default.Schedule
        "distance" -> Icons.Default.Straighten
        else -> Icons.Default.Info
    }

    Surface(
        modifier = modifier.heightIn(min = 64.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
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

            MiniLocationPreview(
                points = listOf(event.place.latitude to event.place.longitude),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
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
                MiniLocationPreview(
                    points = points.map { it.latitude to it.longitude },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniLocationPreview(
    points: List<Pair<Double, Double>>,
    modifier: Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val routeColor = MaterialTheme.colorScheme.primary
    val startColor = MaterialTheme.colorScheme.tertiary

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = surfaceColor
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (points.isEmpty()) return@Canvas

            val minLat = points.minOf { it.first }
            val maxLat = points.maxOf { it.first }
            val minLon = points.minOf { it.second }
            val maxLon = points.maxOf { it.second }

            fun x(lon: Double): Float {
                val range = (maxLon - minLon).coerceAtLeast(0.000001)
                return (((lon - minLon) / range) * (size.width - 28f) + 14f).toFloat()
            }

            fun y(lat: Double): Float {
                val range = (maxLat - minLat).coerceAtLeast(0.000001)
                return (size.height - 14f - ((lat - minLat) / range * (size.height - 28f))).toFloat()
            }

            // Subtle map-like grid without creating a native map view.
            repeat(5) { index ->
                val gx = size.width * index / 4f
                val gy = size.height * index / 4f
                drawLine(gridColor, androidx.compose.ui.geometry.Offset(gx, 0f), androidx.compose.ui.geometry.Offset(gx, size.height), 1f)
                drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, gy), androidx.compose.ui.geometry.Offset(size.width, gy), 1f)
            }

            val path = Path()
            points.forEachIndexed { index, point ->
                val offset = androidx.compose.ui.geometry.Offset(x(point.second), y(point.first))
                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }

            if (points.size > 1) {
                drawPath(
                    path = path,
                    color = routeColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }

            val end = androidx.compose.ui.geometry.Offset(x(points.last().second), y(points.last().first))
            drawCircle(
                color = routeColor,
                radius = 7f,
                center = end
            )
            if (points.size > 1) {
                val start = androidx.compose.ui.geometry.Offset(x(points.first().second), y(points.first().first))
                drawCircle(
                    color = startColor,
                    radius = 5f,
                    center = start
                )
            }
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
    val zone = ZoneId.systemDefault()
    val now = java.time.Instant.now()

    events
        .sortedByDescending { event ->
            if (event is TimelineEvent.Visit && event.departure == null) now else event.timestamp
        }
        .forEach { event ->
            val date = if (event is TimelineEvent.Visit && event.departure == null) {
                now.atZone(zone).toLocalDate()
            } else {
                event.timestamp.atZone(zone).toLocalDate()
            }

            if (date != last) {
                result += TimelineItem.Day(date)
                last = date
            }
            result += TimelineItem.Event(event)
        }

    return result
}
private fun time(i: java.time.Instant) = DateTimeFormatter.ofPattern("HH:mm").format(i.atZone(ZoneId.systemDefault()))
private fun formatMinutes(m: Long) = if (m < 60) m.toString() + "m" else (m / 60).toString() + "h " + (m % 60).toString() + "m"
private fun formatDuration(m: Long): String {
    val minutes = m.coerceAtLeast(0)
    return when {
        minutes < 60 -> "${minutes}m"
        minutes < 24 * 60 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes / (24 * 60)}d ${(minutes / 60) % 24}h"
    }
}
private fun formatDistance(m: Double): String {
    val distance = m.coerceAtLeast(0.0)
    return when {
        distance < 10 -> "0 m"
        distance < 1000 -> {
            val roundedMeters = kotlin.math.round(distance / 10.0).toInt() * 10
            "${roundedMeters} m"
        }
        else -> {
            val roundedKm = kotlin.math.round(distance / 100.0) / 10.0
            if (roundedKm == roundedKm.toLong().toDouble()) {
                "${roundedKm.toLong()} km"
            } else {
                "%.1f km".format(Locale.getDefault(), roundedKm)
            }
        }
    }
}
private fun JourneyMode.label() = when (this) { JourneyMode.WALKING -> "Walking"; JourneyMode.CYCLING -> "Cycling"; JourneyMode.VEHICLE -> "Driving"; JourneyMode.UNKNOWN -> "Journey" }
private fun modeIcon(m: JourneyMode) = when (m) { JourneyMode.WALKING -> Icons.Default.DirectionsWalk; JourneyMode.CYCLING -> Icons.Default.PedalBike; JourneyMode.VEHICLE -> Icons.Default.DirectionsCar; JourneyMode.UNKNOWN -> Icons.Default.Route }

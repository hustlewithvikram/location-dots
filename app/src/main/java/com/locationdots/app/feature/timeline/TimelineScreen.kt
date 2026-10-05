package com.locationdots.app.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface TimelineItem { data class Day(val date: LocalDate) : TimelineItem; data class Event(val event: TimelineEvent) : TimelineItem }

@OptIn(ExperimentalMaterial3Api::class)
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
    onProfileClick: () -> Unit,
    actionButton: com.locationdots.app.feature.settings.ActionButton,
    themeChoice: com.locationdots.app.feature.settings.ThemeChoice,
    onActionButtonClick: () -> Unit,
    onPlacesClick: () -> Unit,
    onInsightsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit
) {
    val listState = rememberLazyListState()
    val items = remember(events) { buildItems(events) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState, items.size, hasMore, isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (hasMore && !isLoadingMore && last >= items.size - 4) onLoadMore()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().statusBarsPadding()
    ) {
    Column(Modifier.fillMaxSize()) {
            if (items.isEmpty() && !isRefreshing && !isLoadingMore) EmptyTimeline(isTracking)
            else LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 112.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item("summary") {
                    TodayCard(
                        events = events,
                        isTracking = isTracking,
                        onSearchClick = onSearchClick,
                        onProfileClick = onProfileClick,
                        actionButton = actionButton,
                        themeChoice = themeChoice,
                        onActionButtonClick = {
                            if (actionButton == com.locationdots.app.feature.settings.ActionButton.TODAY) {
                                scope.launch { listState.animateScrollToItem(0) }
                            } else {
                                onActionButtonClick()
                            }
                        }
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
    val locale = LocalConfiguration.current.locales[0]
    val formatted = DateTimeFormatter.ofPattern(
        if (date == today) "EEE, d MMM" else "d MMM",
        locale
    ).format(date)
    val isToday = date == today
    val isYesterday = date == today.minusDays(1)
    val title = when {
        isToday -> "Today"
        isYesterday -> "Yesterday"
        else -> DateTimeFormatter.ofPattern("EEEE", locale).format(date)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(76.dp),
            maxLines = 1,
            softWrap = false
        )
        Spacer(Modifier.width(8.dp))
        Text(
            formatted,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TimelineEventRow(
    event: TimelineEvent,
    onPlaceClick: (String) -> Unit,
    onJourneyClick: (String) -> Unit
) {
    val eventTime = when (event) {
        is TimelineEvent.Visit -> event.arrival
        is TimelineEvent.Journey -> event.startedAt
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.width(64.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    time(eventTime),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                TimelineDot(event)
            }
            Box(
                Modifier
                    .width(12.dp)
                    .height(74.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Spacer(Modifier.width(12.dp))
        EventCard(
            event,
            onPlaceClick,
            onJourneyClick,
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimelineDot(event: TimelineEvent) {
    val dotColor = if (event is TimelineEvent.Journey) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(
        Modifier
            .size(14.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(dotColor)
    )
}

@Composable
private fun TodayCard(
    events: List<TimelineEvent>,
    isTracking: Boolean,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    actionButton: com.locationdots.app.feature.settings.ActionButton,
    themeChoice: com.locationdots.app.feature.settings.ThemeChoice,
    onActionButtonClick: () -> Unit
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
    val minutes = visits.sumOf {
        val start = maxOf(it.arrival, dayStart)
        val end = minOf(it.departure ?: now, now)
        Duration.between(start, end).toMinutes().coerceAtLeast(0)
    }
    val distance = journeys.sumOf { it.distanceMeters ?: 0.0 }
    val locale = LocalConfiguration.current.locales[0]
    val dateLabel = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale).format(today)

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButtonIcon(
                actionButton = actionButton,
                themeChoice = themeChoice,
                onClick = onActionButtonClick
            )
            Spacer(Modifier.weight(1f))

            FilledTonalIconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, "Search your timeline")
            }
            Spacer(Modifier.width(6.dp))
            FilledTonalIconButton(
                onClick = onProfileClick,
            ) {
                Icon(Icons.Default.Person, "Open profile")
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryMetric(visits.size.toString(), "stops", Icons.Default.Place, Modifier.weight(1f))
                SummaryDivider()
                SummaryMetric(journeys.size.toString(), "trips", Icons.Default.Route, Modifier.weight(1f))
                SummaryDivider()
                SummaryMetric(formatDuration(minutes), "stayed", Icons.Default.Schedule, Modifier.weight(1f))
                SummaryDivider()
                SummaryMetric(formatDistance(distance), "distance", Icons.Default.Straighten, Modifier.weight(1f))
            }
        }
    }
}


@Composable
private fun ActionButtonIcon(
    actionButton: com.locationdots.app.feature.settings.ActionButton,
    themeChoice: com.locationdots.app.feature.settings.ThemeChoice,
    onClick: () -> Unit
) {
    FilledTonalIconButton(onClick = onClick) {
        Icon(
            when (actionButton) {
                com.locationdots.app.feature.settings.ActionButton.THEME -> when (themeChoice) {
                    com.locationdots.app.feature.settings.ThemeChoice.SYSTEM -> Icons.Default.BrightnessAuto
                    com.locationdots.app.feature.settings.ThemeChoice.LIGHT -> Icons.Default.LightMode
                    com.locationdots.app.feature.settings.ThemeChoice.DARK -> Icons.Default.DarkMode
                }
                com.locationdots.app.feature.settings.ActionButton.REFRESH -> Icons.Default.Refresh
                com.locationdots.app.feature.settings.ActionButton.TRACKING -> Icons.Default.MyLocation
                com.locationdots.app.feature.settings.ActionButton.TODAY -> Icons.Default.Today
                com.locationdots.app.feature.settings.ActionButton.EXPORT -> Icons.Default.FileDownload
            },
            contentDescription = actionButton.label()
        )
    }
}
@Composable
private fun SummaryMetric(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    Column(
        modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(5.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(34.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
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
    val isCurrent = event.departure == null
    var now by remember(isCurrent) { mutableStateOf(Instant.now()) }

    LaunchedEffect(isCurrent) {
        if (!isCurrent) return@LaunchedEffect
        while (true) {
            now = Instant.now()
            delay(30_000)
        }
    }

    val end = event.departure ?: now
    val duration = formatMinutes(
        Duration.between(event.arrival, end).toMinutes().coerceAtLeast(0)
    )

    ExpressiveCard(
        modifier = modifier,
        onClick = { onPlaceClick(event.place.id) },
        containerColor = if (isCurrent) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveIconBadge(
                modifier = Modifier.size(42.dp),
                icon = { Icon(Icons.Default.Place, null) }
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        event.place.name ?: "Unnamed place",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                }
                Spacer(Modifier.height(3.dp))
                Text(
                    if (isCurrent) "You're here for $duration"
                    else "Stayed $duration",
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
        onClick = { onJourneyClick(event.id) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column {
            Row(
                Modifier.padding(start = 14.dp, top = 14.dp, end = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveIconBadge(
                    modifier = Modifier.size(42.dp),
                    icon = { Icon(modeIcon(event.mode), null) }
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            event.startPlace?.name ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "To",
                            modifier = Modifier.padding(horizontal = 6.dp).size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            event.endPlace?.name ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        event.mode.label() + " · " +
                            (event.endedAt?.let {
                                formatMinutes(Duration.between(event.startedAt, it).toMinutes().coerceAtLeast(0))
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

            if (points.size > 1) {
                MiniLocationPreview(
                    points = points.map { it.latitude to it.longitude },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
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
private fun time(i: java.time.Instant) = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(i.atZone(ZoneId.systemDefault()))
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

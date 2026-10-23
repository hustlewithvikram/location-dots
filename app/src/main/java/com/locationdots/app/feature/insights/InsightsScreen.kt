package com.vikram.locationdots.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vikram.locationdots.domain.insights.DailyInsight
import com.vikram.locationdots.domain.insights.InsightsSnapshot
import com.vikram.locationdots.domain.insights.PlaceInsight
import com.vikram.locationdots.domain.model.JourneyMode
import com.vikram.locationdots.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    snapshot: InsightsSnapshot,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onTabSelected: (AppTab) -> Unit = {},
    onDeletePlace: (String) -> Unit = {}
) {
    val hasData = snapshot.totalVisits > 0 || snapshot.journeyCount > 0
    val maxDaily = snapshot.dailyVisits.maxOfOrNull { it.visits + it.journeys } ?: 0
    val maxPlaceVisits = snapshot.topPlaces.maxOfOrNull { it.visits } ?: 0
    val totalModes = snapshot.modeBreakdown.values.sum()
    var placePendingDelete by remember { mutableStateOf<PlaceInsight?>(null) }

    PullToRefreshBox(isRefreshing = isLoading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Insights", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            if (hasData) "Your movement, summarized." else "Your movement history will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }

            if (error != null) item {
                ExpressiveCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Insights couldn’t be updated", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = onRefresh) { Text("Retry") }
                    }
                }
            }

            if (!hasData && !isLoading && error == null) item {
                ExpressiveCard(emphasized = true, fillMaxWidth = true) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExpressiveIconBadge(modifier = Modifier.size(64.dp), icon = { Icon(Icons.Default.Route, null) })
                        Text("Your story starts here", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Once Location Dots records visits or journeys, this page will show your travel distance, routines, and most-visited places.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (hasData) {
                item {
                    ExpressiveCard(emphasized = true, fillMaxWidth = true) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("DISTANCE TRAVELLED", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatDistance(snapshot.totalDistanceMeters), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "Across ${snapshot.journeyCount} ${if (snapshot.journeyCount == 1) "journey" else "journeys"} · All time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExpressiveMetric(snapshot.totalVisits.toString(), "visits", Modifier.weight(1f), { Icon(Icons.Default.NearMe, null) })
                        ExpressiveMetric(snapshot.totalPlaces.toString(), "places", Modifier.weight(1f), { Icon(Icons.Default.Place, null) })
                        ExpressiveMetric(formatMinutes(snapshot.totalTimeMinutes), "time at places", Modifier.weight(1f), { Icon(Icons.Default.CalendarToday, null) })
                    }
                }

                if (totalModes > 0) {
                    item { ExpressiveSectionHeader("Journey modes", "How recorded journeys are classified.") }
                    item {
                        ExpressiveCard {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                JourneyMode.entries.filter { (snapshot.modeBreakdown[it] ?: 0) > 0 }.forEach { mode ->
                                    val count = snapshot.modeBreakdown[mode] ?: 0
                                    val fraction = count.toFloat() / totalModes.coerceAtLeast(1)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ExpressiveIconBadge(modifier = Modifier.size(40.dp), icon = { Icon(modeIcon(mode), null) })
                                        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(modeLabel(mode), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                                Text("$count · ${percent(fraction)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(6.dp), trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (snapshot.dailyVisits.isNotEmpty()) {
                    item { ExpressiveSectionHeader("Last 7 days", "Daily visits and journeys.") }
                    item {
                        ExpressiveCard {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    InsightLegendDot(MaterialTheme.colorScheme.primary, "Visits")
                                    InsightLegendDot(MaterialTheme.colorScheme.tertiary, "Journeys")
                                }
                                snapshot.dailyVisits.forEach { day -> DailyInsightRow(day, maxDaily) }
                            }
                        }
                    }
                }

                if (snapshot.topPlaces.isNotEmpty()) {
                    item { ExpressiveSectionHeader("Places you return to", "Ranked by recorded visit count.") }
                    items(snapshot.topPlaces, key = { it.placeId }) { place ->
                        TopPlaceRow(place, maxPlaceVisits, onDelete = { placePendingDelete = place })
                    }
                }

                if (snapshot.totalVisits > 0) {
                    item { ExpressiveSectionHeader("Personal records", "Highlights from your recorded visits.") }
                    item {
                        ExpressiveCard {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                InsightRecordRow("Average visit", formatMinutes(snapshot.averageVisitMinutes), Icons.Default.Place)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InsightRecordRow("Longest visit", formatMinutes(snapshot.longestVisitMinutes), Icons.Default.CalendarToday)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InsightRecordRow(
                                    "Busiest day",
                                    snapshot.busiestDayLabel?.let { "$it · ${snapshot.busiestDayVisits} visits" } ?: "Not enough data yet",
                                    Icons.Default.NearMe,
                                    last = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    placePendingDelete?.let { place ->
        AlertDialog(
            onDismissRequest = { placePendingDelete = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete ${place.name}?") },
            text = {
                Text("The saved place and its recorded visits will be removed. Location history and unrelated journeys will remain on this device. If you visit this area again, Location Dots may discover it again.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePlace(place.placeId)
                    placePendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { placePendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun InsightLegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DailyInsightRow(day: DailyInsight, maxValue: Int) {
    val total = day.visits + day.journeys
    val fraction = if (maxValue <= 0) 0f else (total.toFloat() / maxValue).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(day.label, Modifier.width(42.dp), style = MaterialTheme.typography.labelLarge)
            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                if (total > 0) {
                    Row(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(50))) {
                        if (day.visits > 0) Box(Modifier.weight(day.visits.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                        if (day.journeys > 0) Box(Modifier.weight(day.journeys.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
                    }
                }
            }
            Text(total.toString(), Modifier.width(28.dp).padding(start = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${day.visits} visits · ${day.journeys} journeys", Modifier.padding(start = 42.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TopPlaceRow(place: PlaceInsight, maxVisits: Int, onDelete: () -> Unit) {
    val fraction = if (maxVisits <= 0) 0f else (place.visits.toFloat() / maxVisits).coerceIn(0f, 1f)
    ExpressiveCard {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ExpressiveIconBadge(modifier = Modifier.size(44.dp), icon = { Icon(Icons.Default.Place, null) })
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(place.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${place.visits} ${if (place.visits == 1) "visit" else "visits"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text(formatMinutes(place.timeMinutes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(5.dp), trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete place", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun InsightRecordRow(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    last: Boolean = false
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        ExpressiveIconBadge(modifier = Modifier.size(40.dp), icon = { Icon(icon, null) })
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun formatMinutes(minutes: Long): String =
    when {
        minutes <= 0 -> "0 min"
        minutes < 60 -> "${minutes} min"
        minutes % 60 == 0L -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }

private fun formatDistance(meters: Double): String =
    when {
        meters < 1000 -> "${meters.toInt()} m"
        meters < 10_000 -> "%.1f km".format(meters / 1000.0)
        else -> "${(meters / 1000).toInt()} km"
    }

private fun percent(value: Float): String = "${(value * 100).toInt()}%"

private fun modeLabel(mode: JourneyMode): String = when (mode) {
    JourneyMode.WALKING -> "Walking"
    JourneyMode.CYCLING -> "Cycling"
    JourneyMode.VEHICLE -> "Driving"
    JourneyMode.UNKNOWN -> "Other"
}

private fun modeIcon(mode: JourneyMode) = when (mode) {
    JourneyMode.WALKING -> Icons.Default.DirectionsWalk
    JourneyMode.CYCLING -> Icons.Default.PedalBike
    JourneyMode.VEHICLE -> Icons.Default.DirectionsCar
    JourneyMode.UNKNOWN -> Icons.Default.Route
}

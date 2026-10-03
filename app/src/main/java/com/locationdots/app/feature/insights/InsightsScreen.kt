package com.locationdots.app.feature.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.insights.DailyInsight
import com.locationdots.app.domain.insights.InsightsSnapshot
import com.locationdots.app.domain.insights.PlaceInsight
import com.locationdots.app.domain.model.JourneyMode
import com.locationdots.app.ui.components.*

@Composable
fun InsightsScreen(
    snapshot: InsightsSnapshot,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onTabSelected: (AppTab) -> Unit = {}
) {
    val hasData = snapshot.totalVisits > 0 || snapshot.journeyCount > 0
    val maxDaily = snapshot.dailyVisits.maxOfOrNull { it.visits + it.journeys } ?: 0
    val maxPlaceTime = snapshot.topPlaces.maxOfOrNull { it.timeMinutes } ?: 0L
    val totalModes = snapshot.modeBreakdown.values.sum()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
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
                    ExpressiveIconButton(
                        onClick = onRefresh,
                        icon = { Icon(Icons.Default.Refresh, contentDescription = "Refresh") },
                        contentDescription = "Refresh insights"
                    )
                }
            }

            if (isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }

            if (error != null) {
                item {
                    ExpressiveCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(error, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                            TextButton(onClick = onRefresh) { Text("Retry") }
                        }
                    }
                }
            }

            if (!hasData && !isLoading && error == null) {
                item {
                    ExpressiveCard(emphasized = true) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExpressiveIconBadge(
                                icon = { Icon(Icons.Default.Route, null) },
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                            Text("Not enough activity yet", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Start tracking to build your timeline and unlock movement insights.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (hasData) {
                item {
                    ExpressiveCard(emphasized = true) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Your movement", style = MaterialTheme.typography.titleLarge)
                            Text(formatDistance(snapshot.totalDistanceMeters), style = MaterialTheme.typography.displaySmall)
                            Text(
                                "distance recorded across ${snapshot.journeyCount} ${if (snapshot.journeyCount == 1) "journey" else "journeys"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExpressiveMetric(snapshot.totalPlaces.toString(), "places", Modifier.weight(1f), { Icon(Icons.Default.Place, null) })
                        ExpressiveMetric(snapshot.totalVisits.toString(), "visits", Modifier.weight(1f), { Icon(Icons.Default.NearMe, null) })
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExpressiveMetric(formatMinutes(snapshot.totalTimeMinutes), "time at places", Modifier.weight(1f), { Icon(Icons.Default.CalendarToday, null) })
                        ExpressiveMetric(snapshot.journeyCount.toString(), "journeys", Modifier.weight(1f), { Icon(Icons.Default.Route, null) })
                    }
                }

                item { ExpressiveSectionHeader("Movement modes", "How your recorded journeys were classified.") }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            JourneyMode.entries.filter { (snapshot.modeBreakdown[it] ?: 0) > 0 }.forEach { mode ->
                                val count = snapshot.modeBreakdown[mode] ?: 0
                                val fraction = count.toFloat() / totalModes.coerceAtLeast(1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ExpressiveIconBadge(modifier = Modifier.size(40.dp), icon = { Icon(modeIcon(mode), null) })
                                    Column(
                                        Modifier.weight(1f).padding(start = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(modeLabel(mode), Modifier.weight(1f))
                                            Text(
                                                "${count} · ${percent(fraction)}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            modifier = Modifier.fillMaxWidth(),
                                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { ExpressiveSectionHeader("Last 7 days", "Visits and journeys recorded each day.") }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            snapshot.dailyVisits.forEach { day -> DailyInsightRow(day, maxDaily) }
                        }
                    }
                }

                item { ExpressiveSectionHeader("Top places", "Places with the most recorded visits.") }

                items(snapshot.topPlaces, key = { it.placeId }) { place ->
                    TopPlaceRow(place, maxPlaceTime)
                }

                item { ExpressiveSectionHeader("Your records", "Useful summaries from the data you have collected.") }

                item {
                    ExpressiveCard {
                        Column(Modifier.padding(18.dp)) {
                            InsightRecordRow("Average visit", formatMinutes(snapshot.averageVisitMinutes), Icons.Default.Place)
                            InsightRecordRow("Longest visit", formatMinutes(snapshot.longestVisitMinutes), Icons.Default.CalendarToday)
                            InsightRecordRow(
                                "Busiest day",
                                snapshot.busiestDayLabel?.let { "${it} · ${snapshot.busiestDayVisits} visits" } ?: "No visits in the last 7 days",
                                Icons.Default.NearMe,
                                last = true
                            )
                        }
                    }
                }
            }
        }
    }

@Composable
private fun DailyInsightRow(day: DailyInsight, maxValue: Int) {
    val value = day.visits + day.journeys
    val fraction = if (maxValue == 0) 0f else value.toFloat() / maxValue
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(day.label, Modifier.width(40.dp), style = MaterialTheme.typography.labelLarge)
        Column(
            Modifier.weight(1f).padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Text(
                "${day.visits} visits · ${day.journeys} journeys",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopPlaceRow(place: PlaceInsight, maxTime: Long) {
    val fraction = if (maxTime == 0L) 0f else (place.timeMinutes.toFloat() / maxTime).coerceIn(0f, 1f)
    ExpressiveCard {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ExpressiveIconBadge(icon = { Icon(Icons.Default.Place, null) })
            Column(
                Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    place.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${place.visits} ${if (place.visits == 1) "visit" else "visits"} · ${formatMinutes(place.timeMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
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
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExpressiveIconBadge(modifier = Modifier.size(40.dp), icon = { Icon(icon, null) })
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

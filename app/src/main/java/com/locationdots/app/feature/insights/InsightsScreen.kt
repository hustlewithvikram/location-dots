package com.locationdots.app.feature.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.insights.InsightsSnapshot
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
    Scaffold(bottomBar = { AppBottomBar(AppTab.INSIGHTS, onTabSelected) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Insights", style = MaterialTheme.typography.headlineLarge)
                        Text("Patterns from your movement history.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ExpressiveIconButton(onClick = onRefresh, icon = { Icon(Icons.Default.Refresh, "Refresh") }, emphasized = true)
                }
            }
            if (isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (error != null) item { ExpressiveCard { Text(error, Modifier.padding(18.dp)) } }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExpressiveMetric(snapshot.totalPlaces.toString(), "places", Modifier.weight(1f), { Icon(Icons.Default.Place, null) })
                    ExpressiveMetric(snapshot.totalVisits.toString(), "visits", Modifier.weight(1f), { Icon(Icons.Default.LocationOn, null) })
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExpressiveMetric(snapshot.journeyCount.toString(), "journeys", Modifier.weight(1f), { Icon(Icons.Default.Route, null) })
                    ExpressiveMetric(formatDistance(snapshot.totalDistanceMeters), "distance", Modifier.weight(1f), { Icon(Icons.Default.NearMe, null) })
                }
            }
            item {
                ExpressiveCard(emphasized = true) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Time spent moving through your world", style = MaterialTheme.typography.titleLarge)
                        Text(formatMinutes(snapshot.totalTimeMinutes), style = MaterialTheme.typography.displaySmall)
                        Text("recorded across all saved visits", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { ExpressiveSectionHeader("Movement", "How your journeys break down.") }
            item {
                ExpressiveCard {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        JourneyMode.entries.forEach { mode ->
                            val count = snapshot.modeBreakdown[mode] ?: 0
                            val total = snapshot.modeBreakdown.values.sum().coerceAtLeast(1)
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Icon(modeIcon(mode), null)
                                Text(modeLabel(mode), Modifier.padding(start = 10.dp).weight(1f))
                                Text(count.toString(), style = MaterialTheme.typography.titleMedium)
                            }
                            ExpressiveProgress(count.toFloat() / total.toFloat())
                        }
                    }
                }
            }
            item { ExpressiveSectionHeader("Top places", "Where your time tends to collect.") }
            items(snapshot.topPlaces.take(5), key = { it.placeId }) { place ->
                ExpressiveListRow(place.name, place.visits.toString() + " visits · " + formatMinutes(place.timeMinutes), { Icon(Icons.Default.Place, null) })
            }
            item { ExpressiveSectionHeader("Recent activity", "Visits and journeys over the latest period.") }
            items(snapshot.dailyVisits.takeLast(7), key = { it.label }) { day ->
                ExpressiveListRow(day.label, day.visits.toString() + " visits · " + day.journeys.toString() + " journeys", { Icon(Icons.Default.CalendarToday, null) })
            }
        }
    }
}

private fun formatMinutes(minutes: Long): String = if (minutes < 60) minutes.toString() + " min" else (minutes / 60).toString() + "h " + (minutes % 60).toString() + "m"
private fun formatDistance(meters: Double): String = if (meters < 1000) meters.toInt().toString() + " m" else "%.1f km".format(meters / 1000.0)
private fun modeLabel(mode: JourneyMode): String = when (mode) { JourneyMode.WALKING -> "Walking"; JourneyMode.CYCLING -> "Cycling"; JourneyMode.VEHICLE -> "Driving"; JourneyMode.UNKNOWN -> "Other" }
private fun modeIcon(mode: JourneyMode) = when (mode) { JourneyMode.WALKING -> Icons.Default.DirectionsWalk; JourneyMode.CYCLING -> Icons.Default.PedalBike; JourneyMode.VEHICLE -> Icons.Default.DirectionsCar; JourneyMode.UNKNOWN -> Icons.Default.Route }

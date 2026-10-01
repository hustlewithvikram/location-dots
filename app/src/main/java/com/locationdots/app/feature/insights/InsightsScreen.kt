package com.locationdots.app.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.insights.InsightsSnapshot
import com.locationdots.app.domain.insights.PlaceInsight
import com.locationdots.app.domain.model.JourneyMode

@Composable
fun InsightsScreen(
    snapshot: InsightsSnapshot,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Insights",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        if (isLoading && snapshot == InsightsSnapshot()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        if (error != null) {
            Text(
                error,
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.error
            )
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { OverviewCard(snapshot) }
            item { ActivityCard(snapshot) }
            item { ModeCard(snapshot) }
            item { Text("Most visited", style = MaterialTheme.typography.titleLarge) }
            if (snapshot.topPlaces.isEmpty()) {
                item { EmptyCard("Visit places to build your history.") }
            } else {
                items(snapshot.topPlaces, key = { it.placeId }) { place ->
                    PlaceInsightCard(place)
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(snapshot: InsightsSnapshot) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Your activity", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("Places", snapshot.totalPlaces.toString(), Modifier.weight(1f))
                Stat("Visits", snapshot.totalVisits.toString(), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("Time", formatMinutes(snapshot.totalTimeMinutes), Modifier.weight(1f))
                Stat("Distance", formatDistance(snapshot.totalDistanceMeters), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityCard(snapshot: InsightsSnapshot) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Last 7 days", style = MaterialTheme.typography.titleLarge)
            if (snapshot.dailyVisits.all { it.visits == 0 && it.journeys == 0 }) {
                Text("No recorded activity yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val max = snapshot.dailyVisits.maxOfOrNull { it.visits + it.journeys }?.coerceAtLeast(1) ?: 1
                snapshot.dailyVisits.forEach { day ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(day.label, Modifier.weight(0.24f), style = MaterialTheme.typography.labelMedium)
                        Column(Modifier.weight(0.76f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            LinearProgressIndicator(
                                progress = { (day.visits + day.journeys).toFloat() / max },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                day.visits.toString() + " visits · " + day.journeys + " journeys",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(snapshot: InsightsSnapshot) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Travel modes", style = MaterialTheme.typography.titleLarge)
            JourneyMode.entries
                .filter { snapshot.modeBreakdown[it] ?: 0 > 0 }
                .forEach { mode ->
                    ModeRow(mode, snapshot.modeBreakdown[mode] ?: 0)
                }
            if (snapshot.journeyCount == 0) {
                Text("No journeys recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ModeRow(mode: JourneyMode, count: Int) {
    val icon = when (mode) {
        JourneyMode.WALKING -> Icons.Default.DirectionsWalk
        JourneyMode.CYCLING -> Icons.Default.PedalBike
        JourneyMode.VEHICLE -> Icons.Default.DirectionsCar
        JourneyMode.UNKNOWN -> Icons.Default.Route
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null)
        Text(mode.label(), Modifier.weight(1f))
        Text(count.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PlaceInsightCard(place: PlaceInsight) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Place, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    place.visits.toString() + " visits · " + formatMinutes(place.timeMinutes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Text(text, Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatMinutes(minutes: Long): String =
    if (minutes < 60) minutes.toString() + " min"
    else (minutes / 60).toString() + "h " + (minutes % 60).toString() + "m"

private fun formatDistance(meters: Double): String =
    if (meters < 1000) meters.toInt().toString() + " m"
    else "%.1f km".format(meters / 1000.0)

private fun JourneyMode.label(): String = when (this) {
    JourneyMode.WALKING -> "Walking"
    JourneyMode.CYCLING -> "Cycling"
    JourneyMode.VEHICLE -> "Vehicle"
    JourneyMode.UNKNOWN -> "Unknown"
}

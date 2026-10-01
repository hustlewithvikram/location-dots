package com.locationdots.app.feature.journey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.locationdots.app.domain.model.*
import com.locationdots.app.ui.components.*
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyDetailScreen(journey: TimelineEvent.Journey, onBack: () -> Unit) {
    val duration = journey.endedAt?.let { Duration.between(journey.startedAt, it).toMinutes().coerceAtLeast(0) }
    val speed = if (duration != null && duration > 0 && journey.distanceMeters != null) journey.distanceMeters / 1000.0 / (duration / 60.0) else null
    Scaffold(topBar = { TopAppBar(title = { Text("Journey") }, navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ExpressiveCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp), emphasized = true) {
                Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExpressiveMetric(journey.mode.label(), "mode", Modifier.weight(1f), { Icon(modeIcon(journey.mode), null) })
                    ExpressiveMetric(formatDistance(journey.distanceMeters), "distance", Modifier.weight(1f), { Icon(Icons.Default.Route, null) })
                }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    ExpressiveCard(Modifier.fillMaxWidth()) { JourneyMap(journey.path, journey.startPlace?.name ?: "Start", journey.endPlace?.name ?: "End") }
                }
                item {
                    ExpressiveCard {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExpressiveSectionHeader("Journey details")
                            Stat("From", journey.startPlace?.name ?: "Unknown")
                            Stat("To", journey.endPlace?.name ?: "Unknown")
                            Stat("Started", time(journey.startedAt))
                            Stat("Ended", journey.endedAt?.let(::time) ?: "In progress")
                            Stat("Duration", duration?.let(::formatMinutes) ?: "In progress")
                            Stat("Distance", formatDistance(journey.distanceMeters))
                            Stat("Average speed", speed?.let { "%.1f km/h".format(it) } ?: "Unavailable")
                            Stat("GPS points", journey.path.size.toString())
                        }
                    }
                }
                item {
                    ExpressiveCard {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ExpressiveSectionHeader("Route timeline")
                            Text("Recorded " + journey.path.size + " GPS points")
                            if (journey.path.isNotEmpty()) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(time(journey.path.first().timestamp)); Text(time(journey.path.last().timestamp)) }
                                ExpressiveProgress(1f)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable private fun JourneyMap(points: List<LocationPoint>, startLabel: String, endLabel: String) {
    val coordinates = remember(points) { points.map { LatLng(it.latitude, it.longitude) } }
    if (coordinates.isEmpty()) { Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) { Text("No route data available", color = MaterialTheme.colorScheme.onSurfaceVariant) }; return }
    val camera = rememberCameraPositionState()
    LaunchedEffect(coordinates) { camera.move(if (coordinates.size == 1) CameraUpdateFactory.newLatLngZoom(coordinates.first(), 16f) else CameraUpdateFactory.newLatLngBounds(LatLngBounds.builder().apply { coordinates.forEach(::include) }.build(), 70)) }
    Box(Modifier.fillMaxWidth().height(300.dp)) {
        GoogleMap(Modifier.fillMaxSize(), cameraPositionState = camera, uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false)) {
            if (coordinates.size >= 2) Polyline(points = coordinates, width = 10f)
            Marker(MarkerState(coordinates.first()), title = startLabel)
            if (coordinates.size >= 2) Marker(MarkerState(coordinates.last()), title = endLabel)
        }
        FilledTonalIconButton(onClick = { if (coordinates.size >= 2) camera.move(CameraUpdateFactory.newLatLngBounds(LatLngBounds.builder().apply { coordinates.forEach(::include) }.build(), 70)) }, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) { Icon(Icons.Default.MyLocation, "Fit route") }
    }
}
@Composable private fun Stat(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.titleMedium) } }
private fun time(i: java.time.Instant) = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(i.atZone(ZoneId.systemDefault()))
private fun formatMinutes(m: Long) = if (m < 60) m.toString() + " min" else (m / 60).toString() + "h " + (m % 60).toString() + "m"
private fun formatDistance(m: Double?) = m?.let { if (it < 1000) it.toInt().toString() + " m" else "%.1f km".format(it / 1000.0) } ?: "Unavailable"
private fun JourneyMode.label() = when (this) { JourneyMode.WALKING -> "Walking"; JourneyMode.CYCLING -> "Cycling"; JourneyMode.VEHICLE -> "Driving"; JourneyMode.UNKNOWN -> "Movement" }
private fun modeIcon(m: JourneyMode) = when (m) { JourneyMode.WALKING -> Icons.Default.DirectionsWalk; JourneyMode.CYCLING -> Icons.Default.PedalBike; JourneyMode.VEHICLE -> Icons.Default.DirectionsCar; JourneyMode.UNKNOWN -> Icons.Default.Route }

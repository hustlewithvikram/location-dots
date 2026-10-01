package com.locationdots.app.feature.journey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.locationdots.app.domain.model.JourneyMode
import com.locationdots.app.domain.model.LocationPoint
import com.locationdots.app.domain.model.TimelineEvent
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun JourneyDetailScreen(journey: TimelineEvent.Journey, onBack: () -> Unit) {
    val duration = journey.endedAt?.let { Duration.between(journey.startedAt, it).toMinutes() }
    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    val modeLabel = journey.mode.label()
    val averageSpeedKmh = if (duration != null && duration > 0 && journey.distanceMeters != null) {
        journey.distanceMeters / 1000.0 / (duration / 60.0)
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(
            onClick = onBack,
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            Text("Back")
        }

        Text("Journey", style = MaterialTheme.typography.headlineMedium)

        JourneySummary(
            mode = modeLabel,
            distance = formatDistance(journey.distanceMeters),
            duration = duration?.let(::formatDuration) ?: "In progress",
            averageSpeed = averageSpeedKmh?.let(::formatSpeed) ?: "—"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            JourneyMap(
                points = journey.path,
                startLabel = journey.startPlace?.name ?: "Start",
                endLabel = journey.endPlace?.name ?: "End"
            )
        }

        if (journey.path.size >= 2) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Route timeline", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Recorded ${journey.path.size} GPS points",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(timeFormatter(journey.path.first().timestamp), style = MaterialTheme.typography.labelMedium)
                        Text(timeFormatter(journey.path.last().timestamp), style = MaterialTheme.typography.labelMedium)
                    }
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Route data spans ${formatDuration(Duration.between(journey.path.first().timestamp, journey.path.last().timestamp).toMinutes().coerceAtLeast(0))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatRow("From", journey.startPlace?.name ?: "Unknown place")
                StatRow("To", journey.endPlace?.name ?: "Unknown place")
                StatRow("Mode", modeLabel)
                StatRow(
                    "Started",
                    formatter.format(journey.startedAt.atZone(ZoneId.systemDefault()))
                )
                StatRow(
                    "Ended",
                    journey.endedAt?.let {
                        formatter.format(it.atZone(ZoneId.systemDefault()))
                    } ?: "In progress"
                )
                StatRow("Duration", duration?.let(::formatDuration) ?: "In progress")
                StatRow("Distance", formatDistance(journey.distanceMeters))
                StatRow("Average speed", averageSpeedKmh?.let(::formatSpeed) ?: "Unavailable")
                StatRow("GPS points", journey.path.size.toString())
            }
        }
    }
}

@Composable
private fun JourneySummary(mode: String, distance: String, duration: String, averageSpeed: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem("Mode", mode)
            SummaryItem("Distance", distance)
            SummaryItem("Duration", duration)
            SummaryItem("Avg speed", averageSpeed)
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun JourneyMap(
    points: List<LocationPoint>,
    startLabel: String,
    endLabel: String
) {
    val coordinates = remember(points) {
        points.map { LatLng(it.latitude, it.longitude) }
    }

    if (coordinates.isEmpty()) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No route data available",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val cameraPositionState = rememberCameraPositionState()
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = true
        )
    }

    LaunchedEffect(coordinates) {
        if (coordinates.size == 1) {
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(coordinates.first(), 16f)
            )
        } else {
            val bounds = LatLngBounds.builder().apply {
                coordinates.forEach { include(it) }
            }.build()
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngBounds(bounds, 80)
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(),
            uiSettings = uiSettings
        ) {
            if (coordinates.size >= 2) {
                Polyline(
                    points = coordinates,
                    width = 9f
                )
            }

            Marker(
                state = MarkerState(position = coordinates.first()),
                title = startLabel,
                snippet = "Journey started here"
            )

            if (coordinates.size >= 2) {
                Marker(
                    state = MarkerState(position = coordinates.last()),
                    title = endLabel,
                    snippet = "Journey ended here"
                )
            }
        }

        if (coordinates.size >= 2) {
            FilledTonalIconButton(
                onClick = {
                    val bounds = LatLngBounds.builder().apply {
                        coordinates.forEach { include(it) }
                    }.build()
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngBounds(bounds, 80)
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Fit route"
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 3.dp
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Directions,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    if (coordinates.size >= 2) "Route recorded" else "Location recorded",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RouteTimeline(
    points: List<LocationPoint>,
    startTime: java.time.Instant,
    endTime: java.time.Instant?
) {
    val first = points.firstOrNull()?.timestamp ?: startTime
    val last = points.lastOrNull()?.timestamp ?: endTime ?: startTime
    val elapsed = Duration.between(first, last).toMinutes().coerceAtLeast(0)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Recorded points: ${points.size}",
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                    .format(first.atZone(ZoneId.systemDefault())),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                if (endTime != null) {
                    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                        .format(last.atZone(ZoneId.systemDefault()))
                } else {
                    "In progress"
                },
                style = MaterialTheme.typography.labelMedium
            )
        }
        LinearProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            if (elapsed > 0) "${elapsed} min of route data" else "Route data recorded",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun timeFormatter(timestamp: java.time.Instant): String =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        .format(timestamp.atZone(ZoneId.systemDefault()))

private fun formatSpeed(speedKmh: Double): String =
    if (speedKmh < 10) "%.1f km/h".format(speedKmh) else "%.0f km/h".format(speedKmh)

private fun JourneyMode.label(): String = when (this) {
    JourneyMode.WALKING -> "Walking"
    JourneyMode.CYCLING -> "Cycling"
    JourneyMode.VEHICLE -> "Vehicle"
    JourneyMode.UNKNOWN -> "Movement"
}

private fun formatSpeed(speedKmh: Double): String =
    if (speedKmh < 10) {
        "%.1f km/h".format(speedKmh)
    } else {
        "%.0f km/h".format(speedKmh)
    }

private fun formatDistance(distanceMeters: Double?): String = distanceMeters?.let {
    if (it < 1000) {
        it.toInt().toString() + " m"
    } else {
        "%.1f km".format(it / 1000.0)
    }
} ?: "Unavailable"

private fun formatDuration(minutes: Long): String =
    if (minutes < 60) {
        minutes.toString() + " min"
    } else {
        (minutes / 60).toString() + "h " + (minutes % 60).toString() + "m"
    }


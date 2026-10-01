package com.locationdots.app.feature.journey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val mode = when (journey.mode) {
        JourneyMode.WALKING -> "Walking"
        JourneyMode.CYCLING -> "Cycling"
        JourneyMode.VEHICLE -> "Vehicle"
        JourneyMode.UNKNOWN -> "Movement"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("Back")
        }

        Text("Journey", style = MaterialTheme.typography.headlineMedium)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            JourneyMap(journey.path)
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatRow("From", journey.startPlace?.name ?: "Unknown place")
                StatRow("To", journey.endPlace?.name ?: "Unknown place")
                StatRow("Mode", mode)
                StatRow("Started", formatter.format(journey.startedAt.atZone(ZoneId.systemDefault())))
                StatRow(
                    "Ended",
                    journey.endedAt?.let {
                        formatter.format(it.atZone(ZoneId.systemDefault()))
                    } ?: "In progress"
                )
                StatRow("Duration", duration?.let(::formatDuration) ?: "In progress")
                StatRow("Distance", formatDistance(journey.distanceMeters))
                StatRow("GPS points", journey.path.size.toString())
            }
        }
    }
}

@Composable
private fun JourneyMap(points: List<LocationPoint>) {
    val coordinates = remember(points) {
        points.map { LatLng(it.latitude, it.longitude) }
    }

    if (coordinates.isEmpty()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
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
    val mapProperties = remember {
        MapProperties()
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
                CameraUpdateFactory.newLatLngBounds(bounds, 72)
            )
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings
    ) {
        if (coordinates.size >= 2) {
            Polyline(
                points = coordinates,
                width = 8f
            )
        }

        Marker(
            state = MarkerState(position = coordinates.first()),
            title = "Start",
            snippet = "Journey started here"
        )

        if (coordinates.size >= 2) {
            Marker(
                state = MarkerState(position = coordinates.last()),
                title = "End",
                snippet = "Journey ended here"
            )
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

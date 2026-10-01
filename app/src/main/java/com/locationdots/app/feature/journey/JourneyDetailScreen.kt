package com.locationdots.app.feature.journey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
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
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Journey", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) { JourneyPath(journey.path) }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatRow("From", journey.startPlace?.name ?: "Unknown place")
                StatRow("To", journey.endPlace?.name ?: "Unknown place")
                StatRow("Mode", mode)
                StatRow("Started", formatter.format(journey.startedAt.atZone(ZoneId.systemDefault())))
                StatRow("Ended", journey.endedAt?.let { formatter.format(it.atZone(ZoneId.systemDefault())) } ?: "In progress")
                StatRow("Duration", duration?.let(::formatDuration) ?: "In progress")
                StatRow("Distance", formatDistance(journey.distanceMeters))
                StatRow("GPS points", journey.path.size.toString())
            }
        }
    }
}

@Composable
private fun JourneyPath(points: List<LocationPoint>) {
    Canvas(Modifier.fillMaxWidth().height(260.dp).padding(16.dp)) {
        if (points.size < 2) return@Canvas
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        val latRange = (maxLat - minLat).coerceAtLeast(0.000001)
        val lonRange = (maxLon - minLon).coerceAtLeast(0.000001)
        val mapped = points.map { point ->
            Offset(
                x = ((point.longitude - minLon) / lonRange * size.width).toFloat(),
                y = ((maxLat - point.latitude) / latRange * size.height).toFloat()
            )
        }
        mapped.zipWithNext().forEach { pair -> drawLine(pair.first, pair.second, strokeWidth = 6f) }
        drawCircle(radius = 10f, center = mapped.first())
        drawCircle(radius = 10f, center = mapped.last())
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatDistance(distanceMeters: Double?): String = distanceMeters?.let {
    if (it < 1000) it.toInt().toString() + " m" else "%.1f km".format(it / 1000.0)
} ?: "Unavailable"

private fun formatDuration(minutes: Long): String =
    if (minutes < 60) minutes.toString() + " min" else (minutes / 60).toString() + "h " + (minutes % 60).toString() + "m"
package com.locationdots.app.feature.journey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import org.maplibre.android.geometry.LatLng
import com.locationdots.app.domain.model.*
import com.locationdots.app.ui.components.*
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun JourneyDetailScreen(journey: TimelineEvent.Journey, onBack: () -> Unit) {
    var showMapFullscreen by remember { mutableStateOf(false) }

    val duration = journey.endedAt?.let {
        Duration.between(journey.startedAt, it).toMinutes().coerceAtLeast(0)
    }
    val speed = if (duration != null && duration > 0 && journey.distanceMeters != null) {
        journey.distanceMeters / 1000.0 / (duration / 60.0)
    } else null

    val startName = journey.startPlace?.name ?: "Unknown"
    val endName = journey.endPlace?.name ?: "Unknown"

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Journey", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "\${time(journey.startedAt)} – \${journey.endedAt?.let(::time) ?: "Now"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                ExpressiveCard(
                    Modifier.fillMaxWidth(),
                    emphasized = true,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        JourneyMetric(
                            modeIcon(journey.mode),
                            journey.mode.label(),
                            "mode",
                            Modifier.weight(1f)
                        )
                        JourneyMetric(
                            Icons.Default.Route,
                            formatDistance(journey.distanceMeters),
                            "distance",
                            Modifier.weight(1f)
                        )
                        JourneyMetric(
                            Icons.Default.Schedule,
                            duration?.let(::formatMinutes) ?: "In progress",
                            "duration",
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                ExpressiveCard(
                    Modifier.fillMaxWidth(),
                    emphasized = true
                ) {
                    Box(Modifier.fillMaxWidth().height(320.dp)) {
                        JourneyMap(
                            points = journey.path,
                            modifier = Modifier.fillMaxSize()
                        )
                        FilledTonalIconButton(
                            onClick = { showMapFullscreen = true },
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                        ) {
                            Icon(Icons.Default.Fullscreen, "Open map fullscreen")
                        }
                    }
                }
            }

            item {
                ExpressiveCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Journey details", style = MaterialTheme.typography.headlineSmall)

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Endpoint("FROM", startName, Modifier.weight(1f))
                            Endpoint("TO", endName, Modifier.weight(1f))
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DetailValue("Started", time(journey.startedAt), Modifier.weight(1f))
                            DetailValue("Ended", journey.endedAt?.let(::time) ?: "Now", Modifier.weight(1f))
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DetailValue(
                                "Avg speed",
                                speed?.let { "\%.1f km/h".format(it) } ?: "—",
                                Modifier.weight(1f)
                            )
                            DetailValue("GPS points", journey.path.size.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                ExpressiveCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Route timeline", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    "\${journey.path.size} GPS points recorded",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.Route,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (journey.path.isNotEmpty()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(time(journey.path.first().timestamp))
                                Text(time(journey.path.last().timestamp))
                            }
                            ExpressiveProgress(1f)
                        }
                    }
                }
            }
        }

        if (showMapFullscreen) {
            Dialog(
                onDismissRequest = { showMapFullscreen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    Box(Modifier.fillMaxSize()) {
                        JourneyMap(
                            points = journey.path,
                            modifier = Modifier.fillMaxSize()
                        )
                        FilledTonalIconButton(
                            onClick = { showMapFullscreen = false },
                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                        ) {
                            Icon(Icons.Default.Close, "Close fullscreen map")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyMetric(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpressiveIconBadge(
                modifier = Modifier.size(38.dp),
                icon = { Icon(icon, null) }
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Endpoint(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun JourneyMap(
    points: List<LocationPoint>,
    modifier: Modifier = Modifier
) {
    val coordinates = remember(points) {
        points.map { LatLng(it.latitude, it.longitude) }
    }

    if (coordinates.isEmpty()) {
        Box(
            modifier.height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No route data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LocationMap(
        points = coordinates,
        modifier = modifier.clip(RoundedCornerShape(24.dp)),
        interactive = true,
        drawRoute = true
    )
}

private fun time(i: java.time.Instant) =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        .format(i.atZone(ZoneId.systemDefault()))

private fun formatMinutes(m: Long) =
    if (m < 60) "\${m} min" else "\${m / 60}h \${m % 60}m"

private fun formatDistance(m: Double?) =
    m?.let { if (it < 1000) "\${it.toInt()} m" else "\%.1f km".format(it / 1000.0) }
        ?: "Unavailable"

private fun JourneyMode.label() = when (this) {
    JourneyMode.WALKING -> "Walking"
    JourneyMode.CYCLING -> "Cycling"
    JourneyMode.VEHICLE -> "Driving"
    JourneyMode.UNKNOWN -> "Movement"
}

private fun modeIcon(m: JourneyMode) = when (m) {
    JourneyMode.WALKING -> Icons.Default.DirectionsWalk
    JourneyMode.CYCLING -> Icons.Default.PedalBike
    JourneyMode.VEHICLE -> Icons.Default.DirectionsCar
    JourneyMode.UNKNOWN -> Icons.Default.Route
}

package com.locationdots.app.feature.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.maplibre.android.geometry.LatLng
import com.locationdots.app.domain.model.Place
import com.locationdots.app.ui.components.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun PlacesOverviewScreen(
    places: List<Place>,
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit,
    onTabSelected: (AppTab) -> Unit = {}
) {
    val points = remember(places) { places.map { LatLng(it.latitude, it.longitude) } }
    var isMapFullscreen by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Places", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "The locations that became part of your story.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(330.dp)
                    ) {
                        if (points.isNotEmpty()) {
                            LocationMap(
                                points = points,
                                modifier = Modifier.fillMaxSize(),
                                interactive = true,
                                drawRoute = false
                            )
                        } else {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ExpressiveIconBadge(icon = { Icon(Icons.Default.Place, null) })
                                    Text("No saved places yet", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Saved locations will appear here.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Surface(
                            Modifier.align(Alignment.TopStart).padding(12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = .92f)
                        ) {
                            Text(
                                places.size.toString() + " saved places",
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        if (points.isNotEmpty()) {
                            FilledTonalIconButton(
                                onClick = { isMapFullscreen = true },
                                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .92f)
                                )
                            ) {
                                Icon(Icons.Default.Fullscreen, "Open map fullscreen")
                            }
                        }
                    }
                }
            }
            item {
                ExpressiveSectionHeader(
                    "Saved places",
                    "Tap a place to see its history."
                )
            }
            if (places.isEmpty()) {
                item {
                    ExpressiveCard {
                        Column(
                            Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Place, null, Modifier.size(42.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("No places yet", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Keep tracking to discover your regular places.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(places, key = { it.id }) { place ->
                    ExpressiveListRow(
                        title = place.name ?: "Unnamed place",
                        subtitle = String.format(
                            java.util.Locale.US,
                            "%.5f, %.5f",
                            place.latitude,
                            place.longitude
                        ),
                        icon = { Icon(Icons.Default.Place, null) },
                        onClick = { onPlaceClick(place.id) }
                    )
                }
            }
        }

    if (isMapFullscreen && points.isNotEmpty()) {
        Dialog(
            onDismissRequest = { isMapFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(Modifier.fillMaxSize()) {
                    LocationMap(
                        points = points,
                        modifier = Modifier.fillMaxSize(),
                        interactive = true,
                        drawRoute = false
                    )
                    Row(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(onClick = { isMapFullscreen = false }) {
                            Icon(Icons.Default.Close, "Close fullscreen map")
                        }
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = .92f)
                        ) {
                            Text(
                                places.size.toString() + " saved places",
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

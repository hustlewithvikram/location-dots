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
import androidx.compose.ui.text.style.TextOverflow
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Places", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Your saved places and their history.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(300.dp)) {
                    if (points.isNotEmpty()) {
                        LocationMap(
                            points = points,
                            modifier = Modifier.fillMaxSize(),
                            interactive = true,
                            drawRoute = false
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ExpressiveIconBadge(icon = { Icon(Icons.Default.Place, null) })
                                Text("No saved places yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Places will appear as you discover them.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (points.isNotEmpty()) {
                        Surface(
                            Modifier.align(Alignment.TopStart).padding(12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = .92f)
                        ) {
                            Text(
                                savedPlacesLabel(places.size),
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Saved places", style = MaterialTheme.typography.headlineSmall)
                Text(
                    savedPlacesStoryLabel(places.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (places.isEmpty()) {
            item {
                ExpressiveCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ExpressiveIconBadge(icon = { Icon(Icons.Default.Place, null) })
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
                ExpressiveCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPlaceClick(place.id) }
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpressiveIconBadge(
                            modifier = Modifier.size(48.dp),
                            icon = { Icon(Icons.Default.Place, null) }
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                place.name ?: "Unnamed place",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Saved \${formatPlaceDate(place.createdAt)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Open place",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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


private fun savedPlacesLabel(count: Int) =
    "$count saved ${if (count == 1) "place" else "places"}"

private fun savedPlacesStoryLabel(count: Int) =
    "$count ${if (count == 1) "place" else "places"} in your story"

private fun formatPlaceDate(instant: java.time.Instant): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        .format(instant.atZone(ZoneId.systemDefault()))

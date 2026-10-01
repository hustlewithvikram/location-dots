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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.locationdots.app.domain.model.Place
import com.locationdots.app.ui.components.*
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun PlacesOverviewScreen(
    places: List<Place>,
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit,
    onTabSelected: (AppTab) -> Unit = {}
) {
    val camera = rememberCameraPositionState()
    val points = remember(places) { places.map { LatLng(it.latitude, it.longitude) } }

    LaunchedEffect(points) {
        if (points.isNotEmpty()) {
            val bounds = LatLngBounds.builder().apply { points.forEach(::include) }.build()
            camera.move(if (points.size == 1) CameraUpdateFactory.newLatLngZoom(points.first(), 13f) else CameraUpdateFactory.newLatLngBounds(bounds, 90))
        }
    }

    Scaffold(bottomBar = { AppBottomBar(AppTab.MAP, onTabSelected) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Places", style = MaterialTheme.typography.headlineLarge)
                        Text("The locations that became part of your story.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ExpressiveIconButton(
                        onClick = { if (points.isNotEmpty()) camera.move(CameraUpdateFactory.newLatLngZoom(points.first(), 13f)) },
                        icon = { Icon(Icons.Default.MyLocation, "Center") },
                        emphasized = true
                    )
                }
            }
            item {
                ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().height(330.dp)) {
                        GoogleMap(
                            Modifier.fillMaxSize(),
                            cameraPositionState = camera,
                            uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false)
                        ) {
                            places.forEach { place ->
                                Marker(
                                    state = MarkerState(LatLng(place.latitude, place.longitude)),
                                    title = place.name ?: "Unnamed place",
                                    onClick = { onPlaceClick(place.id); true }
                                )
                            }
                        }
                        Surface(Modifier.align(Alignment.TopStart).padding(12.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .9f)) {
                            Text(places.size.toString() + " saved places", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            item { ExpressiveSectionHeader("Saved places", "Tap a place to see its history.") }
            if (places.isEmpty()) {
                item {
                    ExpressiveCard {
                        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Place, null, Modifier.size(42.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("No places yet", style = MaterialTheme.typography.titleLarge)
                            Text("Keep tracking to discover your regular places.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(places, key = { it.id }) { place ->
                    ExpressiveListRow(
                        title = place.name ?: "Unnamed place",
                        subtitle = String.format(java.util.Locale.US, "%.5f, %.5f", place.latitude, place.longitude),
                        icon = { Icon(Icons.Default.Place, null) },
                        onClick = { onPlaceClick(place.id) }
                    )
                }
            }
        }
    }
}

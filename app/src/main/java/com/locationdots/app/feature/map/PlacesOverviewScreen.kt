package com.locationdots.app.feature.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
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
import com.google.maps.android.compose.rememberCameraPositionState
import com.locationdots.app.domain.model.Place
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesOverviewScreen(
    places: List<Place>,
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit
) {
    val camera = rememberCameraPositionState()
    val coordinates = remember(places) {
        places.map { LatLng(it.latitude, it.longitude) }
    }

    LaunchedEffect(coordinates) {
        when (coordinates.size) {
            0 -> Unit
            1 -> camera.move(
                CameraUpdateFactory.newLatLngZoom(coordinates.first(), 14f)
            )
            else -> {
                val bounds = LatLngBounds.builder().apply {
                    coordinates.forEach(::include)
                }.build()
                camera.move(CameraUpdateFactory.newLatLngBounds(bounds, 80))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Places") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (places.isEmpty()) {
            EmptyPlaces(Modifier.fillMaxSize().padding(padding))
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = camera,
                        properties = MapProperties(),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            mapToolbarEnabled = false,
                            compassEnabled = true
                        )
                    ) {
                        places.forEach { place ->
                            Marker(
                                state = MarkerState(LatLng(place.latitude, place.longitude)),
                                title = place.name ?: "Unnamed place",
                                snippet = "%.5f, %.5f".format(
                                    Locale.US,
                                    place.latitude,
                                    place.longitude
                                ),
                                onClick = {
                                    onPlaceClick(place.id)
                                    true
                                }
                            )
                        }
                    }
                }

                Text(
                    "Saved places",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(places, key = { it.id }) { place ->
                        PlaceRow(place, onPlaceClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(place: Place, onPlaceClick: (String) -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable { onPlaceClick(place.id) }
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    Icons.Default.Place,
                    null,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    place.name ?: "Unnamed place",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "%.5f, %.5f".format(Locale.US, place.latitude, place.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyPlaces(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.LocationOn,
                null,
                modifier = Modifier.size(44.dp)
            )
            Text("No places yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Places will appear as Location Dots learns your visits.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

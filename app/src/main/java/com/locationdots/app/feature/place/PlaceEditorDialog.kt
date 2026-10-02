package com.locationdots.app.feature.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.Place
import com.locationdots.app.ui.components.LocationMap
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import org.maplibre.android.geometry.LatLng
import java.util.Locale

@Composable
fun PlaceEditorDialog(
    initialPlace: Place? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, latitude: Double, longitude: Double) -> Unit,
    onRequestCurrentLocation: ((onLocation: (Double, Double) -> Unit) -> Unit)? = null
) {
    var name by remember(initialPlace?.id) { mutableStateOf(initialPlace?.name.orEmpty()) }
    var latitude by remember(initialPlace?.id) { mutableStateOf(initialPlace?.latitude?.toString().orEmpty()) }
    var longitude by remember(initialPlace?.id) { mutableStateOf(initialPlace?.longitude?.toString().orEmpty()) }
    var error by remember(initialPlace?.id) { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<LocationSearchResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var showMapPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val searchRepository = remember { LocationSearchRepository() }

    fun setCoordinates(lat: Double, lon: Double) {
        latitude = String.format(Locale.US, "%.6f", lat)
        longitude = String.format(Locale.US, "%.6f", lon)
        error = null
    }

    fun searchLocations() {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) {
            searchError = "Enter at least 2 characters."
            results = emptyList()
            return
        }
        scope.launch {
            searching = true
            searchError = null
            results = runCatching { searchRepository.search(cleanQuery) }
                .onFailure { searchError = "Couldn't search right now." }
                .getOrDefault(emptyList())
            if (results.isEmpty() && searchError == null) {
                searchError = "No locations found."
            }
            searching = false
        }
    }

    fun useCurrentLocation() {
        onRequestCurrentLocation?.invoke { lat, lon ->
            setCoordinates(lat, lon)
            showMapPicker = true
        }
    }

    val selectedLat = latitude.toDoubleOrNull()
    val selectedLon = longitude.toDoubleOrNull()
    val mapPoint = LatLng(
        selectedLat ?: 20.5937,
        selectedLon ?: 78.9629
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialPlace == null) "Add saved location" else "Edit saved location") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 620.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Choose a place by searching, tapping the map, or using your current location.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Home", "Office").forEach { preset ->
                        AssistChip(
                            onClick = { name = preset; error = null },
                            label = { Text(preset) }
                        )
                    }
                    AssistChip(
                        onClick = { name = ""; error = null },
                        label = { Text("Other") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Location name") },
                    placeholder = { Text("e.g. Gym, College, Home") }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; searchError = null },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Search a location") },
                        placeholder = { Text("Address, landmark, business…") },
                        trailingIcon = {
                            IconButtonCompat(onClick = ::searchLocations) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                    )
                }

                if (searching) {
                    Text("Searching…", style = MaterialTheme.typography.bodySmall)
                }

                searchError?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (results.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        LazyColumn(modifier = Modifier.heightIn(max = 190.dp)) {
                            items(results) { result ->
                                Column {
                                    TextButton(
                                        onClick = {
                                            name = if (name.isBlank()) result.name else name
                                            setCoordinates(result.latitude, result.longitude)
                                            results = emptyList()
                                            query = result.address
                                            showMapPicker = true
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(result.name, color = MaterialTheme.colorScheme.onSurface)
                                            Text(
                                                result.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showMapPicker = !showMapPicker }) {
                        Icon(Icons.Default.Map, null)
                        Text(if (showMapPicker) "Hide map" else "Pick on map")
                    }
                    if (onRequestCurrentLocation != null) {
                        TextButton(onClick = ::useCurrentLocation) {
                            Icon(Icons.Default.MyLocation, null)
                            Text("Current location")
                        }
                    }
                }

                if (showMapPicker) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LocationMap(
                            points = listOf(mapPoint),
                            modifier = Modifier.fillMaxWidth().height(190.dp),
                            interactive = true,
                            fitRequest = mapPoint,
                            onMapClick = { point ->
                                setCoordinates(point.latitude, point.longitude)
                            }
                        )
                    }
                    Text(
                        "Tap anywhere on the map to place the pin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it; error = null },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it; error = null },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val lat = latitude.toDoubleOrNull()
                    val lon = longitude.toDoubleOrNull()
                    error = when {
                        cleanName.isEmpty() -> "Enter a name."
                        lat == null || lat !in -90.0..90.0 -> "Enter a valid latitude."
                        lon == null || lon !in -180.0..180.0 -> "Enter a valid longitude."
                        else -> null
                    }
                    if (error == null && lat != null && lon != null) {
                        onSave(cleanName, lat, lon)
                    }
                }
            ) {
                Text("Save location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun IconButtonCompat(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.IconButton(onClick = onClick, content = content)
}

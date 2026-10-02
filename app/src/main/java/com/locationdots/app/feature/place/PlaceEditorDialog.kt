package com.locationdots.app.feature.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.DialogProperties
import com.locationdots.app.domain.model.Place
import com.locationdots.app.ui.components.LocationMap
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.maplibre.android.geometry.LatLng
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<LocationSearchResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var showMapPicker by rememberSaveable { mutableStateOf(false) }
    var showCoordinates by rememberSaveable { mutableStateOf(false) }
    var selectedPreset by remember(initialPlace?.id) {
        mutableStateOf(
            when {
                initialPlace?.name.equals("Home", true) -> "Home"
                initialPlace?.name.equals("Office", true) -> "Office"
                initialPlace?.name.equals("Gym", true) -> "Gym"
                else -> "Other"
            }
        )
    }

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

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 760.dp),
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, top = 18.dp, end = 12.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (initialPlace == null) "Add a place" else "Edit place",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Save a location for automatic recognition.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 22.dp,
                        end = 22.dp,
                        top = 18.dp,
                        bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Place type",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Home", "Office", "Gym", "Other").forEach { preset ->
                                    FilterChip(
                                        selected = selectedPreset == preset,
                                        onClick = {
                                            selectedPreset = preset
                                            if (preset != "Other") {
                                                name = preset
                                            } else if (name == "Home" || name == "Office" || name == "Gym") {
                                                name = ""
                                            }
                                            error = null
                                        },
                                        label = { Text(preset) },
                                        leadingIcon = if (selectedPreset == preset) {
                                            { Icon(Icons.Default.Check, null) }
                                        } else {
                                            null
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                selectedPreset = when {
                                    it.equals("Home", true) -> "Home"
                                    it.equals("Office", true) -> "Office"
                                    it.equals("Gym", true) -> "Gym"
                                    else -> "Other"
                                }
                                error = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Place name") },
                            placeholder = { Text("e.g. College, Café, Friend's place") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                            isError = error?.contains("name", true) == true
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Choose location",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Search an address or landmark, use your current position, or pick a point on the map.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                searchError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Search address or place") },
                            placeholder = { Text("e.g. KTHM College, Nashik") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (searching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.padding(14.dp).height(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (query.isNotBlank()) {
                                    IconButton(onClick = { query = ""; results = emptyList(); searchError = null }) {
                                        Icon(Icons.Default.Close, "Clear search")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = ::searchLocations,
                                enabled = !searching && query.trim().length >= 2,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Search, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Search")
                            }
                            if (onRequestCurrentLocation != null) {
                                OutlinedButton(
                                    onClick = ::useCurrentLocation,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.MyLocation, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Current")
                                }
                            }
                        }
                    }

                    if (searchError != null) {
                        item {
                            Text(
                                searchError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (results.isNotEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 210.dp)
                                ) {
                                    items(results) { result ->
                                        TextButton(
                                            onClick = {
                                                name = if (name.isBlank()) result.name else name
                                                selectedPreset = when {
                                                    name.equals("Home", true) -> "Home"
                                                    name.equals("Office", true) -> "Office"
                                                    name.equals("Gym", true) -> "Gym"
                                                    else -> "Other"
                                                }
                                                setCoordinates(result.latitude, result.longitude)
                                                query = result.address
                                                results = emptyList()
                                                searchError = null
                                                showMapPicker = true
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(
                                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(
                                                        result.name,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        result.address,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2
                                                    )
                                                }
                                            }
                                        }
                                        if (result != results.last()) {
                                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { showMapPicker = !showMapPicker },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Map, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (showMapPicker) "Hide map" else "Pick precisely on map")
                        }
                    }

                    if (showMapPicker) {
                        item {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Column {
                                    LocationMap(
                                        points = listOf(mapPoint),
                                        modifier = Modifier.fillMaxWidth().height(220.dp),
                                        interactive = true,
                                        fitRequest = mapPoint,
                                        drawRoute = false,
                                        onMapClick = { point ->
                                            setCoordinates(point.latitude, point.longitude)
                                        }
                                    )
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.EditLocation, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Tap the map to move the pin.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { showCoordinates = !showCoordinates },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.EditLocation, null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (showCoordinates) "Hide coordinate details" else "Edit coordinate details")
                            }

                            if (!showCoordinates) {
                                Text(
                                    if (selectedLat != null && selectedLon != null) {
                                        "Pin: %.5f, %.5f".format(Locale.US, selectedLat, selectedLon)
                                    } else {
                                        "No location selected yet"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
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
                            }
                        }
                    }

                    error?.let {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    it,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val cleanName = name.trim()
                            val lat = latitude.toDoubleOrNull()
                            val lon = longitude.toDoubleOrNull()
                            error = when {
                                cleanName.isEmpty() -> "Enter a place name."
                                lat == null || lat !in -90.0..90.0 -> "Choose a valid location."
                                lon == null || lon !in -180.0..180.0 -> "Choose a valid location."
                                else -> null
                            }
                            if (error == null && lat != null && lon != null) {
                                onSave(cleanName, lat, lon)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (initialPlace == null) "Save place" else "Save changes")
                    }
                }
            }
        }
    }
}

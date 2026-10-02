package com.locationdots.app.feature.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.Place
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

    fun applyPreset(value: String) {
        name = value
        error = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialPlace == null) "Add saved location" else "Edit saved location") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Give this location a name. Future visits within the same area will use this saved name.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Home", "Office").forEach { preset ->
                        AssistChip(
                            onClick = { applyPreset(preset) },
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
                if (onRequestCurrentLocation != null) {
                    TextButton(
                        onClick = {
                            onRequestCurrentLocation { lat, lon ->
                                latitude = String.format(Locale.US, "%.6f", lat)
                                longitude = String.format(Locale.US, "%.6f", lon)
                                error = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use current location")
                    }
                }
                error?.let {
                    Text(
                        it,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

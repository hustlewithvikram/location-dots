package com.locationdots.app.feature.place

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.Place
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(place: Place?, onBack: () -> Unit, onRename: (String?) -> Unit) {
    var name by remember(place?.id, place?.name) { mutableStateOf(place?.name.orEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(place?.name ?: "Place") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("‹", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            )
        }
    ) { padding ->
        if (place == null) {
            Text("Place not found", modifier = Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }

        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)
                .navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Place details", style = MaterialTheme.typography.titleMedium)
                    Text("%.5f, %.5f".format(Locale.US, place.latitude, place.longitude))
                    Text("First recorded · ${formatter.format(place.createdAt)}")
                    Text("Last visited · ${formatter.format(place.updatedAt)}")
                }
            }

            Text("Name this place", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. Home, Office, Gym") }
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onRename(name) }, modifier = Modifier.weight(1f)) { Text("Save") }
                if (place.name != null) {
                    Button(onClick = { name = ""; onRename(null) }, modifier = Modifier.weight(1f)) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}

package com.locationdots.app.feature.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locationdots.app.ui.components.ExpressiveCard
import com.locationdots.app.ui.components.ExpressiveListRow

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("About") }, navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                ExpressiveCard(emphasized = true) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Location Dots", style = MaterialTheme.typography.headlineMedium)
                        Text("A private, automatic timeline of the places you visit and the journeys between them.", style = MaterialTheme.typography.bodyLarge)
                        Text("Version 0.1", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                ExpressiveCard {
                    Column(Modifier.padding(10.dp)) {
                        ExpressiveListRow("On-device location engine", "Fused location + Room persistence.", { Icon(Icons.Default.LocationOn, null) })
                        ExpressiveListRow("Journey processing", "Routes, movement modes and visit detection.", { Icon(Icons.Default.Architecture, null) })
                        ExpressiveListRow("Privacy first", "No account is required to use the local timeline.", { Icon(Icons.Default.Security, null) })
                        ExpressiveListRow("Built with Kotlin", "Jetpack Compose, Material 3 and Room.", { Icon(Icons.Default.Code, null) })
                    }
                }
            }
        }
    }
}

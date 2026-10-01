package com.locationdots.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.locationdots.app.ui.components.ExpressiveCard
import com.locationdots.app.ui.components.ExpressiveIconBadge

@Composable
fun OnboardingScreen(
    hasLocationPermission: Boolean,
    isTracking: Boolean,
    onRequestLocationPermission: () -> Unit,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    val titles = listOf("Remember where you go.", "See every journey.", "Private by design.")
    val descriptions = listOf(
        "Location Dots turns everyday movement into a beautiful personal timeline.",
        "Understand routes, places, distance and movement without manual logging.",
        "Your history is stored locally on your device and stays under your control."
    )
    val icons = listOf(Icons.Default.LocationOn, Icons.Default.Route, Icons.Default.AutoAwesome)

    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text((page + 1).toString() + "/3", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(42.dp))
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer))),
                contentAlignment = Alignment.Center
            ) {
                ExpressiveCard(emphasized = true) {
                    Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ExpressiveIconBadge(Modifier.size(72.dp), { Icon(icons[page], null, Modifier.size(34.dp)) })
                        Text("Location Dots", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            Spacer(Modifier.height(34.dp))
            Text(titles[page], style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(descriptions[page], style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(3) { index ->
                    Box(Modifier.padding(horizontal = 4.dp).size(if (index == page) 28.dp else 8.dp, 8.dp).clip(CircleShape).background(if (index == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest))
                }
            }
            if (page < 2) Button({ page++ }, Modifier.fillMaxWidth()) { Text("Continue") }
            else if (!hasLocationPermission) Button(onRequestLocationPermission, Modifier.fillMaxWidth()) { Text("Allow location access") }
            else if (isTracking) OutlinedButton(onStopTracking, Modifier.fillMaxWidth()) { Text("Pause tracking") }
            else Button(onStartTracking, Modifier.fillMaxWidth()) { Text("Start tracking") }
        }
    }
}

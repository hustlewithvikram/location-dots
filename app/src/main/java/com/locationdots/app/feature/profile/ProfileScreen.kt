package com.locationdots.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.TimelineEvent

@Composable
fun ProfileScreen(
    events: List<TimelineEvent>,
    placesCount: Int,
    isTracking: Boolean,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    val visits = events.count { it is TimelineEvent.Visit }
    val journeys = events.count { it is TimelineEvent.Journey }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Spacer(Modifier.width(12.dp))
            Text("Profile", style = MaterialTheme.typography.headlineSmall)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(92.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Location Dots", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Your local location journal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = if (isTracking) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isTracking) "Tracking active" else "Tracking paused")
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text("Your activity", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileStat(placesCount.toString(), "places", Icons.Default.Place, Modifier.weight(1f))
            ProfileStat(visits.toString(), "visits", Icons.Default.LocationOn, Modifier.weight(1f))
            ProfileStat(journeys.toString(), "journeys", Icons.Default.Route, Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Private by design", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Your location history stays on this device. Location Dots does not require an account.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSettings,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Default.Settings, null)
            Spacer(Modifier.width(8.dp))
            Text("Open settings")
        }
    }
}

@Composable
private fun ProfileStat(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(5.dp))
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

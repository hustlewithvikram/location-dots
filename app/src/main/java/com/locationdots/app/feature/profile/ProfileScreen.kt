package com.vikram.locationdots.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vikram.locationdots.domain.model.TimelineEvent
import com.vikram.locationdots.ui.components.ProfileAvatar
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val profileDoodles = listOf("eco" to Icons.Default.Eco, "night" to Icons.Default.DarkMode, "sun" to Icons.Default.LightMode, "water" to Icons.Default.Water, "flower" to Icons.Default.LocalFlorist, "sparkle" to Icons.Default.AutoAwesome, "pets" to Icons.Default.Pets, "world" to Icons.Default.Public)

@Composable
fun ProfileScreen(
    events: List<TimelineEvent>,
    placesCount: Int,
    isTracking: Boolean,
    profileName: String,
    profilePhotoUri: String?,
    onNameChange: (String) -> Unit,
    onPhotoChange: () -> Unit,
    onPhotoUriChange: (String) -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    val visits = events.count { it is TimelineEvent.Visit }
    val journeys = events.count { it is TimelineEvent.Journey }
    val latestEvent = events.maxByOrNull {
        when (it) {
            is TimelineEvent.Visit -> it.arrival
            is TimelineEvent.Journey -> it.startedAt
        }
    }
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Spacer(Modifier.width(12.dp))
            Text("Profile", style = MaterialTheme.typography.headlineSmall)
        }

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, top = 8.dp, end = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                ProfileIdentityCard(
                    name = profileName,
                    photoUri = profilePhotoUri,
                    onEdit = { showEditDialog = true }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Your activity", style = MaterialTheme.typography.titleLarge)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStat(placesCount.toString(), "places", Icons.Default.Place, Modifier.weight(1f))
                        ProfileStat(visits.toString(), "visits", Icons.Default.LocationOn, Modifier.weight(1f))
                        ProfileStat(journeys.toString(), "journeys", Icons.Default.Route, Modifier.weight(1f))
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Timeline, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Your location journal", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    when {
                                        events.isEmpty() -> "Your visits and journeys will appear here as your timeline grows."
                                        visits == 1 && journeys == 0 -> "Your first recorded visit is part of your story."
                                        else -> "A record of the places you visit and the journeys between them."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Recent activity", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        if (latestEvent == null) {
                            Text(
                                "Nothing recorded yet. Keep Location Dots running to start building your personal timeline.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val latestTime = when (latestEvent) {
                                is TimelineEvent.Visit -> latestEvent.arrival
                                is TimelineEvent.Journey -> latestEvent.startedAt
                            }
                            val latestTitle = when (latestEvent) {
                                is TimelineEvent.Visit -> "Visited a place"
                                is TimelineEvent.Journey -> "Recorded a journey"
                            }
                            Text(latestTitle, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")
                                    .withZone(ZoneId.systemDefault())
                                    .format(latestTime),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    onClick = onSettings
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Personalise Location Dots", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tracking, appearance, maps and data controls",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentName = profileName,
            photoUri = profilePhotoUri,
            onDismiss = { showEditDialog = false },
            onSave = { name ->
                onNameChange(name)
                showEditDialog = false
            },
            onChangePhoto = onPhotoChange,
            onSelectDoodle = { doodle -> onPhotoUriChange("doodle:$doodle") }
        )
    }
}

@Composable
private fun ProfileIdentityCard(
    name: String,
    photoUri: String?,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileAvatar(photoUri = photoUri, name = name, modifier = Modifier.size(88.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    name.ifBlank { "Your name" },
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Your personal location journal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit profile", style = MaterialTheme.typography.labelLarge)
                }
            }
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
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier.padding(vertical = 16.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EditProfileDialog(
    currentName: String,
    photoUri: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onChangePhoto: () -> Unit,
    onSelectDoodle: (String) -> Unit
) {
    var name by remember(currentName) { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Edit profile", style = MaterialTheme.typography.headlineSmall)
                Text("Make your location journal yours.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ProfileAvatar(photoUri = photoUri, name = name, modifier = Modifier.size(88.dp))
                }
                OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose an avatar", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        profileDoodles.forEach { (doodle, icon) ->
                            val selected = photoUri == "doodle:$doodle"
                            Surface(onClick = { onSelectDoodle(doodle) }, modifier = Modifier.size(48.dp).then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier), shape = CircleShape, color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = icon, contentDescription = doodle, modifier = Modifier.size(24.dp), tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                OutlinedButton(onClick = onChangePhoto, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(vertical = 13.dp)) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose a photo")
                }
                Text("Choose a crisp vector avatar or use a photo. Photos are resized for smooth scrolling.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { onSave(name.trim().ifBlank { "Your name" }) }, shape = RoundedCornerShape(16.dp)) { Text("Save changes") } },
        dismissButton = { OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(16.dp)) { Text("Cancel") } }
    )
}

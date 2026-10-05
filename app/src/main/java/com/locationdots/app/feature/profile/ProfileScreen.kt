package com.locationdots.app.feature.profile

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.locationdots.app.domain.model.TimelineEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    events: List<TimelineEvent>,
    placesCount: Int,
    isTracking: Boolean,
    profileName: String,
    profilePhotoUri: String?,
    onNameChange: (String) -> Unit,
    onPhotoChange: () -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    val visits = events.count { it is TimelineEvent.Visit }
    val journeys = events.count { it is TimelineEvent.Journey }
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
            contentPadding = PaddingValues(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProfileIdentityCard(
                    name = profileName,
                    photoUri = profilePhotoUri,
                    isTracking = isTracking,
                    onEdit = { showEditDialog = true }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your activity", style = MaterialTheme.typography.titleLarge)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStat(placesCount.toString(), "places", Icons.Default.Place, Modifier.weight(1f))
                        ProfileStat(visits.toString(), "visits", Icons.Default.LocationOn, Modifier.weight(1f))
                        ProfileStat(journeys.toString(), "journeys", Icons.Default.Route, Modifier.weight(1f))
                    }
                }
            }

            item {
                ProfileSectionCard(
                    icon = Icons.Default.Timeline,
                    title = "Tracking",
                    value = if (isTracking) "Active" else "Paused",
                    description = if (isTracking) {
                        "Location Dots is currently recording your movement on this device."
                    } else {
                        "Tracking is paused. Turn it on in Settings when you want to continue your timeline."
                    },
                    emphasized = isTracking
                )
            }

            item {
                ProfileSectionCard(
                    icon = Icons.Default.Lock,
                    title = "Private by design",
                    value = "Stored on this device",
                    description = "Your profile and location history stay local. No account is required."
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    onClick = onSettings
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary)
                        Column(
                            Modifier.weight(1f).padding(horizontal = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("App settings", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tracking, appearance, maps and data controls",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Location Dots · Local location journal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentName = profileName,
            onDismiss = { showEditDialog = false },
            onSave = { name ->
                onNameChange(name)
                showEditDialog = false
            },
            onChangePhoto = onPhotoChange
        )
    }
}

@Composable
private fun ProfileIdentityCard(
    name: String,
    photoUri: String?,
    isTracking: Boolean,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                ProfileAvatar(photoUri = photoUri, name = name, modifier = Modifier.size(108.dp))
                FilledTonalIconButton(
                    onClick = onEdit,
                    modifier = Modifier.align(Alignment.BottomEnd).size(40.dp)
                ) {
                    Icon(Icons.Default.Edit, "Edit profile", Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                name.ifBlank { "Your name" },
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                if (isTracking) "Tracking is active" else "Tracking is paused",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit profile")
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    photoUri: String?,
    name: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, photoUri) {
        value = if (photoUri.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(photoUri))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
        }
    }

    Surface(
        modifier = modifier.clip(CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    name.trim().firstOrNull()?.uppercase() ?: "L",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ProfileSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    description: String,
    emphasized: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (emphasized) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (emphasized) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(
                Modifier.weight(1f).padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

@Composable
private fun EditProfileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onChangePhoto: () -> Unit
) {
    var name by remember(currentName) { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = onChangePhoto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Change profile photo")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim().ifBlank { "Your name" }) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

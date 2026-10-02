package com.locationdots.app.feature.about

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.locationdots.app.ui.components.ExpressiveCard
import com.locationdots.app.ui.components.ExpressiveIconBadge
import com.locationdots.app.ui.components.ExpressiveListRow

private const val SOURCE_URL = "https://github.com/hustlewithvikram/location-dots"
private const val ISSUES_URL = "https://github.com/hustlewithvikram/location-dots/issues"
private const val RELEASES_URL = "https://github.com/hustlewithvikram/location-dots/releases"
private const val DEVELOPER_URL = "https://github.com/hustlewithvikram"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 10.dp, 18.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("About Location Dots", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "App details, privacy, permissions and project links.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            ExpressiveCard(emphasized = true) {
                Row(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveIconBadge(
                        icon = { Icon(Icons.Default.LocationOn, null) },
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Location Dots", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "A local-first timeline for places and journeys.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Version 0.1.0 · Android 8.0+",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item { AboutSectionTitle("Privacy & security") }

        item {
            ExpressiveCard {
                Column(Modifier.padding(8.dp)) {
                    ExpressiveListRow(
                        "Local by default",
                        "Location history and saved places use the on-device Room database.",
                        { Icon(Icons.Default.Storage, null) }
                    )
                    ExpressiveListRow(
                        "No account required",
                        "The app does not require an account or cloud history.",
                        { Icon(Icons.Default.PersonOff, null) }
                    )
                    ExpressiveListRow(
                        "Explicit sharing",
                        "Data is only shared when you choose a share or export action.",
                        { Icon(Icons.Default.Share, null) }
                    )
                    ExpressiveListRow(
                        "Open project",
                        "The source is publicly available for inspection.",
                        { Icon(Icons.Default.Code, null) },
                        trailing = { Icon(Icons.Default.ChevronRight, null) },
                        onClick = { openUrl(SOURCE_URL) }
                    )
                }
            }
        }

        item { AboutSectionTitle("Permissions") }

        item {
            ExpressiveCard {
                Column(Modifier.padding(8.dp)) {
                    PermissionRow(
                        "Precise & approximate location",
                        "Used to record visits and journeys.",
                        Icons.Default.LocationOn
                    )
                    PermissionRow(
                        "Foreground location service",
                        "Keeps tracking active while the app records your location.",
                        Icons.Default.MyLocation
                    )
                    PermissionRow(
                        "Internet",
                        "Used for map tiles and location search.",
                        Icons.Default.Public
                    )
                    PermissionRow(
                        "Storage on Android 9 and older",
                        "Used only for legacy local crash-log file handling.",
                        Icons.Default.Folder
                    )
                }
            }
        }

        item {
            ExpressiveCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExpressiveIconBadge(
                            icon = { Icon(Icons.Default.Security, null) },
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Why location access?", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Location access is the core permission needed to build the timeline. You can control it from Android settings, and the app does not need an account to use the local timeline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { AboutSectionTitle("Developer") }

        item {
            ExpressiveCard {
                ExpressiveListRow(
                    "Vikram Vishwakarma",
                    "Developer · Location Dots",
                    { Icon(Icons.Default.Person, null) },
                    trailing = { Icon(Icons.Default.ChevronRight, null) },
                    onClick = { openUrl(DEVELOPER_URL) }
                )
            }
        }

        item { AboutSectionTitle("Project") }

        item {
            ExpressiveCard {
                Column(Modifier.padding(8.dp)) {
                    ExpressiveListRow(
                        "Source code",
                        "Browse the complete project on GitHub.",
                        { Icon(Icons.Default.Code, null) },
                        trailing = { Icon(Icons.Default.OpenInNew, null) },
                        onClick = { openUrl(SOURCE_URL) }
                    )
                    ExpressiveListRow(
                        "Report an issue",
                        "Report bugs or request improvements.",
                        { Icon(Icons.Default.BugReport, null) },
                        trailing = { Icon(Icons.Default.OpenInNew, null) },
                        onClick = { openUrl(ISSUES_URL) }
                    )
                    ExpressiveListRow(
                        "Releases",
                        "View published versions and release notes.",
                        { Icon(Icons.Default.Download, null) },
                        trailing = { Icon(Icons.Default.OpenInNew, null) },
                        onClick = { openUrl(RELEASES_URL) }
                    )
                }
            }
        }

        item {
            ExpressiveCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Built with", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Kotlin · Jetpack Compose · Material 3 · Room · MapLibre · OpenFreeMap · OpenStreetMap",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutSectionTitle(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ExpressiveListRow(
        title = title,
        subtitle = subtitle,
        icon = { Icon(icon, null) }
    )
}

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
        contentWindowInsets = WindowInsets.safeDrawing,
        contentPadding = PaddingValues(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "About Location Dots",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "App details, privacy and project links.",
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
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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
                    InfoRow(
                        "Local by default",
                        "Location history and saved places stay in the on-device database.",
                        Icons.Default.Storage
                    )
                    InfoRow(
                        "No account required",
                        "The local timeline does not require an account or cloud history.",
                        Icons.Default.PersonOff
                    )
                    InfoRow(
                        "Explicit sharing",
                        "Data leaves the app only when you choose an export or share action.",
                        Icons.Default.Share
                    )
                    ExpressiveListRow(
                        "Open-source project",
                        "Source code is publicly available.",
                        { Icon(Icons.Default.Code, null) },
                        trailing = { Icon(Icons.Default.OpenInNew, "Open") },
                        onClick = { openUrl(SOURCE_URL) }
                    )
                }
            }
        }

        item { AboutSectionTitle("Permissions") }

        item {
            ExpressiveCard {
                Column(Modifier.padding(8.dp)) {
                    InfoRow(
                        "Location",
                        "Precise or approximate location is used to record visits and journeys.",
                        Icons.Default.LocationOn
                    )
                    InfoRow(
                        "Foreground location service",
                        "Keeps location tracking active while the tracking service is running.",
                        Icons.Default.MyLocation
                    )
                    InfoRow(
                        "Internet",
                        "Used for map tiles and location search.",
                        Icons.Default.Public
                    )
                    InfoRow(
                        "Legacy storage",
                        "On Android 9 and older, used for local crash-log handling.",
                        Icons.Default.Folder
                    )
                }
            }
        }

        item {
            ExpressiveCard {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExpressiveIconBadge(
                            icon = { Icon(Icons.Default.Security, null) },
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Why location access?",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        "Location access is required to build the timeline. You can control it through Android settings, and no account is required.",
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
                    trailing = { Icon(Icons.Default.OpenInNew, "Open") },
                    onClick = { openUrl(DEVELOPER_URL) }
                )
            }
        }

        item { AboutSectionTitle("Project links") }

        item {
            ExpressiveCard {
                Column(Modifier.padding(8.dp)) {
                    ExpressiveListRow(
                        "Source code",
                        "Browse the complete project on GitHub.",
                        { Icon(Icons.Default.Code, null) },
                        trailing = { Icon(Icons.Default.OpenInNew, "Open") },
                        onClick = { openUrl(SOURCE_URL) }
                    )
                    ExpressiveListRow(
                        "Report an issue",
                        "Report bugs or request improvements.",
                        { Icon(Icons.Default.BugReport, null) },
                        trailing = { Icon(Icons.Default.OpenInNew, "Open") },
                        onClick = { openUrl(ISSUES_URL) }
                    )
                    ExpressiveListRow(
                        "Releases",
                        "View versions and release notes.",
                        { Icon(Icons.Default.Download, null) },
                        trailing = { Icon(Icons.Default.OpenInNew, "Open") },
                        onClick = { openUrl(RELEASES_URL) }
                    )
                }
            }
        }

        item {
            ExpressiveCard {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
private fun InfoRow(
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

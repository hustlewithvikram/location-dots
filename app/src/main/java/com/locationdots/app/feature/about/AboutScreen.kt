package com.vikram.locationdots.feature.about

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
import com.vikram.locationdots.ui.components.ExpressiveCard
import com.vikram.locationdots.ui.components.ExpressiveIconBadge
import com.vikram.locationdots.ui.components.ExpressiveListRow
import androidx.core.net.toUri

private const val SOURCE_URL = "https://github.com/hustlewithvikram/location-dots"
private const val ISSUES_URL = "https://github.com/hustlewithvikram/location-dots/issues"
private const val RELEASES_URL = "https://github.com/hustlewithvikram/location-dots/releases"
private const val DEVELOPER_URL = "https://github.com/hustlewithvikram"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
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
                            "Version " + runCatching {
                                @Suppress("DEPRECATION")
                                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
                            }.getOrDefault("Unknown"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item { AboutSectionTitle("What you can do") }

        item {
            ExpressiveCard {
                Column(Modifier.padding(8.dp)) {
                    InfoRow(
                        "Timeline",
                        "Review recorded visits and journeys in chronological order.",
                        Icons.Default.Timeline
                    )
                    InfoRow(
                        "Places",
                        "Save meaningful places and recognize visits to them.",
                        Icons.Default.Place
                    )
                    InfoRow(
                        "Insights",
                        "Explore activity patterns and travel summaries.",
                        Icons.Default.Insights
                    )
                    InfoRow(
                        "Personal backups",
                        "Export selected data or restore a compatible backup.",
                        Icons.Default.FolderCopy
                    )
                }
            }
        }

        item { AboutSectionTitle("App details") }

        item {
            ExpressiveCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    InfoRow("Platform", "Android 8.0 and newer", Icons.Default.PhoneAndroid)
                    InfoRow("Data storage", "On-device database", Icons.Default.Storage)
                    InfoRow("Map technology", "MapLibre with OpenFreeMap", Icons.Default.Map)
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    ExpressiveListRow(
        title = title,
        subtitle = subtitle,
        icon = { Icon(icon, null) }
    )
}

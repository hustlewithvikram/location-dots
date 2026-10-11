package com.locationdots.app.feature.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.locationdots.app.ui.components.ExpressiveCard
import com.locationdots.app.ui.components.ExpressiveIconBadge
import com.locationdots.app.ui.components.ExpressiveListRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL

private const val LATEST_RELEASE_URL = "https://api.github.com/repos/hustlewithvikram/location-dots/releases/latest"

@Composable
fun UpdatesSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var updateAvailable by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var apkUrl by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("Check whether a newer version is available.") }

    fun checkForUpdates() {
        if (checking || downloading) return
        checking = true
        message = "Checking for updates…"
        scope.launch {
            try {
                val release = withContext(Dispatchers.IO) {
                    val connection = URL(LATEST_RELEASE_URL).openConnection()
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.setRequestProperty("Accept", "application/vnd.github+json")
                    JSONObject(connection.getInputStream().bufferedReader().use { it.readText() })
                }
                val tag = release.optString("tag_name").removePrefix("v")
                @Suppress("DEPRECATION")
                val current = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
                val assets = release.optJSONArray("assets")
                var foundApk: String? = null
                if (assets != null) {
                    for (index in 0 until assets.length()) {
                        val asset = assets.optJSONObject(index) ?: continue
                        if (asset.optString("name").endsWith(".apk", true)) {
                            foundApk = asset.optString("browser_download_url").takeIf { it.startsWith("https://") }
                            if (foundApk != null) break
                        }
                    }
                }
                val newer = compareVersions(tag, current) > 0
                latestVersion = tag
                apkUrl = foundApk
                updateAvailable = newer && foundApk != null
                message = when {
                    newer && foundApk == null -> "Version $tag is published, but its APK is not attached yet."
                    updateAvailable -> "Version $tag is ready to download."
                    else -> "You're using the latest version ($current)."
                }
            } catch (error: Exception) {
                updateAvailable = false
                message = if (error.message?.contains("404") == true) {
                    "No published release was found yet."
                } else {
                    "Couldn't check for updates. Check your connection and try again."
                }
            } finally {
                checking = false
            }
        }
    }

    fun downloadUpdate() {
        val url = apkUrl ?: return
        if (downloading) return
        downloading = true
        progress = 0f
        message = "Downloading update…"
        scope.launch {
            try {
                val apkFile = withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection()
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    val total = connection.contentLengthLong
                    val target = File(context.cacheDir, "location-dots-update.apk")
                    connection.getInputStream().use { input ->
                        target.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var received = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                received += count
                                if (total > 0) {
                                    val value = (received.toFloat() / total).coerceIn(0f, 1f)
                                    withContext(Dispatchers.Main) { progress = value }
                                }
                            }
                        }
                    }
                    target
                }
                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", apkFile)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                message = "Download complete. Follow Android's installer to update."
            } catch (error: Exception) {
                message = "Couldn't download the update. Please try again."
            } finally {
                downloading = false
            }
        }
    }

    LaunchedEffect(Unit) { checkForUpdates() }
    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Spacer(Modifier.width(12.dp))
                Text("Updates", style = MaterialTheme.typography.headlineSmall)
            }
        }
        item {
            ExpressiveCard(emphasized = updateAvailable) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExpressiveIconBadge(
                            icon = {
                                Icon(
                                    when {
                                        downloading -> Icons.Default.Downloading
                                        updateAvailable -> Icons.Default.SystemUpdate
                                        checking -> Icons.Default.Sync
                                        else -> Icons.Default.SystemUpdateAlt
                                    },
                                    null
                                )
                            },
                            containerColor = if (updateAvailable) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                when {
                                    downloading -> "Downloading update"
                                    checking -> "Checking for updates"
                                    updateAvailable -> "Update available"
                                    else -> "Software updates"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (downloading) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(36.dp), strokeWidth = 4.dp)
                            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    if (updateAvailable && !downloading) {
                        Button(onClick = ::downloadUpdate, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Download ${latestVersion ?: "update"}")
                        }
                    } else {
                        OutlinedButton(
                            onClick = ::checkForUpdates,
                            enabled = !checking && !downloading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (checking) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, null)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (checking) "Checking…" else "Check for updates")
                        }
                    }
                }
            }
        }
        item {
            ExpressiveCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Update details", style = MaterialTheme.typography.titleMedium)
                    ExpressiveListRow(
                        "Installed version",
                        installedVersion(context),
                        { Icon(Icons.Default.PhoneAndroid, null) },
                        trailing = { }
                    )
                    if (latestVersion != null) {
                        ExpressiveListRow(
                            "Latest release",
                            latestVersion!!,
                            { Icon(Icons.Default.NewReleases, null) },
                            trailing = { }
                        )
                    }
                    Text(
                        "Updates are checked against published GitHub releases. Android will ask you to confirm installation after the APK downloads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun installedVersion(context: android.content.Context): String = runCatching {
    @Suppress("DEPRECATION")
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
}.getOrDefault("Unknown")

private fun compareVersions(first: String, second: String): Int {
    val a = first.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    val b = second.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    for (index in 0 until maxOf(a.size, b.size)) {
        val comparison = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
        if (comparison != 0) return comparison
    }
    return 0
}

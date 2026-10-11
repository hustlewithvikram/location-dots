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
import com.locationdots.app.BuildConfig
import com.locationdots.app.ui.components.ExpressiveCard
import com.locationdots.app.ui.components.ExpressiveIconBadge
import com.locationdots.app.ui.components.ExpressiveListRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.security.MessageDigest

private const val RELEASES_API = "https://api.github.com/repos/hustlewithvikram/location-dots/releases"

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
    var expectedSha256 by remember { mutableStateOf<String?>(null) }
    var showInstallDialog by remember { mutableStateOf(false) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var latestVersionCode by remember { mutableStateOf<Long?>(null) }
    var latestCommit by remember { mutableStateOf<String?>(null) }
    var latestRun by remember { mutableStateOf<String?>(null) }
    val updateChannel = BuildConfig.UPDATE_CHANNEL
    var message by remember { mutableStateOf("Check whether a newer version is available.") }

    fun checkForUpdates() {
        if (checking || downloading) return
        checking = true
        updateAvailable = false
        apkUrl = null
        message = "Checking the ${updateChannel} update channel…"
        scope.launch {
            try {
                val metadata = withContext(Dispatchers.IO) {
                    val endpoint = if (updateChannel == "debug") "${RELEASES_API}/tags/debug-latest" else "${RELEASES_API}/latest"
                    val connection = URL(endpoint).openConnection()
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.setRequestProperty("Accept", "application/vnd.github+json")
                    val release = JSONObject(connection.getInputStream().bufferedReader().use { it.readText() })
                    val assets = release.optJSONArray("assets") ?: throw IllegalStateException("Release has no assets")
                    var metadataUrl: String? = null
                    var apkAssetUrl: String? = null
                    for (index in 0 until assets.length()) {
                        val asset = assets.optJSONObject(index) ?: continue
                        when (asset.optString("name")) {
                            "update.json" -> metadataUrl = asset.optString("browser_download_url")
                            "app-debug.apk", "location-dots-release.apk" -> apkAssetUrl = asset.optString("browser_download_url")
                        }
                    }
                    val url = metadataUrl ?: throw IllegalStateException("Update manifest is missing")
                    val dataConnection = URL(url).openConnection()
                    dataConnection.connectTimeout = 10000
                    dataConnection.readTimeout = 10000
                    val data = JSONObject(dataConnection.getInputStream().bufferedReader().use { it.readText() })
                    val wantedName = data.optString("apk")
                    val download = (0 until assets.length()).mapNotNull { assets.optJSONObject(it) }
                        .firstOrNull { it.optString("name") == wantedName }?.optString("browser_download_url")
                        ?.takeIf { it.startsWith("https://") } ?: apkAssetUrl
                        ?: throw IllegalStateException("APK asset is missing")
                    data.put("_downloadUrl", download)
                    data
                }
                if (metadata.optString("channel") != updateChannel) throw IllegalStateException("Wrong update channel")
                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                @Suppress("DEPRECATION")
                val currentCode = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
                val candidateCode = metadata.optLong("versionCode", -1L)
                val candidateName = metadata.optString("versionName", "Unknown")
                if (candidateCode <= 0L) throw IllegalStateException("Invalid version metadata")
                latestVersion = candidateName
                latestVersionCode = candidateCode
                latestCommit = metadata.optString("commit").takeIf { it.isNotBlank() }
                latestRun = metadata.optString("runNumber").takeIf { it.isNotBlank() }
                expectedSha256 = metadata.optString("sha256").takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }
                apkUrl = metadata.optString("_downloadUrl").takeIf { it.startsWith("https://") }
                updateAvailable = candidateCode > currentCode && apkUrl != null && expectedSha256 != null
                message = when {
                    candidateCode <= currentCode -> "You are using the latest ${updateChannel} build (${installedVersion(context)})."
                    apkUrl == null -> "A newer build exists, but its APK is missing."
                    expectedSha256 == null -> "Update metadata is incomplete. Download blocked for safety."
                    updateAvailable -> "Version ${candidateName} is available on the ${updateChannel} channel."
                    else -> "No compatible update is available."
                }
            } catch (error: Exception) {
                updateAvailable = false
                message = if (error.message?.contains("404") == true) "No published ${updateChannel} update was found." else "Could not check for updates. Check your connection and try again."
            } finally { checking = false }
        }
    }
    fun downloadUpdate() {
        val url = apkUrl ?: return
        val expectedHash = expectedSha256 ?: return
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
                    connection.getInputStream().use { input -> target.outputStream().use { output ->
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
                    } }
                    val hash = MessageDigest.getInstance("SHA-256").digest(target.readBytes()).joinToString("") { "%02x".format(it) }
                    if (!hash.equals(expectedHash, true)) { target.delete(); throw IllegalStateException("checksum mismatch") }
                    target
                }
                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", apkFile)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                downloadedApk = apkFile
                showInstallDialog = true
                message = "Download verified. Ready to install ${latestVersion ?: "update"}."
            } catch (error: Exception) {
                message = if (error.message?.contains("checksum", true) == true) "The downloaded APK failed verification and was discarded." else "Could not download the update. Please try again."
            } finally { downloading = false }
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
                                    else -> if (updateChannel == "debug") "Development updates" else "Stable updates"
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
                    Text(
                        if (updateChannel == "debug") "Development channel · GitHub Actions" else "Stable channel · GitHub Releases",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val installedInfo = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull() }
                    val installedCode = installedInfo?.let {
                        @Suppress("DEPRECATION")
                        if (android.os.Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong()
                    } ?: -1L
                    ExpressiveListRow(
                        "Installed APK",
                        "${installedVersion(context)} · version code $installedCode",
                        { Icon(Icons.Default.PhoneAndroid, null) },
                        trailing = { }
                    )
                    ExpressiveListRow(
                        "Update channel",
                        if (updateChannel == "debug") "Debug / development" else "Release / stable",
                        { Icon(Icons.Default.AccountTree, null) },
                        trailing = { }
                    )
                    if (latestVersion != null) {
                        ExpressiveListRow(
                            if (updateChannel == "debug") "Latest debug build" else "Latest release",
                            "${latestVersion!!} · version code ${latestVersionCode ?: "—"}",
                            { Icon(Icons.Default.NewReleases, null) },
                            trailing = { }
                        )
                    }
                    if (latestCommit != null) {
                        ExpressiveListRow("Published commit", latestCommit!!.take(10), { Icon(Icons.Default.Commit, null) }, trailing = { })
                    }
                    if (latestRun != null && updateChannel == "debug") {
                        ExpressiveListRow("Workflow run", "#$latestRun", { Icon(Icons.Default.Terminal, null) }, trailing = { })
                    }
                    ExpressiveListRow("Package", context.packageName, { Icon(Icons.Default.Apps, null) }, trailing = { })
                    Text(
                        "Updates follow the installed build channel. Downloads are verified before Android is asked to install them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            icon = { Icon(Icons.Default.SystemUpdate, null) },
            title = { Text("Install update?") },
            text = { Text("The APK was downloaded and its SHA-256 checksum verified. Install Location Dots ${latestVersion ?: ""} now? Android will ask you to confirm installation.") },
            confirmButton = {
                TextButton(onClick = {
                    showInstallDialog = false
                    val file = downloadedApk ?: return@TextButton
                    runCatching {
                        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    }.onFailure { message = "Could not open Android's package installer." }
                }) { Text("Install") }
            },
            dismissButton = { TextButton(onClick = { showInstallDialog = false }) { Text("Cancel") } }
        )
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

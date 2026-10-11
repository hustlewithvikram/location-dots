package com.locationdots.app.feature.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.StrokeCap
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.net.HttpURLConnection
import java.security.MessageDigest

private const val RELEASES_API = "https://api.github.com/repos/hustlewithvikram/location-dots/releases"

@Composable
fun UpdatesSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var downloadConnection by remember { mutableStateOf<HttpURLConnection?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var updateAvailable by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var apkUrl by remember { mutableStateOf<String?>(null) }
    var expectedSha256 by remember { mutableStateOf<String?>(null) }
    var showInstallDialog by remember { mutableStateOf(false) }
    var showInstallPermissionDialog by remember { mutableStateOf(false) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var cachedApkVersionCode by remember { mutableStateOf<Long?>(null) }
    var latestVersionCode by remember { mutableStateOf<Long?>(null) }
    var latestCommit by remember { mutableStateOf<String?>(null) }
    var latestRun by remember { mutableStateOf<String?>(null) }
    val updateChannel = BuildConfig.UPDATE_CHANNEL
    var message by remember { mutableStateOf("Check whether a newer version is available.") }

    fun checkForUpdates() {
        if (checking || downloading) return
        checking = true
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
                val candidateFile = File(context.cacheDir, "location-dots-update-${candidateCode}.apk")
                val cachedMatches = candidateCode > currentCode && candidateFile.isFile && expectedSha256 != null &&
                    withContext(Dispatchers.IO) { runCatching { sha256(candidateFile).equals(expectedSha256, true) }.getOrDefault(false) }
                if (cachedMatches) {
                    downloadedApk = candidateFile
                    cachedApkVersionCode = candidateCode
                } else {
                    if (candidateCode <= currentCode || downloadedApk?.exists() != true || cachedApkVersionCode != candidateCode) {
                        downloadedApk?.delete()
                        downloadedApk = null
                        cachedApkVersionCode = null
                    }
                    if (candidateFile.exists()) candidateFile.delete()
                }
                message = when {
                    candidateCode <= currentCode -> "You are using the latest ${updateChannel} build (${installedVersion(context)})."
                    downloadedApk != null && cachedApkVersionCode == candidateCode -> "Version ${candidateName} is downloaded and verified. Ready to install."
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
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadConnection?.disconnect()
        File(context.cacheDir, "location-dots-update-${latestVersionCode ?: "pending"}.part").delete()
        message = "Download cancelled."
    }
    fun discardDownloadedApk() {
        downloadedApk?.delete()
        downloadedApk = null
        cachedApkVersionCode = null
        showInstallDialog = false
        message = if (updateAvailable) "Downloaded APK discarded. You can download it again." else "Downloaded APK discarded."
    }
    fun openInstaller(file: File) {
        try {
            if (!file.isFile) {
                downloadedApk = null
                cachedApkVersionCode = null
                message = "The downloaded APK is no longer available. Download it again."
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                showInstallPermissionDialog = true
                message = "Installation permission is required before Android can install this update."
                return
            }
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (intent.resolveActivity(context.packageManager) == null) {
                message = "No Android package installer is available on this device."
                return
            }
            context.startActivity(intent)
        } catch (error: Exception) {
            message = "Could not launch Android's installer: ${error.localizedMessage ?: "unknown error"}"
        }
    }
    fun downloadUpdate() {
        val url = apkUrl ?: return
        val expectedHash = expectedSha256 ?: return
        val versionCode = latestVersionCode ?: return
        if (downloading || (downloadedApk?.isFile == true && cachedApkVersionCode == versionCode)) return
        downloading = true
        progress = 0f
        message = "Downloading update…"
        val target = File(context.cacheDir, "location-dots-update-${versionCode}.apk")
        val partial = File(context.cacheDir, "location-dots-update-${versionCode}.part")
        partial.delete()
        downloadJob = scope.launch {
            try {
                val apkFile = withContext(Dispatchers.IO) {
                    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 15000
                        instanceFollowRedirects = true
                    }
                    downloadConnection = connection
                    try {
                        connection.connect()
                        if (connection.responseCode !in 200..299) throw IllegalStateException("Download server returned an error")
                        val total = connection.contentLengthLong
                        connection.inputStream.use { input ->
                            partial.outputStream().buffered().use { output ->
                                val buffer = ByteArray(32 * 1024)
                                var received = 0L
                                while (true) {
                                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    output.write(buffer, 0, count)
                                    received += count
                                    if (total > 0L) {
                                        val value = (received.toFloat() / total).coerceIn(0f, 1f)
                                        withContext(Dispatchers.Main) { progress = value }
                                    }
                                }
                            }
                        }
                    } finally {
                        connection.disconnect()
                        downloadConnection = null
                    }
                    if (!sha256(partial).equals(expectedHash, true)) throw IllegalStateException("checksum mismatch")
                    if (target.exists() && !target.delete()) throw IllegalStateException("Could not replace previous APK")
                    if (!partial.renameTo(target)) throw IllegalStateException("Could not save the verified APK")
                    target
                }
                downloadedApk = apkFile
                cachedApkVersionCode = versionCode
                progress = 1f
                message = "Download verified. Ready to install ${latestVersion ?: "update"}."
                showInstallDialog = true
                postInstallNotification(apkFile, latestVersion ?: "update", context)
            } catch (error: CancellationException) {
                partial.delete()
                message = "Download cancelled."
            } catch (error: Exception) {
                partial.delete()
                message = if (error.message?.contains("checksum", true) == true) "The downloaded APK failed verification and was discarded."
                    else "Could not download the update. Please try again."
            } finally {
                downloading = false
                downloadJob = null
                downloadConnection = null
            }
        }
    }
    LaunchedEffect(Unit) { checkForUpdates() }
    BackHandler(onBack = onBack)

    LaunchedEffect(downloadedApk, cachedApkVersionCode, latestVersionCode) {
        if (downloadedApk?.isFile == true && cachedApkVersionCode == latestVersionCode) {
            showInstallDialog = true
        }
    }

    if (showInstallDialog && downloadedApk?.isFile == true && cachedApkVersionCode == latestVersionCode) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            icon = { Icon(Icons.Default.SystemUpdate, null) },
            title = { Text("Update ready to install") },
            text = { Text("Location Dots ${latestVersion ?: ""} has been downloaded and verified. Install it now?") },
            confirmButton = {
                Button(onClick = {
                    showInstallDialog = false
                    downloadedApk?.let(::openInstaller)
                }) { Text("Install") }
            },
            dismissButton = { TextButton(onClick = { showInstallDialog = false }) { Text("Later") } }
        )
    }

    if (showInstallPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showInstallPermissionDialog = false },
            icon = { Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Allow app installation") },
            text = {
                Text(
                    "Android requires Location Dots to be allowed to install apps from this source. " +
                        "This permission is only used to open the verified update you downloaded. " +
                        "On the next screen, enable “Allow from this source”, then return here and tap Install again."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showInstallPermissionDialog = false
                    val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(settingsIntent) }
                        .onFailure { message = "Could not open Android's app-install permission settings." }
                }) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showInstallPermissionDialog = false }) { Text("Cancel") }
            }
        )
    }

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
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Downloading APK", style = MaterialTheme.typography.labelLarge)
                                    Spacer(Modifier.weight(1f))
                                    Text(if (progress > 0f) "${(progress * 100).toInt()}%" else "Connecting…",
                                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            }
                            IconButton(onClick = ::cancelDownload, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel download", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else if (downloadedApk?.isFile == true && cachedApkVersionCode == latestVersionCode) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = ::discardDownloadedApk, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large) {
                                Icon(Icons.Default.DeleteOutline, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Discard")
                            }
                            Button(onClick = { openInstaller(downloadedApk!!) }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large) {
                                Icon(Icons.Default.InstallMobile, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Install")
                            }
                        }
                    } else if (updateAvailable) {
                        Button(onClick = ::downloadUpdate, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Download ${latestVersion ?: "update"}")
                        }
                    } else {
                        OutlinedButton(onClick = ::checkForUpdates, enabled = !checking && !downloading, modifier = Modifier.fillMaxWidth()) {
                            if (checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (checking) "Checking…" else "Check for updates")
                        }
                    }
                }
            }
        }
        item {
            ExpressiveCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Version details", style = MaterialTheme.typography.titleMedium)
                    val installedInfo = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull() }
                    val installedCode = installedInfo?.let {
                        @Suppress("DEPRECATION")
                        if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong()
                    } ?: -1L
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("INSTALLED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(installedVersion(context), style = MaterialTheme.typography.titleMedium)
                            Text("Code $installedCode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("LATEST", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(latestVersion ?: "Not checked", style = MaterialTheme.typography.titleMedium)
                            Text("Code ${latestVersionCode ?: "—"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountTree, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Update channel", style = MaterialTheme.typography.bodyMedium)
                            Text(if (updateChannel == "debug") "Development" else "Stable", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (latestCommit != null && updateChannel == "debug") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Commit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Source commit", style = MaterialTheme.typography.bodyMedium)
                                Text(latestCommit!!.take(10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(32 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
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


private fun postInstallNotification(file: File, version: String, context: Context) {
    runCatching {
        val channelId = "app_updates"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "App updates", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Installation prompt when an update has finished downloading"
                }
            )
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 7401, installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Location Dots update ready")
            .setContentText("Version $version is downloaded and verified. Tap to install.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Version $version is downloaded and verified. Tap to open Android's installer."))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(7401, notification)
    }
}

package com.locationdots.app.logging

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object CrashLogger {
    private const val DIRECTORY_NAME = "Location Dots/Crash Logs"
    private const val RELATIVE_PATH = "Download/Location Dots/Crash Logs/"
    private const val FILE_PREFIX = "location-dots-crash-"
    private const val FILE_SUFFIX = ".txt"

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return

        synchronized(this) {
            if (installed) return

            val appContext = context.applicationContext
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    saveCrash(appContext, thread, throwable)
                } catch (_: Throwable) {
                    // Never prevent the original crash from being handled.
                } finally {
                    previousHandler?.uncaughtException(thread, throwable)
                }
            }

            installed = true
        }
    }

    private fun saveCrash(context: Context, thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat(
            "yyyy-MM-dd_HH-mm-ss_SSS",
            Locale.US
        ).format(Date())
        val uniqueSuffix = UUID.randomUUID().toString().replace("-", "").take(12)
        val fileName = "$FILE_PREFIX$timestamp-$uniqueSuffix$FILE_SUFFIX"

        val stackTrace = StringWriter().also {
            throwable.printStackTrace(PrintWriter(it))
        }.toString()

        val report = buildString {
            appendLine("Location Dots crash report")
            appendLine("================================")
            appendLine("Timestamp: $timestamp")
            appendLine("Crash file: $fileName")
            appendLine("Thread: ${thread.name} (${thread.id})")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Package: ${context.packageName}")
            appendLine()
            appendLine("Exception:")
            appendLine(stackTrace)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToDownloadsWithMediaStore(context, fileName, report)
        } else {
            saveToLegacyDownloads(context, fileName, report)
        }
    }

    private fun saveToDownloadsWithMediaStore(
        context: Context,
        fileName: String,
        report: String
    ) {
        val resolver = context.contentResolver
        val collection: Uri =
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        // The Downloads provider owns this folder. Supplying RELATIVE_PATH creates
        // the Location Dots/Crash Logs folder when it does not already exist.
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(collection, values) ?: return

        try {
            resolver.openOutputStream(uri)?.use { output ->
                output.write(report.toByteArray(Charsets.UTF_8))
                output.flush()
            } ?: throw IllegalStateException("Unable to open crash log output stream")

            val completedValues = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(uri, completedValues, null, null)
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private fun saveToLegacyDownloads(
        context: Context,
        fileName: String,
        report: String
    ) {
        val downloads = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val directory = File(downloads, DIRECTORY_NAME)

        // Explicitly check the folder every time a crash is written.
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("Unable to create crash log directory: $directory")
        }

        File(directory, fileName).writeText(report, Charsets.UTF_8)
    }
}

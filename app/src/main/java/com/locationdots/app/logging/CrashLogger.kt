package com.locationdots.app.logging

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val DIRECTORY_NAME = "crash-logs"
    private const val FILE_PREFIX = "crash-"
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
        val directory = File(context.filesDir, DIRECTORY_NAME)
        if (!directory.exists() && !directory.mkdirs()) return

        val timestamp = SimpleDateFormat(
            "yyyy-MM-dd_HH-mm-ss_SSS",
            Locale.US
        ).format(Date())

        val crashFile = File(directory, "$FILE_PREFIX$timestamp$FILE_SUFFIX")
        val stackTrace = StringWriter().also {
            throwable.printStackTrace(PrintWriter(it))
        }.toString()

        val report = buildString {
            appendLine("Location Dots crash report")
            appendLine("================================")
            appendLine("Timestamp: $timestamp")
            appendLine("Thread: ${thread.name} (${thread.id})")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Package: ${context.packageName}")
            appendLine()
            appendLine("Exception:")
            appendLine(stackTrace)
        }

        crashFile.writeText(report, Charsets.UTF_8)
    }
}

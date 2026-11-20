package com.isg32.carino.platform

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrashReport(
    val title: String,
    val body: String,
    val file: File? = null,
)

/**
 * Uncaught exception persistence and export to Downloads.
 */
object CrashFiles {

    @Volatile
    private var installed = false

    fun crashDir(context: Context): File =
        File(context.applicationContext.filesDir, "crashes").apply { mkdirs() }

    fun installCrashHandler(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val dir = crashDir(app)
                val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }
                File(dir, "native-$stamp.txt").writeText(
                    "Thread: ${thread.name}\n" +
                        "Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n\n" +
                        trace,
                )
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun consumeLatestCrash(context: Context): CrashReport? {
        val dir = crashDir(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?: return null
        val latest = files.firstOrNull() ?: return null
        val text = try {
            latest.readText()
        } catch (_: Exception) {
            return null
        }
        files.forEach { runCatching { it.delete() } }
        return CrashReport(
            title = "Carbon stopped unexpectedly last time",
            body = text,
            file = latest,
        )
    }

    fun buildManualReport(error: Throwable, contextNote: String = "Test report"): CrashReport {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }
        val body = buildString {
            appendLine("Context: $contextNote")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine()
            append(trace.toString())
        }
        return CrashReport(title = "Crash report", body = body)
    }

    fun saveToDownloads(context: Context, name: String, text: String): String {
        val bytes = text.toByteArray()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Downloads is not available")
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: error("Could not write to Downloads")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            File(dir, name).writeBytes(bytes)
        }
        return "Downloads/$name"
    }
}

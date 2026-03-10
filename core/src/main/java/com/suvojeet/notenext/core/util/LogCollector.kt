package com.suvojeet.notenext.core.util

import android.content.Context
import android.os.Build
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

object LogCollector {
    private const val LOG_FILE_NAME = "persistent_logs.txt"

    private fun getLogFile(context: Context): File {
        return File(context.cacheDir, LOG_FILE_NAME)
    }

    fun startLogging(context: Context) {
        // Clear previous logs when starting a new session
        val file = getLogFile(context)
        if (file.exists()) file.delete()
        file.createNewFile()
    }

    fun appendLogs(context: Context) {
        val file = getLogFile(context)
        try {
            // Get current logcat output (incremental would be better but logcat -d is simple)
            // For continuous, we'd ideally use a process that stays open, 
            // but here we can just dump the current buffer to the file periodically.
            val process = Runtime.getRuntime().exec("logcat -d")
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            
            val writer = PrintWriter(FileOutputStream(file, true))
            var line: String? = bufferedReader.readLine()
            while (line != null) {
                writer.println(line)
                line = bufferedReader.readLine()
            }
            writer.flush()
            writer.close()
            
            // Clear logcat buffer after dumping to avoid duplicates in next append
            Runtime.getRuntime().exec("logcat -c")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSavedLogs(context: Context): String {
        val file = getLogFile(context)
        if (!file.exists()) return "No logs recorded."
        return try {
            file.readText()
        } catch (e: Exception) {
            "Failed to read logs: ${e.message}"
        }
    }
fun deleteLogs(context: Context) {
    val file = getLogFile(context)
    if (file.exists()) file.delete()
}

fun collectDeviceInfo(context: Context): String {
    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: Exception) {
        null
    }

    return """
        --- Device Info ---
        App Version: ${packageInfo?.versionName} (${packageInfo?.let { 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else it.versionCode.toLong() 
        }})
        Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
        Device: ${Build.MANUFACTURER} ${Build.MODEL}
        Product: ${Build.PRODUCT}
        Board: ${Build.BOARD}
        Hardware: ${Build.HARDWARE}
        Display: ${Build.DISPLAY}
        Fingerprint: ${Build.FINGERPRINT}
        Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
        -------------------
    """.trimIndent()
}
}


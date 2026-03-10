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
            // Get last few hundred lines of logcat. 
            // Since we're calling this every 5s, 100 lines should be plenty to avoid gaps.
            val process = Runtime.getRuntime().exec("logcat -d -t 100")
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            
            val currentLogs = bufferedReader.readLines()
            
            // Read existing file to check for duplicates (basic check)
            val existingLines = if (file.exists()) file.readLines().takeLast(100) else emptyList()
            
            val writer = PrintWriter(FileOutputStream(file, true))
            currentLogs.forEach { line ->
                if (!existingLines.contains(line)) {
                    writer.println(line)
                }
            }
            writer.flush()
            writer.close()
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


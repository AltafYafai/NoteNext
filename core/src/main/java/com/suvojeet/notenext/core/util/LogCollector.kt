package com.suvojeet.notenext.core.util

import android.content.Context
import android.os.Build
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogCollector {

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

    fun collectLogs(maxLines: Int = 500): String {
        val logBuilder = StringBuilder()
        try {
            val process = Runtime.getRuntime().exec("logcat -d")
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            
            val logs = mutableListOf<String>()
            var line: String? = bufferedReader.readLine()
            while (line != null) {
                logs.add(line)
                line = bufferedReader.readLine()
            }
            
            // Get the last maxLines
            val start = if (logs.size > maxLines) logs.size - maxLines else 0
            for (i in start until logs.size) {
                logBuilder.append(logs[i]).append("\n")
            }
            
        } catch (e: Exception) {
            logBuilder.append("Failed to collect logs: ${e.message}")
        }
        return logBuilder.toString()
    }
}

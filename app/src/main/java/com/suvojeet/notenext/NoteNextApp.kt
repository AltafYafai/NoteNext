package com.suvojeet.notenext

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.suvojeet.notenext.worker.AutoDeleteWorker
import dagger.hilt.android.HiltAndroidApp
import org.acra.config.httpSender
import org.acra.config.toast
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class NoteNextApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            
            // Logcat Configuration
            logcatArguments = listOf("-t", "200", "-v", "time")
            
            toast {
                text = getString(R.string.crash_toast_text)
            }
            
            httpSender {
                uri = "https://collector.tracepot.com/00000000" // Placeholder URL
                httpMethod = HttpSender.Method.POST
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        setupAutoDeleteWorker()
    }

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "logging_service_channel",
                "Logging Service Channel",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for continuous log collection for bug reproduction"
            }
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupAutoDeleteWorker() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<AutoDeleteWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AutoDeleteBinNotes",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }
}

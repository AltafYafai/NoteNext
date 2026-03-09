package com.suvojeet.notenext

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.suvojeet.notenext.worker.AutoDeleteWorker
import dagger.hilt.android.HiltAndroidApp
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraHttpSender
import org.acra.annotation.AcraToast
import org.acra.data.StringFormat
import org.acra.sender.HttpSender
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AcraCore(
    buildConfigClass = BuildConfig::class,
    reportFormat = StringFormat.JSON
)
@AcraHttpSender(
    uri = "https://collector.tracepot.com/00000000", // Placeholder URL
    httpMethod = HttpSender.Method.POST
)
@AcraToast(
    resText = R.string.crash_toast_text
)
@HiltAndroidApp
class NoteNextApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun attachBaseContext(base: android.content.Context?) {
        super.attachBaseContext(base)
        ACRA.init(this)
    }

    override fun onCreate() {
        super.onCreate()
        setupAutoDeleteWorker()
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

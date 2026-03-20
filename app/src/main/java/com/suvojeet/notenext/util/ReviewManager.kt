package com.suvojeet.notenext.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.google.android.play.core.review.ReviewManagerFactory
import java.util.concurrent.TimeUnit

class ReviewManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_review_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FIRST_OPEN_TIME = "first_open_time"
        private const val KEY_APP_OPENS_COUNT = "app_opens_count"
        private const val KEY_REVIEW_REQUESTED = "review_requested"

        private val DAYS_UNTIL_PROMPT = 3L
        private const val LAUNCHES_UNTIL_PROMPT = 5
    }

    fun checkAndRequestReview(activity: Activity) {
        val isReviewRequested = prefs.getBoolean(KEY_REVIEW_REQUESTED, false)
        if (isReviewRequested) {
            return
        }

        val firstOpenTime = prefs.getLong(KEY_FIRST_OPEN_TIME, 0L)
        if (firstOpenTime == 0L) {
            prefs.edit().putLong(KEY_FIRST_OPEN_TIME, System.currentTimeMillis()).apply()
        }

        var appOpensCount = prefs.getInt(KEY_APP_OPENS_COUNT, 0)
        appOpensCount++
        prefs.edit().putInt(KEY_APP_OPENS_COUNT, appOpensCount).apply()

        val timeSinceFirstOpen = System.currentTimeMillis() - firstOpenTime
        val daysSinceFirstOpen = TimeUnit.MILLISECONDS.toDays(timeSinceFirstOpen)

        if (daysSinceFirstOpen >= DAYS_UNTIL_PROMPT || appOpensCount >= LAUNCHES_UNTIL_PROMPT) {
            requestReviewFlow(activity)
        }
    }

    private fun requestReviewFlow(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We got the ReviewInfo object
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown.
                    prefs.edit().putBoolean(KEY_REVIEW_REQUESTED, true).apply()
                }
            } else {
                // There was some problem, log or handle the error.
            }
        }
    }
}

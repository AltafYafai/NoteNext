package com.suvojeet.notenext.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable

/**
 * Use `MaterialTheme.motionScheme` directly in composables.
 * This object exists only as a migration bridge for duration/easing constants.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object Motion {
    // Standard motion durations
    val durationShort = 200
    val durationMedium = 400
    val durationLong = 600

    // Custom easing for edge cases not covered by MotionScheme
    val emphasizedEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val standardEasing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}

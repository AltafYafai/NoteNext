package com.suvojeet.notenext.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object Motion {
    // Expressive springs for different types of UI interactions
    
    @Composable
    fun fastSpec(): MotionScheme = MaterialTheme.motionScheme

    @Composable
    fun defaultSpec(): MotionScheme = MaterialTheme.motionScheme

    @Composable
    fun slowSpec(): MotionScheme = MaterialTheme.motionScheme

    // Snappy spring for quick feedback (buttons, toggles)
    @Deprecated(
        message = "Use MaterialTheme.motionScheme.fastSpatialSpec() instead",
        replaceWith = ReplaceWith("MaterialTheme.motionScheme.fastSpatialSpec()", "androidx.compose.material3.MaterialTheme")
    )
    fun <T> snappy() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // Emphasis spring for hero moments and significant transitions
    @Deprecated(
        message = "Use MaterialTheme.motionScheme.slowSpatialSpec() instead",
        replaceWith = ReplaceWith("MaterialTheme.motionScheme.slowSpatialSpec()", "androidx.compose.material3.MaterialTheme")
    )
    fun <T> emphasis() = spring<T>(
        dampingRatio = 0.6f, // Slightly bouncy
        stiffness = 300f
    )

    // Smooth spring for subtle layout changes
    @Deprecated(
        message = "Use MaterialTheme.motionScheme.defaultSpatialSpec() instead",
        replaceWith = ReplaceWith("MaterialTheme.motionScheme.defaultSpatialSpec()", "androidx.compose.material3.MaterialTheme")
    )
    fun <T> smooth() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Standard motion durations
    val durationShort = 200
    val durationMedium = 400
    val durationLong = 600

    // Custom easing for Material 3 Expressive motion
    val emphasizedEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val standardEasing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}

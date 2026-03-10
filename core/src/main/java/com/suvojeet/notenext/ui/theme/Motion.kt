package com.suvojeet.notenext.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

object Motion {
    // Expressive springs for different types of UI interactions
    
    // Snappy spring for quick feedback (buttons, toggles)
    fun <T> snappy() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // Emphasis spring for hero moments and significant transitions
    fun <T> emphasis() = spring<T>(
        dampingRatio = 0.6f, // Slightly bouncy
        stiffness = 300f
    )

    // Smooth spring for subtle layout changes
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

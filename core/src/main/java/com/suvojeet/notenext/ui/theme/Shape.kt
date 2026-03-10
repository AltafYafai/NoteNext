package com.suvojeet.notenext.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Path
import kotlin.math.min

// Default Expressive Material 3 Shapes for the entire app
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp),
    small = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 8.dp, bottomStart = 8.dp),
    medium = RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp, topEnd = 12.dp, bottomStart = 12.dp),
    large = RoundedCornerShape(topStart = 36.dp, bottomEnd = 36.dp, topEnd = 16.dp, bottomStart = 16.dp),
    extraLarge = RoundedCornerShape(topStart = 48.dp, bottomEnd = 48.dp, topEnd = 24.dp, bottomStart = 24.dp)
)

// Hero shapes for special emphasis (like avatars or featured content)
object HeroShapes {
    val Squircle = GenericShape { size, _ ->
        val width = size.width
        val height = size.height
        val radius = min(width, height) / 2
        
        moveTo(0f, radius)
        cubicTo(0f, 0f, 0f, 0f, radius, 0f)
        lineTo(width - radius, 0f)
        cubicTo(width, 0f, width, 0f, width, radius)
        lineTo(width, height - radius)
        cubicTo(width, height, width, height, width - radius, height)
        lineTo(radius, height)
        cubicTo(0f, height, 0f, height, 0f, height - radius)
        close()
    }

    val Leaf = RoundedCornerShape(topStart = 0.dp, bottomEnd = 0.dp, topEnd = 50.dp, bottomStart = 50.dp)
}

package com.suvojeet.notenext.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.core.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val RobotoFlexFont = GoogleFont("Roboto Flex")

private val RobotoFlexFontFamily = FontFamily(
    Font(googleFont = RobotoFlexFont, fontProvider = provider),
    Font(googleFont = RobotoFlexFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = RobotoFlexFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = RobotoFlexFont, fontProvider = provider, weight = FontWeight.Bold)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// Emphasized styles
val displayLargeEmphasized = AppTypography.displayLarge.copy(fontWeight = FontWeight.Bold)
val displayMediumEmphasized = AppTypography.displayMedium.copy(fontWeight = FontWeight.Bold)
val displaySmallEmphasized = AppTypography.displaySmall.copy(fontWeight = FontWeight.Bold)
val headlineLargeEmphasized = AppTypography.headlineLarge.copy(fontWeight = FontWeight.Bold)
val headlineMediumEmphasized = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Bold)
val headlineSmallEmphasized = AppTypography.headlineSmall.copy(fontWeight = FontWeight.Bold)
val titleLargeEmphasized = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold)
val titleMediumEmphasized = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
val titleSmallEmphasized = AppTypography.titleSmall.copy(fontWeight = FontWeight.Bold)
val bodyLargeEmphasized = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold)
val bodyMediumEmphasized = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Bold)
val bodySmallEmphasized = AppTypography.bodySmall.copy(fontWeight = FontWeight.Bold)
val labelLargeEmphasized = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold)
val labelMediumEmphasized = AppTypography.labelMedium.copy(fontWeight = FontWeight.Bold)
val labelSmallEmphasized = AppTypography.labelSmall.copy(fontWeight = FontWeight.Bold)

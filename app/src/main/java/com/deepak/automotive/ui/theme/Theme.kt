package com.deepak.automotive.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Car UIs are dark by default (less glare at night) and use LARGE touch targets and text:
 * AAOS guidelines ask for >= 76dp touch targets and >= 24sp primary text.
 */
object CarColors {
    val Background = Color(0xFF05080F)
    val Surface = Color(0xFF0E1524)
    val SurfaceHigh = Color(0xFF172238)
    val Cyan = Color(0xFF22D3EE)
    val Green = Color(0xFF34D399)
    val Amber = Color(0xFFFBBF24)
    val Red = Color(0xFFF87171)
    val Violet = Color(0xFFA78BFA)
    val Blue = Color(0xFF60A5FA)
    val Pink = Color(0xFFF472B6)
    val Orange = Color(0xFFFB923C)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
}

private val scheme = darkColorScheme(
    primary = CarColors.Cyan,
    secondary = CarColors.Green,
    tertiary = CarColors.Amber,
    error = CarColors.Red,
    background = CarColors.Background,
    surface = CarColors.Surface,
    surfaceVariant = CarColors.SurfaceHigh,
    onPrimary = Color.Black,
    onBackground = CarColors.TextPrimary,
    onSurface = CarColors.TextPrimary,
    onSurfaceVariant = CarColors.TextSecondary,
)

private val typography = Typography(
    headlineMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun AutomotiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}

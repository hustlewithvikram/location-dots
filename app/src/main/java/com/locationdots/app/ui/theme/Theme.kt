package com.locationdots.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7C4E8), onPrimary = Color(0xFF17213A), primaryContainer = Color(0xFF2E3D63), onPrimaryContainer = Color(0xFFDCE4FF),
    secondary = Color(0xFFCBC3F8), onSecondary = Color(0xFF302C4C), secondaryContainer = Color(0xFF45405F), onSecondaryContainer = Color(0xFFE8E0FF),
    tertiary = Color(0xFFF0B7D6), onTertiary = Color(0xFF49253B), tertiaryContainer = Color(0xFF633D54), onTertiaryContainer = Color(0xFFFFD9EC),
    background = Color(0xFF0D0F14), onBackground = Color(0xFFE4E2E9), surface = Color(0xFF0D0F14), onSurface = Color(0xFFE4E2E9),
    surfaceContainerLowest = Color(0xFF08090D), surfaceContainerLow = Color(0xFF14161C), surfaceContainer = Color(0xFF191B22), surfaceContainerHigh = Color(0xFF23252D), surfaceContainerHighest = Color(0xFF2D2F38),
    onSurfaceVariant = Color(0xFFC4C4CF), outline = Color(0xFF8E8E99), outlineVariant = Color(0xFF45464F)
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF5A6F9F), onPrimary = Color.White, primaryContainer = Color(0xFFE2E7F3), onPrimaryContainer = Color(0xFF18233C),
    secondary = Color(0xFF625A7D), onSecondary = Color.White, secondaryContainer = Color(0xFFE8E0FF), onSecondaryContainer = Color(0xFF1D1835),
    tertiary = Color(0xFF87506E), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFD9EC), onTertiaryContainer = Color(0xFF351027),
    background = Color(0xFFFAF8FF), onBackground = Color(0xFF1A1B20), surface = Color(0xFFFAF8FF), onSurface = Color(0xFF1A1B20),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF3F1F7), surfaceContainer = Color(0xFFEDEBF1), surfaceContainerHigh = Color(0xFFE7E5EB), surfaceContainerHighest = Color(0xFFE1DFE6),
    onSurfaceVariant = Color(0xFF46464F), outline = Color(0xFF777780), outlineVariant = Color(0xFFC7C5CC)
)
@Composable
fun LocationDotsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColors: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit
) {
    val colors = when {
        dynamicColors && darkTheme -> dynamicDarkColorScheme(androidx.compose.ui.platform.LocalContext.current)
        dynamicColors && !darkTheme -> dynamicLightColorScheme(androidx.compose.ui.platform.LocalContext.current)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colors, typography = Typography(), shapes = Shapes(extraLarge = RoundedCornerShape(32.dp), large = RoundedCornerShape(28.dp), medium = RoundedCornerShape(20.dp), small = RoundedCornerShape(16.dp), extraSmall = RoundedCornerShape(12.dp)), content = content)
}

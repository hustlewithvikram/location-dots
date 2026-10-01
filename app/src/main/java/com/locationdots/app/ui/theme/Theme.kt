package com.locationdots.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8C7FF), onPrimary = Color(0xFF10265C), primaryContainer = Color(0xFF263C78), onPrimaryContainer = Color(0xFFDCE2FF),
    secondary = Color(0xFFCBC3F8), onSecondary = Color(0xFF302C4C), secondaryContainer = Color(0xFF45405F), onSecondaryContainer = Color(0xFFE8E0FF),
    tertiary = Color(0xFFF0B7D6), onTertiary = Color(0xFF49253B), tertiaryContainer = Color(0xFF633D54), onTertiaryContainer = Color(0xFFFFD9EC),
    background = Color(0xFF0D0F14), onBackground = Color(0xFFE4E2E9), surface = Color(0xFF0D0F14), onSurface = Color(0xFFE4E2E9),
    surfaceContainerLowest = Color(0xFF08090D), surfaceContainerLow = Color(0xFF14161C), surfaceContainer = Color(0xFF191B22), surfaceContainerHigh = Color(0xFF23252D), surfaceContainerHighest = Color(0xFF2D2F38),
    onSurfaceVariant = Color(0xFFC4C4CF), outline = Color(0xFF8E8E99), outlineVariant = Color(0xFF45464F)
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF465D9C), onPrimary = Color.White, primaryContainer = Color(0xFFDCE2FF), onPrimaryContainer = Color(0xFF00164D),
    secondary = Color(0xFF625A7D), onSecondary = Color.White, secondaryContainer = Color(0xFFE8E0FF), onSecondaryContainer = Color(0xFF1D1835),
    tertiary = Color(0xFF87506E), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFD9EC), onTertiaryContainer = Color(0xFF351027),
    background = Color(0xFFFAF8FF), onBackground = Color(0xFF1A1B20), surface = Color(0xFFFAF8FF), onSurface = Color(0xFF1A1B20),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF3F1F7), surfaceContainer = Color(0xFFEDEBF1), surfaceContainerHigh = Color(0xFFE7E5EB), surfaceContainerHighest = Color(0xFFE1DFE6),
    onSurfaceVariant = Color(0xFF46464F), outline = Color(0xFF777780), outlineVariant = Color(0xFFC7C5CC)
)
@Composable
fun LocationDotsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography(), content = content)
}

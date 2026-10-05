package com.locationdots.app.ui.theme

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Location Dots brand seed: #0F52BA
// The palette below is the fixed fallback; Android 12+ can replace it with
// wallpaper-derived Material 3 dynamic colors through LocationDotsTheme.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFB3C5FF),
    onPrimary = Color(0xFF002A6B),
    primaryContainer = Color(0xFF0F3B82),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFB9C6E9),
    onSecondary = Color(0xFF24314A),
    secondaryContainer = Color(0xFF3A4965),
    onSecondaryContainer = Color(0xFFD9E2FF),
    tertiary = Color(0xFFC2C4E0),
    onTertiary = Color(0xFF2B2E43),
    tertiaryContainer = Color(0xFF42455B),
    onTertiaryContainer = Color(0xFFDEE0FC),
    background = Color(0xFF0C111A),
    onBackground = Color(0xFFE1E6F0),
    surface = Color(0xFF0C111A),
    onSurface = Color(0xFFE1E6F0),
    surfaceContainerLowest = Color(0xFF080D15),
    surfaceContainerLow = Color(0xFF111A29),
    surfaceContainer = Color(0xFF162238),
    surfaceContainerHigh = Color(0xFF1C2A44),
    surfaceContainerHighest = Color(0xFF263754),
    onSurfaceVariant = Color(0xFFC2C9D8),
    outline = Color(0xFF8C94A6),
    outlineVariant = Color(0xFF414B5F)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F52BA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E5FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF4E6290),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE5FF),
    onSecondaryContainer = Color(0xFF0B1A36),
    tertiary = Color(0xFF5D5F7D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE1E1FA),
    onTertiaryContainer = Color(0xFF191A35),
    background = Color(0xFFF8FAFF),
    onBackground = Color(0xFF171A21),
    surface = Color(0xFFF8FAFF),
    onSurface = Color(0xFF171A21),
    // Soft blue-tinted surfaces replace the previous neutral white/grey cards.
    surfaceContainerLowest = Color(0xFFFCFDFF),
    surfaceContainerLow = Color(0xFFF1F5FF),
    surfaceContainer = Color(0xFFECF2FC),
    surfaceContainerHigh = Color(0xFFE6EDF9),
    surfaceContainerHighest = Color(0xFFDDE6F5),
    onSurfaceVariant = Color(0xFF46505F),
    outline = Color(0xFF747D8E),
    outlineVariant = Color(0xFFC4CBD7)
)

@SuppressLint("NewApi")
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

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        shapes = Shapes(
            extraLarge = RoundedCornerShape(32.dp),
            large = RoundedCornerShape(28.dp),
            medium = RoundedCornerShape(20.dp),
            small = RoundedCornerShape(16.dp),
            extraSmall = RoundedCornerShape(12.dp)
        ),
        content = content
    )
}

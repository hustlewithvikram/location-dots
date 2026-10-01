package com.locationdots.app.feature.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onGetStarted: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "splashGlow")
    val glowShift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowShift"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(90)
        contentVisible = true
    }

    val background = MaterialTheme.colorScheme.background
    val blue = MaterialTheme.colorScheme.primary
    val purple = MaterialTheme.colorScheme.secondary
    val pink = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        // The expressive color field deliberately starts in the lower half,
        // leaving the upper area calm and typography-first.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to background,
                            0.40f to background,
                            0.54f to background.copy(alpha = 0.98f),
                            0.70f to blue.copy(alpha = 0.26f),
                            0.84f to purple.copy(alpha = 0.48f),
                            1.00f to pink.copy(alpha = 0.72f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = (glowShift - 0.5f) * 90f
                    translationY = (0.5f - glowShift) * 55f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            blue.copy(alpha = 0.30f * glowAlpha),
                            purple.copy(alpha = 0.20f * glowAlpha),
                            Color.Transparent
                        ),
                        radius = 720f
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = (0.5f - glowShift) * 110f
                    translationY = glowShift * 80f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            pink.copy(alpha = 0.34f * glowAlpha),
                            purple.copy(alpha = 0.18f * glowAlpha),
                            Color.Transparent
                        ),
                        radius = 620f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 28.dp,
                    top = 34.dp + WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding(),
                    end = 28.dp,
                    bottom = 22.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(650)) + slideInVertically(
                    animationSpec = tween(700, easing = FastOutSlowInEasing),
                    initialOffsetY = { -24 }
                )
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "Location",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            lineHeight = MaterialTheme.typography.displayLarge.lineHeight * 0.88f
                        )
                    )
                    Text(
                        "Dots",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            lineHeight = MaterialTheme.typography.displayLarge.lineHeight * 0.88f,
                            brush = Brush.linearGradient(
                                listOf(blue, purple, pink)
                            )
                        )
                    )
                }
            }

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(800, delayMillis = 220)) + slideInVertically(
                    animationSpec = tween(800, delayMillis = 220, easing = FastOutSlowInEasing),
                    initialOffsetY = { 42 }
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Your journey, remembered.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alpha(0.9f)
                    )

                    Button(
                        onClick = onGetStarted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text(
                            "Get Started",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

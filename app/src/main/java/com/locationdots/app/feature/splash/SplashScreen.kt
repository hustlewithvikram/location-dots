package com.locationdots.app.feature.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locationdots.app.ui.theme.LocationDotsTheme

@Composable
fun SplashScreen(
    onGetStarted: () -> Unit
) {
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    val scheme = MaterialTheme.colorScheme

    val illustrationTransition = rememberInfiniteTransition(
        label = "splashIllustration"
    )

    val markerScale by illustrationTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "markerScale"
    )

    val routeProgress by illustrationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "routeProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 20.dp +
                    WindowInsets.safeDrawing
                        .asPaddingValues()
                        .calculateTopPadding(),
                bottom = 18.dp +
                    WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(550)) + slideInVertically(
                animationSpec = tween(
                    650,
                    easing = FastOutSlowInEasing
                ),
                initialOffsetY = { -24 }
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = MaterialTheme.typography.displayMedium.lineHeight * 0.9f
                    ),
                    color = scheme.onBackground
                )

                Text(
                    text = "Dots",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = MaterialTheme.typography.displayMedium.lineHeight * 0.9f
                    ),
                    color = scheme.primary
                )

                Text(
                    text = "Keep track of the places that matter.",
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(
                    tween(
                        800,
                        delayMillis = 120
                    )
                ) + scaleIn(
                    animationSpec = tween(
                        850,
                        delayMillis = 120,
                        easing = FastOutSlowInEasing
                    ),
                    initialScale = 0.9f
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(360.dp)
                    ) {
                        drawMapBackdrop(
                            primary = scheme.primary,
                            secondary = scheme.secondary,
                            tertiary = scheme.tertiary
                        )

                        drawRoute(
                            primary = scheme.primary,
                            progress = routeProgress
                        )

                        drawSmallTree(
                            center = Offset(
                                size.width * 0.16f,
                                size.height * 0.47f
                            ),
                            color = scheme.tertiary.copy(alpha = 0.45f)
                        )

                        drawSmallTree(
                            center = Offset(
                                size.width * 0.76f,
                                size.height * 0.39f
                            ),
                            color = scheme.tertiary.copy(alpha = 0.38f)
                        )

                        drawSmallTree(
                            center = Offset(
                                size.width * 0.34f,
                                size.height * 0.76f
                            ),
                            color = scheme.secondary.copy(alpha = 0.25f)
                        )
                    }

                    DestinationBubble(
                        icon = Icons.Default.Terrain,
                        label = "Mountains",
                        containerColor = scheme.surface,
                        iconColor = scheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = 12.dp,
                                top = 38.dp
                            )
                    )

                    DestinationBubble(
                        icon = Icons.Default.LocationCity,
                        label = "Cities",
                        containerColor = scheme.surface,
                        iconColor = scheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                end = 6.dp,
                                top = 26.dp
                            )
                    )

                    DestinationBubble(
                        icon = Icons.Default.BeachAccess,
                        label = "Beaches",
                        containerColor = scheme.surface,
                        iconColor = scheme.tertiary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = 18.dp,
                                bottom = 42.dp
                            )
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(92.dp)
                            .scale(markerScale),
                        shape = CircleShape,
                        color = scheme.primary.copy(alpha = 0.10f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(62.dp),
                                shape = CircleShape,
                                color = scheme.primary
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = scheme.onPrimary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(
                tween(
                    700,
                    delayMillis = 300
                )
            ) + slideInVertically(
                animationSpec = tween(
                    750,
                    delayMillis = 300,
                    easing = FastOutSlowInEasing
                ),
                initialOffsetY = { 34 }
            )
        ) {
            ExpressiveGetStartedButton(
                onClick = onGetStarted
            )
        }
    }
}

@Composable
private fun DestinationBubble(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        shadowElevation = 5.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 11.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ExpressiveGetStartedButton(
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val transition = rememberInfiniteTransition(
        label = "getStartedButton"
    )

    val arrowOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1100,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowOffset"
    )

    val buttonScale = if (pressed) 0.97f else 1f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(buttonScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(28.dp),
        color = scheme.primary,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 22.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Get Started",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = scheme.onPrimary,
                textAlign = TextAlign.Center
            )

            Surface(
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        translationX = arrowOffset
                    },
                shape = CircleShape,
                color = scheme.onPrimary.copy(alpha = 0.18f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Get started",
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawMapBackdrop(
    primary: Color,
    secondary: Color,
    tertiary: Color
) {
    drawCircle(
        color = primary.copy(alpha = 0.055f),
        radius = size.minDimension * 0.42f,
        center = Offset(
            size.width * 0.52f,
            size.height * 0.52f
        )
    )

    drawOval(
        color = secondary.copy(alpha = 0.055f),
        topLeft = Offset(
            size.width * 0.05f,
            size.height * 0.28f
        ),
        size = Size(
            size.width * 0.36f,
            size.height * 0.22f
        )
    )

    drawOval(
        color = tertiary.copy(alpha = 0.06f),
        topLeft = Offset(
            size.width * 0.58f,
            size.height * 0.18f
        ),
        size = Size(
            size.width * 0.30f,
            size.height * 0.20f
        )
    )
}

private fun DrawScope.drawRoute(
    primary: Color,
    progress: Float
) {
    val route = Path().apply {
        moveTo(
            size.width * 0.05f,
            size.height * 0.78f
        )
        cubicTo(
            size.width * 0.18f,
            size.height * 0.68f,
            size.width * 0.19f,
            size.height * 0.42f,
            size.width * 0.38f,
            size.height * 0.47f
        )
        cubicTo(
            size.width * 0.56f,
            size.height * 0.53f,
            size.width * 0.53f,
            size.height * 0.23f,
            size.width * 0.76f,
            size.height * 0.28f
        )
        cubicTo(
            size.width * 0.91f,
            size.height * 0.32f,
            size.width * 0.78f,
            size.height * 0.64f,
            size.width * 0.91f,
            size.height * 0.79f
        )
    }

    drawPath(
        path = route,
        color = primary.copy(alpha = 0.13f),
        style = Stroke(
            width = 13.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    drawPath(
        path = route,
        color = primary.copy(alpha = 0.62f),
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    val points = listOf(
        Offset(size.width * 0.05f, size.height * 0.78f),
        Offset(size.width * 0.38f, size.height * 0.47f),
        Offset(size.width * 0.76f, size.height * 0.28f),
        Offset(size.width * 0.91f, size.height * 0.79f)
    )

    points.forEachIndexed { index, point ->
        val alpha = if (progress > index / 4f) 1f else 0.55f

        drawCircle(
            color = primary.copy(alpha = 0.13f * alpha),
            radius = 11.dp.toPx(),
            center = point
        )

        drawCircle(
            color = primary.copy(alpha = alpha),
            radius = 5.dp.toPx(),
            center = point
        )
    }
}

private fun DrawScope.drawSmallTree(
    center: Offset,
    color: Color
) {
    drawCircle(
        color = color,
        radius = 13.dp.toPx(),
        center = center
    )

    drawCircle(
        color = color.copy(alpha = 0.85f),
        radius = 10.dp.toPx(),
        center = center + Offset(7.dp.toPx(), 4.dp.toPx())
    )

    drawLine(
        color = color.copy(alpha = 0.75f),
        start = center + Offset(0f, 7.dp.toPx()),
        end = center + Offset(0f, 22.dp.toPx()),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
}

@Preview(
    name = "Get Started - Light",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun SplashScreenPreview() {
    LocationDotsTheme {
        SplashScreen(
            onGetStarted = {}
        )
    }
}

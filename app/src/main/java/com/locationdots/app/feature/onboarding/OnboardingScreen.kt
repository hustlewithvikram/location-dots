package com.locationdots.app.feature.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locationdots.app.feature.splash.SplashScreen
import com.locationdots.app.ui.theme.LocationDotsTheme

private val OnboardingBackground = Color(0xFFFEFEFF)
private val OnboardingText = Color(0xFF111A36)
private val OnboardingSecondaryText = Color(0xFF6E7A99)

private val OnboardingBlue = Color(0xFF2F5BFF)
private val OnboardingButtonBackground = Color(0xFFE8EEFF)

private val OnboardingMapLine = Color(0xFFE9EDF6)
private val OnboardingMapLineSecondary = Color(0xFFF1F3F8)


@Composable
fun OnboardingScreen(
    hasLocationPermission: Boolean,
    isTracking: Boolean,
    onRequestLocationPermission: () -> Unit,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit
) {
    var page by remember {
        mutableIntStateOf(0)
    }

    val titles = listOf(
        "Remember where you go.",
        "See every journey.",
        "Private by design."
    )

    val descriptions = listOf(
        "Location Dots turns everyday movement into a beautiful personal timeline.",
        "Understand routes, places, distance and movement without manual logging.",
        "Your history is stored locally on your device and stays under your control."
    )

    val infiniteTransition = rememberInfiniteTransition(
        label = "onboardingAnimation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val routeProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "routeProgress"
    )

    val pageAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 350,
            easing = FastOutSlowInEasing
        ),
        label = "pageAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingBackground)
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 16.dp +
                        WindowInsets.safeDrawing
                            .asPaddingValues()
                            .calculateTopPadding(),
                bottom = 16.dp +
                        WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding()
            )
    ) {
        /*
         * ───────────────────────────────
         * MAIN CONTENT
         * ───────────────────────────────
         */

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = pageAlpha
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            /*
             * Illustration
             */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        top = 16.dp,
                        bottom = 8.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(
                            width = 350.dp,
                            height = 340.dp
                        )
                ) {
                    drawOnboardingIllustration(
                        page = page,
                        progress = routeProgress
                    )
                }

                /*
                 * Main location icon.
                 */
                when (page) {

                    0 -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(100.dp)
                                .scale(pulse),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(
                                        OnboardingBlue.copy(alpha = 0.055f),
                                        CircleShape
                                    )
                            )

                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = OnboardingBlue,
                                modifier = Modifier.size(62.dp)
                            )
                        }
                    }

                    1 -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(86.dp)
                                .scale(pulse),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .background(
                                        OnboardingBlue.copy(alpha = 0.055f),
                                        CircleShape
                                    )
                            )

                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = null,
                                tint = OnboardingBlue,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    2 -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(92.dp)
                                .scale(pulse),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .background(
                                        OnboardingBlue.copy(alpha = 0.055f),
                                        CircleShape
                                    )
                            )

                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = OnboardingBlue,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }
            }

            /*
             * ─────────────────────────
             * TITLE
             * ─────────────────────────
             */

            Text(
                text = titles[page],
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                style = TextStyle(
                    color = OnboardingText,
                    fontSize = 28.sp,
                    lineHeight = 37.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                textAlign = TextAlign.Center
            )

            /*
             * ─────────────────────────
             * DESCRIPTION
             * ─────────────────────────
             */

            Text(
                text = descriptions[page],
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 18.dp,
                        end = 18.dp,
                        top = 12.dp
                    ),
                style = TextStyle(
                    color = OnboardingSecondaryText,
                    fontSize = 17.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Normal
                ),
                textAlign = TextAlign.Center
            )
        }

        /*
         * ───────────────────────────────
         * PAGE INDICATOR
         * ───────────────────────────────
         */

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    bottom = 14.dp,
                    top = 14.dp
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->

                val active = index == page

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(
                            width = if (active) 28.dp else 7.dp,
                            height = 7.dp
                        )
                        .background(
                            color = if (active) {
                                OnboardingBlue
                            } else {
                                Color(0xFFDCE2EF)
                            },
                            shape = CircleShape
                        )
                )
            }
        }

        /*
         * ───────────────────────────────
         * ACTION
         * ───────────────────────────────
         */

        when {
            page < 2 -> {
                OnboardingActionButton(
                    text = "Continue",
                    onClick = {
                        page++
                    }
                )
            }

            !hasLocationPermission -> {
                OnboardingActionButton(
                    text = "Allow location access",
                    onClick = onRequestLocationPermission
                )
            }

            isTracking -> {
                OnboardingActionButton(
                    text = "Pause tracking",
                    onClick = onStopTracking
                )
            }

            else -> {
                OnboardingActionButton(
                    text = "Start tracking",
                    onClick = onStartTracking
                )
            }
        }
    }
}


/*
 * ═══════════════════════════════════════
 * EXPRESSIVE ACTION BUTTON
 * ═══════════════════════════════════════
 */

@Composable
private fun OnboardingActionButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    val pressed by interactionSource.collectIsPressedAsState()

    val scale = if (pressed) 0.975f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(
                color = OnboardingButtonBackground,
                shape = RoundedCornerShape(38.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                start = 24.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = TextStyle(
                color = OnboardingText,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = OnboardingBlue,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(27.dp)
                )
            }
        }
    }
}


/*
 * ═══════════════════════════════════════
 * ILLUSTRATIONS
 * ═══════════════════════════════════════
 */

private fun DrawScope.drawOnboardingIllustration(
    page: Int,
    progress: Float
) {
    when (page) {

        /*
         * PAGE 1
         *
         * Remember where you go.
         *
         * One main location and several
         * subtle saved locations.
         */
        0 -> {
            drawFaintMap()

            val locations = listOf(
                Offset(
                    size.width * 0.20f,
                    size.height * 0.65f
                ),
                Offset(
                    size.width * 0.38f,
                    size.height * 0.44f
                ),
                Offset(
                    size.width * 0.64f,
                    size.height * 0.58f
                ),
                Offset(
                    size.width * 0.79f,
                    size.height * 0.30f
                )
            )

            locations.forEachIndexed { index, location ->

                if (index != 2) {
                    drawCircle(
                        color = OnboardingBlue.copy(
                            alpha = 0.12f
                        ),
                        radius = 8.dp.toPx(),
                        center = location
                    )

                    drawCircle(
                        color = OnboardingBlue.copy(
                            alpha = 0.55f
                        ),
                        radius = 3.dp.toPx(),
                        center = location
                    )
                }
            }
        }

        /*
         * PAGE 2
         *
         * See every journey.
         */
        1 -> {
            drawFaintMap()

            val start = Offset(
                size.width * 0.14f,
                size.height * 0.72f
            )

            val middle = Offset(
                size.width * 0.48f,
                size.height * 0.50f
            )

            val end = Offset(
                size.width * 0.82f,
                size.height * 0.30f
            )

            val route = Path().apply {

                moveTo(
                    start.x,
                    start.y
                )

                cubicTo(
                    size.width * 0.25f,
                    size.height * 0.73f,
                    size.width * 0.30f,
                    size.height * 0.55f,
                    middle.x,
                    middle.y
                )

                cubicTo(
                    size.width * 0.62f,
                    size.height * 0.46f,
                    size.width * 0.67f,
                    size.height * 0.35f,
                    end.x,
                    end.y
                )
            }

            drawPath(
                path = route,
                color = OnboardingBlue.copy(
                    alpha = 0.07f
                ),
                style = Stroke(
                    width = 11.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            drawPath(
                path = route,
                color = OnboardingBlue,
                style = Stroke(
                    width = 2.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            drawSmallDot(start)
            drawSmallDot(middle)

            val movingPoint = if (progress < 0.5f) {
                cubicPoint(
                    p0 = start,
                    p1 = Offset(
                        size.width * 0.25f,
                        size.height * 0.73f
                    ),
                    p2 = Offset(
                        size.width * 0.30f,
                        size.height * 0.55f
                    ),
                    p3 = middle,
                    t = progress / 0.5f
                )
            } else {
                cubicPoint(
                    p0 = middle,
                    p1 = Offset(
                        size.width * 0.62f,
                        size.height * 0.46f
                    ),
                    p2 = Offset(
                        size.width * 0.67f,
                        size.height * 0.35f
                    ),
                    p3 = end,
                    t = (progress - 0.5f) / 0.5f
                )
            }

            drawCircle(
                color = OnboardingBlue.copy(
                    alpha = 0.10f
                ),
                radius = 9.dp.toPx(),
                center = movingPoint
            )

            drawCircle(
                color = OnboardingBlue,
                radius = 3.dp.toPx(),
                center = movingPoint
            )
        }

        /*
         * PAGE 3
         *
         * Private by design.
         */
        2 -> {
            drawFaintMap()

            /*
             * Subtle circular privacy boundary.
             */
            drawCircle(
                color = OnboardingBlue.copy(
                    alpha = 0.045f
                ),
                radius = size.minDimension * 0.31f,
                center = Offset(
                    size.width / 2f,
                    size.height / 2f
                )
            )

            drawCircle(
                color = OnboardingBlue.copy(
                    alpha = 0.10f
                ),
                radius = size.minDimension * 0.25f,
                center = Offset(
                    size.width / 2f,
                    size.height / 2f
                ),
                style = Stroke(
                    width = 1.5.dp.toPx()
                )
            )

            /*
             * Small surrounding points.
             */
            val points = listOf(
                Offset(
                    size.width * 0.25f,
                    size.height * 0.35f
                ),
                Offset(
                    size.width * 0.74f,
                    size.height * 0.32f
                ),
                Offset(
                    size.width * 0.24f,
                    size.height * 0.68f
                ),
                Offset(
                    size.width * 0.76f,
                    size.height * 0.68f
                )
            )

            points.forEach {
                drawCircle(
                    color = OnboardingBlue.copy(
                        alpha = 0.25f
                    ),
                    radius = 3.dp.toPx(),
                    center = it
                )
            }
        }
    }
}


/*
 * Very faint map background.
 */
private fun DrawScope.drawFaintMap() {

    val width = size.width
    val height = size.height

    val roads = listOf(

        Path().apply {
            moveTo(
                width * -0.05f,
                height * 0.32f
            )

            cubicTo(
                width * 0.18f,
                height * 0.22f,
                width * 0.37f,
                height * 0.38f,
                width * 0.57f,
                height * 0.51f
            )

            cubicTo(
                width * 0.76f,
                height * 0.64f,
                width * 0.91f,
                height * 0.55f,
                width * 1.05f,
                height * 0.64f
            )
        },

        Path().apply {
            moveTo(
                width * 0.05f,
                height * 0.85f
            )

            cubicTo(
                width * 0.21f,
                height * 0.65f,
                width * 0.32f,
                height * 0.50f,
                width * 0.44f,
                height * 0.28f
            )

            cubicTo(
                width * 0.57f,
                height * 0.10f,
                width * 0.77f,
                height * 0.12f,
                width * 1.05f,
                height * 0.02f
            )
        }
    )

    roads.forEach {
        drawPath(
            path = it,
            color = OnboardingMapLine,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}


private fun DrawScope.drawSmallDot(
    center: Offset
) {
    drawCircle(
        color = Color.White,
        radius = 8.dp.toPx(),
        center = center
    )

    drawCircle(
        color = OnboardingBlue,
        radius = 3.5.dp.toPx(),
        center = center
    )
}


private fun cubicPoint(
    p0: Offset,
    p1: Offset,
    p2: Offset,
    p3: Offset,
    t: Float
): Offset {

    val oneMinusT = 1f - t

    val x =
        oneMinusT * oneMinusT * oneMinusT * p0.x +
                3f * oneMinusT * oneMinusT * t * p1.x +
                3f * oneMinusT * t * t * p2.x +
                t * t * t * p3.x

    val y =
        oneMinusT * oneMinusT * oneMinusT * p0.y +
                3f * oneMinusT * oneMinusT * t * p1.y +
                3f * oneMinusT * t * t * p2.y +
                t * t * t * p3.y

    return Offset(
        x = x,
        y = y
    )
}



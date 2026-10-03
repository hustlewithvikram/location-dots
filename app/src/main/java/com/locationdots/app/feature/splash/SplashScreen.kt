package com.locationdots.app.feature.splash

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
import androidx.compose.material3.Icon
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
import com.locationdots.app.ui.theme.LocationDotsTheme

/*
 * Reference palette.
 *
 * These are intentionally NOT taken from MaterialTheme.colorScheme.
 * The reference uses a very specific blue/white palette.
 */
private val SplashBackground = Color(0xFFFEFEFF)
private val SplashText = Color(0xFF111A36)
private val SplashSecondaryText = Color(0xFF6E7A99)

private val SplashBlue = Color(0xFF2F5BFF)

private val SplashButtonBackground = Color(0xFFE8EEFF)

private val SplashMapLine = Color(0xFFE9EDF6)
private val SplashMapLineSecondary = Color(0xFFF1F3F8)


@Composable
fun SplashScreen(
    onGetStarted: () -> Unit
) {
    var started by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        started = true
    }

    /*
     * ─────────────────────────────────────
     * ENTRANCE ANIMATION
     * ─────────────────────────────────────
     */

    val headerAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "headerAlpha"
    )

    val headerOffset by animateFloatAsState(
        targetValue = if (started) 0f else -24f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "headerOffset"
    )

    val mapAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(
            durationMillis = 700,
            delayMillis = 120,
            easing = FastOutSlowInEasing
        ),
        label = "mapAlpha"
    )

    val mapScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.94f,
        animationSpec = tween(
            durationMillis = 750,
            delayMillis = 120,
            easing = FastOutSlowInEasing
        ),
        label = "mapScale"
    )

    val buttonAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(
            durationMillis = 550,
            delayMillis = 260,
            easing = FastOutSlowInEasing
        ),
        label = "buttonAlpha"
    )

    val buttonOffset by animateFloatAsState(
        targetValue = if (started) 0f else 24f,
        animationSpec = tween(
            durationMillis = 650,
            delayMillis = 260,
            easing = FastOutSlowInEasing
        ),
        label = "buttonOffset"
    )

    /*
     * ─────────────────────────────────────
     * CONTINUOUS MICRO ANIMATION
     * ─────────────────────────────────────
     */

    val infiniteTransition = rememberInfiniteTransition(
        label = "splashInfinite"
    )

    val routeProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "routeProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground)
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
         * ═══════════════════════════════════
         * TOP TYPOGRAPHY
         * ═══════════════════════════════════
         *
         * Deliberately pushed toward the top,
         * matching the reference.
         */
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = headerAlpha
                    translationY = headerOffset
                }
        ) {

            Text(
                text = "Your places.",
                style = TextStyle(
                    color = SplashText,
                    fontSize = 42.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Row(
                modifier = Modifier.padding(
                    top = 0.dp
                ),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Marked ",
                    style = TextStyle(
                        color = SplashText,
                        fontSize = 42.sp,
                        lineHeight = 46.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Text(
                    text = "simply.",
                    style = TextStyle(
                        color = SplashBlue,
                        fontSize = 42.sp,
                        lineHeight = 46.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            Text(
                text = "Save the places that matter",
                modifier = Modifier.padding(
                    top = 8.dp
                ),
                style = TextStyle(
                    color = SplashSecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }

        /*
         * ═══════════════════════════════════
         * MINIMAL MAP ILLUSTRATION
         * ═══════════════════════════════════
         */
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = mapAlpha
                    scaleX = mapScale
                    scaleY = mapScale
                },
            contentAlignment = Alignment.Center
        ) {

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(
                        width = 360.dp,
                        height = 380.dp
                    )
            ) {
                drawReferenceMap(
                    progress = routeProgress
                )
            }
        }

        /*
         * ═══════════════════════════════════
         * EXPRESSIVE BUTTON
         * ═══════════════════════════════════
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = buttonAlpha
                    translationY = buttonOffset
                }
        ) {
            ReferenceGetStartedButton(
                onClick = onGetStarted
            )
        }
    }
}


/**
 * Button reproduced from the reference:
 *
 * ┌─────────────────────────────────────────┐
 * │            Get Started          ( → )  │
 * └─────────────────────────────────────────┘
 *
 * The outer container is pale blue.
 * The circular action control is saturated blue.
 */
@Composable
private fun ReferenceGetStartedButton(
    onClick: () -> Unit
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    val pressed by interactionSource.collectIsPressedAsState()

    val scale = if (pressed) 0.975f else 1f

    SurfaceButtonContainer(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    )
}


@Composable
private fun SurfaceButtonContainer(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = SplashButtonBackground,
                shape = RoundedCornerShape(38.dp)
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
            text = "Get Started",
            modifier = Modifier.weight(1f),
            style = TextStyle(
                color = SplashText,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {

            androidx.compose.material3.Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = SplashBlue
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Get Started",
                        tint = Color.White,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }
        }
    }
}


/**
 * Draws the very faint map seen behind the route.
 *
 * The map is intentionally almost invisible.
 * It should be noticed only after looking closely.
 */
private fun DrawScope.drawReferenceMap(
    progress: Float
) {
    val width = size.width
    val height = size.height

    /*
     * ─────────────────────────────────
     * FAINT MAP ROADS
     * ─────────────────────────────────
     */

    val mapRoads = listOf(

        Path().apply {
            moveTo(
                width * -0.08f,
                height * 0.35f
            )

            cubicTo(
                width * 0.12f,
                height * 0.24f,
                width * 0.34f,
                height * 0.34f,
                width * 0.54f,
                height * 0.50f
            )

            cubicTo(
                width * 0.70f,
                height * 0.63f,
                width * 0.86f,
                height * 0.66f,
                width * 1.08f,
                height * 0.76f
            )
        },

        Path().apply {
            moveTo(
                width * -0.05f,
                height * 0.72f
            )

            cubicTo(
                width * 0.15f,
                height * 0.62f,
                width * 0.28f,
                height * 0.49f,
                width * 0.43f,
                height * 0.29f
            )

            cubicTo(
                width * 0.57f,
                height * 0.12f,
                width * 0.76f,
                height * 0.08f,
                width * 1.05f,
                height * -0.02f
            )
        },

        Path().apply {
            moveTo(
                width * 0.26f,
                height * 1.05f
            )

            cubicTo(
                width * 0.38f,
                height * 0.80f,
                width * 0.51f,
                height * 0.70f,
                width * 0.67f,
                height * 0.71f
            )

            cubicTo(
                width * 0.83f,
                height * 0.72f,
                width * 0.92f,
                height * 0.85f,
                width * 1.05f,
                height * 0.96f
            )
        }
    )

    mapRoads.forEach { road ->
        drawPath(
            path = road,
            color = SplashMapLine,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }

    /*
     * A couple of even fainter secondary roads.
     */
    val secondaryRoad = Path().apply {
        moveTo(
            width * 0.05f,
            height * 0.15f
        )

        cubicTo(
            width * 0.27f,
            height * 0.10f,
            width * 0.48f,
            height * 0.20f,
            width * 0.70f,
            height * 0.34f
        )

        cubicTo(
            width * 0.82f,
            height * 0.41f,
            width * 0.94f,
            height * 0.39f,
            width * 1.05f,
            height * 0.32f
        )
    }

    drawPath(
        path = secondaryRoad,
        color = SplashMapLineSecondary,
        style = Stroke(
            width = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    /*
     * ─────────────────────────────────
     * MAIN BLUE ROUTE
     * ─────────────────────────────────
     */

    val start = Offset(
        width * 0.14f,
        height * 0.72f
    )

    val middle = Offset(
        width * 0.50f,
        height * 0.50f
    )

    val destination = Offset(
        width * 0.82f,
        height * 0.31f
    )

    val route = Path().apply {

        moveTo(
            start.x,
            start.y
        )

        cubicTo(
            width * 0.24f,
            height * 0.73f,
            width * 0.29f,
            height * 0.56f,
            middle.x,
            middle.y
        )

        cubicTo(
            width * 0.62f,
            height * 0.47f,
            width * 0.65f,
            height * 0.37f,
            destination.x,
            destination.y
        )
    }

    /*
     * Very subtle route glow.
     */
    drawPath(
        path = route,
        color = SplashBlue.copy(alpha = 0.055f),
        style = Stroke(
            width = 12.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    /*
     * Actual route.
     */
    drawPath(
        path = route,
        color = SplashBlue,
        style = Stroke(
            width = 2.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    /*
     * ─────────────────────────────────
     * ROUTE DOTS
     * ─────────────────────────────────
     */

    drawReferenceDot(
        center = start
    )

    drawReferenceDot(
        center = middle
    )

    /*
     * Neutral dot before destination.
     */
    val neutralDot = destination

    drawCircle(
        color = Color(0xFFCBD1DE),
        radius = 5.dp.toPx(),
        center = neutralDot
    )

    /*
     * ─────────────────────────────────
     * MOVING DOT
     * ─────────────────────────────────
     */

    val movingPoint = when {
        progress < 0.5f -> {
            cubicPoint(
                p0 = start,
                p1 = Offset(
                    width * 0.24f,
                    height * 0.73f
                ),
                p2 = Offset(
                    width * 0.29f,
                    height * 0.56f
                ),
                p3 = middle,
                t = progress / 0.5f
            )
        }

        else -> {
            cubicPoint(
                p0 = middle,
                p1 = Offset(
                    width * 0.62f,
                    height * 0.47f
                ),
                p2 = Offset(
                    width * 0.65f,
                    height * 0.37f
                ),
                p3 = destination,
                t = (progress - 0.5f) / 0.5f
            )
        }
    }

    drawCircle(
        color = SplashBlue.copy(alpha = 0.08f),
        radius = 9.dp.toPx(),
        center = movingPoint
    )

    drawCircle(
        color = SplashBlue,
        radius = 3.dp.toPx(),
        center = movingPoint
    )
}


private fun DrawScope.drawReferenceDot(
    center: Offset
) {
    /*
     * White outer halo.
     */
    drawCircle(
        color = Color.White,
        radius = 9.dp.toPx(),
        center = center
    )

    /*
     * Blue center.
     */
    drawCircle(
        color = SplashBlue,
        radius = 4.dp.toPx(),
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

package com.aperture.camera.ui.components

import android.graphics.Rect
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated flagship-style halo ring overlay around the physical front camera punch-hole / cutout.
 *
 * Automatically detects the exact hardware cutout location (left, center, or right punch-hole)
 * via Android's DisplayCutout API and displays:
 * 1. A 360-degree illuminated golden sweep arc on camera switch.
 * 2. An expanding radial neon breathing pulse.
 * 3. An active circular countdown timer ring during selfie countdowns.
 */
@Composable
fun FrontCameraHaloOverlay(
    isFrontCamera: Boolean,
    timerRemainingSeconds: Int = 0,
    timerTotalSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current

    // Animation progress states
    val sweepAngle = remember { Animatable(0f) }
    val ringAlpha = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(1f) }
    val pulseAlpha = remember { Animatable(0f) }

    // Trigger ring sweep animation whenever switching to front camera
    LaunchedEffect(isFrontCamera) {
        if (isFrontCamera) {
            // Reset animatables
            sweepAngle.snapTo(0f)
            ringAlpha.snapTo(1f)
            pulseScale.snapTo(1f)
            pulseAlpha.snapTo(0.75f)

            // Launch concurrent slow, elegant sweep & pulse animations
            launch {
                sweepAngle.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(durationMillis = 1150, easing = FastOutSlowInEasing)
                )
                // Hold briefly for visual prominence then smoothly fade out
                delay(750)
                ringAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 650, easing = LinearOutSlowInEasing)
                )
            }

            launch {
                // Subtle, tight radial pulse
                pulseScale.animateTo(
                    targetValue = 1.18f,
                    animationSpec = tween(durationMillis = 1200, easing = LinearOutSlowInEasing)
                )
            }

            launch {
                pulseAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
                )
            }
        } else {
            sweepAngle.snapTo(0f)
            ringAlpha.snapTo(0f)
            pulseAlpha.snapTo(0f)
        }
    }

    // Determine if timer countdown is active on front camera
    val isTimerActive = isFrontCamera && timerRemainingSeconds > 0 && timerTotalSeconds > 0
    val timerProgress = if (isTimerActive) {
        timerRemainingSeconds.toFloat() / timerTotalSeconds.toFloat()
    } else 0f

    // Render Canvas only if there is an active animation or countdown
    if (ringAlpha.value > 0.005f || pulseAlpha.value > 0.005f || isTimerActive) {
        Canvas(modifier = modifier.fillMaxSize()) {
            // 1. Locate Physical Camera Punch-Hole Bounds & Status Bar Inset
            val statusBarHeightPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.rootWindowInsets?.getInsets(android.view.WindowInsets.Type.statusBars())?.top?.toFloat()
                    ?: with(density) { 32.dp.toPx() }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                view.rootWindowInsets?.systemWindowInsetTop?.toFloat()
                    ?: with(density) { 32.dp.toPx() }
            } else {
                with(density) { 32.dp.toPx() }
            }

            val cutoutRect: Rect? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val insets = view.rootWindowInsets
                val cutout = insets?.displayCutout
                val rects = cutout?.boundingRects ?: emptyList()
                // Find top-aligned cutout (punch hole or notch at top 25% of screen)
                rects.firstOrNull { it.top <= size.height * 0.25f } ?: rects.firstOrNull()
            } else {
                null
            }

            val threeDpPx = with(density) { 3.dp.toPx() }
            val yOffsetCorrection = with(density) { 5.0.dp.toPx() }
            val fallbackRadius = with(density) { 5.dp.toPx() }
            val defaultTopOffset = with(density) { 33.0.dp.toPx() }

            val centerX: Float
            val centerY: Float
            val baseRadius: Float

            if (cutoutRect != null && !cutoutRect.isEmpty) {
                centerX = cutoutRect.exactCenterX()
                // Shift down 5pt from top to center perfectly over the circular lens
                centerY = cutoutRect.exactCenterY() + yOffsetCorrection
                val cutoutHalfWidth = cutoutRect.width() / 2f
                val cutoutHalfHeight = cutoutRect.height() / 2f
                // Reduced radius by 1pt more
                val rawRadius = maxOf(cutoutHalfWidth, cutoutHalfHeight)
                baseRadius = (rawRadius - threeDpPx).coerceAtLeast(with(density) { 3.5.dp.toPx() })
            } else {
                // Fallback: Top-center alignment
                centerX = size.width / 2f
                centerY = defaultTopOffset
                baseRadius = fallbackRadius
            }

            val strokeWidth = with(density) { 1.8.dp.toPx() }
            val glowStrokeWidth = with(density) { 3.5.dp.toPx() }

            // 2. Draw Subtle Radial Glow Pulse Ring (Breathing effect)
            if (pulseAlpha.value > 0.01f) {
                val currentPulseRadius = baseRadius * pulseScale.value
                drawCircle(
                    color = Color(0xFFFFD600).copy(alpha = pulseAlpha.value * 0.35f),
                    radius = currentPulseRadius + with(density) { 1.2.dp.toPx() },
                    center = Offset(centerX, centerY),
                    style = Stroke(width = glowStrokeWidth)
                )
                drawCircle(
                    color = Color(0xFFFFFFFF).copy(alpha = pulseAlpha.value * 0.65f),
                    radius = currentPulseRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = strokeWidth)
                )
            }

            // 3. Draw 360-Degree Sweep Ring on Camera Switch
            if (ringAlpha.value > 0.01f && sweepAngle.value > 0f) {
                val arcTopLeft = Offset(centerX - baseRadius, centerY - baseRadius)
                val arcSize = Size(baseRadius * 2f, baseRadius * 2f)

                // Soft Outer Amber Glow
                drawArc(
                    color = Color(0xFFFFD600).copy(alpha = ringAlpha.value * 0.35f),
                    startAngle = -90f,
                    sweepAngle = sweepAngle.value,
                    useCenter = false,
                    topLeft = Offset(centerX - (baseRadius + with(density) { 1.dp.toPx() }), centerY - (baseRadius + with(density) { 1.dp.toPx() })),
                    size = Size((baseRadius + with(density) { 1.dp.toPx() }) * 2f, (baseRadius + with(density) { 1.dp.toPx() }) * 2f),
                    style = Stroke(width = glowStrokeWidth, cap = StrokeCap.Round)
                )

                // Crisp Bright Golden Sweep Line
                drawArc(
                    color = Color(0xFFFFD600).copy(alpha = ringAlpha.value),
                    startAngle = -90f,
                    sweepAngle = sweepAngle.value,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Bright Leading Head Dot
                if (sweepAngle.value in 5f..355f) {
                    val angleRad = Math.toRadians((sweepAngle.value - 90.0)).toFloat()
                    val headX = centerX + baseRadius * kotlin.math.cos(angleRad)
                    val headY = centerY + baseRadius * kotlin.math.sin(angleRad)
                    drawCircle(
                        color = Color.White.copy(alpha = ringAlpha.value),
                        radius = with(density) { 2.0.dp.toPx() },
                        center = Offset(headX, headY)
                    )
                }
            }

            // 4. Draw Active Selfie Countdown Ring
            if (isTimerActive) {
                val timerArcTopLeft = Offset(centerX - (baseRadius + with(density) { 0.8.dp.toPx() }), centerY - (baseRadius + with(density) { 0.8.dp.toPx() }))
                val timerArcRadius = baseRadius + with(density) { 0.8.dp.toPx() }
                val timerArcSize = Size(timerArcRadius * 2f, timerArcRadius * 2f)

                // Track background ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = timerArcRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = with(density) { 1.6.dp.toPx() })
                )

                // Active countdown arc
                val countdownSweep = 360f * timerProgress
                drawArc(
                    color = Color(0xFFFFD600),
                    startAngle = -90f,
                    sweepAngle = countdownSweep,
                    useCenter = false,
                    topLeft = timerArcTopLeft,
                    size = timerArcSize,
                    style = Stroke(width = with(density) { 2.0.dp.toPx() }, cap = StrokeCap.Round)
                )
            }
        }
    }
}

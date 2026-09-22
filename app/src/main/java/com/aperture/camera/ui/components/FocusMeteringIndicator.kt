package com.aperture.camera.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun FocusMeteringIndicator(
    focusPoint: Pair<Float, Float>?,
    isFocusing: Boolean,
    modifier: Modifier = Modifier
) {
    if (focusPoint == null) return

    val scale = remember { Animatable(1.5f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(focusPoint) {
        scale.snapTo(1.5f)
        alpha.snapTo(1f)
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(isFocusing) {
        if (!isFocusing) {
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 800, delayMillis = 1200)
            )
        }
    }

    if (alpha.value > 0.01f) {
        val targetSizeDp = 70.dp
        Box(modifier = modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .size(targetSizeDp)
                    .offset {
                        IntOffset(
                            x = (focusPoint.first - 35 * density).roundToInt(),
                            y = (focusPoint.second - 35 * density).roundToInt()
                        )
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val ringColor = Color(0xFFFFD600).copy(alpha = alpha.value)
                val currentRadius = (size.width / 2f - 4) * scale.value

                // Draw outer focus circle
                drawCircle(
                    color = ringColor,
                    radius = currentRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Draw 4 small focus ticks
                val tickLength = 6.dp.toPx()
                drawLine(ringColor, Offset(center.x, center.y - currentRadius), Offset(center.x, center.y - currentRadius + tickLength), 2.dp.toPx())
                drawLine(ringColor, Offset(center.x, center.y + currentRadius), Offset(center.x, center.y + currentRadius - tickLength), 2.dp.toPx())
                drawLine(ringColor, Offset(center.x - currentRadius, center.y), Offset(center.x - currentRadius + tickLength, center.y), 2.dp.toPx())
                drawLine(ringColor, Offset(center.x + currentRadius, center.y), Offset(center.x + currentRadius - tickLength, center.y), 2.dp.toPx())
            }
        }
    }
}

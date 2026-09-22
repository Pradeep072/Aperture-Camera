package com.aperture.camera.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * High-speed shutter screen flash overlay providing visual capture feedback
 * over the viewfinder without obstructive toast banners.
 */
@Composable
fun ShutterFlashOverlay(
    triggerTime: Long,
    modifier: Modifier = Modifier
) {
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(triggerTime) {
        if (triggerTime > 0L) {
            // Instantaneous bright shutter flash ramp-up
            alphaAnim.snapTo(0.85f)
            // Ultra-smooth decay simulating mechanical shutter exposure
            alphaAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 160,
                    easing = LinearOutSlowInEasing
                )
            )
        }
    }

    if (alphaAnim.value > 0.005f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = alphaAnim.value))
        )
    }
}

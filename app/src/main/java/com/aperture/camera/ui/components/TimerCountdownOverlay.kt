package com.aperture.camera.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun TimerCountdownOverlay(
    secondsRemaining: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = secondsRemaining > 0,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val scale = remember { Animatable(1.5f) }

        LaunchedEffect(secondsRemaining) {
            scale.snapTo(1.8f)
            scale.animateTo(1.0f, tween(300))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$secondsRemaining",
                color = Color(0xFFFFD600),
                fontSize = 96.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.scale(scale.value)
            )
        }
    }
}

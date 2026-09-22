package com.aperture.camera.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoStable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aperture.camera.camera.VideoRecordingState
import com.aperture.camera.data.model.VideoQualityOption

@Composable
fun VideoControlsTopBar(
    selectedQuality: VideoQualityOption,
    targetFps: Int,
    isStabilizationOn: Boolean,
    isTorchOn: Boolean,
    isAudioOn: Boolean,
    isRecording: Boolean,
    onToggleQuality: () -> Unit,
    onToggleFps: () -> Unit,
    onToggleStabilization: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quality & FPS Pills (Disabled during recording)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable(enabled = !isRecording) { onToggleQuality() }
            ) {
                Text(
                    text = selectedQuality.title,
                    color = if (isRecording) Color.Gray else Color(0xFFFFD600),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable(enabled = !isRecording) { onToggleFps() }
            ) {
                Text(
                    text = "${targetFps}FPS",
                    color = if (isRecording) Color.Gray else Color(0xFFFFD600),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Toggles: EIS, Torch, Mic
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // EIS Stabilization
            Surface(
                color = if (isStabilizationOn) Color(0xFFFFD600).copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.5f),
                shape = CircleShape
            ) {
                IconButton(
                    onClick = onToggleStabilization,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoStable,
                        contentDescription = "Video Stabilization",
                        tint = if (isStabilizationOn) Color(0xFFFFD600) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Torch
            Surface(
                color = if (isTorchOn) Color(0xFFFFD600).copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.5f),
                shape = CircleShape
            ) {
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Torch",
                        tint = if (isTorchOn) Color(0xFFFFD600) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Mic
            Surface(
                color = if (isAudioOn) Color.Black.copy(alpha = 0.5f) else Color(0xFFFF3B30).copy(alpha = 0.2f),
                shape = CircleShape
            ) {
                IconButton(
                    onClick = onToggleAudio,
                    modifier = Modifier.size(34.dp),
                    enabled = !isRecording
                ) {
                    Icon(
                        imageVector = if (isAudioOn) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Audio Toggle",
                        tint = if (isAudioOn) Color.White else Color(0xFFFF3B30),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoRecordingActiveBanner(
    recordingState: VideoRecordingState,
    onPauseResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeState = recordingState as? VideoRecordingState.RecordingActive ?: return

    val infiniteTransition = rememberInfiniteTransition(label = "recordingDot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (activeState.isPaused) 1f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    val minutes = activeState.durationSec / 60
    val seconds = activeState.durationSec % 60
    val formattedTime = "%02d:%02d".format(minutes, seconds)

    Surface(
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Blinking Red Recording Dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30).copy(alpha = dotAlpha))
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (activeState.isPaused) "$formattedTime (PAUSED)" else formattedTime,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Pause/Resume icon
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF282B34))
                    .clickable { onPauseResume() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (activeState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (activeState.isPaused) "Resume" else "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

package com.aperture.camera.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aperture.camera.data.model.CaptureMode

@Composable
fun ModeSelectorBar(
    currentMode: CaptureMode,
    isDualSupported: Boolean,
    onSelectMode: (CaptureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = CaptureMode.values().toList()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (mode in modes) {
            val isSelected = mode == currentMode
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFFFD600) else Color.LightGray.copy(alpha = 0.6f),
                label = "modeTextColor"
            )

            Box(
                modifier = Modifier
                    .clickable { onSelectMode(mode) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.title,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

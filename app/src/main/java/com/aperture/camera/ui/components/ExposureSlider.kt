package com.aperture.camera.ui.components

import android.util.Range
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun ExposureSlider(
    visible: Boolean,
    exposureIndex: Int,
    exposureRange: Range<Int>,
    exposureStep: Float,
    onExposureChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && exposureRange.upper > exposureRange.lower,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val evValue = exposureIndex * exposureStep
        val evFormatted = if (evValue > 0) "+%.1f EV".format(evValue) else "%.1f EV".format(evValue)

        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = Color(0xFFFFD600),
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = evFormatted,
                    color = Color(0xFFFFD600),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Vertical slider simulated with rotated slider
                Slider(
                    value = exposureIndex.toFloat(),
                    onValueChange = { onExposureChange(it.roundToInt()) },
                    valueRange = exposureRange.lower.toFloat()..exposureRange.upper.toFloat(),
                    steps = (exposureRange.upper - exposureRange.lower) - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFD600),
                        activeTrackColor = Color(0xFFFFD600),
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .height(130.dp)
                        .width(28.dp)
                        .rotate(270f)
                )
            }
        }
    }
}

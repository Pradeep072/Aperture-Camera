package com.aperture.camera.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aperture.camera.camera.ProSettingsState
import kotlin.math.roundToInt

enum class ProControlTab(val title: String) {
    ISO("ISO"),
    SHUTTER("SEC"),
    FOCUS("FOCUS"),
    WB("WB")
}

@Composable
fun ProControlsPanel(
    visible: Boolean,
    isManualSensorSupported: Boolean,
    proSettings: ProSettingsState,
    onUpdateSettings: (ProSettingsState) -> Unit,
    onResetPro: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ProControlTab.ISO) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (!isManualSensorSupported) {
                    Text(
                        text = "Note: This camera sensor does not report full manual hardware support. Some manual parameters may be emulated or restricted.",
                        color = Color(0xFFFF9500),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Tabs: ISO, SEC, FOCUS, WB + RESET
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (tab in ProControlTab.values()) {
                            val isSelected = tab == selectedTab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFFFD600) else Color(0xFF22252E))
                                    .clickable { selectedTab = tab }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = onResetPro,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("AUTO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Tab Slider
                when (selectedTab) {
                    ProControlTab.ISO -> {
                        IsoControlRow(
                            isAuto = proSettings.isAutoIso,
                            isoValue = proSettings.manualIso,
                            onToggleAuto = {
                                onUpdateSettings(proSettings.copy(isAutoIso = !proSettings.isAutoIso))
                            },
                            onIsoChange = {
                                onUpdateSettings(proSettings.copy(isAutoIso = false, manualIso = it))
                            }
                        )
                    }
                    ProControlTab.SHUTTER -> {
                        ShutterControlRow(
                            isAuto = proSettings.isAutoShutter,
                            shutterNs = proSettings.manualShutterNs,
                            onToggleAuto = {
                                onUpdateSettings(proSettings.copy(isAutoShutter = !proSettings.isAutoShutter))
                            },
                            onShutterChange = {
                                onUpdateSettings(proSettings.copy(isAutoShutter = false, manualShutterNs = it))
                            }
                        )
                    }
                    ProControlTab.FOCUS -> {
                        FocusControlRow(
                            isAuto = proSettings.isAutoFocus,
                            focusDistance = proSettings.manualFocusDistance,
                            onToggleAuto = {
                                onUpdateSettings(proSettings.copy(isAutoFocus = !proSettings.isAutoFocus))
                            },
                            onFocusChange = {
                                onUpdateSettings(proSettings.copy(isAutoFocus = false, manualFocusDistance = it))
                            }
                        )
                    }
                    ProControlTab.WB -> {
                        WbControlRow(
                            isAuto = proSettings.isAutoWb,
                            kelvin = proSettings.manualWbKelvin,
                            onToggleAuto = {
                                onUpdateSettings(proSettings.copy(isAutoWb = !proSettings.isAutoWb))
                            },
                            onKelvinChange = {
                                onUpdateSettings(proSettings.copy(isAutoWb = false, manualWbKelvin = it))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IsoControlRow(
    isAuto: Boolean,
    isoValue: Int,
    onToggleAuto: () -> Unit,
    onIsoChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isAuto) "ISO: AUTO" else "ISO: $isoValue",
                color = Color(0xFFFFD600),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            AutoBadge(isAuto = isAuto, onClick = onToggleAuto)
        }

        Slider(
            value = isoValue.toFloat(),
            onValueChange = { onIsoChange(it.roundToInt()) },
            valueRange = 50f..3200f,
            enabled = !isAuto,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFD600),
                activeTrackColor = Color(0xFFFFD600)
            )
        )
    }
}

@Composable
private fun ShutterControlRow(
    isAuto: Boolean,
    shutterNs: Long,
    onToggleAuto: () -> Unit,
    onShutterChange: (Long) -> Unit
) {
    val sec = shutterNs / 1_000_000_000.0
    val formatted = if (sec < 1.0) "1/%.0fs".format(1.0 / sec) else "%.1fs".format(sec)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isAuto) "Shutter: AUTO" else "Shutter: $formatted",
                color = Color(0xFFFFD600),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            AutoBadge(isAuto = isAuto, onClick = onToggleAuto)
        }

        Slider(
            value = shutterNs.toFloat(),
            onValueChange = { onShutterChange(it.toLong()) },
            valueRange = 250_000f..1_000_000_000f, // 1/4000s to 1s
            enabled = !isAuto,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFD600),
                activeTrackColor = Color(0xFFFFD600)
            )
        )
    }
}

@Composable
private fun FocusControlRow(
    isAuto: Boolean,
    focusDistance: Float,
    onToggleAuto: () -> Unit,
    onFocusChange: (Float) -> Unit
) {
    val distanceText = when {
        focusDistance == 0f -> "Infinity (∞)"
        focusDistance > 0f -> "~%.1f cm".format(100f / focusDistance)
        else -> "Auto"
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isAuto) "Focus: AUTO (AF)" else "Focus: $distanceText (MF)",
                color = Color(0xFFFFD600),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            AutoBadge(isAuto = isAuto, onClick = onToggleAuto)
        }

        Slider(
            value = focusDistance,
            onValueChange = onFocusChange,
            valueRange = 0f..10f, // 0 = infinity, 10 = 10cm macro
            enabled = !isAuto,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFD600),
                activeTrackColor = Color(0xFFFFD600)
            )
        )
    }
}

@Composable
private fun WbControlRow(
    isAuto: Boolean,
    kelvin: Int,
    onToggleAuto: () -> Unit,
    onKelvinChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isAuto) "WB: AUTO" else "WB: ${kelvin}K",
                color = Color(0xFFFFD600),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            AutoBadge(isAuto = isAuto, onClick = onToggleAuto)
        }

        Slider(
            value = kelvin.toFloat(),
            onValueChange = { onKelvinChange(it.roundToInt()) },
            valueRange = 2500f..8500f,
            enabled = !isAuto,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFD600),
                activeTrackColor = Color(0xFFFFD600)
            )
        )
    }
}

@Composable
private fun AutoBadge(isAuto: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isAuto) Color(0xFFFFD600).copy(alpha = 0.2f) else Color(0xFF22252E))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (isAuto) "AUTO ON" else "MANUAL",
            color = if (isAuto) Color(0xFFFFD600) else Color.LightGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

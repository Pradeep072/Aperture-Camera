package com.aperture.camera.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aperture.camera.data.model.CameraSettings
import com.aperture.camera.data.model.GridType
import com.aperture.camera.data.model.VideoQualityOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settings: CameraSettings,
    onDismiss: () -> Unit,
    onUpdateGridType: (GridType) -> Unit,
    onUpdateGeotag: (Boolean) -> Unit,
    onUpdateShutterSound: (Boolean) -> Unit,
    onUpdateSaveOriginal: (Boolean) -> Unit,
    onUpdateRawCapture: (Boolean) -> Unit,
    onUpdateVideoQuality: (VideoQualityOption) -> Unit,
    onUpdateTargetFps: (Int) -> Unit,
    onUpdateVideoStabilization: (Boolean) -> Unit,
    onUpdateAudio: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF16181F),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Camera Settings",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF282B34))
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Composition Section
                item {
                    SectionHeader("Composition & Framing")
                }

                item {
                    Text("Grid Overlay", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (type in GridType.values()) {
                            val isSelected = type == settings.gridType
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFFFD600) else Color(0xFF22252E))
                                    .clickable { onUpdateGridType(type) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type.title,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Photo Section
                item {
                    SectionHeader("Photography")
                }

                item {
                    SettingToggleRow(
                        title = "Maximum Quality Master",
                        description = "Always save uncompressed maximum sensor resolution to DCIM/Camera",
                        isChecked = settings.isSaveOriginalEnabled,
                        onCheckedChange = onUpdateSaveOriginal
                    )
                }

                item {
                    SettingToggleRow(
                        title = "RAW / DNG Output",
                        description = "Save uncompressed RAW sensor data if supported by the lens",
                        isChecked = settings.isRawCaptureEnabled,
                        onCheckedChange = onUpdateRawCapture
                    )
                }

                item {
                    SettingToggleRow(
                        title = "GPS Geotagging",
                        description = "Embed location coordinates in photo EXIF metadata",
                        isChecked = settings.isGeotagEnabled,
                        onCheckedChange = onUpdateGeotag
                    )
                }

                item {
                    SettingToggleRow(
                        title = "Shutter Sound",
                        description = "Play capture audio feedback",
                        isChecked = settings.isShutterSoundEnabled,
                        onCheckedChange = onUpdateShutterSound
                    )
                }

                // Video Section
                item {
                    SectionHeader("Video Recording")
                }

                item {
                    Text("Default Quality", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (quality in VideoQualityOption.values()) {
                            val isSelected = quality == settings.videoQuality
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFFFD600) else Color(0xFF22252E))
                                    .clickable { onUpdateVideoQuality(quality) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = quality.title.replace(" UHD", "").replace(" FHD", ""),
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                item {
                    SettingToggleRow(
                        title = "Electronic Stabilization (EIS)",
                        description = "Smooth out handheld video jitter",
                        isChecked = settings.isVideoStabilizationEnabled,
                        onCheckedChange = onUpdateVideoStabilization
                    )
                }

                item {
                    SettingToggleRow(
                        title = "Record Audio",
                        description = "Capture microphone sound during video recording",
                        isChecked = settings.isAudioEnabled,
                        onCheckedChange = onUpdateAudio
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = Color(0xFFFFD600),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = description, color = Color.Gray, fontSize = 12.sp)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFFFFD600)
            )
        )
    }
}

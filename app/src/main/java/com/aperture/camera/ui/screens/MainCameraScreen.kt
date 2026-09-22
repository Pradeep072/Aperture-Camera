package com.aperture.camera.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aperture.camera.about.AboutCameraScreen
import com.aperture.camera.about.AboutCameraViewModel
import com.aperture.camera.camera.VideoRecordingState
import com.aperture.camera.data.model.CaptureMode
import com.aperture.camera.data.model.FlashModeOption
import com.aperture.camera.data.model.TimerDuration
import com.aperture.camera.data.model.VideoQualityOption
import com.aperture.camera.ui.components.LensSelectorChips
import com.aperture.camera.ui.components.ModeSelectorBar
import com.aperture.camera.ui.components.ProControlsPanel
import com.aperture.camera.ui.components.TimerCountdownOverlay
import com.aperture.camera.ui.components.VideoControlsTopBar
import com.aperture.camera.ui.components.VideoRecordingActiveBanner
import com.aperture.camera.ui.components.ViewfinderPreview
import com.aperture.camera.ui.viewmodel.CameraViewModel

@Composable
fun MainCameraScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var activePreviewView by remember { mutableStateOf<PreviewView?>(null) }
    var lastBackPressTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    // Double-back to exit handling
    BackHandler(enabled = true) {
        when {
            uiState.isDocumentEditorOpen -> viewModel.closeDocumentEditor()
            uiState.viewingMediaItem != null -> viewModel.closeMediaViewer()
            uiState.isAboutOpen -> viewModel.toggleAboutScreen(false)
            uiState.isSettingsOpen -> viewModel.toggleSettingsDialog(false)
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressTime = now
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Initial camera binding when preview view becomes available or capture mode changes
    LaunchedEffect(uiState.isInitialized, uiState.captureMode, activePreviewView) {
        val pv = activePreviewView
        if (uiState.isInitialized && uiState.selectedLens != null && pv != null) {
            viewModel.bindCamera(lifecycleOwner, pv)
        }
    }

    // Handle toast messages
    LaunchedEffect(uiState.statusToastMessage) {
        uiState.statusToastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusToast()
        }
    }

    // 1. If viewing Document Scanner Editor
    if (uiState.isDocumentEditorOpen && uiState.documentScanBitmap != null) {
        DocumentScanEditorScreen(
            initialBitmap = uiState.documentScanBitmap!!,
            onSavePdf = { pdfBytes, name ->
                viewModel.saveDocumentPdf(pdfBytes, name)
            },
            onSaveImage = { jpegBytes, name ->
                viewModel.saveDocumentImage(jpegBytes, name)
            },
            onAddAnotherPage = {
                viewModel.closeDocumentEditor()
                viewModel.triggerShutter()
            },
            onDismiss = {
                viewModel.closeDocumentEditor()
            }
        )
        return
    }

    // 2. If viewing specific media item, show full-screen media viewer
    if (uiState.viewingMediaItem != null) {
        MediaViewerScreen(
            mediaItem = uiState.viewingMediaItem!!,
            onNavigateBack = { viewModel.closeMediaViewer() },
            onDeleteMedia = { viewModel.deleteMedia(it) }
        )
        return
    }

    // 3. If viewing About Camera screen
    if (uiState.isAboutOpen) {
        val aboutViewModel: AboutCameraViewModel = viewModel()
        AboutCameraScreen(
            viewModel = aboutViewModel,
            onNavigateBack = { viewModel.toggleAboutScreen(false) }
        )
        return
    }

    // 4. Root Viewfinder Container
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E11))
    ) {
        // Center Viewfinder
        ViewfinderPreview(
            gridType = settings.gridType,
            isDocumentMode = uiState.captureMode == CaptureMode.DOCUMENT,
            zoomRatio = sessionState.zoomRatio,
            isFocusing = sessionState.isFocusing,
            focusPoint = sessionState.focusPoint,
            exposureIndex = sessionState.exposureIndex,
            exposureRange = sessionState.exposureRange,
            exposureStep = sessionState.exposureStep,
            onPreviewViewAvailable = { pv ->
                activePreviewView = pv
                if (uiState.isInitialized && uiState.selectedLens != null) {
                    viewModel.bindCamera(lifecycleOwner, pv)
                }
            },
            onTapToFocus = { x, y, factory ->
                viewModel.tapToFocus(factory, x, y)
            },
            onZoomChange = { zoom ->
                viewModel.setZoomRatio(zoom)
            },
            onExposureChange = { index ->
                viewModel.setExposureCompensation(index)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Action Bar Overlay with Status Bar Insets
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            TopCameraActionBar(
                flashMode = uiState.flashMode,
                timerDuration = uiState.timerDuration,
                onCycleFlash = { viewModel.cycleFlashMode() },
                onCycleTimer = { viewModel.cycleTimerDuration() },
                onOpenAbout = { viewModel.toggleAboutScreen(true) },
                onOpenSettings = { viewModel.toggleSettingsDialog(true) }
            )

            // Video Mode Top Bar
            if (uiState.captureMode == CaptureMode.VIDEO) {
                val isRecording = uiState.videoRecordingState is VideoRecordingState.RecordingActive
                VideoControlsTopBar(
                    selectedQuality = settings.videoQuality,
                    targetFps = settings.targetFps,
                    isStabilizationOn = settings.isVideoStabilizationEnabled,
                    isTorchOn = sessionState.isTorchOn,
                    isAudioOn = settings.isAudioEnabled,
                    isRecording = isRecording,
                    onToggleQuality = {
                        val next = when (settings.videoQuality) {
                            VideoQualityOption.SD -> VideoQualityOption.HD
                            VideoQualityOption.HD -> VideoQualityOption.FHD
                            VideoQualityOption.FHD -> VideoQualityOption.UHD
                            VideoQualityOption.UHD -> VideoQualityOption.SD
                        }
                        viewModel.settingsRepo.updateVideoQuality(next)
                    },
                    onToggleFps = {
                        val next = if (settings.targetFps == 30) 60 else 30
                        viewModel.settingsRepo.updateTargetFps(next)
                    },
                    onToggleStabilization = {
                        viewModel.settingsRepo.updateVideoStabilization(!settings.isVideoStabilizationEnabled)
                    },
                    onToggleTorch = {
                        val nextTorch = !sessionState.isTorchOn
                        viewModel.cameraController.applyFlashMode(
                            if (nextTorch) FlashModeOption.TORCH else FlashModeOption.OFF
                        )
                    },
                    onToggleAudio = {
                        viewModel.settingsRepo.updateAudioEnabled(!settings.isAudioEnabled)
                    }
                )
            }

            // Video Active Recording Indicator
            if (uiState.videoRecordingState is VideoRecordingState.RecordingActive) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    VideoRecordingActiveBanner(
                        recordingState = uiState.videoRecordingState,
                        onPauseResume = { viewModel.pauseResumeVideo() }
                    )
                }
            }
        }

        // Lens Badges Selector (0.5x, 1x, 2x, Macro, Front)
        LensSelectorChips(
            lensBadges = uiState.dynamicLensBadges,
            selectedLens = uiState.selectedLens,
            onSelectLens = { badge ->
                activePreviewView?.let { pv ->
                    viewModel.selectLens(badge, lifecycleOwner, pv)
                }
            },
            onFlipCamera = {
                activePreviewView?.let { pv ->
                    viewModel.flipCamera(lifecycleOwner, pv)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 195.dp)
        )

        // Pro Controls Panel (when PRO mode is active)
        ProControlsPanel(
            visible = uiState.captureMode == CaptureMode.PRO,
            isManualSensorSupported = uiState.selectedLens?.let { true } ?: false,
            proSettings = uiState.proSettings,
            onUpdateSettings = { viewModel.updateProSettings(it) },
            onResetPro = { viewModel.resetProSettings() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 145.dp)
        )

        // Bottom Controls Section (Mode selector, Shutter, Gallery Thumbnail, Flip)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .navigationBarsPadding()
                .padding(bottom = 14.dp)
        ) {
            // Mode Selector Bar (PHOTO, DOCS, VIDEO, PRO, DUAL)
            ModeSelectorBar(
                currentMode = uiState.captureMode,
                isDualSupported = uiState.isDualSupported,
                onSelectMode = { mode ->
                    activePreviewView?.let { pv ->
                        viewModel.selectMode(mode, lifecycleOwner, pv)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Shutter Row: Gallery Thumbnail | Shutter Button | Flip Camera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Gallery Thumbnail
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF22252E))
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable {
                            openDefaultGalleryApp(context, uiState.lastCapturedMediaItem?.uri)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.lastCapturedMediaItem != null) {
                        AsyncImage(
                            model = uiState.lastCapturedMediaItem!!.uri,
                            contentDescription = "Recent Capture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = Color.LightGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 2. Shutter Button
                ShutterButton(
                    captureMode = uiState.captureMode,
                    isRecording = uiState.videoRecordingState is VideoRecordingState.RecordingActive,
                    isCapturing = uiState.isCapturingPhoto,
                    onClick = { viewModel.triggerShutter() }
                )

                // 3. Quick Flip Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22252E))
                        .clickable {
                            activePreviewView?.let { pv ->
                                viewModel.flipCamera(lifecycleOwner, pv)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Timer Countdown Overlay
        TimerCountdownOverlay(secondsRemaining = uiState.timerRemainingSeconds)

        // Settings Dialog Sheet
        if (uiState.isSettingsOpen) {
            SettingsDialog(
                settings = settings,
                onDismiss = { viewModel.toggleSettingsDialog(false) },
                onUpdateGridType = { viewModel.settingsRepo.updateGridType(it) },
                onUpdateGeotag = { viewModel.settingsRepo.updateGeotagEnabled(it) },
                onUpdateShutterSound = { viewModel.settingsRepo.updateShutterSoundEnabled(it) },
                onUpdateSaveOriginal = { viewModel.settingsRepo.updateSaveOriginalEnabled(it) },
                onUpdateRawCapture = { viewModel.settingsRepo.updateRawCaptureEnabled(it) },
                onUpdateVideoQuality = { viewModel.settingsRepo.updateVideoQuality(it) },
                onUpdateTargetFps = { viewModel.settingsRepo.updateTargetFps(it) },
                onUpdateVideoStabilization = { viewModel.settingsRepo.updateVideoStabilization(it) },
                onUpdateAudio = { viewModel.settingsRepo.updateAudioEnabled(it) }
            )
        }
    }
}

@Composable
private fun TopCameraActionBar(
    flashMode: FlashModeOption,
    timerDuration: TimerDuration,
    onCycleFlash: () -> Unit,
    onCycleTimer: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flash Mode Icon
        Surface(color = Color.Black.copy(alpha = 0.5f), shape = CircleShape) {
            IconButton(onClick = onCycleFlash, modifier = Modifier.size(38.dp)) {
                val icon = when (flashMode) {
                    FlashModeOption.OFF -> Icons.Default.FlashOff
                    FlashModeOption.AUTO -> Icons.Default.FlashAuto
                    FlashModeOption.ON -> Icons.Default.FlashOn
                    FlashModeOption.TORCH -> Icons.Default.Highlight
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Flash",
                    tint = if (flashMode != FlashModeOption.OFF) Color(0xFFFFD600) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Timer Icon
        Surface(color = Color.Black.copy(alpha = 0.5f), shape = CircleShape) {
            IconButton(onClick = onCycleTimer, modifier = Modifier.size(38.dp)) {
                val icon = when (timerDuration) {
                    TimerDuration.OFF -> Icons.Default.Timer
                    TimerDuration.SEC_3 -> Icons.Default.Timer3
                    TimerDuration.SEC_10 -> Icons.Default.Timer10
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Timer",
                    tint = if (timerDuration != TimerDuration.OFF) Color(0xFFFFD600) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // "About Camera" Specs Icon
        Surface(color = Color.Black.copy(alpha = 0.5f), shape = CircleShape) {
            IconButton(onClick = onOpenAbout, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About Camera Specs",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Settings Icon
        Surface(color = Color.Black.copy(alpha = 0.5f), shape = CircleShape) {
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ShutterButton(
    captureMode: CaptureMode,
    isRecording: Boolean,
    isCapturing: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isCapturing) 0.88f else 1.0f,
        label = "shutterScale"
    )

    Box(
        modifier = Modifier
            .size(76.dp)
            .scale(scale)
            .clip(CircleShape)
            .border(3.5.dp, Color.White, CircleShape)
            .clickable { onClick() }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (captureMode == CaptureMode.VIDEO) {
            if (isRecording) {
                // Red square stop indicator
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFF3B30))
                )
            } else {
                // Red recording circle
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                )
            }
        } else if (captureMode == CaptureMode.DOCUMENT) {
            // Document Scanner yellow accent shutter with document icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFFFFD600)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Scan Document",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            // White Photo Shutter
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

private fun openDefaultGalleryApp(context: Context, targetUri: Uri?) {
    try {
        if (targetUri != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(targetUri, context.contentResolver.getType(targetUri) ?: "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }
    } catch (_: Exception) { }

    try {
        val galleryIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_GALLERY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(galleryIntent)
    } catch (_: Exception) {
        try {
            val viewAllIntent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewAllIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "No gallery app installed", Toast.LENGTH_SHORT).show()
        }
    }
}

package com.aperture.camera.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import androidx.camera.core.MeteringPointFactory
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.aperture.camera.camera.CameraHardwareDetector
import com.aperture.camera.camera.CameraManagerController
import com.aperture.camera.camera.DeviceHardwareDetector
import com.aperture.camera.camera.ProSettingsState
import com.aperture.camera.camera.VideoRecordingState
import com.aperture.camera.data.model.CameraSettings
import com.aperture.camera.data.model.CameraSpec
import com.aperture.camera.data.model.CaptureMode
import com.aperture.camera.data.model.FlashModeOption
import com.aperture.camera.data.model.FullDeviceDiagnostics
import com.aperture.camera.data.model.LensBadge
import com.aperture.camera.data.model.MediaItem
import com.aperture.camera.data.model.TimerDuration
import com.aperture.camera.data.repository.MediaStoreRepository
import com.aperture.camera.data.repository.SettingsRepository
import com.aperture.camera.location.LocationProviderHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CameraUiState(
    val isPermissionsGranted: Boolean = false,
    val isInitialized: Boolean = false,
    val specs: List<CameraSpec> = emptyList(),
    val dynamicLensBadges: List<LensBadge> = emptyList(),
    val selectedLens: LensBadge? = null,
    val captureMode: CaptureMode = CaptureMode.PHOTO,
    val flashMode: FlashModeOption = FlashModeOption.OFF,
    val timerDuration: TimerDuration = TimerDuration.OFF,
    val timerRemainingSeconds: Int = 0,
    val isCapturingPhoto: Boolean = false,
    val lastCapturedThumbnail: Bitmap? = null,
    val lastCapturedMediaItem: MediaItem? = null,
    val viewingMediaItem: MediaItem? = null,
    val proSettings: ProSettingsState = ProSettingsState(),
    val videoRecordingState: VideoRecordingState = VideoRecordingState.Idle,
    val isSettingsOpen: Boolean = false,
    val isAboutOpen: Boolean = false,
    val isDualSupported: Boolean = false,
    val statusToastMessage: String? = null,
    val fullDiagnostics: FullDeviceDiagnostics? = null,
    val documentScanBitmap: Bitmap? = null,
    val isDocumentEditorOpen: Boolean = false
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val mediaStoreRepo = MediaStoreRepository(application)
    val settingsRepo = SettingsRepository(application)
    val hardwareDetector = CameraHardwareDetector(application)
    val deviceHardwareDetector = DeviceHardwareDetector(application)
    val cameraController = CameraManagerController(application, mediaStoreRepo)
    val locationHelper = LocationProviderHelper(application)

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    val settings: StateFlow<CameraSettings> = settingsRepo.settings
    val sessionState = cameraController.sessionState

    private var countdownJob: Job? = null

    init {
        loadLatestMedia()
    }

    fun onPermissionsGranted() {
        _uiState.value = _uiState.value.copy(isPermissionsGranted = true)
        locationHelper.startLocationUpdates()
        initializeCameraHardware()
    }

    private fun initializeCameraHardware() {
        viewModelScope.launch(Dispatchers.Default) {
            val specs = hardwareDetector.detectAllCameras()
            val badges = hardwareDetector.buildDynamicLensBadges(specs)
            val diagnostics = deviceHardwareDetector.getFullDiagnostics(specs)

            val defaultLens = badges.firstOrNull { !it.isFront && it.label == "1x" }
                ?: badges.firstOrNull { !it.isFront }
                ?: badges.firstOrNull()

            cameraController.initialize { provider ->
                val dualSupported = cameraController.concurrentCameraManager.isConcurrentSupported(provider)
                _uiState.value = _uiState.value.copy(
                    isInitialized = true,
                    specs = specs,
                    dynamicLensBadges = badges,
                    selectedLens = defaultLens,
                    isDualSupported = dualSupported,
                    fullDiagnostics = diagnostics
                )
            }
        }
    }

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val lens = _uiState.value.selectedLens ?: return
        val mode = _uiState.value.captureMode
        val videoQuality = settings.value.videoQuality

        cameraController.bindCameraUseCases(
            lifecycleOwner = lifecycleOwner,
            surfaceProvider = previewView.surfaceProvider,
            badge = lens,
            captureMode = mode,
            videoQuality = videoQuality
        )

        // If in Pro mode, apply pro settings
        if (mode == CaptureMode.PRO) {
            cameraController.proModeController.applyProSettings(
                cameraControl = cameraController.currentCamera?.cameraControl,
                settings = _uiState.value.proSettings
            )
        }
    }

    fun selectLens(badge: LensBadge, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (_uiState.value.videoRecordingState is VideoRecordingState.RecordingActive) {
            return
        }
        val prevBadge = _uiState.value.selectedLens
        _uiState.value = _uiState.value.copy(selectedLens = badge)

        val isFacingChanged = prevBadge?.isFront != badge.isFront
        val isLensTypeChanged = prevBadge?.lensType != badge.lensType
        val isPhysicalChanged = prevBadge?.physicalCameraId != badge.physicalCameraId
        val isCameraActive = cameraController.currentCamera != null

        if (isFacingChanged || isLensTypeChanged || isPhysicalChanged || !isCameraActive) {
            bindCamera(lifecycleOwner, previewView)
        } else {
            cameraController.applyLensBadge(badge)
        }
    }

    fun flipCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val badges = _uiState.value.dynamicLensBadges
        val current = _uiState.value.selectedLens

        val targetBadge = if (current?.isFront == true) {
            badges.firstOrNull { !it.isFront && it.label == "1x" }
                ?: badges.firstOrNull { !it.isFront }
        } else {
            badges.firstOrNull { it.isFront }
        }

        targetBadge?.let {
            selectLens(it, lifecycleOwner, previewView)
        }
    }

    fun selectMode(mode: CaptureMode, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (_uiState.value.videoRecordingState is VideoRecordingState.RecordingActive) {
            return
        }
        _uiState.value = _uiState.value.copy(captureMode = mode)
        bindCamera(lifecycleOwner, previewView)
    }

    fun cycleFlashMode() {
        val current = _uiState.value.flashMode
        val next = when (current) {
            FlashModeOption.OFF -> FlashModeOption.AUTO
            FlashModeOption.AUTO -> FlashModeOption.ON
            FlashModeOption.ON -> FlashModeOption.TORCH
            FlashModeOption.TORCH -> FlashModeOption.OFF
        }
        _uiState.value = _uiState.value.copy(flashMode = next)
        cameraController.applyFlashMode(next)
    }

    fun cycleTimerDuration() {
        val current = _uiState.value.timerDuration
        val next = when (current) {
            TimerDuration.OFF -> TimerDuration.SEC_3
            TimerDuration.SEC_3 -> TimerDuration.SEC_10
            TimerDuration.SEC_10 -> TimerDuration.OFF
        }
        _uiState.value = _uiState.value.copy(timerDuration = next)
    }

    fun triggerShutter() {
        when (_uiState.value.captureMode) {
            CaptureMode.PHOTO, CaptureMode.PRO -> {
                val timerSec = _uiState.value.timerDuration.seconds
                if (timerSec > 0) {
                    startCountdown(timerSec) { executePhotoCapture() }
                } else {
                    executePhotoCapture()
                }
            }
            CaptureMode.DOCUMENT -> {
                executeDocumentCapture()
            }
            CaptureMode.VIDEO -> {
                toggleVideoRecording()
            }
            CaptureMode.DUAL -> {
                executePhotoCapture()
            }
        }
    }

    private fun startCountdown(seconds: Int, onComplete: () -> Unit) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _uiState.value = _uiState.value.copy(timerRemainingSeconds = i)
                delay(1000)
            }
            _uiState.value = _uiState.value.copy(timerRemainingSeconds = 0)
            onComplete()
        }
    }

    private fun executePhotoCapture() {
        _uiState.value = _uiState.value.copy(isCapturingPhoto = true)
        viewModelScope.launch {
            val location = if (settings.value.isGeotagEnabled) locationHelper.getCurrentLocation() else null

            cameraController.takePhoto(
                location = location,
                onPhotoCaptured = { uri, bitmap ->
                    _uiState.value = _uiState.value.copy(
                        isCapturingPhoto = false,
                        lastCapturedThumbnail = bitmap,
                        statusToastMessage = "Photo saved to DCIM/Camera"
                    )
                    loadLatestMedia()
                },
                onError = { err ->
                    _uiState.value = _uiState.value.copy(
                        isCapturingPhoto = false,
                        statusToastMessage = "Capture failed: ${err.message}"
                    )
                }
            )
        }
    }

    private fun executeDocumentCapture() {
        _uiState.value = _uiState.value.copy(isCapturingPhoto = true)
        cameraController.captureDocumentBitmap(
            onBitmapCaptured = { bitmap ->
                _uiState.value = _uiState.value.copy(
                    isCapturingPhoto = false,
                    documentScanBitmap = bitmap,
                    isDocumentEditorOpen = true
                )
            },
            onError = { err ->
                _uiState.value = _uiState.value.copy(
                    isCapturingPhoto = false,
                    statusToastMessage = "Scan capture failed: ${err.message}"
                )
            }
        )
    }

    fun saveDocumentPdf(pdfBytes: ByteArray, displayName: String) {
        viewModelScope.launch {
            val uri = mediaStoreRepo.savePdfDocument(pdfBytes, displayName)
            if (uri != null) {
                _uiState.value = _uiState.value.copy(
                    isDocumentEditorOpen = false,
                    documentScanBitmap = null,
                    statusToastMessage = "PDF Document saved to Documents/ApertureScanner"
                )
            } else {
                _uiState.value = _uiState.value.copy(statusToastMessage = "Failed to save PDF document")
            }
        }
    }

    fun saveDocumentImage(jpegBytes: ByteArray, displayName: String) {
        viewModelScope.launch {
            val uri = mediaStoreRepo.saveDocumentImage(jpegBytes, displayName)
            if (uri != null) {
                _uiState.value = _uiState.value.copy(
                    isDocumentEditorOpen = false,
                    documentScanBitmap = null,
                    statusToastMessage = "Scanned image saved to DCIM/Camera"
                )
                loadLatestMedia()
            } else {
                _uiState.value = _uiState.value.copy(statusToastMessage = "Failed to save scanned image")
            }
        }
    }

    fun closeDocumentEditor() {
        _uiState.value = _uiState.value.copy(
            isDocumentEditorOpen = false,
            documentScanBitmap = null
        )
    }

    private fun toggleVideoRecording() {
        val state = _uiState.value.videoRecordingState
        if (state is VideoRecordingState.RecordingActive) {
            stopVideoRecording()
        } else {
            startVideoRecording()
        }
    }

    fun startVideoRecording() {
        val recorder = cameraController.videoRecorderManager.currentRecorder ?: return
        val isAudio = settings.value.isAudioEnabled

        cameraController.videoRecorderManager.startRecording(
            recorder = recorder,
            enableAudio = isAudio,
            onEvent = { recordingState ->
                _uiState.value = _uiState.value.copy(videoRecordingState = recordingState)
                if (recordingState is VideoRecordingState.Finalized) {
                    _uiState.value = _uiState.value.copy(statusToastMessage = "Video saved to DCIM/Camera")
                    loadLatestMedia()
                }
            }
        )
    }

    fun stopVideoRecording() {
        cameraController.videoRecorderManager.stopRecording()
    }

    fun pauseVideoRecording() {
        cameraController.videoRecorderManager.pauseRecording()
    }

    fun resumeVideoRecording() {
        cameraController.videoRecorderManager.resumeRecording()
    }

    fun pauseResumeVideo() {
        val state = _uiState.value.videoRecordingState
        if (state is VideoRecordingState.RecordingActive) {
            if (state.isPaused) {
                resumeVideoRecording()
            } else {
                pauseVideoRecording()
            }
        }
    }

    fun updateProSettings(newSettings: ProSettingsState) {
        _uiState.value = _uiState.value.copy(proSettings = newSettings)
        cameraController.proModeController.applyProSettings(
            cameraControl = cameraController.currentCamera?.cameraControl,
            settings = newSettings
        )
    }

    fun updateProSettings(update: (ProSettingsState) -> ProSettingsState) {
        val updated = update(_uiState.value.proSettings)
        _uiState.value = _uiState.value.copy(proSettings = updated)
        cameraController.proModeController.applyProSettings(
            cameraControl = cameraController.currentCamera?.cameraControl,
            settings = updated
        )
    }

    fun resetProSettings() {
        val reset = ProSettingsState()
        _uiState.value = _uiState.value.copy(proSettings = reset)
        cameraController.proModeController.applyProSettings(
            cameraControl = cameraController.currentCamera?.cameraControl,
            settings = reset
        )
    }

    fun tapToFocus(meteringPointFactory: MeteringPointFactory, x: Float, y: Float) {
        cameraController.tapToFocus(meteringPointFactory, x, y)
    }

    fun setZoomRatio(ratio: Float) {
        cameraController.setZoomRatio(ratio)
    }

    fun setExposureCompensation(index: Int) {
        cameraController.setExposureCompensation(index)
    }

    fun openMediaViewer(item: MediaItem) {
        _uiState.value = _uiState.value.copy(viewingMediaItem = item)
    }

    fun closeMediaViewer() {
        _uiState.value = _uiState.value.copy(viewingMediaItem = null)
    }

    fun deleteMedia(item: MediaItem) {
        viewModelScope.launch {
            mediaStoreRepo.deleteMediaItem(item.uri)
            closeMediaViewer()
            loadLatestMedia()
        }
    }

    fun loadLatestMedia() {
        viewModelScope.launch {
            val item = mediaStoreRepo.getLatestMediaItem()
            _uiState.value = _uiState.value.copy(lastCapturedMediaItem = item)
        }
    }

    fun toggleSettingsDialog(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isSettingsOpen = isOpen)
    }

    fun toggleAboutScreen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isAboutOpen = isOpen)
    }

    fun clearStatusToast() {
        _uiState.value = _uiState.value.copy(statusToastMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        locationHelper.stopLocationUpdates()
        cameraController.release()
    }
}

package com.aperture.camera.data.model

import android.net.Uri

enum class GridType(val title: String) {
    NONE("Off"),
    RULE_OF_THIRDS("3 x 3"),
    GOLDEN_RATIO("Golden"),
    SQUARE("Square 1:1")
}

enum class TimerDuration(val seconds: Int, val title: String) {
    OFF(0, "Off"),
    SEC_3(3, "3s"),
    SEC_10(10, "10s")
}

enum class VideoQualityOption(val title: String) {
    SD("SD 480p"),
    HD("HD 720p"),
    FHD("FHD 1080p"),
    UHD("4K UHD")
}

enum class FlashModeOption {
    OFF,
    AUTO,
    ON,
    TORCH
}

data class CameraSettings(
    val defaultLensId: String = "",
    val gridType: GridType = GridType.NONE,
    val isGeotagEnabled: Boolean = true,
    val isShutterSoundEnabled: Boolean = true,
    val isSaveOriginalEnabled: Boolean = true,
    val isRawCaptureEnabled: Boolean = true,
    val videoQuality: VideoQualityOption = VideoQualityOption.UHD,
    val targetFps: Int = 60,
    val isVideoStabilizationEnabled: Boolean = true,
    val isAudioEnabled: Boolean = true
)

data class MediaItem(
    val uri: Uri,
    val name: String,
    val isVideo: Boolean,
    val dateAdded: Long,
    val sizeBytes: Long,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L
)

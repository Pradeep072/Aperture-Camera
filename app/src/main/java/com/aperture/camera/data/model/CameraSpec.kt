package com.aperture.camera.data.model

import android.util.Range
import android.util.Size

data class ResolutionInfo(
    val width: Int,
    val height: Int
) {
    val megaPixels: Double
        get() = (width * height) / 1_000_000.0

    val formatted: String
        get() = "$width x $height (%.1f MP)".format(megaPixels)
}

data class VideoFormatInfo(
    val size: Size,
    val maxFps: Int,
    val fpsRanges: List<Range<Int>> = emptyList()
)

data class CameraSpec(
    val cameraId: String,
    val isPhysical: Boolean,
    val parentLogicalId: String? = null,
    val physicalCameraIds: List<String> = emptyList(),
    val facing: String, // "Back", "Front", "External"
    val isFront: Boolean,
    val lensType: LensType,
    val hardwareLevel: String, // "LEGACY", "LIMITED", "FULL", "LEVEL_3", "EXTERNAL"
    val focalLengths: List<Float>,
    val focalLength35mm: Float,
    val apertures: List<Float>,
    val sensorPhysicalSize: String, // e.g. "6.40 x 4.80 mm"
    val sensorPixelArraySize: String, // e.g. "4032 x 3024"
    val maxPixelArraySize: String? = null, // e.g. "8160 x 6144"
    val sensorMegaPixels: Double = 0.0, // Primary advertised / physical MP (e.g. 50.0 MP)
    val binnedOutputMegaPixels: Double = 0.0, // Default pixel-binned output MP (e.g. 12.5 MP)
    val isQuadBayer: Boolean = false,
    val claimedAdvertisedTier: String = "", // e.g. "50 MP / 100 MP Class (Multi-Pixel Matrix)"
    val pixelBinningTechnology: String = "", // e.g. "4-in-1 Quad-Bayer / 9-in-1 Nonacell Super-Pixel Fusion"
    val fovHorizontal: Float = 0f,
    val fovVertical: Float = 0f,
    val cropFactor: Float,
    val maxDigitalZoom: Float,
    val zoomRatioRange: Range<Float>?,
    val hasFlash: Boolean,
    val hasOis: Boolean,
    val hasEis: Boolean,
    val autoFocusModes: List<String>,
    val minFocusDistanceDiopters: Float,
    val minFocusDistanceCm: Float?, // calculated from diopters
    val isoRange: Range<Int>?,
    val exposureTimeRangeNs: Range<Long>?,
    val exposureCompensationRange: Range<Int>?,
    val exposureCompensationStep: Float,
    val capabilities: List<String>,
    val isRawSupported: Boolean,
    val isManualSensorSupported: Boolean,
    val isLogicalMultiCamera: Boolean,
    val isConcurrentSupported: Boolean,
    val concurrentPairIds: List<String> = emptyList(),
    val is10BitHdrSupported: Boolean,
    val photoResolutions: List<ResolutionInfo> = emptyList(),
    val rawResolutions: List<ResolutionInfo> = emptyList(),
    val videoFormats: List<VideoFormatInfo> = emptyList(),
    val highSpeedVideoFormats: List<VideoFormatInfo> = emptyList()
)

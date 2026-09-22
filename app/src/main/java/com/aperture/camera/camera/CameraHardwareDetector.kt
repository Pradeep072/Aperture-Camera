package com.aperture.camera.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.aperture.camera.data.model.CameraSpec
import com.aperture.camera.data.model.LensBadge
import com.aperture.camera.data.model.LensType
import com.aperture.camera.data.model.ResolutionInfo
import com.aperture.camera.data.model.VideoFormatInfo
import java.util.Locale
import kotlin.math.atan
import kotlin.math.hypot

class CameraHardwareDetector(private val context: Context) {

    private val tag = "CameraHardwareDetector"

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /**
     * Inspects the entire device camera subsystem and builds comprehensive specs
     * for every logical camera and physical camera sensor.
     */
    fun detectAllCameras(): List<CameraSpec> {
        val specs = mutableListOf<CameraSpec>()
        val concurrentSets = getConcurrentCameraSets()
        val processedCameraIds = mutableSetOf<String>()

        // 1. Enumerate standard logical camera IDs
        try {
            val logicalIds = cameraManager.cameraIdList
            Log.d(tag, "Discovered logical camera IDs: ${logicalIds.joinToString()}")

            for (logicalId in logicalIds) {
                try {
                    val chars = cameraManager.getCameraCharacteristics(logicalId)
                    val concurrentWith = concurrentSets.filter { it.contains(logicalId) }
                        .flatten()
                        .filter { it != logicalId }
                        .distinct()

                    val logicalSpec = buildCameraSpec(
                        cameraId = logicalId,
                        isPhysical = false,
                        parentLogicalId = null,
                        characteristics = chars,
                        concurrentPairIds = concurrentWith
                    )
                    specs.add(logicalSpec)
                    processedCameraIds.add(logicalId)
                    Log.d(tag, "Added logical camera #$logicalId (${logicalSpec.facing} ${logicalSpec.lensType})")

                    // Inspect physical cameras behind logical multi-cameras (API 28+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        try {
                            val physicalIds = chars.physicalCameraIds
                            Log.d(tag, "Logical camera #$logicalId physical IDs: ${physicalIds.joinToString()}")
                            for (physId in physicalIds) {
                                if (physId in processedCameraIds) continue
                                try {
                                    val physChars = cameraManager.getCameraCharacteristics(physId)
                                    val physSpec = buildCameraSpec(
                                        cameraId = physId,
                                        isPhysical = true,
                                        parentLogicalId = logicalId,
                                        characteristics = physChars,
                                        concurrentPairIds = emptyList()
                                    )
                                    specs.add(physSpec)
                                    processedCameraIds.add(physId)
                                    Log.d(tag, "Added physical sub-camera #$physId for logical #$logicalId")
                                } catch (pe: Exception) {
                                    Log.w(tag, "Could not query physical camera #$physId: ${pe.message}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(tag, "Physical cameras inspection skipped for #$logicalId: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to inspect logical camera #$logicalId: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to list camera IDs: ${e.message}")
        }

        // 2. Probe for hidden physical / auxiliary camera IDs that OEMs omit from cameraIdList (e.g. IDs 2..8)
        val probeIds = (0..8).map { it.toString() }.filter { it !in processedCameraIds }
        for (probeId in probeIds) {
            try {
                val probeChars = cameraManager.getCameraCharacteristics(probeId)
                val facingInt = probeChars.get(CameraCharacteristics.LENS_FACING)
                if (facingInt != null) {
                    val isFront = facingInt == CameraCharacteristics.LENS_FACING_FRONT
                    val parentId = if (isFront) "1" else "0"
                    val probeSpec = buildCameraSpec(
                        cameraId = probeId,
                        isPhysical = true,
                        parentLogicalId = parentId,
                        characteristics = probeChars,
                        concurrentPairIds = emptyList()
                    )
                    specs.add(probeSpec)
                    processedCameraIds.add(probeId)
                    Log.d(tag, "Discovered auxiliary/physical camera #$probeId (${probeSpec.facing} ${probeSpec.lensType})")
                }
            } catch (_: Exception) {
                // Ignore IDs that do not exist
            }
        }

        // 3. Multi-lens logical camera decomposition:
        // If rear logical camera has multiple focal lengths (e.g. Ultra-wide 2.2mm + Main 5.56mm)
        // and no separate physical specs were discovered via physicalCameraIds or probing:
        val backSpecs = specs.filter { !it.isFront }
        val mainLogicalBack = backSpecs.firstOrNull { !it.isPhysical }
        val physicalBackSpecs = backSpecs.filter { it.isPhysical }

        if (mainLogicalBack != null && physicalBackSpecs.isEmpty() && mainLogicalBack.focalLengths.size > 1) {
            val primaryFocal = mainLogicalBack.focalLengths.maxOrNull() ?: 5.0f
            val altFocals = mainLogicalBack.focalLengths.filter { it != primaryFocal }

            for ((idx, altFocal) in altFocals.withIndex()) {
                val crop = mainLogicalBack.cropFactor
                val eq35 = altFocal * crop
                val isUw = eq35 < 24f
                val lensType = if (isUw) LensType.ULTRA_WIDE else LensType.TELEPHOTO

                val synthSpec = mainLogicalBack.copy(
                    cameraId = "${mainLogicalBack.cameraId}.${idx + 1}",
                    isPhysical = true,
                    parentLogicalId = mainLogicalBack.cameraId,
                    lensType = lensType,
                    focalLengths = listOf(altFocal),
                    focalLength35mm = eq35,
                    apertures = if (isUw) mainLogicalBack.apertures.filter { it >= 2.0f }.ifEmpty { mainLogicalBack.apertures } else mainLogicalBack.apertures
                )
                specs.add(synthSpec)
                Log.d(tag, "Synthesized rear lens spec #${synthSpec.cameraId} ($lensType) with focal ${altFocal}mm")
            }
        }

        // 4. Post-processing: Link physical rear sensor IDs to Logical Camera #0
        val physicalBack = specs.filter { it.isPhysical && !it.isFront }
        val physicalBackIds = physicalBack.map { it.cameraId }

        if (physicalBackIds.isNotEmpty()) {
            val logicalZeroIdx = specs.indexOfFirst { it.cameraId == "0" }
            if (logicalZeroIdx != -1) {
                val orig = specs[logicalZeroIdx]
                specs[logicalZeroIdx] = orig.copy(
                    isPhysical = false,
                    isLogicalMultiCamera = true,
                    physicalCameraIds = if (orig.physicalCameraIds.isEmpty()) physicalBackIds else orig.physicalCameraIds
                )
            }
        }

        return specs
    }

    /**
     * Builds dynamic lens badges for the viewfinder based on detected lenses.
     * Exposes verified, rock-solid options: [1x], [2x], [Front].
     */
    fun buildDynamicLensBadges(specs: List<CameraSpec>): List<LensBadge> {
        val badges = mutableListOf<LensBadge>()

        // 1. Back Cameras
        val backSpecs = specs.filter { !it.isFront }
        val mainLogicalBack = backSpecs.firstOrNull { !it.isPhysical } ?: backSpecs.firstOrNull()
        val logicalCameraId = mainLogicalBack?.cameraId ?: "0"

        val baseFocal35 = mainLogicalBack?.focalLength35mm?.takeIf { it > 0f } ?: 26f
        val primaryFocalMm = mainLogicalBack?.focalLengths?.firstOrNull() ?: 5.0f
        val mainAperture = mainLogicalBack?.apertures?.firstOrNull() ?: 1.8f

        // 1x Main Wide Badge
        badges.add(
            LensBadge(
                cameraId = logicalCameraId,
                physicalCameraId = null,
                isPhysical = false,
                parentLogicalId = null,
                lensType = LensType.WIDE,
                label = "1x",
                focalLength35mm = baseFocal35,
                physicalFocalLengthMm = primaryFocalMm,
                aperture = mainAperture,
                zoomRatio = 1.0f,
                isMacro = false,
                isFront = false
            )
        )

        // 2x Zoom Badge
        badges.add(
            LensBadge(
                cameraId = logicalCameraId,
                physicalCameraId = null,
                isPhysical = false,
                parentLogicalId = null,
                lensType = LensType.TELEPHOTO,
                label = "2x",
                focalLength35mm = baseFocal35 * 2.0f,
                physicalFocalLengthMm = primaryFocalMm,
                aperture = mainAperture,
                zoomRatio = 2.0f,
                isMacro = false,
                isFront = false
            )
        )

        // 2. Front Camera
        val frontSpec = specs.firstOrNull { it.isFront && !it.isPhysical }
            ?: specs.firstOrNull { it.isFront }
        val frontId = frontSpec?.cameraId ?: "1"
        val frontFocal = frontSpec?.focalLength35mm ?: 24f
        val frontFocalMm = frontSpec?.focalLengths?.firstOrNull() ?: 3.0f

        badges.add(
            LensBadge(
                cameraId = frontId,
                physicalCameraId = null,
                isPhysical = false,
                parentLogicalId = null,
                lensType = LensType.FRONT,
                label = "Front",
                focalLength35mm = frontFocal,
                physicalFocalLengthMm = frontFocalMm,
                aperture = frontSpec?.apertures?.firstOrNull() ?: 2.3f,
                zoomRatio = 1.0f,
                isMacro = false,
                isFront = true
            )
        )

        return badges
    }

    private fun getConcurrentCameraSets(): Set<Set<String>> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                return cameraManager.concurrentCameraIds
            } catch (_: Exception) { }
        }
        return emptySet()
    }

    private fun buildCameraSpec(
        cameraId: String,
        isPhysical: Boolean,
        parentLogicalId: String?,
        characteristics: CameraCharacteristics,
        concurrentPairIds: List<String>
    ): CameraSpec {
        // Lens Facing
        val facingInt = characteristics.get(CameraCharacteristics.LENS_FACING)
        val facing = when (facingInt) {
            CameraCharacteristics.LENS_FACING_FRONT -> "Front"
            CameraCharacteristics.LENS_FACING_BACK -> "Back"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
            else -> "Unknown"
        }
        val isFront = facingInt == CameraCharacteristics.LENS_FACING_FRONT

        // Hardware level
        val hwLevelInt = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        val hardwareLevel = when (hwLevelInt) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
            else -> "UNKNOWN"
        }

        // Focal Lengths & Apertures
        val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
        val apertures = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.toList() ?: emptyList()

        // Sensor Physical Size & Pixel Array
        val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val pixelArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val sensorSizeStr = if (sensorSize != null) "%.2f x %.2f mm".format(sensorSize.width, sensorSize.height) else "Unknown"
        val pixelArrayStr = if (pixelArray != null) "${pixelArray.width} x ${pixelArray.height}" else "Unknown"

        // Maximum Resolution Sensor Matrix (API 31+ for 50MP / 64MP / 100MP / 108MP / 200MP sensors)
        var maxPixelArray: android.util.Size? = null
        var maxPhotoSizes: List<ResolutionInfo> = emptyList()
        var maxRawSizes: List<ResolutionInfo> = emptyList()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                maxPixelArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION)
                if (maxPixelArray == null) {
                    val activeMaxRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION)
                    if (activeMaxRect != null && activeMaxRect.width() > 0 && activeMaxRect.height() > 0) {
                        maxPixelArray = android.util.Size(activeMaxRect.width(), activeMaxRect.height())
                    }
                }
                val maxMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
                val maxJpeg = maxMap?.getOutputSizes(ImageFormat.JPEG)?.map {
                    ResolutionInfo(it.width, it.height)
                } ?: emptyList()
                val maxHighResJpeg = maxMap?.getHighResolutionOutputSizes(ImageFormat.JPEG)?.map {
                    ResolutionInfo(it.width, it.height)
                } ?: emptyList()
                val maxRaw = maxMap?.getOutputSizes(ImageFormat.RAW_SENSOR)?.map {
                    ResolutionInfo(it.width, it.height)
                } ?: emptyList()
                val maxHighResRaw = maxMap?.getHighResolutionOutputSizes(ImageFormat.RAW_SENSOR)?.map {
                    ResolutionInfo(it.width, it.height)
                } ?: emptyList()

                maxPhotoSizes = (maxJpeg + maxHighResJpeg).distinctBy { "${it.width}x${it.height}" }
                maxRawSizes = (maxRaw + maxHighResRaw).distinctBy { "${it.width}x${it.height}" }
            } catch (_: Exception) { }
        }

        // 35mm Equivalent Focal Length & Crop Factor
        val sensorDiag = if (sensorSize != null && sensorSize.width > 0 && sensorSize.height > 0) {
            hypot(sensorSize.width.toDouble(), sensorSize.height.toDouble()).toFloat()
        } else {
            0f
        }
        val cropFactor = if (sensorDiag > 0f) 43.267f / sensorDiag else 1.0f
        val primaryFocal = focalLengths.firstOrNull() ?: 4.0f
        val focalLength35mm = primaryFocal * cropFactor

        // FOV calculations
        val fovH = if (sensorSize != null && primaryFocal > 0f) {
            (2f * atan(sensorSize.width / (2f * primaryFocal)) * (180f / Math.PI.toFloat()))
        } else 0f
        val fovV = if (sensorSize != null && primaryFocal > 0f) {
            (2f * atan(sensorSize.height / (2f * primaryFocal)) * (180f / Math.PI.toFloat()))
        } else 0f

        // Focus Distance
        val minFocusDiopters = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0.0f
        val minFocusCm = if (minFocusDiopters > 0f) (100.0f / minFocusDiopters) else null

        // Capabilities
        val caps = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
        val capStrings = mutableListOf<String>()
        var hasRaw = false
        var hasManual = false
        var hasLogical = false
        var hasDepth = false

        for (cap in caps) {
            when (cap) {
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW -> {
                    capStrings.add("RAW Capture")
                    hasRaw = true
                }
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> {
                    capStrings.add("Manual Sensor (Pro)")
                    hasManual = true
                }
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> capStrings.add("Manual Post-Processing")
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> capStrings.add("Burst Capture")
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA -> {
                    capStrings.add("Logical Multi-Camera")
                    hasLogical = true
                }
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> capStrings.add("High Speed Video (Slow-Mo)")
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> {
                    capStrings.add("Depth Output")
                    hasDepth = true
                }
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING -> capStrings.add("Private Reprocessing")
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> capStrings.add("YUV Reprocessing")
            }
        }

        // Stream Configuration Resolutions (including high-resolution output sizes API 23+)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val standardJpeg = try {
            map?.getOutputSizes(ImageFormat.JPEG)?.map {
                ResolutionInfo(it.width, it.height)
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        val highResJpeg = try {
            map?.getHighResolutionOutputSizes(ImageFormat.JPEG)?.map {
                ResolutionInfo(it.width, it.height)
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        val standardPhotoSizes = (standardJpeg + highResJpeg)
            .distinctBy { "${it.width}x${it.height}" }
            .sortedByDescending { it.width.toLong() * it.height.toLong() }

        val photoSizes = (maxPhotoSizes + standardPhotoSizes)
            .distinctBy { "${it.width}x${it.height}" }
            .sortedByDescending { it.width.toLong() * it.height.toLong() }

        val standardRaw = if (hasRaw) {
            try {
                val raw1 = map?.getOutputSizes(ImageFormat.RAW_SENSOR)?.map {
                    ResolutionInfo(it.width, it.height)
                } ?: emptyList()
                val rawHighRes = map?.getHighResolutionOutputSizes(ImageFormat.RAW_SENSOR)?.map {
                    ResolutionInfo(it.width, it.height)
                } ?: emptyList()
                (raw1 + rawHighRes).distinctBy { "${it.width}x${it.height}" }
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()

        val rawSizes = (maxRawSizes + standardRaw)
            .distinctBy { "${it.width}x${it.height}" }
            .sortedByDescending { it.width.toLong() * it.height.toLong() }

        val videoSizes = try {
            map?.getOutputSizes(MediaRecorder::class.java)?.map { size ->
                VideoFormatInfo(
                    size = size,
                    maxFps = 30,
                    fpsRanges = emptyList()
                )
            }?.sortedByDescending { it.size.width * it.size.height } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        // Safely query High Speed Video FPS ranges
        val highSpeedSizes = try {
            val hsSizes = map?.highSpeedVideoSizes?.toList() ?: emptyList()
            hsSizes.mapNotNull { size ->
                try {
                    val fpsRanges = map?.getHighSpeedVideoFpsRangesFor(size)?.toList() ?: emptyList()
                    val maxFps = fpsRanges.maxOfOrNull { it.upper } ?: 120
                    VideoFormatInfo(
                        size = size,
                        maxFps = maxFps,
                        fpsRanges = fpsRanges
                    )
                } catch (_: Exception) {
                    null
                }
            }.sortedByDescending { it.size.width * it.size.height }
        } catch (_: Exception) {
            emptyList()
        }

        // Megapixels (Standard Output vs Full Physical Hardware Array)
        val pixelArrayMp = if (pixelArray != null && pixelArray.width > 0 && pixelArray.height > 0) {
            (pixelArray.width.toLong() * pixelArray.height.toLong()) / 1_000_000.0
        } else {
            0.0
        }
        val activeArrayMp = if (activeArray != null && activeArray.width() > 0 && activeArray.height() > 0) {
            (activeArray.width().toLong() * activeArray.height().toLong()) / 1_000_000.0
        } else {
            0.0
        }

        val standardStreamMp = standardJpeg.maxOfOrNull { (it.width.toLong() * it.height.toLong()) / 1_000_000.0 } ?: 0.0
        val maxHardwareArrayMp = if (maxPixelArray != null && maxPixelArray.width > 0 && maxPixelArray.height > 0) {
            (maxPixelArray.width.toLong() * maxPixelArray.height.toLong()) / 1_000_000.0
        } else {
            0.0
        }
        val highestStreamMp = photoSizes.maxOfOrNull { (it.width.toLong() * it.height.toLong()) / 1_000_000.0 } ?: 0.0

        val standardMp = when {
            standardStreamMp > 0.0 -> standardStreamMp
            pixelArrayMp > 0.0 -> pixelArrayMp
            else -> activeArrayMp
        }

        val maxAvailableMp = maxOf(maxHardwareArrayMp, highestStreamMp, pixelArrayMp, activeArrayMp)
        val isExplicitHighRes = maxAvailableMp > (standardMp * 1.15)
        val calculatedMp = if (isExplicitHighRes) maxAvailableMp else standardMp

        val maxPixelArrayStr = when {
            maxPixelArray != null -> "${maxPixelArray.width} x ${maxPixelArray.height}"
            isExplicitHighRes && photoSizes.isNotEmpty() -> "${photoSizes.first().width} x ${photoSizes.first().height}"
            else -> null
        }

        // Lens Type Classification
        val lensType = if (isFront) {
            LensType.FRONT
        } else if (hasDepth || (calculatedMp in 0.1..3.0 && minFocusDiopters == 0f)) {
            LensType.DEPTH
        } else if (focalLength35mm < 18.0f || focalLengths.any { it < 3.0f }) {
            LensType.ULTRA_WIDE
        } else if (minFocusCm != null && minFocusCm <= 4.0f && calculatedMp in 2.0..8.0) {
            LensType.MACRO
        } else if (focalLength35mm in 18.0f..40.0f || apertures.any { it <= 2.0f }) {
            LensType.WIDE
        } else {
            LensType.TELEPHOTO
        }

        // Determine Advertised Sensor Tier and Binning Technology
        val (advertisedTier, binningTech, isQuadBayer) = when {
            // Case A: Explicit High-Res array detected in Camera2 HAL (API 31+ or unbinned stream)
            isExplicitHighRes -> {
                val tierStr = when {
                    maxAvailableMp >= 180.0 -> "200 MP Ultra-Matrix Class"
                    maxAvailableMp in 85.0..150.0 -> "%.0f MP High-Res Sensor Matrix".format(Locale.US, maxAvailableMp)
                    maxAvailableMp in 58.0..84.0 -> "64 MP High-Res Sensor Matrix"
                    maxAvailableMp in 42.0..57.0 -> "50 MP / 48 MP High-Res Matrix"
                    else -> "%.0f MP Sensor Matrix Class".format(Locale.US, maxAvailableMp)
                }
                val techStr = when {
                    maxAvailableMp >= 180.0 -> "16-in-1 Hexadeca-Pixel Fusion (%.1f MP Active Stream)".format(Locale.US, standardMp)
                    maxAvailableMp in 85.0..150.0 -> "9-in-1 Nonacell Super-Pixel Fusion (%.1f MP Active Stream)".format(Locale.US, standardMp)
                    else -> "4-in-1 Quad-Bayer Super-Pixel Fusion (%.1f MP Active Stream)".format(Locale.US, standardMp)
                }
                Triple(tierStr, techStr, true)
            }
            // Case B: Rear Primary Wide Camera with binned 9.5 - 14.5 MP output (Standard OEM HAL behavior on 50MP/100MP/108MP devices)
            !isFront && (lensType == LensType.WIDE || (!isPhysical && cameraId == "0")) && standardMp in 9.5..14.5 -> {
                Triple(
                    "50 MP / 100 MP / 108 MP Class Matrix",
                    "4-in-1 Quad-Bayer / 9-in-1 Nonacell Super-Pixel (%.1f MP Active HAL Stream)".format(Locale.US, standardMp),
                    true
                )
            }
            // Case C: Rear Camera with 14.6 - 18.0 MP output (64MP binned 4-in-1)
            !isFront && standardMp in 14.6..18.0 -> {
                Triple(
                    "64 MP Class Sensor Matrix",
                    "4-in-1 Quad-Bayer Fusion (%.1f MP Active HAL Stream)".format(Locale.US, standardMp),
                    true
                )
            }
            // Case D: Front Selfie Camera with 7.0 - 9.0 MP output (32MP binned 4-in-1)
            isFront && standardMp in 7.0..9.0 -> {
                Triple(
                    "32 MP Selfie Sensor Matrix Class",
                    "4-in-1 Quad-Bayer Super-Pixel (%.1f MP Active HAL Stream)".format(Locale.US, standardMp),
                    true
                )
            }
            // Case E: Front Selfie Camera with 11.5 - 16.5 MP output (50MP/60MP binned 4-in-1)
            isFront && standardMp in 11.5..16.5 -> {
                Triple(
                    "50 MP / 60 MP Front Portrait Matrix Class",
                    "4-in-1 Quad-Bayer Super-Pixel (%.1f MP Active HAL Stream)".format(Locale.US, standardMp),
                    true
                )
            }
            // Case F: Dedicated Ultra-Wide / Macro / Native sensor
            else -> {
                val nativeLabel = when {
                    lensType == LensType.ULTRA_WIDE -> "%.1f MP Native Ultra-Wide".format(Locale.US, standardMp)
                    lensType == LensType.MACRO -> "%.1f MP Native Macro".format(Locale.US, standardMp)
                    lensType == LensType.DEPTH -> "%.1f MP Depth Sensor".format(Locale.US, standardMp)
                    else -> "%.1f MP Native Sensor".format(Locale.US, standardMp)
                }
                Triple(
                    nativeLabel,
                    "1:1 Direct Sensor Readout (No Binning)",
                    false
                )
            }
        }

        // Zoom capabilities
        val maxDigitalZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
        val zoomRatioRange = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
            } catch (_: Exception) { null }
        } else null

        // OIS & EIS
        val oisModes = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        val hasOis = oisModes?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) == true

        val eisModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
        val hasEis = eisModes?.contains(CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON) == true

        // Flash
        val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

        // AF modes
        val afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
        val afModeStrings = afModes?.map { mode ->
            when (mode) {
                CameraMetadata.CONTROL_AF_MODE_AUTO -> "Auto"
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "Continuous Picture"
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "Continuous Video"
                CameraMetadata.CONTROL_AF_MODE_MACRO -> "Macro"
                CameraMetadata.CONTROL_AF_MODE_EDOF -> "EDOF"
                CameraMetadata.CONTROL_AF_MODE_OFF -> "Manual / Off"
                else -> "Mode $mode"
            }
        } ?: emptyList()

        // ISO range
        val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)

        // Exposure Time
        val exposureTimeRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)

        // Exposure Compensation
        val expCompRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        val expCompStepRational = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
        val expCompStep = expCompStepRational?.toFloat() ?: 0.333f

        // Physical IDs
        val physicalIdsList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                characteristics.physicalCameraIds.toList()
            } catch (_: Exception) { emptyList() }
        } else emptyList()

        // 10-bit HDR (API 33+)
        var has10BitHdr = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val dynProfiles = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)
                if (dynProfiles != null && dynProfiles.supportedProfiles.isNotEmpty()) {
                    has10BitHdr = true
                }
            } catch (_: Exception) { }
        }

        return CameraSpec(
            cameraId = cameraId,
            isPhysical = isPhysical,
            parentLogicalId = parentLogicalId,
            physicalCameraIds = physicalIdsList,
            facing = facing,
            isFront = isFront,
            lensType = lensType,
            hardwareLevel = hardwareLevel,
            focalLengths = focalLengths,
            focalLength35mm = focalLength35mm,
            apertures = apertures,
            sensorPhysicalSize = sensorSizeStr,
            sensorPixelArraySize = pixelArrayStr,
            maxPixelArraySize = maxPixelArrayStr,
            sensorMegaPixels = calculatedMp,
            binnedOutputMegaPixels = standardMp,
            isQuadBayer = isQuadBayer,
            claimedAdvertisedTier = advertisedTier,
            pixelBinningTechnology = binningTech,
            fovHorizontal = fovH,
            fovVertical = fovV,
            cropFactor = cropFactor,
            maxDigitalZoom = maxDigitalZoom,
            zoomRatioRange = zoomRatioRange,
            hasFlash = hasFlash,
            hasOis = hasOis,
            hasEis = hasEis,
            autoFocusModes = afModeStrings,
            minFocusDistanceDiopters = minFocusDiopters,
            minFocusDistanceCm = minFocusCm,
            isoRange = isoRange,
            exposureTimeRangeNs = exposureTimeRange,
            exposureCompensationRange = expCompRange,
            exposureCompensationStep = expCompStep,
            capabilities = capStrings,
            isRawSupported = hasRaw,
            isManualSensorSupported = hasManual,
            isLogicalMultiCamera = hasLogical,
            isConcurrentSupported = concurrentPairIds.isNotEmpty(),
            concurrentPairIds = concurrentPairIds,
            is10BitHdrSupported = has10BitHdr,
            photoResolutions = photoSizes,
            rawResolutions = rawSizes,
            videoFormats = videoSizes,
            highSpeedVideoFormats = highSpeedSizes
        )
    }
}

package com.aperture.camera.about

import com.aperture.camera.data.model.FullDeviceDiagnostics
import java.util.Locale

object SpecsReportFormatter {

    fun generateMarkdownReport(diagnostics: FullDeviceDiagnostics): String {
        val sb = StringBuilder()
        val sys = diagnostics.system
        val mem = diagnostics.memory
        val st = diagnostics.storage
        val co = diagnostics.cameraOverview

        sb.appendLine("# Device & Camera Hardware Diagnostic Report")
        sb.appendLine("**App:** Aperture Camera")
        sb.appendLine("**Generated:** System Live Hardware Probe")
        sb.appendLine()

        sb.appendLine("## 1. System & Device Profile")
        sb.appendLine("- **Device:** ${sys.manufacturer} ${sys.model}")
        sb.appendLine("- **Codename:** `${sys.deviceCodename}`")
        sb.appendLine("- **Platform / SoC:** ${sys.socModel} (${sys.hardware})")
        sb.appendLine("- **Board:** ${sys.board}")
        sb.appendLine("- **OS Version:** Android ${sys.androidVersion} (API ${sys.apiLevel})")
        sb.appendLine("- **Security Patch:** ${sys.securityPatch}")
        sb.appendLine("- **Display Resolution:** ${sys.displayResolution}")
        sb.appendLine("- **Screen Refresh Rate:** %.0f Hz".format(Locale.US, sys.refreshRateHz))
        sb.appendLine()

        sb.appendLine("## 2. Memory & Storage Resources")
        sb.appendLine("- **Total RAM:** %.2f GB".format(Locale.US, mem.totalRamGb))
        sb.appendLine("- **Available RAM:** %.2f GB".format(Locale.US, mem.availableRamGb))
        sb.appendLine("- **Used RAM:** %.2f GB (${mem.ramUsagePercent}%)".format(Locale.US, mem.usedRamGb))
        sb.appendLine("- **Total Internal Storage:** %.1f GB".format(Locale.US, st.totalStorageGb))
        sb.appendLine("- **Free Storage:** %.1f GB".format(Locale.US, st.freeStorageGb))
        sb.appendLine("- **Used Storage:** %.1f GB (${st.storageUsagePercent}%)".format(Locale.US, st.usedStorageGb))
        sb.appendLine()

        sb.appendLine("## 3. Camera Subsystem Overview")
        sb.appendLine("- **Total Physical Cameras:** ${co.totalSensorsCount}")
        sb.appendLine("- **Rear Physical Sensors:** ${co.backCamerasCount}")
        sb.appendLine("- **Front Physical Sensors:** ${co.frontCamerasCount}")
        sb.appendLine("- **Logical Multi-Camera Pipelines:** ${co.logicalCamerasCount}")
        sb.appendLine("- **Multi-Camera Hardware Fusion:** ${if (co.isMultiCameraSupported) "Supported" else "Not Supported"}")
        sb.appendLine("- **Concurrent Dual Streaming:** ${if (co.isConcurrentDualSupported) "Supported" else "Not Supported"}")
        sb.appendLine("- **10-bit HDR / Dynamic Profiles:** ${if (co.is10BitHdrSupported) "Supported" else "Standard 8-bit"}")
        sb.appendLine()

        sb.appendLine("## 4. Camera-Relevant Hardware Sensors")
        for (sensor in diagnostics.hardwareSensors) {
            val status = if (sensor.isAvailable) "Available" else "Not Available"
            sb.appendLine("### ${sensor.typeName} ($status)")
            sb.appendLine("- **Purpose:** ${sensor.purpose}")
            if (sensor.isAvailable) {
                sb.appendLine("- **Sensor Name:** ${sensor.name}")
                sb.appendLine("- **Vendor:** ${sensor.vendor} (v${sensor.version})")
                sb.appendLine("- **Power Consumption:** %.2f mA".format(Locale.US, sensor.powerMa))
                sb.appendLine("- **Max Range:** %.2f".format(Locale.US, sensor.maxRange))
                sb.appendLine("- **Resolution:** %.4f".format(Locale.US, sensor.resolution))
            }
            sb.appendLine()
        }

        sb.appendLine("## 5. Camera Sensors Specification")
        sb.appendLine()

        for (spec in diagnostics.cameraSpecs) {
            val titleType = if (spec.isPhysical) "Physical Sensor" else "Logical Camera"
            val parentInfo = if (spec.isPhysical && spec.parentLogicalId != null) " (Behind Logical Camera #${spec.parentLogicalId})" else ""
            sb.appendLine("### Camera ID: #${spec.cameraId} — ${spec.facing} / ${spec.lensType.displayName} ($titleType$parentInfo)")
            sb.appendLine()
            sb.appendLine("#### Optics & Sensor")
            sb.appendLine("- **Lens Classification:** ${spec.lensType.displayName}")
            if (spec.isQuadBayer) {
                sb.appendLine("- **Hardware Sensor Matrix:** %.1f MP (%s)".format(Locale.US, spec.sensorMegaPixels, spec.maxPixelArraySize ?: spec.sensorPixelArraySize))
                sb.appendLine("- **Default Binned Output:** %.1f MP (%s)".format(Locale.US, spec.binnedOutputMegaPixels, spec.sensorPixelArraySize))
                sb.appendLine("- **Pixel Binning:** 4-in-1 Quad-Bayer Hardware Array")
            } else {
                sb.appendLine("- **Sensor Megapixels:** %.1f MP".format(Locale.US, spec.sensorMegaPixels))
                sb.appendLine("- **Pixel Array Size:** ${spec.sensorPixelArraySize}")
            }
            sb.appendLine("- **Focal Lengths:** ${spec.focalLengths.joinToString(", ") { "%.2f mm".format(Locale.US, it) }}")
            sb.appendLine("- **35mm Equivalent:** ~%.1f mm".format(Locale.US, spec.focalLength35mm))
            sb.appendLine("- **Field of View (FOV):** %.1f° H × %.1f° V".format(Locale.US, spec.fovHorizontal, spec.fovVertical))
            sb.appendLine("- **Crop Factor:** %.2fx".format(Locale.US, spec.cropFactor))
            sb.appendLine("- **Apertures:** ${spec.apertures.joinToString(", ") { "f/%.2f".format(Locale.US, it) }}")
            sb.appendLine("- **Physical Sensor Size:** ${spec.sensorPhysicalSize}")
            sb.appendLine("- **Hardware Level:** ${spec.hardwareLevel}")
            sb.appendLine()

            sb.appendLine("#### Features & Capabilities")
            sb.appendLine("- **Optical Image Stabilization (OIS):** ${if (spec.hasOis) "Supported" else "Not Supported"}")
            sb.appendLine("- **Electronic Video Stabilization (EIS):** ${if (spec.hasEis) "Supported" else "Not Supported"}")
            sb.appendLine("- **Flash Unit:** ${if (spec.hasFlash) "Available" else "None"}")
            sb.appendLine("- **RAW/DNG Capture:** ${if (spec.isRawSupported) "Yes" else "No"}")
            sb.appendLine("- **Manual Pro Sensor Controls:** ${if (spec.isManualSensorSupported) "Yes" else "No"}")
            sb.appendLine("- **Concurrent Streaming:** ${if (spec.isConcurrentSupported) "Yes (with ${spec.concurrentPairIds.joinToString(", ")})" else "No"}")
            sb.appendLine("- **10-bit HDR:** ${if (spec.is10BitHdrSupported) "Supported" else "Standard 8-bit"}")
            if (spec.physicalCameraIds.isNotEmpty()) {
                sb.appendLine("- **Physical Camera IDs behind logical:** ${spec.physicalCameraIds.joinToString(", ")}")
            }
            sb.appendLine()

            sb.appendLine("#### Sensor Control Ranges")
            spec.isoRange?.let { sb.appendLine("- **ISO Range:** ${it.lower} .. ${it.upper}") }
            spec.exposureTimeRangeNs?.let {
                try {
                    val minNs = it.lower.toDouble()
                    val maxNs = it.upper.toDouble()
                    val minSec = minNs / 1_000_000_000.0
                    val maxSec = maxNs / 1_000_000_000.0
                    if (minSec > 0.0) {
                        val shutterFrac = (1.0 / minSec).toLong()
                        sb.appendLine("- **Exposure Range:** 1/${shutterFrac}s .. %.2fs".format(Locale.US, maxSec))
                    } else {
                        sb.appendLine("- **Exposure Range:** 0s .. %.2fs".format(Locale.US, maxSec))
                    }
                } catch (_: Exception) { }
            }
            spec.exposureCompensationRange?.let {
                sb.appendLine("- **Exposure Compensation:** ${it.lower} .. ${it.upper} (Step: %.2f EV)".format(Locale.US, spec.exposureCompensationStep))
            }
            if (spec.minFocusDistanceCm != null) {
                sb.appendLine("- **Min Focus Distance:** ~%.1f cm".format(Locale.US, spec.minFocusDistanceCm))
            }
            sb.appendLine("- **Autofocus Modes:** ${spec.autoFocusModes.joinToString(", ")}")
            sb.appendLine()

            sb.appendLine("#### Supported Resolutions")
            sb.appendLine("- **Photo (JPEG):** ${spec.photoResolutions.take(6).joinToString(", ") { "${it.width}x${it.height}" }}")
            if (spec.rawResolutions.isNotEmpty()) {
                sb.appendLine("- **RAW Resolutions:** ${spec.rawResolutions.take(3).joinToString(", ") { "${it.width}x${it.height}" }}")
            }
            if (spec.videoFormats.isNotEmpty()) {
                sb.appendLine("- **Video Formats:** ${spec.videoFormats.take(5).joinToString(", ") { "${it.size.width}x${it.size.height}" }}")
            }
            if (spec.highSpeedVideoFormats.isNotEmpty()) {
                sb.appendLine("- **High-Speed Video:** ${spec.highSpeedVideoFormats.joinToString(", ") { "${it.size.width}x${it.size.height} (${it.maxFps}fps)" }}")
            }
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine()
        }

        return sb.toString()
    }
}

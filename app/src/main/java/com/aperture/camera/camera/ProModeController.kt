package com.aperture.camera.camera

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.RggbChannelVector
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.CameraControl

/**
 * Immutable state holder for manual photography parameters in Pro mode.
 */
data class ProSettingsState(
    val isAutoIso: Boolean = true,
    val manualIso: Int = 100,
    val isAutoShutter: Boolean = true,
    val manualShutterNs: Long = 10_000_000L, // 1/100s
    val isAutoFocus: Boolean = true,
    val manualFocusDistance: Float = 0.0f, // diopters (0 = infinity)
    val isAutoWb: Boolean = true,
    val manualWbKelvin: Int = 5000,
    val exposureCompensation: Int = 0
)

/**
 * Controller for configuring manual sensor parameters (ISO, Shutter Speed,
 * Manual Focus Diopters, White Balance RGGB Gains) via Camera2Interop.
 */
class ProModeController {

    private var currentSettings = ProSettingsState()

    /**
     * Applies manual ISO, shutter speed, manual focus distance, or white balance gains
     * to the active CameraControl instance.
     */
    fun applyProSettings(
        cameraControl: CameraControl?,
        settings: ProSettingsState
    ) {
        if (cameraControl == null) return
        currentSettings = settings

        val cam2Control = Camera2CameraControl.from(cameraControl)
        val builder = CaptureRequestOptions.Builder()

        // 1. Exposure & ISO
        if (!settings.isAutoIso || !settings.isAutoShutter) {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CameraMetadata.CONTROL_AE_MODE_OFF
            )

            if (!settings.isAutoIso) {
                builder.setCaptureRequestOption(
                    CaptureRequest.SENSOR_SENSITIVITY,
                    settings.manualIso
                )
            }

            if (!settings.isAutoShutter) {
                builder.setCaptureRequestOption(
                    CaptureRequest.SENSOR_EXPOSURE_TIME,
                    settings.manualShutterNs
                )
            }
        } else {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CameraMetadata.CONTROL_AE_MODE_ON
            )
            // Apply exposure compensation index
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                settings.exposureCompensation
            )
        }

        // 2. Focus Distance
        if (!settings.isAutoFocus) {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_OFF
            )
            builder.setCaptureRequestOption(
                CaptureRequest.LENS_FOCUS_DISTANCE,
                settings.manualFocusDistance
            )
        } else {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
        }

        // 3. White Balance
        if (!settings.isAutoWb) {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CameraMetadata.CONTROL_AWB_MODE_OFF
            )
            val gains = kelvinToRggbGains(settings.manualWbKelvin)
            builder.setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_MODE,
                CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX
            )
            builder.setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_GAINS,
                gains
            )
        } else {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CameraMetadata.CONTROL_AWB_MODE_AUTO
            )
        }

        cam2Control.setCaptureRequestOptions(builder.build())
    }

    fun resetToAuto(cameraControl: CameraControl?) {
        if (cameraControl == null) return
        val cam2Control = Camera2CameraControl.from(cameraControl)
        cam2Control.clearCaptureRequestOptions()
    }

    /**
     * Converts a correlated color temperature (Kelvin, 2000K..10000K) to RGGB channel gains.
     */
    private fun kelvinToRggbGains(kelvin: Int): RggbChannelVector {
        val temp = kelvin / 100.0

        val rGain: Float
        val bGain: Float

        if (temp <= 66) {
            rGain = 1.0f
            val b = if (temp <= 19) 0.0 else 138.5177312231 * Math.log(temp - 10) - 305.0447927307
            bGain = (b / 255.0).coerceIn(0.2, 2.5).toFloat()
        } else {
            val r = 329.698727446 * Math.pow(temp - 60, -0.1332047592)
            rGain = (r / 255.0).coerceIn(0.4, 2.5).toFloat()
            bGain = 1.0f
        }

        return RggbChannelVector(rGain, 1.0f, 1.0f, bGain)
    }
}

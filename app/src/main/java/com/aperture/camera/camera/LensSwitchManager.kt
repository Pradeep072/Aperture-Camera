package com.aperture.camera.camera

import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import com.aperture.camera.data.model.LensBadge
import com.aperture.camera.data.model.LensType

data class LensSwitchResult(
    val cameraSelector: CameraSelector,
    val targetZoomRatio: Float,
    val targetFocalLengthMm: Float? = null,
    val isDirectPhysical: Boolean,
    val noticeMessage: String? = null
)

class LensSwitchManager {

    private val tag = "LensSwitchManager"

    /**
     * Resolves the appropriate CameraSelector for a selected LensBadge.
     * Back cameras are routed through DEFAULT_BACK_CAMERA (Logical Camera #0)
     * which Qualcomm HAL uses to seamlessly switch between Ultra-Wide, Wide, and Telephoto physical sensors.
     */
    fun resolveLensSwitch(badge: LensBadge, provider: ProcessCameraProvider? = null): LensSwitchResult {
        val selector = if (badge.isFront) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        return LensSwitchResult(
            cameraSelector = selector,
            targetZoomRatio = badge.zoomRatio,
            targetFocalLengthMm = badge.physicalFocalLengthMm,
            isDirectPhysical = false,
            noticeMessage = null
        )
    }

    /**
     * Applies lens settings (safe zoom ratio via CameraX CameraControl).
     */
    fun applyLensSettings(
        cameraControl: CameraControl?,
        cameraInfo: CameraInfo?,
        badge: LensBadge
    ) {
        if (cameraControl == null) return

        val zoomState = cameraInfo?.zoomState?.value
        val minZoom = zoomState?.minZoomRatio ?: 1.0f
        val maxZoom = zoomState?.maxZoomRatio ?: 10.0f

        val targetZoom = badge.zoomRatio.coerceIn(minZoom, maxZoom)
        try {
            cameraControl.setZoomRatio(targetZoom)
        } catch (e: Exception) {
            Log.e(tag, "Failed to apply zoom ratio: ${e.message}")
        }
    }
}

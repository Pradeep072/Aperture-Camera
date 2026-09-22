package com.aperture.camera.camera

import android.content.Context
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner

data class DualCameraSession(
    val primaryCamera: androidx.camera.core.Camera?,
    val secondaryCamera: androidx.camera.core.Camera?,
    val primaryImageCapture: ImageCapture? = null,
    val isSupported: Boolean,
    val message: String? = null
)

class ConcurrentCameraManager(private val context: Context) {

    fun isConcurrentSupported(cameraProvider: ProcessCameraProvider): Boolean {
        return try {
            val concurrentList = cameraProvider.availableConcurrentCameraInfos
            concurrentList.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    fun bindDualCamera(
        lifecycleOwner: LifecycleOwner,
        cameraProvider: ProcessCameraProvider,
        primarySurfaceProvider: Preview.SurfaceProvider,
        secondarySurfaceProvider: Preview.SurfaceProvider
    ): DualCameraSession {
        val availablePairs = try {
            cameraProvider.availableConcurrentCameraInfos
        } catch (e: Exception) {
            emptyList<List<CameraInfo>>()
        }

        if (availablePairs.isEmpty()) {
            return DualCameraSession(
                primaryCamera = null,
                secondaryCamera = null,
                primaryImageCapture = null,
                isSupported = false,
                message = "This device hardware does not support concurrent front and back camera streaming."
            )
        }

        val firstPair = availablePairs.firstOrNull() ?: emptyList()
        val backCameraInfo = firstPair.firstOrNull { it.lensFacing == CameraSelector.LENS_FACING_BACK }
        val frontCameraInfo = firstPair.firstOrNull { it.lensFacing == CameraSelector.LENS_FACING_FRONT }

        val backSelector = backCameraInfo?.cameraSelector ?: CameraSelector.DEFAULT_BACK_CAMERA
        val frontSelector = frontCameraInfo?.cameraSelector ?: CameraSelector.DEFAULT_FRONT_CAMERA

        // 1. Attempt binding with ImageCapture on primary camera
        try {
            cameraProvider.unbindAll()

            val primaryPreview = Preview.Builder().build().apply {
                setSurfaceProvider(primarySurfaceProvider)
            }
            val secondaryPreview = Preview.Builder().build().apply {
                setSurfaceProvider(secondarySurfaceProvider)
            }
            val primaryCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val primaryConfig = ConcurrentCamera.SingleCameraConfig(
                backSelector,
                UseCaseGroup.Builder()
                    .addUseCase(primaryPreview)
                    .addUseCase(primaryCapture)
                    .build(),
                lifecycleOwner
            )

            val secondaryConfig = ConcurrentCamera.SingleCameraConfig(
                frontSelector,
                UseCaseGroup.Builder().addUseCase(secondaryPreview).build(),
                lifecycleOwner
            )

            val concurrentCamera = cameraProvider.bindToLifecycle(listOf(primaryConfig, secondaryConfig))
            val cameras = concurrentCamera.cameras

            return DualCameraSession(
                primaryCamera = cameras.getOrNull(0),
                secondaryCamera = cameras.getOrNull(1),
                primaryImageCapture = primaryCapture,
                isSupported = true,
                message = null
            )
        } catch (_: Exception) {
            // 2. Fallback: Bind Dual Previews only (for devices with strict dual-stream HAL limits)
            try {
                cameraProvider.unbindAll()

                val primaryPreview = Preview.Builder().build().apply {
                    setSurfaceProvider(primarySurfaceProvider)
                }
                val secondaryPreview = Preview.Builder().build().apply {
                    setSurfaceProvider(secondarySurfaceProvider)
                }

                val primaryConfig = ConcurrentCamera.SingleCameraConfig(
                    backSelector,
                    UseCaseGroup.Builder().addUseCase(primaryPreview).build(),
                    lifecycleOwner
                )

                val secondaryConfig = ConcurrentCamera.SingleCameraConfig(
                    frontSelector,
                    UseCaseGroup.Builder().addUseCase(secondaryPreview).build(),
                    lifecycleOwner
                )

                val concurrentCamera = cameraProvider.bindToLifecycle(listOf(primaryConfig, secondaryConfig))
                val cameras = concurrentCamera.cameras

                return DualCameraSession(
                    primaryCamera = cameras.getOrNull(0),
                    secondaryCamera = cameras.getOrNull(1),
                    primaryImageCapture = null,
                    isSupported = true,
                    message = null
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return DualCameraSession(
                    primaryCamera = null,
                    secondaryCamera = null,
                    primaryImageCapture = null,
                    isSupported = false,
                    message = "Dual camera session failed: ${e.localizedMessage}"
                )
            }
        }
    }
}

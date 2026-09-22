package com.aperture.camera.camera

import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Location
import android.net.Uri
import android.util.Range
import android.view.Surface
import androidx.camera.camera2.interop.Camera2CameraInfo
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.aperture.camera.data.model.CaptureMode
import com.aperture.camera.data.model.FlashModeOption
import com.aperture.camera.data.model.LensBadge
import com.aperture.camera.data.model.VideoQualityOption
import com.aperture.camera.data.repository.MediaStoreRepository
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class CameraSessionState(
    val isInitialized: Boolean = false,
    val currentLens: LensBadge? = null,
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 1.0f,
    val exposureIndex: Int = 0,
    val exposureRange: Range<Int> = Range(0, 0),
    val exposureStep: Float = 0.333f,
    val flashMode: FlashModeOption = FlashModeOption.OFF,
    val isTorchOn: Boolean = false,
    val hasFlashUnit: Boolean = false,
    val isFocusing: Boolean = false,
    val focusPoint: Pair<Float, Float>? = null,
    val errorMessage: String? = null
)

class CameraManagerController(
    private val context: Context,
    private val mediaStoreRepository: MediaStoreRepository
) {
    private var cameraProvider: ProcessCameraProvider? = null
    var currentCamera: Camera? = null
        private set

    private var previewUseCase: Preview? = null
    private var imageCaptureUseCase: ImageCapture? = null
    private var videoCaptureUseCase: VideoCapture<Recorder>? = null

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    val lensSwitchManager = LensSwitchManager()
    val proModeController = ProModeController()
    val videoRecorderManager = VideoRecorderManager(context, mediaStoreRepository)
    val concurrentCameraManager = ConcurrentCameraManager(context)

    private val _sessionState = MutableStateFlow(CameraSessionState())
    val sessionState: StateFlow<CameraSessionState> = _sessionState.asStateFlow()

    fun initialize(onInitialized: (ProcessCameraProvider) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                _sessionState.value = _sessionState.value.copy(isInitialized = true)
                cameraProvider?.let(onInitialized)
            } catch (e: Exception) {
                _sessionState.value = _sessionState.value.copy(errorMessage = "Camera initialization failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Binds camera use cases based on current capture mode and selected lens.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        badge: LensBadge,
        captureMode: CaptureMode,
        videoQuality: VideoQualityOption,
        rotation: Int = Surface.ROTATION_0
    ) {
        val provider = cameraProvider ?: return
        try {
            provider.unbindAll()

            // 1. Build Preview with Highest Available Resolution
            val highResStrategy = ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .build()

            val previewBuilder = Preview.Builder()
                .setResolutionSelector(highResStrategy)
                .setTargetRotation(rotation)

            previewUseCase = previewBuilder.build().apply {
                setSurfaceProvider(surfaceProvider)
            }

            // 2. Build ImageCapture with High Quality & Full Sensor Resolution
            val imageCaptureBuilder = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setResolutionSelector(highResStrategy)
                .setJpegQuality(98)
                .setTargetRotation(rotation)

            imageCaptureUseCase = imageCaptureBuilder.build()

            // 3. Build VideoCapture (for Video mode)
            val recorder = videoRecorderManager.createRecorder(videoQuality)
            videoCaptureUseCase = VideoCapture.withOutput(recorder)

            // Resolve camera selector
            val switchResult = lensSwitchManager.resolveLensSwitch(badge, provider)
            val cameraSelector = switchResult.cameraSelector

            // Bind use cases according to mode
            val camera = when (captureMode) {
                CaptureMode.PHOTO, CaptureMode.DOCUMENT, CaptureMode.PRO -> {
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        previewUseCase,
                        imageCaptureUseCase
                    )
                }
                CaptureMode.VIDEO -> {
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        previewUseCase,
                        videoCaptureUseCase
                    )
                }
                CaptureMode.DUAL -> {
                    // Handled separately by ConcurrentCameraManager
                    return
                }
            }

            currentCamera = camera

            // Update Zoom State
            camera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                _sessionState.value = _sessionState.value.copy(
                    zoomRatio = zoomState.zoomRatio,
                    minZoomRatio = zoomState.minZoomRatio,
                    maxZoomRatio = zoomState.maxZoomRatio
                )
            }

            // Update Exposure State
            val expState = camera.cameraInfo.exposureState
            _sessionState.value = _sessionState.value.copy(
                exposureIndex = expState.exposureCompensationIndex,
                exposureRange = expState.exposureCompensationRange,
                exposureStep = expState.exposureCompensationStep.toFloat(),
                hasFlashUnit = camera.cameraInfo.hasFlashUnit(),
                currentLens = badge
            )

            // Apply lens settings (zoom ratio, focal length, macro AF mode)
            lensSwitchManager.applyLensSettings(
                cameraControl = camera.cameraControl,
                cameraInfo = camera.cameraInfo,
                badge = badge
            )

            // Apply Flash
            applyFlashMode(_sessionState.value.flashMode)

        } catch (e: Exception) {
            e.printStackTrace()
            _sessionState.value = _sessionState.value.copy(errorMessage = "Failed to switch lens (${badge.label}): ${e.message}")
        }
    }

    /**
     * Binds concurrent dual cameras (front + back) for Picture-in-Picture streaming.
     */
    fun bindDualCamera(
        lifecycleOwner: LifecycleOwner,
        primarySurfaceProvider: Preview.SurfaceProvider,
        secondarySurfaceProvider: Preview.SurfaceProvider
    ): DualCameraSession {
        val provider = cameraProvider ?: return DualCameraSession(
            primaryCamera = null,
            secondaryCamera = null,
            isSupported = false,
            message = "Camera provider is not initialized."
        )
        val session = concurrentCameraManager.bindDualCamera(
            lifecycleOwner = lifecycleOwner,
            cameraProvider = provider,
            primarySurfaceProvider = primarySurfaceProvider,
            secondarySurfaceProvider = secondarySurfaceProvider
        )
        currentCamera = session.primaryCamera
        return session
    }

    /**
     * Seamlessly updates optical lens settings (zoom, focal length, macro AF) on active session.
     */
    fun applyLensBadge(badge: LensBadge) {
        _sessionState.value = _sessionState.value.copy(currentLens = badge)
        val control = currentCamera?.cameraControl ?: return
        lensSwitchManager.applyLensSettings(
            cameraControl = control,
            cameraInfo = currentCamera?.cameraInfo,
            badge = badge
        )
    }

    /**
     * Tap to focus and metering
     */
    fun tapToFocus(meteringPointFactory: MeteringPointFactory, x: Float, y: Float) {
        val control = currentCamera?.cameraControl ?: return
        val point = meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(4, TimeUnit.SECONDS)
            .build()

        _sessionState.value = _sessionState.value.copy(isFocusing = true, focusPoint = Pair(x, y))

        control.startFocusAndMetering(action).addListener({
            _sessionState.value = _sessionState.value.copy(isFocusing = false)
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Set zoom ratio with optical focal length support
     */
    fun setZoomRatio(ratio: Float, focalLengthMm: Float? = null) {
        val control = currentCamera?.cameraControl ?: return
        val currentLens = _sessionState.value.currentLens
        val updatedLens = currentLens?.copy(
            zoomRatio = ratio,
            physicalFocalLengthMm = focalLengthMm ?: currentLens.physicalFocalLengthMm
        ) ?: LensBadge(
            cameraId = "0",
            physicalCameraId = null,
            isPhysical = false,
            parentLogicalId = null,
            lensType = com.aperture.camera.data.model.LensType.WIDE,
            label = "${ratio}x",
            focalLength35mm = 26f * ratio,
            physicalFocalLengthMm = focalLengthMm ?: 5.0f,
            aperture = 1.8f,
            zoomRatio = ratio,
            isMacro = false,
            isFront = false
        )
        lensSwitchManager.applyLensSettings(
            cameraControl = control,
            cameraInfo = currentCamera?.cameraInfo,
            badge = updatedLens
        )
    }

    /**
     * Set exposure compensation index
     */
    fun setExposureCompensation(index: Int) {
        val control = currentCamera?.cameraControl ?: return
        val range = _sessionState.value.exposureRange
        val clamped = index.coerceIn(range.lower, range.upper)
        control.setExposureCompensationIndex(clamped)
        _sessionState.value = _sessionState.value.copy(exposureIndex = clamped)
    }

    /**
     * Set Flash Mode (OFF, AUTO, ON, TORCH)
     */
    fun applyFlashMode(flashMode: FlashModeOption) {
        _sessionState.value = _sessionState.value.copy(flashMode = flashMode)
        val imageCapture = imageCaptureUseCase
        val control = currentCamera?.cameraControl

        when (flashMode) {
            FlashModeOption.OFF -> {
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
                control?.enableTorch(false)
                _sessionState.value = _sessionState.value.copy(isTorchOn = false)
            }
            FlashModeOption.AUTO -> {
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_AUTO
                control?.enableTorch(false)
                _sessionState.value = _sessionState.value.copy(isTorchOn = false)
            }
            FlashModeOption.ON -> {
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON
                control?.enableTorch(false)
                _sessionState.value = _sessionState.value.copy(isTorchOn = false)
            }
            FlashModeOption.TORCH -> {
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
                control?.enableTorch(true)
                _sessionState.value = _sessionState.value.copy(isTorchOn = true)
            }
        }
    }

    /**
     * Captures photo at maximum sensor quality, writes EXIF GPS metadata and saves to MediaStore.
     */
     fun takePhoto(
        location: Location? = null,
        onPhotoCaptured: (Uri?, Bitmap?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val imageCapture = imageCaptureUseCase ?: run {
            onError(IllegalStateException("ImageCapture is not initialized"))
            return
        }

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bytes: ByteArray
                    try {
                        val buffer: ByteBuffer = image.planes[0].buffer
                        bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                    } catch (e: Exception) {
                        image.close()
                        CoroutineScope(Dispatchers.Main).launch { onError(e) }
                        return
                    } finally {
                        image.close()
                    }

                    try {
                        // Extract camera info parameters for EXIF
                        val camInfo = currentCamera?.cameraInfo
                        val currentLensBadge = _sessionState.value.currentLens
                        val focalLength = currentLensBadge?.physicalFocalLengthMm ?: camInfo?.let {
                            try {
                                val cam2 = Camera2CameraInfo.from(it)
                                val focals = cam2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                                focals?.firstOrNull()
                            } catch (_: Exception) { null }
                        }
                        val aperture = currentLensBadge?.aperture ?: camInfo?.let {
                            try {
                                val cam2 = Camera2CameraInfo.from(it)
                                val aps = cam2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                                aps?.firstOrNull()
                            } catch (_: Exception) { null }
                        }

                        // Generate thumbnail bitmap for gallery chip
                        val sampleOptions = BitmapFactory.Options().apply {
                            inSampleSize = 8
                        }
                        val thumbRaw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, sampleOptions)
                        val thumbBitmap = thumbRaw?.let { decodeOrientedBitmap(it, bytes) } ?: thumbRaw

                        CoroutineScope(Dispatchers.IO).launch {
                            val savedUri = mediaStoreRepository.savePhoto(
                                jpegBytes = bytes,
                                focalLength = focalLength,
                                aperture = aperture,
                                location = location
                            )

                            withContext(Dispatchers.Main) {
                                onPhotoCaptured(savedUri, thumbBitmap)
                            }
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch { onError(e) }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    CoroutineScope(Dispatchers.Main).launch { onError(exception) }
                }
            }
        )
    }

    /**
     * Captures high-resolution bitmap for Document Scanner editor.
     */
    fun captureDocumentBitmap(
        onBitmapCaptured: (Bitmap) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val imageCapture = imageCaptureUseCase ?: run {
            onError(IllegalStateException("ImageCapture is not initialized"))
            return
        }

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bytes: ByteArray
                    try {
                        val buffer: ByteBuffer = image.planes[0].buffer
                        bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                    } catch (e: Exception) {
                        image.close()
                        CoroutineScope(Dispatchers.Main).launch { onError(e) }
                        return
                    } finally {
                        image.close()
                    }

                    try {
                        val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        val finalBitmap = if (rawBitmap != null) {
                            decodeOrientedBitmap(rawBitmap, bytes)
                        } else {
                            throw IllegalStateException("Failed to decode document bitmap")
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            onBitmapCaptured(finalBitmap)
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch { onError(e) }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    CoroutineScope(Dispatchers.Main).launch { onError(exception) }
                }
            }
        )
    }

    private fun decodeOrientedBitmap(bitmap: Bitmap, jpegBytes: ByteArray): Bitmap {
        try {
            val exif = ExifInterface(ByteArrayInputStream(jpegBytes))
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (_: Exception) {
            return bitmap
        }
    }

    fun release() {
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}

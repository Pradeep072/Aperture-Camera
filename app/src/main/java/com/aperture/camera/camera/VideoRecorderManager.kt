package com.aperture.camera.camera

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.aperture.camera.data.model.VideoQualityOption
import com.aperture.camera.data.repository.MediaStoreRepository
import java.util.concurrent.TimeUnit

sealed class VideoRecordingState {
    object Idle : VideoRecordingState()
    data class RecordingActive(val durationSec: Long, val isPaused: Boolean) : VideoRecordingState()
    data class Finalized(val uri: Uri, val durationMs: Long) : VideoRecordingState()
    data class Error(val message: String) : VideoRecordingState()
}

class VideoRecorderManager(
    private val context: Context,
    private val mediaStoreRepository: MediaStoreRepository
) {

    private var activeRecording: Recording? = null
    var currentRecorder: Recorder? = null
        private set

    fun createRecorder(qualityOption: VideoQualityOption): Recorder {
        val targetQuality = when (qualityOption) {
            VideoQualityOption.UHD -> Quality.UHD
            VideoQualityOption.FHD -> Quality.FHD
            VideoQualityOption.HD -> Quality.HD
            VideoQualityOption.SD -> Quality.SD
        }

        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(targetQuality, Quality.FHD, Quality.HD, Quality.SD),
            androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        )

        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()

        currentRecorder = recorder
        return recorder
    }

    @SuppressLint("MissingPermission")
    fun startRecording(
        recorder: Recorder,
        enableAudio: Boolean,
        onEvent: (VideoRecordingState) -> Unit
    ) {
        val contentValues = mediaStoreRepository.createVideoContentValues()
        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        val pendingRecording: PendingRecording = recorder.prepareRecording(context, outputOptions)
        if (enableAudio) {
            try {
                pendingRecording.withAudioEnabled()
            } catch (_: SecurityException) {
                // Audio permission wasn't granted, proceed with video only
            }
        }

        activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    onEvent(VideoRecordingState.RecordingActive(durationSec = 0, isPaused = false))
                }
                is VideoRecordEvent.Status -> {
                    val durationNanos = event.recordingStats.recordedDurationNanos
                    val seconds = TimeUnit.NANOSECONDS.toSeconds(durationNanos)
                    val isPaused = activeRecording?.let { false } ?: false
                    onEvent(VideoRecordingState.RecordingActive(durationSec = seconds, isPaused = isPaused))
                }
                is VideoRecordEvent.Pause -> {
                    val durationNanos = event.recordingStats.recordedDurationNanos
                    val seconds = TimeUnit.NANOSECONDS.toSeconds(durationNanos)
                    onEvent(VideoRecordingState.RecordingActive(durationSec = seconds, isPaused = true))
                }
                is VideoRecordEvent.Resume -> {
                    val durationNanos = event.recordingStats.recordedDurationNanos
                    val seconds = TimeUnit.NANOSECONDS.toSeconds(durationNanos)
                    onEvent(VideoRecordingState.RecordingActive(durationSec = seconds, isPaused = false))
                }
                is VideoRecordEvent.Finalize -> {
                    activeRecording = null
                    if (event.hasError()) {
                        onEvent(VideoRecordingState.Error("Recording error code: ${event.error}"))
                    } else {
                        val uri = event.outputResults.outputUri
                        val durationMs = TimeUnit.NANOSECONDS.toMillis(event.recordingStats.recordedDurationNanos)
                        onEvent(VideoRecordingState.Finalized(uri = uri, durationMs = durationMs))
                    }
                }
            }
        }
    }

    fun pauseRecording() {
        activeRecording?.pause()
    }

    fun resumeRecording() {
        activeRecording?.resume()
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    val isRecording: Boolean
        get() = activeRecording != null
}

package com.aperture.camera.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.aperture.camera.data.model.CameraSettings
import com.aperture.camera.data.model.GridType
import com.aperture.camera.data.model.VideoQualityOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aperture_camera_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<CameraSettings> = _settings.asStateFlow()

    private fun loadSettings(): CameraSettings {
        val gridName = prefs.getString("grid_type", GridType.NONE.name) ?: GridType.NONE.name
        val gridType = try { GridType.valueOf(gridName) } catch (_: Exception) { GridType.NONE }

        val videoQualityName = prefs.getString("video_quality", VideoQualityOption.UHD.name) ?: VideoQualityOption.UHD.name
        val videoQuality = try { VideoQualityOption.valueOf(videoQualityName) } catch (_: Exception) { VideoQualityOption.UHD }

        return CameraSettings(
            defaultLensId = prefs.getString("default_lens_id", "") ?: "",
            gridType = gridType,
            isGeotagEnabled = prefs.getBoolean("geotag_enabled", true),
            isShutterSoundEnabled = prefs.getBoolean("shutter_sound_enabled", true),
            isSaveOriginalEnabled = prefs.getBoolean("save_original_enabled", true),
            isRawCaptureEnabled = prefs.getBoolean("raw_capture_enabled", true),
            videoQuality = videoQuality,
            targetFps = prefs.getInt("target_fps", 60),
            isVideoStabilizationEnabled = prefs.getBoolean("video_stabilization_enabled", true),
            isAudioEnabled = prefs.getBoolean("audio_enabled", true)
        )
    }

    fun updateGridType(gridType: GridType) {
        prefs.edit().putString("grid_type", gridType.name).apply()
        _settings.value = _settings.value.copy(gridType = gridType)
    }

    fun updateGeotagEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("geotag_enabled", enabled).apply()
        _settings.value = _settings.value.copy(isGeotagEnabled = enabled)
    }

    fun updateShutterSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("shutter_sound_enabled", enabled).apply()
        _settings.value = _settings.value.copy(isShutterSoundEnabled = enabled)
    }

    fun updateSaveOriginalEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("save_original_enabled", enabled).apply()
        _settings.value = _settings.value.copy(isSaveOriginalEnabled = enabled)
    }

    fun updateRawCaptureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("raw_capture_enabled", enabled).apply()
        _settings.value = _settings.value.copy(isRawCaptureEnabled = enabled)
    }

    fun updateVideoQuality(quality: VideoQualityOption) {
        prefs.edit().putString("video_quality", quality.name).apply()
        _settings.value = _settings.value.copy(videoQuality = quality)
    }

    fun updateTargetFps(fps: Int) {
        prefs.edit().putInt("target_fps", fps).apply()
        _settings.value = _settings.value.copy(targetFps = fps)
    }

    fun updateVideoStabilization(enabled: Boolean) {
        prefs.edit().putBoolean("video_stabilization_enabled", enabled).apply()
        _settings.value = _settings.value.copy(isVideoStabilizationEnabled = enabled)
    }

    fun updateAudioEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("audio_enabled", enabled).apply()
        _settings.value = _settings.value.copy(isAudioEnabled = enabled)
    }

    fun updateDefaultLensId(lensId: String) {
        prefs.edit().putString("default_lens_id", lensId).apply()
        _settings.value = _settings.value.copy(defaultLensId = lensId)
    }
}

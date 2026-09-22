package com.aperture.camera.camera

import android.content.Context
import android.media.MediaActionSound
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Manages native camera action sounds (shutter clicks, video recording cues)
 * and tactile haptic feedback for shooting operations.
 */
class CameraSoundAndHapticsManager(private val context: Context) {

    private val mediaActionSound by lazy {
        MediaActionSound().apply {
            try {
                load(MediaActionSound.SHUTTER_CLICK)
                load(MediaActionSound.START_VIDEO_RECORDING)
                load(MediaActionSound.STOP_VIDEO_RECORDING)
            } catch (_: Exception) {}
        }
    }

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Plays camera shutter sound if enabled, and fires crisp haptic feedback.
     */
    fun playShutterClick(enabled: Boolean = true) {
        if (enabled) {
            try {
                mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
            } catch (_: Exception) {}
        }
        performHapticClick()
    }

    /**
     * Plays video start cue sound if enabled, and fires haptic feedback.
     */
    fun playStartVideo(enabled: Boolean = true) {
        if (enabled) {
            try {
                mediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING)
            } catch (_: Exception) {}
        }
        performHapticClick()
    }

    /**
     * Plays video stop cue sound if enabled, and fires haptic feedback.
     */
    fun playStopVideo(enabled: Boolean = true) {
        if (enabled) {
            try {
                mediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING)
            } catch (_: Exception) {}
        }
        performHapticClick()
    }

    /**
     * Executes short tactile vibration for physical button feedback.
     */
    fun performHapticClick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(28)
            }
        } catch (_: Exception) {}
    }

    /**
     * Releases audio hardware resources when ViewModel is destroyed.
     */
    fun release() {
        try {
            mediaActionSound.release()
        } catch (_: Exception) {}
    }
}

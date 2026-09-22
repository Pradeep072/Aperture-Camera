package com.aperture.camera.data.model

enum class LensType(val displayName: String) {
    ULTRA_WIDE("Ultra-Wide"),
    WIDE("Wide"),
    TELEPHOTO("Telephoto"),
    FRONT("Front"),
    MACRO("Macro"),
    DEPTH("Depth / ToF"),
    UNKNOWN("Standard")
}

data class LensBadge(
    val cameraId: String, // Logical ID or direct ID
    val physicalCameraId: String? = null, // Sub-camera ID behind logical multi-camera (e.g. "3" for Ultra-Wide)
    val isPhysical: Boolean = false,
    val parentLogicalId: String? = null,
    val lensType: LensType,
    val label: String, // e.g. "0.5x", "1x", "2x", "Macro", "Front"
    val focalLength35mm: Float,
    val physicalFocalLengthMm: Float? = null,
    val aperture: Float? = null,
    val zoomRatio: Float = 1.0f,
    val isMacro: Boolean = false,
    val isFront: Boolean = false
)

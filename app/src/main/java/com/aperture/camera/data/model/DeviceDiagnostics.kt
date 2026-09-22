package com.aperture.camera.data.model

data class SystemInfo(
    val manufacturer: String,
    val model: String,
    val deviceCodename: String,
    val board: String,
    val hardware: String,
    val socModel: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val displayResolution: String,
    val refreshRateHz: Float
)

data class MemoryDiagnostics(
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val usedRamBytes: Long,
    val totalRamGb: Double,
    val availableRamGb: Double,
    val usedRamGb: Double,
    val ramUsagePercent: Int,
    val isLowMemory: Boolean
)

data class StorageDiagnostics(
    val totalStorageBytes: Long,
    val freeStorageBytes: Long,
    val usedStorageBytes: Long,
    val totalStorageGb: Double,
    val freeStorageGb: Double,
    val usedStorageGb: Double,
    val storageUsagePercent: Int
)

data class HardwareSensorInfo(
    val typeName: String,
    val name: String,
    val vendor: String,
    val version: Int,
    val powerMa: Float,
    val maxRange: Float,
    val resolution: Float,
    val purpose: String,
    val isAvailable: Boolean
)

data class CameraSubsystemOverview(
    val totalSensorsCount: Int,
    val logicalCamerasCount: Int,
    val physicalCamerasCount: Int,
    val backCamerasCount: Int,
    val frontCamerasCount: Int,
    val isMultiCameraSupported: Boolean,
    val isConcurrentDualSupported: Boolean,
    val is10BitHdrSupported: Boolean
)

data class FullDeviceDiagnostics(
    val system: SystemInfo,
    val memory: MemoryDiagnostics,
    val storage: StorageDiagnostics,
    val cameraOverview: CameraSubsystemOverview,
    val hardwareSensors: List<HardwareSensorInfo>,
    val cameraSpecs: List<CameraSpec>
)

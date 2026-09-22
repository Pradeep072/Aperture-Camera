package com.aperture.camera.camera

import android.app.ActivityManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.Display
import com.aperture.camera.data.model.CameraSpec
import com.aperture.camera.data.model.CameraSubsystemOverview
import com.aperture.camera.data.model.FullDeviceDiagnostics
import com.aperture.camera.data.model.HardwareSensorInfo
import com.aperture.camera.data.model.MemoryDiagnostics
import com.aperture.camera.data.model.StorageDiagnostics
import com.aperture.camera.data.model.SystemInfo

class DeviceHardwareDetector(private val context: Context) {

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    private val activityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }

    fun getFullDiagnostics(cameraSpecs: List<CameraSpec>): FullDeviceDiagnostics {
        return FullDeviceDiagnostics(
            system = getSystemInfo(),
            memory = getMemoryDiagnostics(),
            storage = getStorageDiagnostics(),
            cameraOverview = buildCameraOverview(cameraSpecs),
            hardwareSensors = getCameraRelevantSensors(),
            cameraSpecs = cameraSpecs
        )
    }

    fun getSystemInfo(): SystemInfo {
        var resStr = "Unknown"
        var refreshRate = 60f

        try {
            val metrics = context.resources.displayMetrics
            resStr = "${metrics.widthPixels} x ${metrics.heightPixels}"

            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
            if (display != null) {
                refreshRate = display.refreshRate
            }
        } catch (_: Exception) { }

        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() && it != "unknown" } ?: Build.HARDWARE
        } else {
            Build.HARDWARE
        }

        return SystemInfo(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            deviceCodename = Build.DEVICE,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            socModel = socModel,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A",
            displayResolution = resStr,
            refreshRateHz = refreshRate
        )
    }

    fun getMemoryDiagnostics(): MemoryDiagnostics {
        try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)

            val totalBytes = memInfo.totalMem
            val availableBytes = memInfo.availMem
            val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)

            val totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
            val availableGb = availableBytes / (1024.0 * 1024.0 * 1024.0)
            val usedGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
            val usagePercent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0

            return MemoryDiagnostics(
                totalRamBytes = totalBytes,
                availableRamBytes = availableBytes,
                usedRamBytes = usedBytes,
                totalRamGb = totalGb,
                availableRamGb = availableGb,
                usedRamGb = usedGb,
                ramUsagePercent = usagePercent,
                isLowMemory = memInfo.lowMemory
            )
        } catch (_: Exception) {
            return MemoryDiagnostics(0, 0, 0, 0.0, 0.0, 0.0, 0, false)
        }
    }

    fun getStorageDiagnostics(): StorageDiagnostics {
        try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

            val totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
            val freeGb = freeBytes / (1024.0 * 1024.0 * 1024.0)
            val usedGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
            val usagePercent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0

            return StorageDiagnostics(
                totalStorageBytes = totalBytes,
                freeStorageBytes = freeBytes,
                usedStorageBytes = usedBytes,
                totalStorageGb = totalGb,
                freeStorageGb = freeGb,
                usedStorageGb = usedGb,
                storageUsagePercent = usagePercent
            )
        } catch (_: Exception) {
            return StorageDiagnostics(0, 0, 0, 0.0, 0.0, 0.0, 0)
        }
    }

    fun getCameraRelevantSensors(): List<HardwareSensorInfo> {
        val sm = sensorManager ?: return emptyList()
        val sensors = mutableListOf<HardwareSensorInfo>()

        try {
            // 1. Gyroscope (Stabilization / OIS / EIS)
            val gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            sensors.add(
                buildSensorInfo(
                    typeName = "Gyroscope",
                    sensor = gyro,
                    purpose = "OIS optical stabilization & EIS electronic video stabilization"
                )
            )

            // 2. Accelerometer (Orientation / Horizon Level)
            val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            sensors.add(
                buildSensorInfo(
                    typeName = "Accelerometer",
                    sensor = accel,
                    purpose = "Device rotation, orientation lock & visual horizon leveling"
                )
            )

            // 3. Ambient Light (Auto-Exposure & Lux)
            val light = sm.getDefaultSensor(Sensor.TYPE_LIGHT)
            sensors.add(
                buildSensorInfo(
                    typeName = "Ambient Light Sensor",
                    sensor = light,
                    purpose = "Scene illuminance metering, night mode trigger & auto exposure"
                )
            )

            // 4. Proximity Sensor (Pocket Mode / Face proximity)
            val prox = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            sensors.add(
                buildSensorInfo(
                    typeName = "Proximity Sensor",
                    sensor = prox,
                    purpose = "Accidental pocket touch rejection & front camera proximity"
                )
            )

            // 5. Magnetometer / Geomagnetic Field (Compass Geotagging)
            val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            sensors.add(
                buildSensorInfo(
                    typeName = "Magnetometer (Compass)",
                    sensor = mag,
                    purpose = "EXIF compass heading metadata & camera orientation tracking"
                )
            )

            // 6. Gravity Sensor (Inertial gravity vector)
            val gravity = sm.getDefaultSensor(Sensor.TYPE_GRAVITY)
            if (gravity != null) {
                sensors.add(
                    buildSensorInfo(
                        typeName = "Gravity Sensor",
                        sensor = gravity,
                        purpose = "Gravity vector tracking for motion blur reduction"
                    )
                )
            }

            // 7. Barometer / Pressure (Altitude EXIF tag)
            val pressure = sm.getDefaultSensor(Sensor.TYPE_PRESSURE)
            if (pressure != null) {
                sensors.add(
                    buildSensorInfo(
                        typeName = "Barometer (Pressure)",
                        sensor = pressure,
                        purpose = "Atmospheric altitude estimation for photo EXIF tags"
                    )
                )
            }
        } catch (_: Exception) { }

        return sensors
    }

    private fun buildSensorInfo(typeName: String, sensor: Sensor?, purpose: String): HardwareSensorInfo {
        return if (sensor != null) {
            HardwareSensorInfo(
                typeName = typeName,
                name = sensor.name ?: "Generic $typeName",
                vendor = sensor.vendor ?: "Hardware Vendor",
                version = sensor.version,
                powerMa = sensor.power,
                maxRange = sensor.maximumRange,
                resolution = sensor.resolution,
                purpose = purpose,
                isAvailable = true
            )
        } else {
            HardwareSensorInfo(
                typeName = typeName,
                name = "Not Available",
                vendor = "N/A",
                version = 0,
                powerMa = 0f,
                maxRange = 0f,
                resolution = 0f,
                purpose = purpose,
                isAvailable = false
            )
        }
    }

    fun buildCameraOverview(specs: List<CameraSpec>): CameraSubsystemOverview {
        // True physical camera sensors on the device:
        // - Any spec that is marked isPhysical == true
        // - Plus front camera #1 (if not already physical)
        // - Excludes logical pipeline #0 when physical back sensors exist
        val hasPhysicalBackSensors = specs.any { it.isPhysical && !it.isFront }
        val logicalPipelines = specs.filter {
            !it.isPhysical && (it.physicalCameraIds.isNotEmpty() || (hasPhysicalBackSensors && it.cameraId == "0"))
        }
        val physicalSensors = specs.filter {
            it.isPhysical || (it !in logicalPipelines)
        }
        val backSensors = physicalSensors.filter { !it.isFront }
        val frontSensors = physicalSensors.filter { it.isFront }

        return CameraSubsystemOverview(
            totalSensorsCount = physicalSensors.size,
            logicalCamerasCount = logicalPipelines.size,
            physicalCamerasCount = physicalSensors.size,
            backCamerasCount = backSensors.size,
            frontCamerasCount = frontSensors.size,
            isMultiCameraSupported = specs.any { it.isLogicalMultiCamera || it.physicalCameraIds.isNotEmpty() },
            isConcurrentDualSupported = specs.any { it.isConcurrentSupported },
            is10BitHdrSupported = specs.any { it.is10BitHdrSupported }
        )
    }
}

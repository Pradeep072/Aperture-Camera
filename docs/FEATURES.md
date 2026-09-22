# Implemented Features Registry

This document lists every implemented feature in **Iris Camera** alongside its implementing Kotlin class and package path.

---

## 1. Camera & Hardware Layer

| Feature | Description | Implementing File / Class |
|---|---|---|
| **Camera Hardware Detection** | Queries `CameraManager` and `CameraCharacteristics` to enumerate logical and physical camera sensors, lens types, pixel arrays, and hardware levels. | [`CameraHardwareDetector.kt`](../app/src/main/java/com/aperture/camera/camera/CameraHardwareDetector.kt)<br>`com.aperture.camera.camera.CameraHardwareDetector` |
| **Quad-Bayer Sensor Analysis** | Detects 4-in-1 Quad-Bayer pixel binning matrices (e.g., 50.0 MP physical matrix $\to$ 12.5 MP binned output). | [`CameraHardwareDetector.kt`](../app/src/main/java/com/aperture/camera/camera/CameraHardwareDetector.kt)<br>`com.aperture.camera.camera.CameraHardwareDetector` |
| **CameraX Lifecycle Controller** | Manages `ProcessCameraProvider`, binds `Preview`, `ImageCapture`, and `VideoCapture` use cases with Studio-grade ISP pipeline. | [`CameraManagerController.kt`](../app/src/main/java/com/aperture/camera/camera/CameraManagerController.kt)<br>`com.aperture.camera.camera.CameraManagerController` |
| **Dynamic Lens Switch Manager** | Resolves verified lens options (`1x`, `2x`, `Front`) and applies safe zoom ratios clamped to the device range. | [`LensSwitchManager.kt`](../app/src/main/java/com/aperture/camera/camera/LensSwitchManager.kt)<br>`com.aperture.camera.camera.LensSwitchManager` |
| **Pro Mode Sensor Controller** | Sets manual ISO sensitivity, manual shutter speed (exposure time), manual focus distance (diopters), and white balance color temperature. | [`ProModeController.kt`](../app/src/main/java/com/aperture/camera/camera/ProModeController.kt)<br>`com.aperture.camera.camera.ProModeController` |
| **Video Recording Manager** | Manages CameraX `Recorder`, quality profiles (SD, HD, FHD, UHD), 30/60 FPS, EIS stabilization, pause/resume, and audio toggling. | [`VideoRecorderManager.kt`](../app/src/main/java/com/aperture/camera/camera/VideoRecorderManager.kt)<br>`com.aperture.camera.camera.VideoRecorderManager` |
| **Concurrent Dual Camera Stream** | Manages concurrent front + back camera sessions on devices supporting concurrent multi-streaming. | [`ConcurrentCameraManager.kt`](../app/src/main/java/com/aperture/camera/camera/ConcurrentCameraManager.kt)<br>`com.aperture.camera.camera.ConcurrentCameraManager` |
| **Device Hardware Telemetry** | Collects RAM usage, storage breakdown, SoC chipset info, display specs, and camera-relevant hardware sensors (Gyro, Accel, Light, Proximity). | [`DeviceHardwareDetector.kt`](../app/src/main/java/com/aperture/camera/camera/DeviceHardwareDetector.kt)<br>`com.aperture.camera.camera.DeviceHardwareDetector` |

---

## 2. Document Scanning & Image Processing

| Feature | Description | Implementing File / Class |
|---|---|---|
| **Corner Detection & Aspect Ratio Estimation** | Detects or initializes 4-corner document polygons and calculates quadrilateral perspective width and height. | [`DocumentScannerHelper.kt`](../app/src/main/java/com/aperture/camera/document/DocumentScannerHelper.kt)<br>`com.aperture.camera.document.DocumentScannerHelper` |
| **Perspective Warp Transformation** | Applies geometric `Matrix.setPolyToPoly` perspective transform to un-skew document captures into upright rectangular images. | [`DocumentScannerHelper.kt`](../app/src/main/java/com/aperture/camera/document/DocumentScannerHelper.kt)<br>`com.aperture.camera.document.DocumentScannerHelper` |
| **Document Filters** | Applies High-Contrast B&W binarization, Magic Color contrast enhancement, and standard color adjustments to documents. | [`DocumentScannerHelper.kt`](../app/src/main/java/com/aperture/camera/document/DocumentScannerHelper.kt)<br>`com.aperture.camera.document.DocumentScannerHelper` |
| **Optimized PDF Generation** | Generates standard A4 multi-page/single-page PDFs with 200 DPI JPEG compression (drops PDF size by 98% to ~300KB). | [`DocumentScannerHelper.kt`](../app/src/main/java/com/aperture/camera/document/DocumentScannerHelper.kt)<br>`com.aperture.camera.document.DocumentScannerHelper` |
| **Interactive Document Editor UI** | 4-corner drag handles with edge margins to prevent back gesture interference, live crop preview, page selector, and export buttons. | [`DocumentScanEditorScreen.kt`](../app/src/main/java/com/aperture/camera/ui/screens/DocumentScanEditorScreen.kt)<br>`com.aperture.camera.ui.screens.DocumentScanEditorScreen` |

---

## 3. Storage, Repositories & Location

| Feature | Description | Implementing File / Class |
|---|---|---|
| **MediaStore Storage Repository** | Saves photos to `DCIM/Camera/`, videos to `Movies/ApertureCamera/`, and PDFs to `Documents/ApertureScanner/` with EXIF tag embedding. | [`MediaStoreRepository.kt`](../app/src/main/java/com/aperture/camera/data/repository/MediaStoreRepository.kt)<br>`com.aperture.camera.data.repository.MediaStoreRepository` |
| **Settings Repository** | Manages persistent user preferences (grid type, geotagging, shutter sound, video quality, target FPS, EIS) using Jetpack DataStore Preferences. | [`SettingsRepository.kt`](../app/src/main/java/com/aperture/camera/data/repository/SettingsRepository.kt)<br>`com.aperture.camera.data.repository.SettingsRepository` |
| **GPS Location Provider** | Queries fused location provider for GPS coordinates when photo geotagging is enabled. | [`LocationProviderHelper.kt`](../app/src/main/java/com/aperture/camera/location/LocationProviderHelper.kt)<br>`com.aperture.camera.location.LocationProviderHelper` |

---

## 4. UI Screens & Viewfinder Components

| Feature | Description | Implementing File / Class |
|---|---|---|
| **Main Camera Viewfinder** | Central viewfinder with swipeable mode selector, shutter button, quick flip, top action bar, and double-back exit handler. | [`MainCameraScreen.kt`](../app/src/main/java/com/aperture/camera/ui/screens/MainCameraScreen.kt)<br>`com.aperture.camera.ui.screens.MainCameraScreen` |
| **Viewfinder Preview Surface** | Encapsulates CameraX `PreviewView`, touch gesture detectors (tap-to-focus, pinch-to-zoom, EV drag), and composition grid overlays. | [`ViewfinderPreview.kt`](../app/src/main/java/com/aperture/camera/ui/components/ViewfinderPreview.kt)<br>`com.aperture.camera.ui.components.ViewfinderPreview` |
| **Lens Selector Chips** | Interactive animated pill chips for switching between `1x`, `2x`, and `Front` lenses. | [`LensSelectorChips.kt`](../app/src/main/java/com/aperture/camera/ui/components/LensSelectorChips.kt)<br>`com.aperture.camera.ui.components.LensSelectorChips` |
| **Mode Selector Bar** | Swipeable bottom navigation bar for switching between `PHOTO`, `DOCS`, `VIDEO`, `PRO`, and `DUAL` modes. | [`ModeSelectorBar.kt`](../app/src/main/java/com/aperture/camera/ui/components/ModeSelectorBar.kt)<br>`com.aperture.camera.ui.components.ModeSelectorBar` |
| **Pro Controls Panel** | Bottom sliders for manual ISO, Shutter Speed, Focus Distance, and White Balance with Reset action. | [`ProControlsPanel.kt`](../app/src/main/java/com/aperture/camera/ui/components/ProControlsPanel.kt)<br>`com.aperture.camera.ui.components.ProControlsPanel` |
| **Video Controls Top Bar** | Top toolbar for toggling video quality (SD/HD/FHD/UHD), 30/60 FPS, Video EIS, Torch LED, and Audio recording during video mode. | [`VideoControlsBar.kt`](../app/src/main/java/com/aperture/camera/ui/components/VideoControlsBar.kt)<br>`com.aperture.camera.ui.components.VideoControlsTopBar` |
| **About Camera Screen** | Diagnostics screen displaying expandable cards for every sensor, RAM/storage stats, Markdown report generation, Copy, and Share. | [`AboutCameraScreen.kt`](../app/src/main/java/com/aperture/camera/about/AboutCameraScreen.kt)<br>`com.aperture.camera.about.AboutCameraScreen` |
| **Specs Report Formatter** | Formats full hardware specs into structured, shareable Markdown diagnostic reports. | [`SpecsReportFormatter.kt`](../app/src/main/java/com/aperture/camera/about/SpecsReportFormatter.kt)<br>`com.aperture.camera.about.SpecsReportFormatter` |
| **Settings Dialog** | Modal bottom sheet for configuring grids, geotagging, shutter sound, RAW capture, video quality, and audio. | [`SettingsDialog.kt`](../app/src/main/java/com/aperture/camera/ui/screens/SettingsDialog.kt)<br>`com.aperture.camera.ui.screens.SettingsDialog` |
| **Media Viewer Screen** | Full-screen image/video viewer with pinch-to-zoom, file info, delete action, and system share sheet integration. | [`MediaViewerScreen.kt`](../app/src/main/java/com/aperture/camera/ui/screens/MediaViewerScreen.kt)<br>`com.aperture.camera.ui.screens.MediaViewerScreen` |
| **Dual Camera Screen** | Picture-in-Picture dual camera preview for simultaneous front + rear capture on supported devices. | [`DualCameraScreen.kt`](../app/src/main/java/com/aperture/camera/ui/screens/DualCameraScreen.kt)<br>`com.aperture.camera.ui.screens.DualCameraScreen` |
| **Permission Rationale Screen** | Onboarding screen explaining Camera, Audio, and Location permission requirements with direct Settings shortcut. | [`PermissionRationaleScreen.kt`](../app/src/main/java/com/aperture/camera/ui/screens/PermissionRationaleScreen.kt)<br>`com.aperture.camera.ui.screens.PermissionRationaleScreen` |
| **Camera View Model** | Central StateFlow/Coroutine ViewModel coordinating camera operations, UI state, timers, document scanning, and settings. | [`CameraViewModel.kt`](../app/src/main/java/com/aperture/camera/ui/viewmodel/CameraViewModel.kt)<br>`com.aperture.camera.ui.viewmodel.CameraViewModel` |

# Architecture Guide

This document outlines the architecture, package structure, unidirectional data flow (UDF), and extension guides for **Aperture Camera**.

---

## 1. High-Level Architecture Overview

Aperture follows the recommended **Android Architecture Patterns** using:
- **Clean Layered Separation**: Hardware Abstraction $\to$ Data / Repositories $\to$ ViewModel Layer $\to$ Jetpack Compose UI.
- **Unidirectional Data Flow (UDF)**: UI events flow up to the `CameraViewModel`, which updates state in `StateFlow<CameraUiState>` and `StateFlow<CameraSessionState>`. The Compose UI observes these state flows reactively.
- **Structured Concurrency**: All camera I/O, MediaStore disk writes, and document processing run in background Coroutine dispatchers (`Dispatchers.IO` / `Dispatchers.Default`).

```
┌─────────────────────────────────────────────────────────────┐
│                    Jetpack Compose UI                       │
│  (MainCameraScreen, ViewfinderPreview, ModeSelectorBar,     │
│   LensSelectorChips, ProControlsPanel, DocumentScanEditor)  │
└──────────────────────────────▲──────────────────────────────┘
                               │ Observes UI State
                               │ Dispatches Actions
┌──────────────────────────────▼──────────────────────────────┐
│                      CameraViewModel                        │
│   (StateFlow<CameraUiState>, countdown timers, media logic) │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
┌──────────────▼──────────────┐ ┌──────────────▼──────────────┐
│   CameraManagerController   │ │      Data Repositories      │
│ (CameraX / Camera2 Interop) │ │ (MediaStoreRepo, Settings)  │
└──────────────┬──────────────┘ └──────────────┬──────────────┘
               │                               │
┌──────────────▼───────────────────────────────▼──────────────┐
│              Android Camera2 & MediaStore HAL               │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Package Structure

```
com.aperture.camera/
├── ApertureApp.kt                # Application subclass
├── MainActivity.kt               # Single activity hosting Compose UI
│
├── camera/                       # Low-level camera & hardware layer
│   ├── CameraHardwareDetector.kt # Probes CameraManager & CameraCharacteristics
│   ├── CameraManagerController.kt# CameraX ProcessCameraProvider wrapper
│   ├── LensSwitchManager.kt      # Safe zoom ratio & lens badge switching
│   ├── ProModeController.kt      # Manual ISO, Shutter Speed, Focus, WB
│   ├── VideoRecorderManager.kt   # CameraX VideoCapture & Recorder management
│   ├── ConcurrentCameraManager.kt# Dual Front+Back simultaneous stream handler
│   └── DeviceHardwareDetector.kt # System RAM, storage, SoC & sensor telemetry
│
├── document/                     # Document scanning & PDF engine
│   └── DocumentScannerHelper.kt  # Perspective warp matrix, filters & PDF export
│
├── data/
│   ├── model/                    # Immutable data models
│   │   ├── CameraSpec.kt         # Comprehensive camera hardware metadata
│   │   ├── LensInfo.kt           # LensBadge and LensType definitions
│   │   ├── CaptureMode.kt        # PHOTO, DOCS, VIDEO, PRO, DUAL
│   │   ├── CameraSettings.kt     # Preferences model
│   │   ├── DeviceDiagnostics.kt  # System hardware diagnostics model
│   │   └── MediaItem.kt          # Captured media model
│   └── repository/
│       ├── MediaStoreRepository.kt # Photo, video & document file persistence
│       └── SettingsRepository.kt   # DataStore preferences persistence
│
├── location/
│   └── LocationProviderHelper.kt # GPS location queries for EXIF geotagging
│
├── about/                        # Hardware diagnostics & specs screen
│   ├── AboutCameraScreen.kt      # Expandable sensor cards & specs UI
│   ├── AboutCameraViewModel.kt   # Diagnostics state holder
│   └── SpecsReportFormatter.kt   # Formatted Markdown report generator
│
└── ui/
    ├── theme/                    # App theme, typography & palette
    ├── components/               # Modular Compose components
    │   ├── ViewfinderPreview.kt
    │   ├── LensSelectorChips.kt
    │   ├── ModeSelectorBar.kt
    │   ├── ProControlsPanel.kt
    │   ├── VideoControlsBar.kt
    │   ├── FocusMeteringIndicator.kt
    │   ├── ExposureSlider.kt
    │   ├── TimerCountdownOverlay.kt
    │   └── GridOverlay.kt
    ├── screens/                  # Screen-level composables
    │   ├── MainCameraScreen.kt
    │   ├── DocumentScanEditorScreen.kt
    │   ├── MediaViewerScreen.kt
    │   ├── DualCameraScreen.kt
    │   ├── SettingsDialog.kt
    │   └── PermissionRationaleScreen.kt
    └── viewmodel/
        └── CameraViewModel.kt    # Root camera UI ViewModel
```

---

## 3. Data Flow

1. **Initialization**:
   - App requests permissions in [`MainActivity.kt`](../app/src/main/java/com/aperture/camera/MainActivity.kt).
   - Upon permission grant, [`CameraViewModel.kt`](../app/src/main/java/com/aperture/camera/ui/viewmodel/CameraViewModel.kt) executes [`CameraHardwareDetector.detectAllCameras()`](../app/src/main/java/com/aperture/camera/camera/CameraHardwareDetector.kt) and builds verified lens badges.
   - [`CameraManagerController.initialize()`](../app/src/main/java/com/aperture/camera/camera/CameraManagerController.kt) retrieves `ProcessCameraProvider`.
2. **Viewfinder Surface Binding**:
   - [`ViewfinderPreview.kt`](../app/src/main/java/com/aperture/camera/ui/components/ViewfinderPreview.kt) provides the `PreviewView.SurfaceProvider`.
   - `CameraViewModel.bindCamera()` binds `Preview`, `ImageCapture`, and optionally `VideoCapture` according to the current `CaptureMode`.
3. **Capture Execution**:
   - Tapping the shutter button dispatches `CameraViewModel.triggerShutter()`.
   - In `PHOTO` mode, `CameraManagerController.takePhoto()` captures bytes, embeds EXIF GPS tags, writes to `MediaStoreRepository`, and emits the new thumbnail bitmap to the UI.
   - In `DOCS` mode, `CameraManagerController.captureDocumentBitmap()` decodes the frame and launches `DocumentScanEditorScreen` for 4-corner perspective adjustment and PDF export.

---

## 4. Extension Guides

### A. How to Add a New Camera Mode
1. **Define the Enum**: Add the new mode to [`CaptureMode.kt`](../app/src/main/java/com/aperture/camera/data/model/CaptureMode.kt):
   ```kotlin
   enum class CaptureMode(val displayName: String) {
       PHOTO("PHOTO"),
       DOCS("DOCS"),
       VIDEO("VIDEO"),
       PRO("PRO"),
       DUAL("DUAL"),
       NIGHT("NIGHT") // New mode
   }
   ```
2. **Update Viewfinder Controls**: In [`ModeSelectorBar.kt`](../app/src/main/java/com/aperture/camera/ui/components/ModeSelectorBar.kt), the bar automatically adapts to `CaptureMode.entries`.
3. **Handle Use Case Binding**: In [`CameraManagerController.bindCameraUseCases()`](../app/src/main/java/com/aperture/camera/camera/CameraManagerController.kt), handle any special use case configuration (e.g., enabling CameraX `NightImageCaptureExtender` or slow-motion high-speed session).
4. **Trigger Shutter**: In [`CameraViewModel.triggerShutter()`](../app/src/main/java/com/aperture/camera/ui/viewmodel/CameraViewModel.kt), specify the capture action for the new mode.

### B. How to Add a New Document Filter
1. **Define the Filter**: In [`DocumentScannerHelper.kt`](../app/src/main/java/com/aperture/camera/document/DocumentScannerHelper.kt), add the filter to `DocumentFilterMode`:
   ```kotlin
   enum class DocumentFilterMode(val label: String) {
       ORIGINAL("Original"),
       COLOR_ENHANCED("Color Scan"),
       BW_CLEAN("B&W Clean"),
       MAGIC_COLOR("Magic Color"),
       SEPIA_DOC("Warm Vintage") // New filter
   }
   ```
2. **Implement the Processing Logic**: In `DocumentScannerHelper.applyFilter()`, add the corresponding `ColorMatrix` or pixel transformation algorithm.

### C. How to Add New Hardware Telemetry
1. **Update Data Model**: Add the field to [`DeviceDiagnostics.kt`](../app/src/main/java/com/aperture/camera/data/model/DeviceDiagnostics.kt) (e.g., Battery Temperature or Camera Sensor Temperature).
2. **Query the System API**: In [`DeviceHardwareDetector.kt`](../app/src/main/java/com/aperture/camera/camera/DeviceHardwareDetector.kt), query the relevant Android system service (e.g., `BatteryManager` or `HardwarePropertiesManager`).
3. **Render in Diagnostics UI**: Add the metric to [`AboutCameraScreen.kt`](../app/src/main/java/com/aperture/camera/about/AboutCameraScreen.kt) and include it in [`SpecsReportFormatter.kt`](../app/src/main/java/com/aperture/camera/about/SpecsReportFormatter.kt).

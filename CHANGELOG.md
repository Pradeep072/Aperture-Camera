# Changelog

All notable changes to **Aperture Camera** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.2.1] - 2026-09-23

### Fixed
- **Dual Camera Capture & Composite PiP Photo Generation**:
  - Resolved `IllegalArgumentException: Not bound to a valid Camera` when triggering the shutter in Dual Camera mode.
  - Implemented `triggerDualPhotoCapture` and `createDualCompositeBitmap` to capture and composite both primary and secondary preview frames into a single high-fidelity Picture-in-Picture JPEG with rounded corners, Aperture gold border (`#FFD600`), and EXIF metadata saved directly to MediaStore.
- **Dual Mode Visibility & Hardware Explanation**:
  - Always display `DUAL` mode in the bottom mode selector across all devices. On devices without concurrent multi-camera ISP hardware support, selecting `DUAL` now presents an informative explanation card detailing the vendor HAL limitation rather than omitting the tab.
  - Hoisted `isDualSwapped` state and toggle callback in `MainCameraScreen` for seamless camera role swapping during dual capture.

---

## [1.2.0] - 2026-09-22

### Added
- **Front Camera Punch-Hole Halo Ring Overlay (`FrontCameraHaloOverlay.kt`)**:
  - Automatically detects physical display cutout / punch-hole bounds using Android's `DisplayCutout` API with concentric lens alignment.
  - Smooth 360° illuminated golden sweep arc with radial breathing glow pulse upon front camera activation.
  - Synchronized circular countdown progress ring around the physical selfie lens during 3s/10s timer capture.
- **Dual-Metric Sensor Resolution (Advertised Hardware Matrix vs. Active HAL Output)**:
  - Accurately classifies and displays both manufacturer-advertised sensor matrix tiers (50 MP, 64 MP, 100 MP, 108 MP, 200 MP) and active Camera2 HAL capture streams (12.0 MP / 12.5 MP / 16 MP / 8 MP).
  - High-tech **Pixel Binning Explainer Card** in the About section detailing Quad-Bayer (4-in-1), Nonacell (9-in-1), super-pixel fusion, and OEM HAL stream behavior.
  - Formatted markdown diagnostics report with copy and system share sheet integration.
- **Enhanced Dual Camera PiP Mode**:
  - 40% larger Picture-in-Picture secondary camera preview with active camera lens badge (`FRONT`/`REAR`) and explicit stream swap button.
- **Visual & Audio Shutter Feedback**:
  - High-speed white screen flash overlay feedback (`ShutterFlashOverlay.kt`), system audio shutter cue, haptic vibration pulse, and thumbnail animation.

### Fixed
- **Photo Capture & MediaStore Save Stability**:
  - Removed HAL-incompatible Camera2 interop hardware requirement keys (`LENS_OPTICAL_STABILIZATION_MODE`, `DISTORTION_CORRECTION_MODE`, `SHADING_MODE`) on front cameras and devices without hardware OIS coils to eliminate `ERROR_CAPTURE_FAILED` crashes.
  - Refactored `MediaStoreRepository.kt` to inject EXIF tags prior to MediaStore insertion, resolving file descriptor locks and `SecurityException` under Scoped Storage.
  - Added legacy external storage permission declarations for Android 8/9 backward compatibility.

---

## [1.1.0] - 2026-09-22

## [1.0.0] - 2026-09-20

### Added
- **Full Camera Hardware Diagnostics**:
  - Direct probing of all physical and logical camera sensors via `CameraCharacteristics`.
  - Detection and breakdown of 50 MP and 32 MP Quad-Bayer 4-in-1 pixel binning arrays alongside standard output megapixels.
  - Calculation of 35mm-equivalent focal lengths, physical focal lengths, sensor physical dimensions, crop factors, and FOV.
  - Live RAM, storage capacity, SoC chipset model, display resolution, and refresh rate reporting.
  - One-tap Markdown specs report generation with clipboard copy and system share sheet actions.
- **Dynamic Lens Switching**:
  - Grounded lens options (`1x`, `2x`, `Front`) mapped to verified device hardware capabilities.
- **Document Scanner & PDF Generation**:
  - Interactive 4-corner perspective selection with magnifying loupe handles and screen edge padding.
  - Geometric `Matrix.setPolyToPoly` perspective transform.
  - 4 Document filters: *Color Scan*, *B&W High-Contrast*, *Magic Color*, and *Original Photo*.
  - Standard A4 PDF generation with 200 DPI JPEG compression (98% size reduction to ~300KB).
- **Pro Manual Mode**:
  - Manual ISO slider ($50 \dots 3200$), Shutter Speed ($1/4000\text{s} \dots 1\text{s}$), Focus Distance ($0 \dots 10\text{ diopters}$), and Kelvin White Balance ($2500\text{K} \dots 8500\text{K}$).
- **Video Recording**:
  - Quality selector (SD 480p, HD 720p, FHD 1080p, UHD 4K), 30/60 FPS toggle, EIS stabilization toggle, torch LED toggle, audio mute, and pause/resume.
- **UX & Safety**:
  - Direct system Photos/Gallery app launch on thumbnail click via `Intent.ACTION_VIEW`.
  - Double-back press to exit protection (2-second window).
  - Torch flash mode in cyclical toggle (`OFF -> AUTO -> ON -> TORCH -> OFF`).
  - GPS geotagging with fused location provider and EXIF tags.
- **Adaptive Launcher Icon & Branding**:
  - Modern geometric 6-blade aperture vector icon and updated name to **Aperture**.

---

## [0.5.0] - 2026-09-18

### Added
- Multi-mode swipeable navigation bar (Photo, Docs, Video, Pro, Dual).
- Initial Document Scanner prototype with 4-corner polygon selection.
- Studio-quality Camera2 ISP pipeline (High-Quality noise reduction, edge mode, tonemap, and distortion correction).

---

## [0.2.0] - 2026-09-15

### Added
- Pro Manual Controls panel with sliders for ISO, Exposure Time, Focus Distance, and White Balance.
- Video mode integration using CameraX `Recorder` and `VideoCapture`.
- Settings bottom sheet with DataStore persistence (grid lines, geotagging, shutter sounds, video quality).

---

## [0.1.0] - 2026-09-10

### Added
- Initial project setup with Kotlin 2.0 and Jetpack Compose Material 3.
- Basic CameraX `ProcessCameraProvider` binding for Preview and ImageCapture.
- Viewfinder preview with tap-to-focus and pinch-to-zoom.
- MediaStore photo persistence to `DCIM/Camera/`.

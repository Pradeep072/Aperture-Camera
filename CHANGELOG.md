# Changelog

All notable changes to **Aperture Camera** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

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

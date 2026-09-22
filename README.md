# Aperture Camera

> A high-performance, native Android photography, document scanning, and video camera app built with modern Android development best practices.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![CameraX](https://img.shields.io/badge/CameraX-1.4.1-green.svg)](https://developer.android.com/training/camerax)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/API-26%2B%20(Android%208.0%2B)-brightgreen.svg)](https://android-arsenal.com/api?level=26)

---

## Overview

**Aperture** is a native Android camera application engineered from the ground up to deliver uncompromising capture quality, full camera sensor diagnostics, professional manual exposure controls, multi-mode capture (Photo, Document Scanner with perspective crop & PDF generation, Video, Pro Manual, and Dual Camera), and real-time hardware telemetry.

Aperture interfaces directly with the Android **Camera2 API** and **Jetpack CameraX 1.4.1**, providing hardware-grounded lens detection, Quad-Bayer pixel binning resolution analysis, and studio-grade image processing pipeline.

---

## Screenshots

| Viewfinder & Lenses | Pro Manual Controls | Document Scanner & PDF | Hardware Diagnostics |
| :---: | :---: | :---: | :---: |
| *(Screenshot Placeholder)* | *(Screenshot Placeholder)* | *(Screenshot Placeholder)* | *(Screenshot Placeholder)* |

---

## Features

### 1. Hardware Detection & Diagnostics ("About Camera" Screen)
- **Comprehensive Sensor Enumeration**: Probes every logical and physical camera sensor on the device using `CameraManager` and `CameraCharacteristics`.
- **Quad-Bayer & Pixel Binning Detection**: Analyzes high-resolution sensor matrices (e.g., 50.0 MP Quad-Bayer vs 12.5 MP binned output, 32.0 MP Front vs 8.0 MP binned output).
- **Physical Lens Optics**: Calculates physical focal lengths, 35mm-equivalent focal lengths, sensor physical dimensions, crop factors, horizontal/vertical field of view (FOV), and maximum apertures.
- **Hardware Capabilities Telemetry**: Inspects optical stabilization (OIS), electronic video stabilization (EIS), 10-bit HDR / dynamic range profiles, flash units, auto-focus modes, minimum focus distances (cm/diopters), ISO sensitivity ranges, exposure duration ranges, and exposure compensation steps.
- **System Resource Monitoring**: Live RAM usage, total/available memory, internal storage capacity, SoC chipset model, display resolution, and refresh rate.
- **Diagnostics Reporting**: One-tap **Copy Report** (formatted Markdown) and **Share** (system share sheet).

### 2. Viewfinder & Lens Switching
- **Dynamic Lens Badges**: Automatically resolves verified, rock-solid lens options (**`1x`**, **`2x`**, **`Front`**) grounded in device hardware capabilities.
- **Touch Interactions**: Tap-to-focus with animated metering ring, vertical exposure compensation slider (`+/- EV`), and smooth pinch-to-zoom up to maximum digital zoom.
- **Composition Grids**: Off, 3x3 (Rule of Thirds), Golden Ratio ($1:1.618$), and 1:1 Square Crop overlay.
- **Flash / Torch Controls**: Off, Auto, On, and continuous flashlight **Torch** mode.
- **Self-Timer**: Off, 3 seconds, and 10 seconds with animated full-screen countdown overlay.
- **Double-Back Exit Protection**: Prevents accidental exits with a 2-second confirmation window.

### 3. Capture Modes
- **Photo Mode**: High-resolution image capture with JPEG 100% quality, studio noise reduction, edge enhancement, and full EXIF metadata preservation (Lens Model, Focal Length, Aperture, ISO, Shutter Speed, and optional GPS Geotagging).
- **Document Scanner (DOCS)**:
  - Interactive 4-corner perspective selection with magnifying loupe handles and screen edge padding.
  - Quadrilateral perspective warp and aspect ratio normalization.
  - 4 Document filters: **Color Scan**, **B&W (High-Contrast Document)**, **Magic Color (Enhanced Clarity)**, and **Original Photo**.
  - Direct single-page or multi-page **PDF generation** with JPEG compression (optimizes file size from ~35MB to ~300KB) and instant save to `Documents/ApertureScanner/` or `DCIM/Camera/`.
- **Video Mode**: Hardware-accelerated recording with quality selector (**SD 480p**, **HD 720p**, **FHD 1080p**, **UHD 4K**), 30/60 FPS toggle, EIS video stabilization toggle, live duration counter, and pause/resume capabilities.
- **Pro Mode**: Manual ISO sensitivity adjustment ($50 \dots 3200$), manual Shutter Speed ($1/4000\text{s} \dots 1\text{s}$), manual Focus Distance ($0.0 \dots 10.0\text{ diopters}$), and manual White Balance color temperature ($2500\text{K} \dots 8500\text{K}$).
- **Dual Camera Mode**: Concurrent Front + Back camera streaming in Picture-in-Picture (PiP) layout on supported devices.

### 4. Media Storage & Gallery Integration
- **Direct System Gallery Launch**: Tapping the thumbnail directly opens Google Photos / Moto Gallery in full resolution via `Intent.ACTION_VIEW`.
- **MediaStore Standards**: Automatically registers photos in `DCIM/Camera/`, videos in `Movies/ApertureCamera/`, and PDFs in `Documents/ApertureScanner/`.

---

## Tech Stack

| Component | Technology | Description |
|---|---|---|
| **Language** | Kotlin `2.0.21` | 100% Kotlin codebase |
| **UI Framework** | Jetpack Compose + Material 3 | Declarative modern UI architecture |
| **Camera Framework** | CameraX `1.4.1` + Camera2 Interop | Unified camera use cases and low-level sensor access |
| **Architecture** | MVVM + Unidirectional Data Flow | StateFlow / Coroutines reactive pattern |
| **Image Loading** | Coil `2.7.0` | High-performance asynchronous image loading |
| **Storage & Prefs** | Jetpack DataStore Preferences `1.1.1` | Asynchronous key-value settings storage |
| **Metadata** | AndroidX ExifInterface `1.3.7` | EXIF tag embedding & orientation parsing |
| **Build Tooling** | Gradle `8.11` + AGP `8.7.2` | Gradle Version Catalog (`libs.versions.toml`) |

---

## Build & Installation

### Prerequisites
- **Android Studio**: Ladybug (2024.2+), Koala, or newer.
- **JDK**: Java 17+ (e.g. bundled Android Studio JBR).
- **Android SDK**: `minSdkVersion 26` (Android 8.0 Oreo), `targetSdkVersion 35` (Android 15).

### 1. Build via Android Studio
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/ApertureCamera.git
   ```
2. Open Android Studio and choose **Open** $\to$ select the `ApertureCamera` directory.
3. Allow Gradle to sync the project dependencies automatically.
4. Select your target device or emulator and press **Run** (`Shift + F10`).

### 2. Build via Command Line
```bash
# Assemble Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew test

# Run Android Lint
./gradlew lint
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Running on a Physical Device

1. On your Android device, navigate to **Settings** $\to$ **About Phone**.
2. Tap **Build Number** 7 times to enable **Developer Options**.
3. In **Developer Options**, enable **USB Debugging**.
4. Connect your phone to your computer via USB cable.
5. Deploy and install directly using:
   ```bash
   ./gradlew installDebug
   ```

---

## Known Hardware Limitations

1. **OEM Auxiliary Camera Access**:
   - Some device manufacturers (e.g., certain Qualcomm / MediaTek implementations) restrict direct third-party access to auxiliary physical camera IDs (e.g., separate physical macro or ultra-wide endpoints).
   - Aperture automatically detects supported optical zoom ratios and routes requests through verified logical camera pipelines to prevent black screens.
2. **Concurrent Dual Streaming**:
   - Simultaneous front and rear camera capture requires hardware Image Signal Processor (ISP) support exposed through `CameraManager.getConcurrentCameraIds()`. If unsupported by the device SoC, Aperture clearly indicates hardware incompatibility.
3. **Camera2 Hardware Levels (`LEGACY` / `LIMITED` / `FULL` / `LEVEL_3`)**:
   - Devices operating in `LEGACY` mode do not support manual exposure duration or manual ISO controls in Pro mode.

---

## Community & Contribution

Contributions, bug reports, and feature suggestions are welcome!
- Please read our [CONTRIBUTING.md](CONTRIBUTING.md) for contribution workflows, code style, and testing requirements.
- Review our [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before participating.
- Check [ROADMAP.md](ROADMAP.md) for planned features and good first issues.
- For security disclosures, see [SECURITY.md](SECURITY.md).

---

## License

```
Copyright 2026 Pradeep Maurya

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

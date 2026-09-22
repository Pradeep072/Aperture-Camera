# Project Roadmap & Future Ideas

This roadmap outlines planned features, enhancements, and community ideas for **Iris Camera**. Items are tagged with `good first issue`, `help wanted`, and `future milestone`.

---

## 🟢 Good First Issues (Beginner Friendly)

- [ ] **Custom Grid Colors & Opacity Settings** `good first issue`
  - Allow users to customize composition grid line color (White, Yellow, Cyan, Red) and stroke thickness in Settings.
  - *Files*: [`GridOverlay.kt`](app/src/main/java/com/aperture/camera/ui/components/GridOverlay.kt), [`CameraSettings.kt`](app/src/main/java/com/aperture/camera/data/model/CameraSettings.kt).

- [ ] **Haptic Shutter Feedback** `good first issue`
  - Add optional subtle vibration feedback on shutter click, lens change, and exposure slider adjustments.
  - *Files*: [`CameraViewModel.kt`](app/src/main/java/com/aperture/camera/ui/viewmodel/CameraViewModel.kt).

- [ ] **Volume Button Shutter Action** `good first issue`
  - Allow volume up/down buttons to trigger photo capture, start/stop video recording, or zoom.
  - *Files*: [`MainActivity.kt`](app/src/main/java/com/aperture/camera/MainActivity.kt).

- [ ] **Display Real-time FPS Counter** `good first issue`
  - Add a lightweight composable overlay showing real-time viewfinder preview FPS and frame drops.
  - *Files*: [`ViewfinderPreview.kt`](app/src/main/java/com/aperture/camera/ui/components/ViewfinderPreview.kt).

---

## 🟡 Help Wanted (Community Contributions)

- [ ] **Real-time Exposure Histogram / Waveform Monitor** `help wanted`
  - Compute a live luminance (RGB / Luma) histogram from `ImageAnalysis` analyzer to assist in manual exposure metering in Pro mode.
  - *Files*: [`ProControlsPanel.kt`](app/src/main/java/com/aperture/camera/ui/components/ProControlsPanel.kt), [`CameraManagerController.kt`](app/src/main/java/com/aperture/camera/camera/CameraManagerController.kt).

- [ ] **Custom 3D LUT Photographic Color Grading** `help wanted`
  - Implement a `.cube` or `.png` 3D Look-Up Table (LUT) shader engine for custom film simulations and cinema color grades on live preview.
  - *Files*: [`CameraManagerController.kt`](app/src/main/java/com/aperture/camera/camera/CameraManagerController.kt).

- [ ] **Multi-Page Batch Document Scanning** `help wanted`
  - Enhance Document Scanner to support continuous multi-page capture with reordering, thumbnail carousel, and bulk PDF export.
  - *Files*: [`DocumentScanEditorScreen.kt`](app/src/main/java/com/aperture/camera/ui/screens/DocumentScanEditorScreen.kt), [`DocumentScannerHelper.kt`](app/src/main/java/com/aperture/camera/document/DocumentScannerHelper.kt).

- [ ] **External Bluetooth / USB-C Microphone Audio Routing** `help wanted`
  - Detect connected Bluetooth headsets and USB microphones, enabling external audio source selection in Video Mode.
  - *Files*: [`VideoRecorderManager.kt`](app/src/main/java/com/aperture/camera/camera/VideoRecorderManager.kt), [`VideoControlsBar.kt`](app/src/main/java/com/aperture/camera/ui/components/VideoControlsBar.kt).

---

## 🔵 Future Milestones

- [ ] **RAW + DNG Post-Processing Editor** `future milestone`
  - Capture 16-bit sensor DNG files via `ImageFormat.RAW_SENSOR` and provide an in-app RAW development editor (exposure curves, shadow recovery, white point balance).
- [ ] **Focus Peaking & Zebra Striping** `future milestone`
  - Highlighting in-focus edges with colored highlights on the live preview for precise manual focus pulling.
- [ ] **Time-lapse & Intervalometer Capture** `future milestone`
  - Configurable capture intervals ($0.5\text{s} \dots 60\text{s}$) with automated video compilation.
- [ ] **QR Code & Barcode Fast Scanner Integration** `future milestone`
  - Background ML Kit barcode detection overlay with instant URL preview and action sheet.

---

## How to Work on a Roadmap Item
1. Comment on or open an issue on GitHub stating your intent to work on the item.
2. Fork the repository and create a feature branch (`feature/your-feature-name`).
3. Follow the guidelines in [CONTRIBUTING.md](CONTRIBUTING.md) and submit a pull request!

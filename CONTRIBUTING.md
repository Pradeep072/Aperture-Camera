# Contributing to Aperture Camera

Thank you for your interest in contributing to **Aperture Camera**! We welcome contributions from the community—whether it's fixing bugs, improving documentation, adding new features, or optimizing camera performance.

---

## Code of Conduct

All contributors are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md) (Contributor Covenant 2.1). Please read it before participating.

---

## How Can I Contribute?

### 1. Reporting Bugs
- Check existing [GitHub Issues](https://github.com/your-username/ApertureCamera/issues) to avoid duplicates.
- Use our [Bug Report Template](.github/ISSUE_TEMPLATE/bug_report.md).
- **Mandatory Information for Camera Bugs**:
  - Device Manufacturer & Model (e.g., *Motorola Edge 30*, *Google Pixel 8*).
  - Android OS Version & API Level (e.g., *Android 14, API 34*).
  - SoC Chipset (e.g., *Snapdragon 778G+*).
  - Logcat output during camera initialization or capture.

### 2. Suggesting Features
- Browse [ROADMAP.md](ROADMAP.md) for existing ideas and good first issues.
- Use our [Feature Request Template](.github/ISSUE_TEMPLATE/feature_request.md).

### 3. Submitting Code (Pull Requests)

#### Development Workflow:
1. **Fork the Repository** on GitHub.
2. **Clone your fork locally**:
   ```bash
   git clone https://github.com/<your-username>/ApertureCamera.git
   cd ApertureCamera
   ```
3. **Create a topic branch**:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/bug-description
   ```
4. **Implement your changes**:
   - Follow Kotlin & Compose code conventions (4 spaces indentation, no wildcard imports).
   - Add KDoc documentation to any new public methods or classes.
   - Maintain Unidirectional Data Flow (UDF) through ViewModels.
5. **Test locally**:
   - Ensure the app builds without errors:
     ```bash
     ./gradlew assembleDebug
     ```
   - Run unit tests:
     ```bash
     ./gradlew test
     ```
   - Run Android Lint:
     ```bash
     ./gradlew lint
     ```
6. **Test on a physical device**:
   - Because Camera2 HAL behavior varies across device manufacturers, always test your changes on a real physical Android device whenever possible.
7. **Commit your changes**:
   - Write clear, concise commit messages following [Conventional Commits](https://www.conventionalcommits.org/):
     - `feat: add live exposure histogram overlay`
     - `fix: prevent potential NPE on legacy camera characteristics`
     - `docs: update build instructions for Android Studio Ladybug`
8. **Push to your fork & open a Pull Request**:
   - Fill out our [Pull Request Template](.github/PULL_REQUEST_TEMPLATE.md).
   - Link any related issue numbers (e.g., `Fixes #42`).

---

## Coding Guidelines

- **Kotlin First**: Use idiomatic Kotlin, sealed interfaces, data classes, and extension functions.
- **Compose Best Practices**:
  - Prefer stateless composables where possible.
  - Hoist state to `CameraViewModel`.
  - Avoid heavy computations inside composable bodies; use `remember` or `LaunchedEffect`.
- **Camera Safety**:
  - Always guard camera hardware characteristics queries against `null` returns.
  - Never assume auxiliary physical camera IDs can be bound directly without checking device capabilities.
- **Resource Management**:
  - Close `ImageProxy` instances promptly in callbacks to avoid stalling the CameraX frame buffer.
  - Recycle Bitmaps or leverage Kotlin garbage collection responsibly during image processing.

Thank you for helping make **Aperture Camera** even better!

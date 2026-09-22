# Third-Party Dependencies & Libraries

This document lists all libraries, SDKs, and build plugins used in **Iris Camera**, derived from [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) and [`app/build.gradle.kts`](../app/build.gradle.kts).

---

## License Compliance Summary

| License Type | Count | Permissive / Compliant |
|---|:---:|:---:|
| **Apache License 2.0** | 18 | Yes |
| **MIT License** | 0 | Yes |
| **BSD License** | 0 | Yes |
| **GPL / AGPL / Copyleft** | 0 | **None (0% GPL/Copyleft)** |

All third-party dependencies are strictly open-source and licensed under the **Apache License 2.0**, fully compliant with the project's Apache 2.0 license.

---

## Detailed Dependencies Table

| Library / Artifact | Version | Purpose in Iris | License | Source Repository |
|---|---|---|---|---|
| `androidx.core:core-ktx` | `1.15.0` | Kotlin extensions for Android core framework APIs and compat utilities | Apache 2.0 | [AndroidX Core](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/core/) |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.8.7` | Lifecycle-aware coroutine scopes and dispatchers | Apache 2.0 | [AndroidX Lifecycle](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/lifecycle/) |
| `androidx.lifecycle:lifecycle-runtime-compose` | `2.8.7` | `collectAsStateWithLifecycle()` for lifecycle-aware Compose state observation | Apache 2.0 | [AndroidX Lifecycle](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/lifecycle/) |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.8.7` | `viewModel()` integration for Jetpack Compose | Apache 2.0 | [AndroidX Lifecycle](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/lifecycle/) |
| `androidx.activity:activity-compose` | `1.9.3` | `ComponentActivity.setContent` and `BackHandler` back gesture integration | Apache 2.0 | [AndroidX Activity](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/activity/) |
| `androidx.compose:compose-bom` | `2024.10.01` | Bill of Materials for aligned Jetpack Compose dependencies | Apache 2.0 | [Jetpack Compose BOM](https://developer.android.com/jetpack/compose/bom) |
| `androidx.compose.ui:ui` | BOM | Core Jetpack Compose UI primitives, Layout, Modifiers, and Canvas rendering | Apache 2.0 | [Jetpack Compose UI](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/ui/) |
| `androidx.compose.ui:ui-graphics` | BOM | Vector drawables, Colors, Matrix transformations, and Bitmap graphics | Apache 2.0 | [Jetpack Compose Graphics](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/ui/) |
| `androidx.compose.ui:ui-tooling-preview` | BOM | `@Preview` tooling support for Android Studio design previews | Apache 2.0 | [Jetpack Compose Tooling](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/ui/) |
| `androidx.compose.material3:material3` | BOM | Material 3 UI design system components (Scaffold, Cards, Sliders, Buttons) | Apache 2.0 | [Compose Material 3](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/material3/) |
| `androidx.compose.material:material-icons-extended` | BOM | Extended Material Design icons (Flash, Timer, Info, Settings, Lens) | Apache 2.0 | [Material Icons](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/material/) |
| `androidx.navigation:navigation-compose` | `2.8.3` | Composable navigation graph and screen transitions | Apache 2.0 | [AndroidX Navigation](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/navigation/) |
| `androidx.datastore:datastore-preferences` | `1.1.1` | Non-blocking, transactional key-value persistent settings storage | Apache 2.0 | [AndroidX DataStore](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/datastore/) |
| `androidx.exifinterface:exifinterface` | `1.3.7` | Reading and writing EXIF metadata and orientation on captured photos | Apache 2.0 | [AndroidX ExifInterface](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/exifinterface/) |
| `androidx.camera:camera-core` | `1.4.1` | Core CameraX APIs, ImageProxy, CameraControl, CameraInfo, and UseCases | Apache 2.0 | [AndroidX CameraX](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/camera/) |
| `androidx.camera:camera-camera2` | `1.4.1` | Camera2 implementation of CameraX and `Camera2Interop` sensor access | Apache 2.0 | [AndroidX Camera2](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/camera/) |
| `androidx.camera:camera-lifecycle` | `1.4.1` | Lifecycle binding (`bindToLifecycle`) for ProcessCameraProvider | Apache 2.0 | [AndroidX CameraX Lifecycle](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/camera/) |
| `androidx.camera:camera-video` | `1.4.1` | High-performance video recording via CameraX `Recorder` and `VideoCapture` | Apache 2.0 | [AndroidX CameraX Video](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/camera/) |
| `androidx.camera:camera-view` | `1.4.1` | `PreviewView` viewfinder surface view and PreviewView.SurfaceProvider | Apache 2.0 | [AndroidX CameraX View](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/camera/) |
| `androidx.camera:camera-extensions` | `1.4.1` | CameraX vendor extensions (Night, HDR, Bokeh, Face Retouch) | Apache 2.0 | [AndroidX CameraX Extensions](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/camera/) |
| `io.coil-kt:coil-compose` | `2.7.0` | Asynchronous image loading for gallery thumbnails and media viewer | Apache 2.0 | [Coil Repository](https://github.com/coil-kt/coil) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.9.0` | Asynchronous background dispatchers (`Dispatchers.IO`, `Dispatchers.Main`) | Apache 2.0 | [Kotlinx Coroutines](https://github.com/Kotlin/kotlinx.coroutines) |
| `com.google.android.material:material` | `1.12.0` | Material Design Android base theming and resource utilities | Apache 2.0 | [Material Android](https://github.com/material-components/material-components-android) |

---

## Build Plugins

| Plugin ID | Version | Purpose | License |
|---|---|---|---|
| `com.android.application` | `8.7.2` | Android Gradle Plugin for compiling, packaging, and signing Android APKs | Apache 2.0 |
| `org.jetbrains.kotlin.android` | `2.0.21` | Kotlin compiler plugin for Android | Apache 2.0 |
| `org.jetbrains.kotlin.plugin.compose` | `2.0.21` | Kotlin Compose compiler plugin for Compose multiplatform runtime optimization | Apache 2.0 |

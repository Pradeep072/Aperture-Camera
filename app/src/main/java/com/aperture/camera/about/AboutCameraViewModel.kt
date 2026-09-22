package com.aperture.camera.about

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aperture.camera.camera.CameraHardwareDetector
import com.aperture.camera.camera.DeviceHardwareDetector
import com.aperture.camera.data.model.CameraSpec
import com.aperture.camera.data.model.FullDeviceDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AboutCameraUiState(
    val isLoading: Boolean = true,
    val diagnostics: FullDeviceDiagnostics? = null,
    val specs: List<CameraSpec> = emptyList(),
    val markdownReport: String = "",
    val errorMessage: String? = null
)

class AboutCameraViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "AboutCameraViewModel"
    private val detector = CameraHardwareDetector(application)
    private val deviceDetector = DeviceHardwareDetector(application)
    private val _uiState = MutableStateFlow(AboutCameraUiState())
    val uiState: StateFlow<AboutCameraUiState> = _uiState.asStateFlow()

    init {
        loadHardwareSpecs()
    }

    fun loadHardwareSpecs() {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val specs = detector.detectAllCameras()
                val diagnostics = deviceDetector.getFullDiagnostics(specs)
                val report = try {
                    SpecsReportFormatter.generateMarkdownReport(diagnostics)
                } catch (re: Exception) {
                    Log.w(tag, "Report formatting issue: ${re.message}")
                    "# Camera Hardware Report\nDetected ${specs.size} sensors."
                }

                _uiState.value = AboutCameraUiState(
                    isLoading = false,
                    diagnostics = diagnostics,
                    specs = specs,
                    markdownReport = report,
                    errorMessage = null
                )
            } catch (e: Exception) {
                Log.e(tag, "Hardware probe failed: ${e.message}", e)
                try {
                    val fallbackSpecs = detector.detectAllCameras()
                    val fallbackDiag = deviceDetector.getFullDiagnostics(fallbackSpecs)
                    _uiState.value = AboutCameraUiState(
                        isLoading = false,
                        diagnostics = fallbackDiag,
                        specs = fallbackSpecs,
                        markdownReport = "Detected ${fallbackSpecs.size} camera sensors.",
                        errorMessage = null
                    )
                } catch (fe: Exception) {
                    _uiState.value = AboutCameraUiState(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to probe device hardware"
                    )
                }
            }
        }
    }
}

package com.aperture.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aperture.camera.ui.screens.MainCameraScreen
import com.aperture.camera.ui.screens.PermissionRationaleScreen
import com.aperture.camera.ui.theme.ApertureCameraTheme
import com.aperture.camera.ui.viewmodel.CameraViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        if (cameraGranted) {
            viewModel.onPermissionsGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on while camera is open
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Check if camera permission is already granted
        if (hasCameraPermission()) {
            viewModel.onPermissionsGranted()
        }

        setContent {
            ApertureCameraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D0E11)
                ) {
                    CameraAppContent(
                        viewModel = viewModel,
                        onRequestPermissions = { requestAppPermissions() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission() && !viewModel.uiState.value.isPermissionsGranted) {
            viewModel.onPermissionsGranted()
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAppPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }
}

@Composable
private fun CameraAppContent(
    viewModel: CameraViewModel,
    onRequestPermissions: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isPermissionsGranted) {
        MainCameraScreen(viewModel = viewModel)
    } else {
        PermissionRationaleScreen(onRequestPermissions = onRequestPermissions)
    }
}

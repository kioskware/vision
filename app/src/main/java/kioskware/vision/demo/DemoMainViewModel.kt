package kioskware.vision.demo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kioskware.vision.camera.BackendCamera
import kioskware.vision.camera.BackendCameras
import kioskware.vision.camera.CameraParams
import kioskware.vision.impl.defaultBackendCameras

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var backendCameras: BackendCameras? = null
    private var currentBackendCamera: BackendCamera? = null
    private var lifecycleOwner: androidx.lifecycle.LifecycleOwner? = null
    private var diagnostics: CameraDetectionDiagnostics? = null

    fun initializeCameras(context: Context) {
        // Zapisz LifecycleOwner jeśli context jest ComponentActivity
        if (context is androidx.lifecycle.LifecycleOwner) {
            lifecycleOwner = context
        }

        // Inicjalizuj diagnostykę
        diagnostics = CameraDetectionDiagnostics(context)

        viewModelScope.launch {
            try {
                val cameras = defaultBackendCameras(context)
                backendCameras = cameras

                // Obserwuj dostępne kamery
                cameras.availableCameras.collect { availableCameras ->
                    _uiState.value = _uiState.value.copy(
                        availableCameras = availableCameras,
                        isLoading = false
                    )

                    // Automatycznie wybierz pierwszą dostępną kamerę
                    if (_uiState.value.selectedCamera == null && availableCameras.isNotEmpty()) {
                        selectCamera(availableCameras.first())
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Błąd inicjalizacji kamer: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun selectCamera(cameraParams: CameraParams) {
        viewModelScope.launch {
            try {
                // Zatrzymaj obecną kamerę jeśli jest aktywna
                currentBackendCamera?.let { camera ->
                    camera.stopCamera()
                }

                val camera = backendCameras?.getCamera(cameraParams)
                currentBackendCamera = camera

                _uiState.value = _uiState.value.copy(
                    selectedCamera = cameraParams,
                    currentBackendCamera = camera,
                    error = null
                )

                // Automatycznie uruchom nową kamerę
                startCamera()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Błąd wyboru kamery: ${e.message}"
                )
            }
        }
    }

    fun startCamera() {
        viewModelScope.launch {
            try {
                val owner = lifecycleOwner
                val camera = currentBackendCamera

                if (owner != null && camera != null) {
                    camera.startCamera(
                        lifecycleOwner = owner,
                        resolution = android.util.Size(1280, 720),
                        backpressureStrategy = kioskware.vision.BackpressureStrategy.KeepOnlyLatest,
                        targetRotation = kioskware.vision.Rotation.Degrees0,
                        outputImageFormat = kioskware.vision.OutputImageFormat.YUV_420_888,
                        imageProcessors = emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Nie można uruchomić kamery - brak LifecycleOwner lub kamery"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Błąd uruchamiania kamery: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun setCameraPermission(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(hasCameraPermission = hasPermission)
    }

    fun setDropdownExpanded(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isDropdownExpanded = expanded)
    }

    fun runCameraDiagnostics() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isDiagnosticsRunning = true,
                    diagnosticsResult = null
                )

                val report = diagnostics?.runFullDiagnostics() ?: "Diagnostyka niedostępna"

                _uiState.value = _uiState.value.copy(
                    isDiagnosticsRunning = false,
                    diagnosticsResult = report
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDiagnosticsRunning = false,
                    error = "Błąd diagnostyki: ${e.message}"
                )
            }
        }
    }

    fun clearDiagnosticsResult() {
        _uiState.value = _uiState.value.copy(diagnosticsResult = null)
    }

    override fun onCleared() {
        super.onCleared()
        // Zatrzymaj kamerę gdy ViewModel jest czyszczony
        currentBackendCamera?.let { camera ->
            viewModelScope.launch {
                try {
                    camera.stopCamera()
                } catch (e: Exception) {
                    // Log błędu przy zatrzymywaniu kamery
                    println("Błąd zatrzymywania kamery: ${e.message}")
                }
            }
        }
    }
}

data class MainUiState(
    val hasCameraPermission: Boolean = false,
    val availableCameras: List<CameraParams> = emptyList(),
    val selectedCamera: CameraParams? = null,
    val currentBackendCamera: BackendCamera? = null,
    val isDropdownExpanded: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isDiagnosticsRunning: Boolean = false,
    val diagnosticsResult: String? = null
)

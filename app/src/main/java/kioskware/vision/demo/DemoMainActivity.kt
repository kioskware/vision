package kioskware.vision.demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kioskware.vision.camera.BackendCamera
import kioskware.vision.camera.CameraParams
import kioskware.vision.camera.CameraState
import kioskware.vision.demo.ui.theme.KioskwareVisionDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KioskwareVisionDemoTheme {
                VisionDemoApp()
            }
        }
    }
}

@Composable
fun VisionDemoApp(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Inicjalizacja kamer przy pierwszym uruchomieniu
    LaunchedEffect(Unit) {
        viewModel.initializeCameras(context)
    }

    // Zarządzanie uprawnieniami do kamery
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Aktualizuj stan uprawnień w ViewModel
    LaunchedEffect(hasCameraPermission) {
        viewModel.setCameraPermission(hasCameraPermission)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setCameraPermission(isGranted)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Kioskware Vision Demo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            // Wyświetl błąd jeśli wystąpił
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Błąd",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("OK")
                        }
                    }
                }
            }

            if (!uiState.hasCameraPermission) {
                CameraPermissionContent(
                    onRequestPermission = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            } else {
                CameraSelectionAndPreview(
                    uiState = uiState,
                    onCameraSelected = viewModel::selectCamera,
                    onDropdownExpandedChanged = viewModel::setDropdownExpanded,
                    onRunDiagnostics = viewModel::runCameraDiagnostics,
                    onClearDiagnostics = viewModel::clearDiagnosticsResult
                )
            }
        }
    }
}

@Composable
fun CameraPermissionContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Aplikacja potrzebuje dostępu do kamery",
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onRequestPermission) {
            Text("Udziel dostępu do kamery")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSelectionAndPreview(
    uiState: MainUiState,
    onCameraSelected: (CameraParams) -> Unit,
    onDropdownExpandedChanged: (Boolean) -> Unit,
    onRunDiagnostics: () -> Unit,
    onClearDiagnostics: () -> Unit,
) {
    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dropdown do wyboru kamery
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Wybór kamery",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = uiState.isDropdownExpanded,
                        onExpandedChange = onDropdownExpandedChanged
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedCamera?.displayName ?: uiState.selectedCamera?.cameraId ?: "Wybierz kamerę",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Kamera") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Rozwiń listę"
                                )
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = uiState.isDropdownExpanded,
                            onDismissRequest = { onDropdownExpandedChanged(false) }
                        ) {
                            uiState.availableCameras.forEach { camera ->
                                DropdownMenuItem(
                                    text = {
                                        Text(camera.displayName ?: camera.cameraId)
                                    },
                                    onClick = {
                                        onCameraSelected(camera)
                                        onDropdownExpandedChanged(false)
                                    }
                                )
                            }
                        }
                    }

                    if (uiState.availableCameras.isEmpty()) {
                        Text(
                            text = "Brak dostępnych kamer",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // Podgląd z kamery z rozszerzonymi informacjami
        uiState.currentBackendCamera?.let { backendCamera ->
            item {
                EnhancedCameraPreview(backendCamera = backendCamera)
            }
        }

        // Informacje o wybranej kamerze
        uiState.selectedCamera?.let { camera ->
            item {
                CameraInfoCard(camera = camera)
            }
        }

        // Sekcja diagnostyki kamer
        item {
            CameraDiagnosticsCard(
                uiState = uiState,
                onRunDiagnostics = onRunDiagnostics,
                onClearDiagnostics = onClearDiagnostics
            )
        }
    }
}

@Composable
fun CameraDiagnosticsCard(
    uiState: MainUiState,
    onRunDiagnostics: () -> Unit,
    onClearDiagnostics: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Diagnostyka kamer",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Uruchom diagnostykę aby sprawdzić wszystkie dostępne kamery w urządzeniu i zidentyfikować potencjalne problemy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Przycisk uruchamiania diagnostyki
            Button(
                onClick = onRunDiagnostics,
                enabled = !uiState.isDiagnosticsRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isDiagnosticsRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uruchamianie diagnostyki...")
                } else {
                    Text("Uruchom diagnostykę kamer")
                }
            }

            // Wyniki diagnostyki
            uiState.diagnosticsResult?.let { result ->
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Wyniki diagnostyki",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            TextButton(onClick = onClearDiagnostics) {
                                Text("Zamknij")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Scrollowalne pole z wynikami diagnostyki
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            LazyColumn {
                                item {
                                    Text(
                                        text = result,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraInfoCard(camera: CameraParams) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Informacje o kamerze",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Dodanie przewijania dla długich informacji
            Column {
                Text("ID: ${camera.cameraId}")
                camera.displayName?.let {
                    Text("Nazwa: $it")
                }
                camera.facing?.let {
                    Text("Kierunek: $it")
                }
                camera.resolutions?.let { resolutions ->
                    Text("Dostępne rozdzielczości:")
                    resolutions.forEach { resolution ->
                        Text(
                            text = "  • ${resolution.width}x${resolution.height}",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                camera.fieldOfView?.let { fov ->
                    Text("Pole widzenia: ${fov.horizontal}° x ${fov.vertical}°")
                }
                camera.hardwareInfo?.let {
                    Text("Info sprzętowe: $it")
                }
            }
        }
    }
}

@Composable
fun EnhancedCameraPreview(backendCamera: BackendCamera) {
    val cameraSnapshot by backendCamera.cameraSnapshot.collectAsStateWithLifecycle()
    val cameraState by backendCamera.cameraState.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Podgląd kamery",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Status kamery
            val currentState = cameraState
            val stateText = when (currentState) {
                is CameraState.Idle -> "Bezczynny"
                is CameraState.Starting -> "Uruchamianie"
                is CameraState.Started -> "Uruchomiony"
                is CameraState.Stopping -> "Zatrzymywanie"
                is CameraState.ConfiguredError -> "Błąd konfiguracji: ${currentState.error.message}"
                is CameraState.Error -> "Błąd: ${currentState.error.message}"
                else -> "Nieznany stan"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stan: $stateText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                // Wskaźnik stanu
                when (currentState) {
                    is CameraState.Starting, is CameraState.Stopping -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    is CameraState.Started -> {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                    is CameraState.ConfiguredError, is CameraState.Error -> {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    MaterialTheme.colorScheme.error,
                                    androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                    androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Podgląd obrazu
            val currentSnapshot = cameraSnapshot
            currentSnapshot?.contentBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Podgląd z kamery",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat()),
                    contentScale = ContentScale.Fit
                )

                // Szczegółowe informacje o snapshot
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Informacje o klatce",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Rozdzielczość:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${bitmap.width}x${bitmap.height}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Column {
                                Text(
                                    text = "Czas przetwarzania:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${currentSnapshot.processingDuration / 1_000_000}ms",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Timestamp:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${currentSnapshot.timestamp}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Column {
                                Text(
                                    text = "Format:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = bitmap.config?.name ?: "Unknown",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (currentState) {
                            is CameraState.Starting -> {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Uruchamianie kamery...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is CameraState.ConfiguredError, is CameraState.Error -> {
                                Text(
                                    text = "Błąd kamery",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Sprawdź połączenie z kamerą",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            else -> {
                                Text(
                                    text = "Brak podglądu",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Kamera się uruchamia...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VisionDemoAppPreview() {
    KioskwareVisionDemoTheme {
        VisionDemoApp()
    }
}
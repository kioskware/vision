package kioskware.vision.demo

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log

/**
 * Enhanced diagnostic tool for camera detection issues
 */
internal class CameraDetectionDiagnostics(private val context: Context) {

    private val TAG = "CameraDetectionDiag"

    suspend fun runFullDiagnostics(): String {
        val report = StringBuilder()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            val cameraIds = cameraManager.cameraIdList
            report.appendLine("=== DIAGNOZA KAMER ===")
            report.appendLine("Znalezione ID kamer: ${cameraIds.size}")
            report.appendLine("Lista ID: ${cameraIds.joinToString(", ")}")
            report.appendLine()

            val frontCameras = mutableListOf<String>()
            val backCameras = mutableListOf<String>()
            val externalCameras = mutableListOf<String>()
            val unknownCameras = mutableListOf<String>()
            val logicalCameras = mutableListOf<String>()
            val physicalCameras = mutableListOf<String>()

            for (cameraId in cameraIds) {
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)

                    // Sprawdź kierunek kamery
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    when (facing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> frontCameras.add(cameraId)
                        CameraCharacteristics.LENS_FACING_BACK -> backCameras.add(cameraId)
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> externalCameras.add(cameraId)
                        else -> unknownCameras.add(cameraId)
                    }

                    // Sprawdź czy to kamera logiczna
                    val physicalCameraIds = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            characteristics.physicalCameraIds
                        } else {
                            emptySet()
                        }
                    } catch (e: Exception) {
                        emptySet<String>()
                    }

                    if (physicalCameraIds.isNotEmpty()) {
                        logicalCameras.add(cameraId)
                    } else {
                        physicalCameras.add(cameraId)
                    }

                    // Szczegółowe informacje o kamerze
                    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    val isBackwardCompatible = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) ?: false

                    report.appendLine("Kamera $cameraId:")
                    report.appendLine("  - Kierunek: ${getFacingString(facing)}")
                    report.appendLine("  - Poziom sprzętowy: ${getHardwareLevelString(hardwareLevel)}")
                    report.appendLine("  - Kompatybilność wsteczna: $isBackwardCompatible")
                    report.appendLine("  - Typ: ${if (physicalCameraIds.isNotEmpty()) "Logiczna" else "Fizyczna"}")

                    if (physicalCameraIds.isNotEmpty()) {
                        report.appendLine("  - Fizyczne kamery: ${physicalCameraIds.joinToString(", ")}")
                    }

                    // Sprawdź dostępne rozdzielczości
                    val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val jpegSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
                    report.appendLine("  - Rozdzielczości JPEG: ${jpegSizes?.size ?: 0}")

                    report.appendLine()

                } catch (e: Exception) {
                    report.appendLine("BŁĄD przy analizie kamery $cameraId: ${e.message}")
                    Log.e(TAG, "Error analyzing camera $cameraId", e)
                }
            }

            report.appendLine("=== PODSUMOWANIE ===")
            report.appendLine("Kamery przednie: ${frontCameras.size} (${frontCameras.joinToString(", ")})")
            report.appendLine("Kamery tylne: ${backCameras.size} (${backCameras.joinToString(", ")})")
            report.appendLine("Kamery zewnętrzne: ${externalCameras.size} (${externalCameras.joinToString(", ")})")
            report.appendLine("Kamery nieznane: ${unknownCameras.size} (${unknownCameras.joinToString(", ")})")
            report.appendLine()
            report.appendLine("Kamery logiczne: ${logicalCameras.size} (${logicalCameras.joinToString(", ")})")
            report.appendLine("Kamery fizyczne: ${physicalCameras.size} (${physicalCameras.joinToString(", ")})")

            // Analiza potencjalnych problemów
            report.appendLine()
            report.appendLine("=== ANALIZA PROBLEMÓW ===")

            if (frontCameras.size > 1) {
                report.appendLine("⚠️ PROBLEM: Wykryto ${frontCameras.size} kamer przednich - prawdopodobnie kamery logiczne zawierają duplikaty")
            }

            if (logicalCameras.isNotEmpty()) {
                report.appendLine("ℹ️ INFO: Wykryto kamery logiczne - mogą one powodować wielokrotne wykrywanie tej samej kamery fizycznej")
            }

            if (backCameras.isEmpty()) {
                report.appendLine("⚠️ PROBLEM: Nie wykryto żadnych kamer tylnych")
            }

        } catch (e: Exception) {
            report.appendLine("KRYTYCZNY BŁĄD: ${e.message}")
            Log.e(TAG, "Critical error in diagnostics", e)
        }

        val result = report.toString()
        Log.d(TAG, result)
        return result
    }

    private fun getFacingString(facing: Int?): String {
        return when (facing) {
            CameraCharacteristics.LENS_FACING_FRONT -> "Przednia"
            CameraCharacteristics.LENS_FACING_BACK -> "Tylna"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "Zewnętrzna"
            else -> "Nieznana ($facing)"
        }
    }

    private fun getHardwareLevelString(level: Int?): String {
        return when (level) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Legacy"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Limited"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Full"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Level 3"
            else -> "Nieznany ($level)"
        }
    }
}

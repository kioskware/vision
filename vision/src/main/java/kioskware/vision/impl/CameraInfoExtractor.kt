package kioskware.vision.impl

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import kioskware.vision.camera.CameraParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString
import kotlin.collections.toList
import kotlin.math.atan

/**
 * Extractor class for camera information.
 * Provides functions to extract camera parameters using CameraX and Camera2 APIs.
 */
internal class CameraInfoExtractor(private val context: Context) {

    /**
     * Extracts camera parameters for all available cameras on the device.
     * @return List of CameraParams containing information about each camera.
     */
    suspend fun getAllCameraParams(): List<CameraParams> = withContext(Dispatchers.IO) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraParams = mutableListOf<CameraParams>()

        try {
            // Get all available camera IDs
            val cameraIds = cameraManager.cameraIdList

            for (cameraId in cameraIds) {
                try {
                    val params = extractCameraParams(cameraId, cameraManager)
                    cameraParams.add(params)
                } catch (e: Exception) {
                    // Log error and continue with next camera
                    println("Error extracting params for camera $cameraId: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Error getting camera list: ${e.message}")
        }

        cameraParams
    }

    /**
     * Extracts camera parameters for a specific camera ID.
     * @param cameraId The camera ID to extract parameters for.
     * @return CameraParams object containing the camera information.
     */
    suspend fun getCameraParams(cameraId: String): CameraParams? = withContext(Dispatchers.IO) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            extractCameraParams(cameraId, cameraManager)
        } catch (e: Exception) {
            Log.w(
                "CameraInfoExtractor",
                "Error extracting params for camera $cameraId: ${e.message}"
            )
            null
        }
    }

    private fun extractCameraParams(
        cameraId: String,
        cameraManager: CameraManager
    ): CameraParams {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)

        // Get resolutions
        val resolutions = getResolutions(characteristics)

        // Get display name
        val displayName = getDisplayName(cameraId, characteristics)

        // Get field of view
        val fieldOfView = getFieldOfView(characteristics)

        // Get facing direction
        val facing = getFacing(characteristics)

        // Get hardware info
        val hardwareInfo = getHardwareInfo(characteristics)

        return CameraParams(
            cameraId = cameraId,
            resolutions = resolutions,
            displayName = displayName,
            fieldOfView = fieldOfView,
            facing = facing,
            hardwareInfo = hardwareInfo
        )
    }

    private fun getResolutions(characteristics: CameraCharacteristics): List<Size>? {
        return try {
            val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            configMap?.getOutputSizes(ImageFormat.JPEG)?.toList()
        } catch (e: Exception) {
            null
        }
    }

    private fun getDisplayName(cameraId: String, characteristics: CameraCharacteristics): String? {
        return try {
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val facingString = when (facing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                CameraCharacteristics.LENS_FACING_BACK -> "Back"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                else -> "Unknown"
            }
            "Camera $cameraId ($facingString)"
        } catch (e: Exception) {
            "Camera $cameraId"
        }
    }

    private fun getFieldOfView(characteristics: CameraCharacteristics): CameraParams.FoV? {
        return try {
            val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)

            if (focalLengths != null && focalLengths.isNotEmpty() && sensorSize != null) {
                val focalLength = focalLengths[0] // Use first focal length

                // Calculate field of view using sensor size and focal length
                // Convert radians to degrees by multiplying by 180/π
                val horizontalFoV = 2 * atan((sensorSize.width / 2) / focalLength) * 180 / Math.PI
                val verticalFoV = 2 * atan((sensorSize.height / 2) / focalLength) * 180 / Math.PI

                CameraParams.FoV(
                    horizontal = horizontalFoV.toFloat(),
                    vertical = verticalFoV.toFloat()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getFacing(characteristics: CameraCharacteristics): CameraParams.Facing? {
        return try {
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            when (facing) {
                CameraCharacteristics.LENS_FACING_FRONT -> CameraParams.Facing.Front
                CameraCharacteristics.LENS_FACING_BACK -> CameraParams.Facing.Back
                CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraParams.Facing.External
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getHardwareInfo(characteristics: CameraCharacteristics): String? {
        return try {
            val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

            val levelString = when (hardwareLevel) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Legacy"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Limited"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Full"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Level 3"
                else -> "Unknown"
            }

            val capabilitiesString = capabilities?.joinToString(", ") { capability ->
                when (capability) {
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "Backward Compatible"
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "Manual Sensor"
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> "Manual Post Processing"
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW"
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING -> "Private Reprocessing"
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS -> "Read Sensor Settings"
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "Burst Capture"
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV Reprocessing"
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "Depth Output"
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> "High Speed Video"
                    else -> "Unknown ($capability)"
                }
            } ?: "None"

            "Hardware Level: $levelString, Capabilities: $capabilitiesString"
        } catch (e: Exception) {
            null
        }
    }
}

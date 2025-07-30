package kioskware.vision.impl

import android.content.Context
import kioskware.vision.camera.BackendCamera
import kioskware.vision.camera.BackendCameras
import kioskware.vision.camera.CameraParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Factory function to create an instance of [BackendCameras].
 * This function initializes the backend cameras with the provided context.
 *
 * @param context The Android context used for camera operations.
 * @return An instance of [BackendCameras] that manages backend cameras.
 */
fun defaultBackendCameras(context: Context): BackendCameras {
    return BackendBuildInCamerasImpl(context)
}

/**
 * Implementation of [BackendCameras] interface that manages backend cameras.
 * This class handles the discovery of available cameras and provides access to [BackendCamera] instances.
 *
 * @param context The Android context used for camera operations.
 */
internal class BackendBuildInCamerasImpl(
    private val context: Context
) : BackendCameras {

//    private val cameraManager: CameraManager =
//        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    //private val mExecutor = Executor { command -> command.run() }
    private val cameraInfoExtractor = CameraInfoExtractor(context)

    private val camerasMap = ConcurrentHashMap<String, BackendCamera>()
    private val cameraParamsMap = ConcurrentHashMap<String, CameraParams>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val camerasScope = CoroutineScope(
        Dispatchers.Default.limitedParallelism(1) + SupervisorJob()
    )

    private val _availableCameras = MutableStateFlow<List<CameraParams>>(emptyList())
    override val availableCameras = _availableCameras.asStateFlow()

//    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
//        override fun onCameraAvailable(cameraId: String) {
//            Log.d(
//                "BackendBuildInCamerasImpl",
//                "Camera $cameraId is available"
//            )
//            camerasScope.launch {
//                ensureCameraParams(cameraId)
//            }
//        }
//
//        override fun onCameraUnavailable(cameraId: String) {
//            Log.d(
//                "BackendBuildInCamerasImpl",
//                "Camera $cameraId is unavailable"
//            )
//            camerasScope.launch {
//                //removeCamera(cameraId)
//            }
//        }
//    }

    override suspend fun getCamera(cameraId: String): BackendCamera? {
        return ensureCameraParams(cameraId)?.let { params ->
            camerasMap[cameraId] ?: createCamera(params)
                .also { camera ->
                    camera?.let { camerasMap[cameraId] = it }
                }
        }
    }

    init {
        // Camera is consider unavailable also when it's connected but
        // already in use, so we cannot simply detect actual availability
        // using availability callback think about it
//        cameraManager.registerAvailabilityCallback(
//            mExecutor,
//            cameraCallback
//        )
        camerasScope.launch {
            cameraInfoExtractor.getAllCameraParams().also {
                it.forEach { params ->
                    cameraParamsMap[params.cameraId] = params
                }
                _availableCameras.value = it
            }
        }
    }

    private suspend fun ensureCameraParams(cameraId: String): CameraParams? {
        return cameraParamsMap[cameraId] ?: cameraInfoExtractor.getCameraParams(cameraId)
            ?.also { params ->
                cameraParamsMap[cameraId] = params
                updateCamerasStateFlow()
            }
    }

    private suspend fun removeCamera(cameraId: String) {
        cameraParamsMap.remove(cameraId)
        camerasMap.remove(cameraId)?.stopCamera()
        updateCamerasStateFlow()
    }

    private fun createCamera(cameraParams: CameraParams): BackendCamera? {
        return BackendCameraXImpl(
            context = context,
            cameraParams = cameraParams
        )
    }

    private fun updateCamerasStateFlow() {
        _availableCameras.value = cameraParamsMap.values
            .toList()
            .sortedBy { it.cameraId }
    }

}

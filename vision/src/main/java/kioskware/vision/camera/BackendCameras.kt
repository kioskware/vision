package kioskware.vision.camera

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface representing a repository for managing backend cameras.
 */
interface BackendCameras {

    /**
     * StateFlow emitting currently available camera devices.
     */
    val availableCameras: StateFlow<List<CameraParams>>

    /**
     * Gets [BackendCamera] for the specified camera ID.
     *
     * Same [cameraId] will always return the same [BackendCamera] instance
     * for all lifetime of the [BackendCameras] object.
     *
     * @param cameraId The unique identifier for the camera.
     * @return The [BackendCamera] instance for the specified camera ID, or null if not found.
     */
    suspend fun getCamera(cameraId: String): BackendCamera?

    /**
     * Gets [BackendCamera] for the specified [CameraParams].
     *
     * Same [cameraParams] will always return the same [BackendCamera] instance
     * for all lifetime of the [BackendCameras] object.
     *
     * @param cameraParams The parameters of the camera to retrieve.
     * @return The [BackendCamera] instance for the specified camera parameters, or null if not found.
     */
    suspend fun getCamera(cameraParams: CameraParams): BackendCamera? =
        getCamera(cameraParams.cameraId)

    /**
     * Gets the [CameraParams] for the specified camera ID.
     *
     * @param cameraId The unique identifier for the camera.
     * @return The [CameraParams] instance for the specified camera ID, or null if not found.
     */
    suspend fun getCameraParams(cameraId: String): CameraParams? =
        getCamera(cameraId)?.cameraParams?.value

    /**
     * Calls [BackendCamera.stopCamera] for all available cameras.
     * This will stop all cameras and release their resources.
     *
     * @return List of [CameraState] for each camera that
     * was stopped in order of their appearance in [availableCameras].
     */
    suspend fun stopAllCameras(): List<CameraState> {
        return availableCameras.value.mapNotNull { cameraParams ->
            getCamera(cameraParams)?.stopCamera()
        }
    }

}
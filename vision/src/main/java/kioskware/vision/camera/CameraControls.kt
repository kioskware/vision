package kioskware.vision.camera

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for controlling camera settings such as torch, zoom, focus, and exposure.
 * Implementations should provide the actual logic to control the camera hardware.
 */
interface CameraControls {

    /**
     * StateFlow that emits the current torch (flashlight) state of the camera.
     * True if the torch is enabled, false otherwise.
     */
    val torchEnabled: StateFlow<Boolean>

    /**
     * StateFlow that emits the current zoom ratio of the camera.
     * The zoom ratio is a float value representing the level of zoom applied.
     */
    val zoomRatio: StateFlow<Float>

    /**
     * StateFlow that emits the current linear zoom of the camera.
     * The linear zoom is a float value representing the level of linear zoom applied.
     */
    val linearZoom: StateFlow<Float>

    /**
     * StateFlow that emits the current focus point of the camera.
     * The focus point is represented as a pair of float values (x, y) in the range [0.0, 1.0].
     */
    val focusPoint: StateFlow<Pair<Float, Float>?>

    /**
     * StateFlow that emits the current exposure compensation index of the camera.
     * The exposure compensation index is an integer value representing the level of exposure adjustment.
     */
    val exposureCompensationIndex: StateFlow<Int>

    /**
     * Sets the torch (flashlight) state of the camera.
     *
     * Function suspends until the operation is complete.
     * @param enabled True to enable the torch, false to disable it.
     * @return True if the operation was successful, false otherwise.
     */
    suspend fun setTorchEnabled(enabled: Boolean) : Boolean

    /**
     * Sets the zoom ratio of the camera.
     *
     * Function suspends until the operation is complete.
     * @param zoomRatio The desired zoom ratio.
     * @return True if the operation was successful, false otherwise.
     */
    suspend fun setZoomRatio(zoomRatio: Float) : Boolean

    /**
     * Sets the linear zoom of the camera.
     *
     * Function suspends until the operation is complete.
     * @param linearZoom The desired linear zoom value.
     * @return True if the operation was successful, false otherwise.
     */
    suspend fun setLinearZoom(linearZoom: Float) : Boolean

    /**
     * Sets the focus point of the camera.
     *
     * Function suspends until the operation is complete.
     * @param x The x coordinate of the focus point (0.0 to 1.0).
     * @param y The y coordinate of the focus point (0.0 to 1.0).
     * @return True if the operation was successful, false otherwise.
     */
    suspend fun setFocusPoint(x: Float, y: Float) : Boolean

    /**
     * Sets the exposure compensation index of the camera.
     *
     * Function suspends until the operation is complete.
     * @param value The desired exposure compensation index.
     * @return True if the operation was successful, false otherwise.
     */
    suspend fun setExposureCompensationIndex(value: Int) : Boolean

}
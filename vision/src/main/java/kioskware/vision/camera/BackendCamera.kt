package kioskware.vision.camera

import android.util.Size
import androidx.lifecycle.LifecycleOwner
import kioskware.vision.BackpressureStrategy
import kioskware.vision.OutputImageFormat
import kioskware.vision.Rotation
import kioskware.vision.processor.ImageProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface representing the backend camera functionality.
 * @property cameraId The unique identifier for the camera.
 */
abstract class BackendCamera(
    val cameraId: String
) {

    /**
     * Camera parameters including resolution, display name, field of view, facing direction, etc.
     * @return A StateFlow that emits the current [CameraParams] instance.
     */
    abstract val cameraParams: StateFlow<CameraParams>

    /**
     * Camera controls for managing camera operations such as zoom, focus, etc.
     * @return A StateFlow that emits the current [androidx.camera.core.CameraControl] instance or null if not available.
     */
    abstract val cameraControl: StateFlow<CameraControls?>

    /**
     * StateFlow emitting the current state of the camera.
     */
    abstract val cameraState: StateFlow<CameraState>

    /**
     * Flow emitting latest snapshots from the camera.
     * Snapshot will contain the latest image from the camera (if available)
     * timestamp, processing results and other metadata.
     *
     * @return A StateFlow that emits [CameraSnapshot?] instances.
     */
    abstract val cameraSnapshot: StateFlow<CameraSnapshot?>

    /**
     * MutableStateFlow emitting the current configuration for the camera snapshots.
     * (emitted by [cameraSnapshot])
     * This configuration includes settings for camera view and visualisation.
     *
     * This flow can be used to enable or disable the camera view
     * and visualisation features by changing its value.
     *
     * @return A MutableStateFlow that emits [CameraSnapshotConfig] instances.
     */
    abstract val cameraSnapshotConfig: MutableStateFlow<CameraSnapshotConfig>

    /**
     * Configuration for the camera.
     * @param resolution The resolution of the camera.
     * @param backpressureStrategy The backpressure strategy to be used for image analysis.
     * @param targetRotation The target rotation for the camera.
     * @param outputImageFormat The format of the output image.
     * @param imageProcessors A list of image processors to be applied to the camera images.
     * @return A configuration object containing all necessary parameters for the camera setup.
     */
    data class Config(
        val resolution: Size,
        val backpressureStrategy: BackpressureStrategy,
        val targetRotation: Rotation,
        val outputImageFormat: OutputImageFormat,
        val imageProcessors: List<ImageProcessor<*>>
    )

    /**
     * Starts the camera with a configuration object.
     * If camera is already started, it will be stopped and restarted with the new configuration.
     * @param config The configuration object containing all necessary parameters for the camera setup.
     * @param lifecycleOwner The lifecycle owner for the camera.
     * @return A [CameraState] after performing the start operation.
     */
    abstract suspend fun startCamera(
        config: Config,
        lifecycleOwner: LifecycleOwner
    ) : CameraState

    /**
     * Starts the camera with the specified configuration.
     * @param resolution The resolution of the camera.
     * Actual resolution may differ based on the camera capabilities.
     * @param backpressureStrategy The backpressure strategy to be used for image analysis.
     * @param targetRotation The target rotation for the camera.
     * @param outputImageFormat The format of the output image.
     * @param lifecycleOwner The lifecycle owner for the camera.
     * @param imageProcessors A list of image processors to be applied to the camera images.
     * @return A [CameraState] after performing the start operation.
     */
    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        resolution: Size = Size(1280, 720),
        backpressureStrategy: BackpressureStrategy = BackpressureStrategy.KeepOnlyLatest,
        targetRotation: Rotation = Rotation.Degrees0,
        outputImageFormat: OutputImageFormat = OutputImageFormat.YUV_420_888,
        imageProcessors: List<ImageProcessor<*>> = emptyList()
    ) : CameraState {
        val config = Config(
            resolution = resolution,
            backpressureStrategy = backpressureStrategy,
            targetRotation = targetRotation,
            outputImageFormat = outputImageFormat,
            imageProcessors = imageProcessors
        )
        return startCamera(config, lifecycleOwner)
    }

    /**
     * Stops the camera and releases resources.
     * @return A [CameraState] after performing the stop operation.
     */
    abstract suspend fun stopCamera() : CameraState

}
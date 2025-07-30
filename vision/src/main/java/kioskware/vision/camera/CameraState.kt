package kioskware.vision.camera

/**
 * Represents the state of the camera.
 * This interface defines various states that the camera can be in,
 * such as idle, starting, started, stopping, and error states.
 * Each state can carry additional information, such as configuration details or error messages.
 */
sealed interface CameraState {

    /**
     * Represents the idle state of the camera.
     * In this state, the camera is not actively configured or running.
     */
    object Idle : CameraState

    /**
     * Represents a configured state of the camera.
     * This state carries the configuration details of the camera.
     */
    interface Configured : CameraState {
        val config: BackendCamera.Config
    }

    /**
     * Represents the starting state of the camera.
     * This state indicates that the camera is in the process of starting up.
     * It carries the configuration details used to start the camera.
     */
    data class Starting(
        override val config: BackendCamera.Config
    ) : Configured

    /**
     * Represents the started state of the camera.
     * This state indicates that the camera has successfully started and is capturing images.
     * It carries the configuration details used to start the camera.
     */
    data class Started(
        override val config: BackendCamera.Config
    ) : Configured

    /**
     * Represents the stopping state of the camera.
     * This state indicates that the camera is in the process of stopping.
     * It carries the configuration details used to stop the camera.
     */
    data class Stopping(
        override val config: BackendCamera.Config
    ) : Configured

    /**
     * Represents an error state of the camera.
     * This state indicates that an error has occurred while operating the camera.
     * It carries the error details.
     */
    open class Error(
        val error: Throwable
    ) : CameraState

    /**
     * Represents a configured error state of the camera.
     * This state indicates that an error occurred after the camera was configured.
     * It carries both the configuration details and the error details.
     */
    class ConfiguredError(
        override val config: BackendCamera.Config,
        error: Throwable
    ) : Error(error), Configured

}
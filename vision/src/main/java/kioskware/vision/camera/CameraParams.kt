package kioskware.vision.camera

import android.util.Size

/**
 * Data class representing the parameters of a camera.
 * This includes the camera ID, supported resolutions, display name, field of view,
 * facing direction, and hardware information.
 *
 * @property cameraId The unique identifier for the camera.
 * @property resolutions A list of supported resolutions for the camera. Null if this information is not available.
 * @property displayName The display name of the camera. Null if this information is not available.
 * @property fieldOfView The field of view (FoV) of the camera. Null if this information is not available.
 * @property facing The facing direction of the camera (front, back, or external). Null if this information is not available.
 * @property hardwareInfo Additional hardware information about the camera. Null if this information is not available.
 */
data class CameraParams(
    val cameraId: String,
    val resolutions: List<Size>? = null,
    val displayName: String? = null,
    val fieldOfView: FoV? = null,
    val facing: Facing? = null,
    val hardwareInfo: String? = null
) {

    /**
     * Represents the field of view (FoV) of the camera.
     * @property horizontal The horizontal field of view in degrees.
     * @property vertical The vertical field of view in degrees.
     */
    data class FoV(
        val horizontal: Float,
        val vertical: Float
    )

    /**
     * Enum class representing the facing direction of the camera.
     * This is used to indicate whether the camera is front-facing, back-facing, or external.
     */
    enum class Facing {
        /**
         * Represents a front-facing camera.
         * Typically used for selfies or video calls.
         */
        Front,
        /**
         * Represents a back-facing camera.
         * Usually used for taking photos or videos of the environment.
         */
        Back,
        /**
         * Represents an external camera.
         * This could be a USB or other type of camera that is not built into the device.
         */
        External
    }

}

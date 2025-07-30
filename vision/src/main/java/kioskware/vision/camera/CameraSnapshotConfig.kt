package kioskware.vision.camera

/**
 * Configuration for the camera bitmap.
 * @param cameraViewEnabled Flag to enable or disable the camera view.
 * @param visualisationEnabled Flag to enable or disable visualisation of the camera bitmap.
 */
data class CameraSnapshotConfig(
    val cameraViewEnabled: Boolean = true,
    val visualisationEnabled: Boolean = false
)
package kioskware.vision

/**
 * Enum class representing the rotation options for camera images.
 * This is used to specify how the camera image should be rotated before processing or displaying.
 * @property degrees The rotation in degrees.
 * @property radians The rotation in radians, calculated from the degrees.
 */
enum class Rotation(
    val degrees: Int
) {

    /**
     * Represents no rotation.
     */
    Degrees0(0),

    /**
     * Represents a 90-degree clockwise rotation.
     */
    Degrees90(90),

    /**
     * Represents a 180-degree clockwise rotation.
     */
    Degrees180(180),

    /**
     * Represents a 270-degree clockwise rotation.
     */
    Degrees270(270);

    /**
     * The rotation in radians, calculated from the degrees.
     */
    val radians: Float = Math.toRadians(degrees.toDouble()).toFloat()

}
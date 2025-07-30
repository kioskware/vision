package kioskware.vision

/**
 * Enum class representing the output image formats supported by the camera.
 * This is used to specify the format in which images should be processed or returned.
 */
enum class OutputImageFormat {
    /**
     * Represents the YUV_420_888 image format.
      */
    YUV_420_888,

    /**
     * Represents RGBA_8888 image format.
     */
    RGBA_8888
}
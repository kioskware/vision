package kioskware.vision.processor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.Image

/**
 * Interface for processing images, typically used in computer vision tasks.
 * Implementations should define how to process an image and optionally draw on a canvas.
 */
abstract class ImageProcessor<T> {

    /**
     * Flag to enable or disable the image processing.
     * If false, the processor will not perform any operations.
     * This will also disable visualisation, even if `visualisationEnabled` is true.
     */
    var enabled: Boolean = true

    /**
     * Flag to enable or disable visualisation of the processing results.
     * If true, implementations should draw visualisation on the provided canvas.
     */
    var visualisationEnabled: Boolean = true

    /**
     * Processes the given image and optionally draws visualisation on the provided canvas.
     * @param image The image bitmap to be processed. Rotation is handled by the caller, so `rotationDegrees` are always 0.
     * @param overlayCanvas An optional [android.graphics.Canvas] to draw visualisation on top of the image.
     * If `visualisationEnabled` is false, this will be null.
     */
    suspend fun process(
        image: Bitmap,
        overlayCanvas: Canvas? = null
    ) : T? {
        if (enabled) {
            return onProcess(
                image,
                if (visualisationEnabled) overlayCanvas else null
            )
        }
        return null
    }

    /**
     * Optional cleanup logic to be executed when the processor is no longer needed.
     * This can be overridden by subclasses to perform any necessary cleanup.
     */
    open suspend fun release() {
        // Optional cleanup logic can be added here if needed.
    }

    /**
     * Actual processing logic for the image.
     * @param image The image bitmap to be processed. Rotation is handled by the caller, so `rotationDegrees` are always 0.
     * @param overlayCanvas An optional [Canvas] to draw additional information on top of the image.
     * Implementation should draw visualisation of analysis or other processing on this canvas if provided.
     */
    protected abstract suspend fun onProcess(
        image: Bitmap,
        overlayCanvas: Canvas? = null
    ) : T

}
package kioskware.vision.camera

import android.graphics.Bitmap
import kioskware.vision.processor.ProcessingResults

/**
 * Represents a snapshot of the camera at a specific moment in time.
 * This data class encapsulates the camera ID, the bitmap image captured,
 * the configuration of the bitmap, processing results, and a timestamp.
 *
 * @property cameraParams The parameters of the camera, including resolution, facing direction, and other metadata.
 * @property contentBitmap The bitmap image captured by the camera, or null if not available (e.g., view is disabled).
 * @property overlayBitmap The bitmap image representing the camera overlay from image processors, or null if not available (e.g., processor visualisation is disabled).
 * @property processingResults The results of any processing applied to the image.
 * @property timestamp The time when the snapshot was taken, defaults to current time in milliseconds.
 * @property processingDuration The duration in nanoseconds that the processing took to complete.
 */
data class CameraSnapshot(
    val cameraParams: CameraParams,
    val processingDuration: Long,
    val processingResults: ProcessingResults,
    val contentBitmap: Bitmap? = null,
    val overlayBitmap: Bitmap? = null,
    val timestamp: Long = System.currentTimeMillis()
)
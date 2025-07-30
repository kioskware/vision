package kioskware.vision

/**
 * Enum class representing the backpressure strategies for camera image processing.
 * This is used to control how the camera handles image frames when the processing
 * cannot keep up with the frame rate.
 */
enum class BackpressureStrategy {

    /**
     * Drop the oldest frame when a new frame arrives and processing is not ready.
     * This strategy is useful when the latest frame is more important than the previous ones.
     */
    KeepOnlyLatest,

    /**
     * Block the camera frame producer until the processing is ready to handle the next frame.
     * This strategy ensures that no frames are dropped, but may introduce latency.
     */
    BlockProducer

}
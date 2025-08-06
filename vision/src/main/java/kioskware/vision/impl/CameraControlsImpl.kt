package kioskware.vision.impl

import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import kioskware.vision.camera.CameraControls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume

/**
 * Implementation of CameraControls that wraps CameraX CameraControl
 */
internal class CameraControlsImpl(
    private val cameraControl: CameraControl,
    private val cameraScope: CoroutineScope
) : CameraControls {

    private val mExecutor = Executor { command ->
        cameraScope.launch { command.run() }
    }

    private val _torchEnabled = MutableStateFlow(false)
    override val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    override val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _linearZoom = MutableStateFlow(0.0f)
    override val linearZoom: StateFlow<Float> = _linearZoom.asStateFlow()

    private val _focusPoint = MutableStateFlow<Pair<Float, Float>?>(null)
    override val focusPoint: StateFlow<Pair<Float, Float>?> = _focusPoint.asStateFlow()

    private val _exposureCompensationIndex = MutableStateFlow(0)
    override val exposureCompensationIndex: StateFlow<Int> =
        _exposureCompensationIndex.asStateFlow()

    override suspend fun setTorchEnabled(enabled: Boolean): Boolean {
        return try {
            val future = cameraControl.enableTorch(enabled)
            suspendCancellableCoroutine { continuation ->
                future.addListener({
                    try {
                        future.get()
                        _torchEnabled.value = enabled
                        continuation.resume(true)
                    } catch (_: Exception) {
                        continuation.resume(false)
                    }
                }, mExecutor)

                continuation.invokeOnCancellation {
                    future.cancel(true)
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun setZoomRatio(zoomRatio: Float): Boolean {
        return try {
            val future = cameraControl.setZoomRatio(zoomRatio)
            suspendCancellableCoroutine { continuation ->
                future.addListener({
                    try {
                        future.get()
                        _zoomRatio.value = zoomRatio
                        continuation.resume(true)
                    } catch (_: Exception) {
                        continuation.resume(false)
                    }
                }, mExecutor)

                continuation.invokeOnCancellation {
                    future.cancel(true)
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun setLinearZoom(linearZoom: Float): Boolean {
        return try {
            val future = cameraControl.setLinearZoom(linearZoom)
            suspendCancellableCoroutine { continuation ->
                future.addListener({
                    try {
                        future.get()
                        _linearZoom.value = linearZoom
                        continuation.resume(true)
                    } catch (_: Exception) {
                        continuation.resume(false)
                    }
                }, mExecutor)

                continuation.invokeOnCancellation {
                    future.cancel(true)
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun setFocusPoint(x: Float, y: Float): Boolean {
        return try {
            val meteringPointFactory =
                SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
            val meteringPoint = meteringPointFactory.createPoint(x, y)
            val focusMeteringAction =
                FocusMeteringAction.Builder(meteringPoint)
                    .build()

            val future = cameraControl.startFocusAndMetering(focusMeteringAction)
            suspendCancellableCoroutine { continuation ->
                future.addListener({
                    try {
                        val result = future.get()
                        if (result.isFocusSuccessful) {
                            _focusPoint.value = Pair(x, y)
                            continuation.resume(true)
                        } else {
                            continuation.resume(false)
                        }
                    } catch (_: Exception) {
                        continuation.resume(false)
                    }
                }, mExecutor)

                continuation.invokeOnCancellation {
                    future.cancel(true)
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun setExposureCompensationIndex(value: Int): Boolean {
        return try {
            val future = cameraControl.setExposureCompensationIndex(value)
            suspendCancellableCoroutine { continuation ->
                future.addListener({
                    try {
                        future.get()
                        _exposureCompensationIndex.value = value
                        continuation.resume(true)
                    } catch (_: Exception) {
                        continuation.resume(false)
                    }
                }, mExecutor)

                continuation.invokeOnCancellation {
                    future.cancel(true)
                }
            }
        } catch (_: Exception) {
            false
        }
    }

}
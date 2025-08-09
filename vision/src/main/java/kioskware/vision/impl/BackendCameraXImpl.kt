package kioskware.vision.impl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.Log
import android.view.Surface
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.LifecycleOwner
import kioskware.vision.BackpressureStrategy
import kioskware.vision.OutputImageFormat
import kioskware.vision.Rotation
import kioskware.vision.camera.BackendCamera
import kioskware.vision.camera.CameraControls
import kioskware.vision.camera.CameraParams
import kioskware.vision.camera.CameraSnapshot
import kioskware.vision.camera.CameraSnapshotConfig
import kioskware.vision.camera.CameraState
import kioskware.vision.processor.ImageProcessor
import kioskware.vision.processor.ProcessingResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BackendCameraXImpl(
    private val context: Context,
    cameraParams: CameraParams,
) : BackendCamera(cameraParams.cameraId) {

    @OptIn(ExperimentalCoroutinesApi::class)
    private var analysisScope =
        CoroutineScope(Dispatchers.Default.limitedParallelism(1) + SupervisorJob())

    private var cameraScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _cameraControl = MutableStateFlow<CameraControls?>(null)

    private val _cameraParams = MutableStateFlow(cameraParams)
    override val cameraParams: StateFlow<CameraParams> = _cameraParams.asStateFlow()

    override val cameraControl: StateFlow<CameraControls?> = _cameraControl

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    override val cameraState: StateFlow<CameraState> = _cameraState

    override val cameraSnapshotConfig = MutableStateFlow(CameraSnapshotConfig())

    private val _cameraSnapshot = MutableStateFlow<CameraSnapshot?>(null)
    override val cameraSnapshot = _cameraSnapshot.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null

    private val analysisExecutor = Executor { command ->
        analysisScope.launch { command.run() }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override suspend fun startCamera(
        config: Config,
        lifecycleOwner: LifecycleOwner
    ): CameraState {
        try {
            // If camera is already started, stop it first
            if (_cameraState.value is CameraState.Started) {
                stopCamera()
            }

            _cameraState.value = CameraState.Starting(config)

            // Create new scope if previous was cancelled
            if (!analysisScope.isActive) {
                analysisScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            }
            if (!cameraScope.isActive) {
                cameraScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            }
            
            // Check camera permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return CameraState.ConfiguredError(
                    config = config,
                    error = SecurityException("Camera permission is not granted.")
                ).also {
                    _cameraState.value = it
                }
            }

            // Get CameraProvider
            cameraProvider = getCameraProvider()

            // Create ImageAnalysis use case
            imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                config.resolution,
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER
                            )
                        )
                        .build()
                )
                .setTargetRotation(config.targetRotation.toCameraX())
                .setBackpressureStrategy(config.backpressureStrategy.toCameraX())
                .setOutputImageFormat(config.outputImageFormat.toCameraX())
                .build()

            // Set analyzer that processes images through ImageProcessors and emits bitmaps
            imageAnalysis?.setAnalyzer(analysisExecutor) { imageProxy ->
                val processingStartNanos = System.nanoTime()
                try {
                    val snapConfig = cameraSnapshotConfig.value
                    // Bitmap for rendering camera image
                    var contentBitmap: Bitmap = try {
                        imageProxy.toBitmap()
                    } catch (e: Exception) {
                        Log.e("BackendCameraXImpl", "Failed to convert ImageProxy to Bitmap", e)
                        return@setAnalyzer
                    }
                    // Convert ImageProxy to Bitmap
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    // Apply rotation if needed
                    if (rotationDegrees != 0) {
                        contentBitmap = rotateBitmap(contentBitmap, rotationDegrees.toFloat())
                    }
                    // Bitmap for rendering overlays
                    val overlayBitmap = if (snapConfig.visualisationEnabled) {
                        createBitmap(contentBitmap.width, contentBitmap.height)
                    } else {
                        null
                    }

                    val processingResultsMap= mutableMapOf<ImageProcessor<*>, Any?>()

                    // Get the Image from ImageProxy for processors
                    runBlocking {
                        config.imageProcessors.map { processor ->
                            async {
                                val canvas = overlayBitmap?.let { Canvas(it) }
                                // Process the image with each ImageProcessor
                                processor.process(contentBitmap, canvas).also {
                                    processingResultsMap[processor] = it
                                }
                            }
                        }.awaitAll()
                    }

                    // Send snapshot with processed results
                    _cameraSnapshot.value = CameraSnapshot(
                        cameraParams = cameraParams.value,
                        contentBitmap = if(snapConfig.cameraViewEnabled) contentBitmap else null,
                        overlayBitmap = overlayBitmap,
                        processingResults = ProcessingResults(processingResultsMap),
                        processingDuration = (System.nanoTime() - processingStartNanos),
                    )
                } catch (_: Exception) {
                    // In case of error, send null
                    _cameraSnapshot.value = null
                } finally {
                    imageProxy.close()
                }
            }

            // Unbind all previous use cases
            cameraProvider?.unbindAll()

            // Bind use cases to camera
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelectorForId(cameraId),
                imageAnalysis
            )
            camera?.cameraInfo?.cameraState?.observe(lifecycleOwner) { cs ->
                when (cs.type) {
                    androidx.camera.core.CameraState.Type.PENDING_OPEN -> {
                        _cameraState.value = CameraState.Starting(config)
                    }

                    androidx.camera.core.CameraState.Type.OPENING -> {
                        _cameraState.value = CameraState.Starting(config)
                    }

                    androidx.camera.core.CameraState.Type.OPEN -> {
                        _cameraState.value = CameraState.Started(config)
                    }

                    androidx.camera.core.CameraState.Type.CLOSING -> {
                        _cameraState.value = CameraState.Stopping(config)
                    }

                    androidx.camera.core.CameraState.Type.CLOSED -> {
                        _cameraState.value = CameraState.Idle
                    }
                }
                cs.error?.let { error ->
                    _cameraState.value = CameraState.ConfiguredError(
                        config = config,
                        error = Exception(
                            "Camera state error: ${error.type}",
                            error.cause
                        )
                    )
                }
            }
            // Update camera control
            _cameraControl.value = camera?.cameraControl?.let {
                CameraControlsImpl(
                    cameraControl = it,
                    cameraScope = cameraScope
                )
            }
            return CameraState.Started(config)
        } catch (exception: Exception) {
            val errorState = CameraState.ConfiguredError(config, exception)
            _cameraState.value = errorState
            return errorState
        }
    }

    override suspend fun stopCamera(): CameraState {
        Log.v("BackendCameraXImpl", "Stopping camera with ID: $cameraId")
        return try {
            val currentConfig = when (val state = _cameraState.value) {
                is CameraState.Configured -> state.config
                else -> null
            }
            currentConfig?.let {
                for (processor in it.imageProcessors) {
                    // Cancel all processing jobs for each processor
                    try {
                        processor.release()
                    } catch (e: Exception) {
                        // Ignore errors during release
                    }
                }
            }
            if (currentConfig != null) {
                _cameraState.value = CameraState.Stopping(currentConfig)
            }
            _cameraSnapshot.value = null
            // Unbind all use cases
            cameraProvider?.unbindAll()
            // Clear references
            camera = null
            imageAnalysis = null
            _cameraControl.value = null
            val newState = CameraState.Idle
            _cameraState.value = newState
            newState
        } catch (exception: Exception) {
            val currentConfig = when (val state = _cameraState.value) {
                is CameraState.Configured -> state.config
                else -> null
            }
            val errorState = if (currentConfig != null) {
                CameraState.ConfiguredError(currentConfig, exception)
            } else {
                CameraState.Error(exception)
            }
            _cameraState.value = errorState
            errorState
        } finally {
            // Release camera provider
            cameraProvider = null
            // Cancel all coroutines in cameraScope
            analysisScope.cancel()
            cameraScope.cancel()
        }
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            val cameraProviderFuture = ProcessCameraProvider.Companion.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    continuation.resume(cameraProviderFuture.get())
                } catch (exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            }, ContextCompat.getMainExecutor(context))
        }

    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    private fun cameraSelectorForId(cameraId: String): CameraSelector {
        val filter = CameraFilter { cameraInfos: List<CameraInfo> ->
            cameraInfos.filter {
                Camera2CameraInfo.from(it).cameraId == cameraId
            }
        }
        return CameraSelector.Builder()
            .addCameraFilter(filter)
            .build()
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Converts custom BackpressureStrategy to CameraX ImageAnalysis.BackpressureStrategy
     */
    private fun BackpressureStrategy.toCameraX(): Int {
        return when (this) {
            BackpressureStrategy.KeepOnlyLatest -> ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            BackpressureStrategy.BlockProducer -> ImageAnalysis.STRATEGY_BLOCK_PRODUCER
        }
    }

    /**
     * Converts custom Rotation to CameraX rotation constant
     */
    private fun Rotation.toCameraX(): Int {
        return when (this) {
            Rotation.Degrees0 -> Surface.ROTATION_0
            Rotation.Degrees90 -> Surface.ROTATION_90
            Rotation.Degrees180 -> Surface.ROTATION_180
            Rotation.Degrees270 -> Surface.ROTATION_270
        }
    }

    /**
     * Converts custom OutputImageFormat to CameraX ImageAnalysis output format
     */
    private fun OutputImageFormat.toCameraX(): Int {
        return when (this) {
            OutputImageFormat.YUV_420_888 -> ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
            OutputImageFormat.RGBA_8888 -> ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
        }
    }

}
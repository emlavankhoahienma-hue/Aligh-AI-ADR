package com.alignai.camera.service

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.alignai.camera.data.models.CameraCaptureMode
import com.alignai.camera.data.models.CapturedPhotoItem
import com.alignai.camera.data.models.LiveCameraStats
import com.alignai.camera.domain.FilmFilterEngine
import com.alignai.camera.domain.HistogramEngine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraService(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _minZoom = MutableStateFlow(1.0f)
    val minZoom: StateFlow<Float> = _minZoom.asStateFlow()

    private val _maxZoom = MutableStateFlow(10.0f)
    val maxZoom: StateFlow<Float> = _maxZoom.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _liveStats = MutableStateFlow(LiveCameraStats())
    val liveStats: StateFlow<LiveCameraStats> = _liveStats.asStateFlow()

    var onPhotoCaptured: ((CapturedPhotoItem) -> Unit)? = null
    var onVideoSaved: ((Uri) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        captureMode: CameraCaptureMode = CameraCaptureMode.PHOTO
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView, captureMode)
            } catch (e: Exception) {
                Log.e("CameraService", "Use case binding failed", e)
                onError?.invoke("Không thể khởi động camera: ${e.localizedMessage}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("RestrictedApi")
    fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        captureMode: CameraCaptureMode
    ) {
        val provider = cameraProvider ?: return
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // 1. Preview
        val aspectRatio = if (captureMode.isVideo) AspectRatio.RATIO_16_9 else AspectRatio.RATIO_4_3
        preview = Preview.Builder()
            .setTargetAspectRatio(aspectRatio)
            .build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // 2. Image Capture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(aspectRatio)
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            .build()

        // 3. Image Analysis (AI framing + Histogram)
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(aspectRatio)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build().also { analysis ->
                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    // Pass to AI Vision
                    AIVisionEngine.shared.processImageProxy(imageProxy)
                }
            }

        // 4. Video Capture
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.FHD))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            provider.unbindAll()
            camera = if (captureMode.isVideo) {
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture, imageAnalysis)
            } else {
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis)
            }

            camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { state ->
                _zoomRatio.value = state.zoomRatio
                _minZoom.value = state.minZoomRatio
                _maxZoom.value = state.maxZoomRatio
            }

        } catch (exc: Exception) {
            Log.e("CameraService", "Use case binding failed", exc)
            onError?.invoke("Lỗi liên kết Camera: ${exc.localizedMessage}")
        }
    }

    // MARK: - Auto-Focus & Metering Action
    fun focusAt(xNorm: Float, yNorm: Float, previewView: PreviewView) {
        val camera = camera ?: return
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(xNorm * previewView.width, yNorm * previewView.height)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()

        camera.cameraControl.startFocusAndMetering(action)
    }

    // MARK: - Zoom Control
    fun setZoom(ratio: Float) {
        val clamped = ratio.coerceIn(_minZoom.value, _maxZoom.value)
        camera?.cameraControl?.setZoomRatio(clamped)
    }

    // MARK: - Flash Control
    fun setFlashMode(flashMode: Int) {
        imageCapture?.flashMode = flashMode
    }

    fun toggleTorch(enable: Boolean) {
        camera?.cameraControl?.enableTorch(enable)
    }

    // MARK: - Pro Manual Controls (Camera2Interop)
    @OptIn(ExperimentalCamera2Interop::class)
    fun setManualISO(iso: Int?) {
        val cameraControl = camera?.cameraControl ?: return
        val camera2Control = Camera2CameraControl.from(cameraControl)
        val builder = CaptureRequestOptions.Builder()

        if (iso != null && iso > 0) {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
        } else {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        }
        camera2Control.setCaptureRequestOptions(builder.build())
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun setManualShutterSpeed(shutterDenominator: Long?) {
        val cameraControl = camera?.cameraControl ?: return
        val camera2Control = Camera2CameraControl.from(cameraControl)
        val builder = CaptureRequestOptions.Builder()

        if (shutterDenominator != null && shutterDenominator > 0) {
            val exposureTimeNs = 1_000_000_000L / shutterDenominator
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTimeNs)
        } else {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        }
        camera2Control.setCaptureRequestOptions(builder.build())
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun setManualFocusDistance(distanceNormalized: Float?) {
        val cameraControl = camera?.cameraControl ?: return
        val camera2Control = Camera2CameraControl.from(cameraControl)
        val builder = CaptureRequestOptions.Builder()

        if (distanceNormalized != null) {
            // distance 0.0 (infinity) to ~10.0 (macro) diopters
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            builder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, distanceNormalized * 10.0f)
        } else {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        }
        camera2Control.setCaptureRequestOptions(builder.build())
    }

    fun setExposureCompensation(ev: Float) {
        camera?.cameraControl?.setExposureCompensationIndex((ev * 2).toInt())
    }

    // MARK: - Capture Photo
    fun takePhoto(onResult: (Bitmap, Uri?) -> Unit) {
        val imageCapture = imageCapture ?: return
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "AlignAI_$name.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AlignAI")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    if (savedUri != null) {
                        try {
                            context.contentResolver.openInputStream(savedUri)?.use { stream ->
                                val bitmap = BitmapFactory.decodeStream(stream)
                                onResult(bitmap, savedUri)
                            }
                        } catch (e: Exception) {
                            Log.e("CameraService", "Decode failed", e)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraService", "Capture failed", exception)
                    onError?.invoke("Chụp ảnh thất bại: ${exception.localizedMessage}")
                }
            }
        )
    }

    // MARK: - Video Recording
    fun startVideoRecording() {
        val videoCapture = videoCapture ?: return
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "AlignAI_$name.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/AlignAI")
            }
        }

        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        try {
            currentRecording = videoCapture.output
                .prepareRecording(context, mediaStoreOutput)
                .apply {
                    // Audio recording
                    try {
                        withAudioEnabled()
                    } catch (e: SecurityException) {
                        Log.w("CameraService", "Audio permission not granted")
                    }
                }
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> _isRecording.value = true
                        is VideoRecordEvent.Finalize -> {
                            _isRecording.value = false
                            if (!recordEvent.hasError()) {
                                onVideoSaved?.invoke(recordEvent.outputResults.outputUri)
                            } else {
                                onError?.invoke("Lỗi ghi video: ${recordEvent.cause?.localizedMessage}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("CameraService", "Video recording start failed", e)
            onError?.invoke("Không thể bắt đầu quay: ${e.localizedMessage}")
        }
    }

    fun stopVideoRecording() {
        currentRecording?.stop()
        currentRecording = null
        _isRecording.value = false
    }

    fun shutdown() {
        cameraExecutor.shutdown()
        analysisExecutor.shutdown()
    }
}

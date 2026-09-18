package com.alignai.camera.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alignai.camera.data.models.*
import com.alignai.camera.domain.CompositionCalculator
import com.alignai.camera.domain.FilmFilterEngine
import com.alignai.camera.domain.SpatialTrackingEngine
import com.alignai.camera.service.AIVisionEngine
import com.alignai.camera.service.CameraService
import com.alignai.camera.service.ProVideoControlsService
import com.alignai.camera.service.SensorMotionService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.hypot

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val cameraService = CameraService(application)
    val sensorService = SensorMotionService(application)
    val proControls = ProVideoControlsService.shared

    // Capture Mode
    private val _captureMode = MutableStateFlow(CameraCaptureMode.PHOTO)
    val captureMode: StateFlow<CameraCaptureMode> = _captureMode.asStateFlow()

    // AI Session State
    private val _aiSessionState = MutableStateFlow<AISessionState>(AISessionState.Idle)
    val aiSessionState: StateFlow<AISessionState> = _aiSessionState.asStateFlow()

    // Composition Rule
    private val _activeCompositionRule = MutableStateFlow(CompositionRule.RULE_OF_THIRDS)
    val activeCompositionRule: StateFlow<CompositionRule> = _activeCompositionRule.asStateFlow()

    // Film Preset
    private val _activeFilmPreset = MutableStateFlow(FilmPreset.STANDARD)
    val activeFilmPreset: StateFlow<FilmPreset> = _activeFilmPreset.asStateFlow()

    // Active AI Indicator (Local/Cloud)
    private val _activeAIIndicator = MutableStateFlow(ActiveAIIndicatorType.NONE)
    val activeAIIndicator: StateFlow<ActiveAIIndicatorType> = _activeAIIndicator.asStateFlow()

    // Tracking Target
    private val _currentTargetPoint = MutableStateFlow<PointF?>(null)
    val currentTargetPoint: StateFlow<PointF?> = _currentTargetPoint.asStateFlow()

    private val _trackingQuality = MutableStateFlow(TrackingQuality.LOST)
    val trackingQuality: StateFlow<TrackingQuality> = _trackingQuality.asStateFlow()

    // Metrics
    private val _alignmentDistance = MutableStateFlow(1.0f)
    val alignmentDistance: StateFlow<Float> = _alignmentDistance.asStateFlow()

    private val _alignmentScore = MutableStateFlow(0.0)
    val alignmentScore: StateFlow<Double> = _alignmentScore.asStateFlow()

    private val _isPerfectAlignment = MutableStateFlow(false)
    val isPerfectAlignment: StateFlow<Boolean> = _isPerfectAlignment.asStateFlow()

    private val _autoCaptureCountdown = MutableStateFlow<Int?>(null)
    val autoCaptureCountdown: StateFlow<Int?> = _autoCaptureCountdown.asStateFlow()

    // Detected Faces and Subjects
    private val _detectedFaces = MutableStateFlow<List<RectF>>(emptyList())
    val detectedFaces: StateFlow<List<RectF>> = _detectedFaces.asStateFlow()

    private val _detectedSubjectRect = MutableStateFlow<RectF?>(null)
    val detectedSubjectRect: StateFlow<RectF?> = _detectedSubjectRect.asStateFlow()

    // Histogram
    private val _histogramBars = MutableStateFlow<List<HistogramBarData>>(emptyList())
    val histogramBars: StateFlow<List<HistogramBarData>> = _histogramBars.asStateFlow()

    // Media
    private val _latestCapturedPhoto = MutableStateFlow<CapturedPhotoItem?>(null)
    val latestCapturedPhoto: StateFlow<CapturedPhotoItem?> = _latestCapturedPhoto.asStateFlow()

    private val _recordedVideoUri = MutableStateFlow<Uri?>(null)
    val recordedVideoUri: StateFlow<Uri?> = _recordedVideoUri.asStateFlow()

    // Tap to focus & AE/AF lock
    private val _focusPoint = MutableStateFlow<PointF?>(null)
    val focusPoint: StateFlow<PointF?> = _focusPoint.asStateFlow()

    private val _isAEAFLocked = MutableStateFlow(false)
    val isAEAFLocked: StateFlow<Boolean> = _isAEAFLocked.asStateFlow()

    private val _sunExposureBias = MutableStateFlow(0f)
    val sunExposureBias: StateFlow<Float> = _sunExposureBias.asStateFlow()

    // Flash Mode: Auto, On, Off
    private val _flashMode = MutableStateFlow(ImageCapture.FLASH_MODE_AUTO)
    val flashMode: StateFlow<Int> = _flashMode.asStateFlow()

    // UI Drawers & Sheets
    private val _isShowingFilmDrawer = MutableStateFlow(false)
    val isShowingFilmDrawer: StateFlow<Boolean> = _isShowingFilmDrawer.asStateFlow()

    private val _isShowingSettings = MutableStateFlow(false)
    val isShowingSettings: StateFlow<Boolean> = _isShowingSettings.asStateFlow()

    private val _isCompositionRuleSheetPresented = MutableStateFlow(false)
    val isCompositionRuleSheetPresented: StateFlow<Boolean> = _isCompositionRuleSheetPresented.asStateFlow()

    private val _selectedProTab = MutableStateFlow(ProVideoParameterTab.ISO)
    val selectedProTab: StateFlow<ProVideoParameterTab> = _selectedProTab.asStateFlow()

    private val _isProDrawerExpanded = MutableStateFlow(false)
    val isProDrawerExpanded: StateFlow<Boolean> = _isProDrawerExpanded.asStateFlow()

    // Video Recording
    private val _videoFormat = MutableStateFlow(VideoFormatOption.HD_30)
    val videoFormat: StateFlow<VideoFormatOption> = _videoFormat.asStateFlow()

    private val _videoDurationSeconds = MutableStateFlow(0)
    val videoDurationSeconds: StateFlow<Int> = _videoDurationSeconds.asStateFlow()

    private var countdownJob: Job? = null
    private var videoTimerJob: Job? = null
    private var lastDetectionResult: SubjectDetectionResult = SubjectDetectionResult()

    init {
        // Setup AI Vision callback
        AIVisionEngine.shared.onDetectionCompleted = { result ->
            lastDetectionResult = result
            _detectedFaces.value = result.faceRectangles
            _detectedSubjectRect.value = result.dominantSubjectRect

            // When AI Session is active, recalculate framing target
            if (_aiSessionState.value is AISessionState.TargetPlaced || _aiSessionState.value is AISessionState.AlignmentPerfect) {
                val targetResult = CompositionCalculator.shared.calculateTarget(
                    result,
                    _activeCompositionRule.value,
                    cameraService.zoomRatio.value
                )

                // Update alignment metrics
                _currentTargetPoint.value = targetResult.targetPoint
                _alignmentDistance.value = targetResult.distance
                _alignmentScore.value = targetResult.alignmentScore

                if (targetResult.isAligned) {
                    if (_aiSessionState.value !is AISessionState.AlignmentPerfect) {
                        _aiSessionState.value = AISessionState.AlignmentPerfect
                        startAutoCaptureCountdown()
                    }
                } else {
                    if (_aiSessionState.value is AISessionState.AlignmentPerfect) {
                        cancelAutoCaptureCountdown()
                        _aiSessionState.value = AISessionState.TargetPlaced(isLocked = true)
                    }
                }
            }
        }

        // Setup Spatial Engine callback
        SpatialTrackingEngine.shared.onSpatialTargetUpdated = { point, confidence, quality ->
            _trackingQuality.value = quality
            if (_aiSessionState.value is AISessionState.TargetPlaced || _aiSessionState.value is AISessionState.AlignmentPerfect) {
                _currentTargetPoint.value = point
                val dist = hypot(point.x - 0.5f, point.y - 0.5f)
                _alignmentDistance.value = dist
                val score = (1.0 - (dist / 0.40f).toDouble()).coerceIn(0.0, 1.0)
                _alignmentScore.value = score
            }
        }

        // Start motion sensors
        sensorService.start()
    }

    override fun onCleared() {
        super.onCleared()
        sensorService.stop()
        cameraService.shutdown()
        SpatialTrackingEngine.shared.stopTracking()
    }

    fun setCaptureMode(mode: CameraCaptureMode) {
        _captureMode.value = mode
    }

    // MARK: - AI Framing Session Trigger
    fun triggerAISession() {
        if (_aiSessionState.value.isSessionActive) {
            // Cancel session
            resetAISession()
            return
        }

        _aiSessionState.value = AISessionState.Analyzing
        _activeAIIndicator.value = ActiveAIIndicatorType.LOCAL

        viewModelScope.launch {
            delay(400) // Brief analysis period
            val targetResult = CompositionCalculator.shared.calculateTarget(
                lastDetectionResult,
                _activeCompositionRule.value,
                cameraService.zoomRatio.value
            )

            _currentTargetPoint.value = targetResult.targetPoint
            _alignmentDistance.value = targetResult.distance
            _alignmentScore.value = targetResult.alignmentScore
            SpatialTrackingEngine.shared.lockAnchor(targetResult.targetPoint, cameraService.zoomRatio.value)
            _aiSessionState.value = AISessionState.TargetPlaced(isLocked = true)
        }
    }

    fun resetAISession() {
        cancelAutoCaptureCountdown()
        SpatialTrackingEngine.shared.stopTracking()
        _aiSessionState.value = AISessionState.Idle
        _currentTargetPoint.value = null
        _activeAIIndicator.value = ActiveAIIndicatorType.NONE
        _isPerfectAlignment.value = false
        _alignmentDistance.value = 1.0f
    }

    fun pinTarget(screenNormPoint: PointF) {
        SpatialTrackingEngine.shared.lockAnchor(screenNormPoint, cameraService.zoomRatio.value)
        _currentTargetPoint.value = screenNormPoint
        _aiSessionState.value = AISessionState.TargetPlaced(isLocked = true)
    }

    // MARK: - Auto-Capture Countdown
    private fun startAutoCaptureCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (count in 3 downTo 1) {
                _autoCaptureCountdown.value = count
                delay(700)
            }
            _autoCaptureCountdown.value = null
            takePhoto()
        }
    }

    private fun cancelAutoCaptureCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _autoCaptureCountdown.value = null
    }

    // MARK: - Photo Capture
    fun takePhoto() {
        _aiSessionState.value = AISessionState.Capturing
        cameraService.takePhoto { rawBitmap, uri ->
            viewModelScope.launch {
                // Apply active film preset
                val processedBitmap = FilmFilterEngine.shared.applyPreset(rawBitmap, _activeFilmPreset.value)
                val item = CapturedPhotoItem(
                    bitmap = processedBitmap,
                    uri = uri,
                    sceneType = lastDetectionResult.detectedScene,
                    appliedPreset = _activeFilmPreset.value,
                    compositionRule = _activeCompositionRule.value,
                    alignmentScore = _alignmentScore.value
                )
                _latestCapturedPhoto.value = item
                _aiSessionState.value = AISessionState.Done
                delay(1500)
                resetAISession()
            }
        }
    }

    // MARK: - Video Recording
    fun toggleVideoRecording() {
        if (cameraService.isRecording.value) {
            cameraService.stopVideoRecording()
            videoTimerJob?.cancel()
            _videoDurationSeconds.value = 0
        } else {
            cameraService.startVideoRecording()
            videoTimerJob?.cancel()
            _videoDurationSeconds.value = 0
            videoTimerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _videoDurationSeconds.value += 1
                }
            }
        }
    }

    // MARK: - Tap to focus & lock
    fun tapToFocus(normPoint: PointF, previewView: PreviewView) {
        _focusPoint.value = normPoint
        cameraService.focusAt(normPoint.x, normPoint.y, previewView)
        viewModelScope.launch {
            delay(3000)
            if (!_isAEAFLocked.value) {
                _focusPoint.value = null
            }
        }
    }

    fun lockAEAF(normPoint: PointF, previewView: PreviewView) {
        _focusPoint.value = normPoint
        _isAEAFLocked.value = true
        cameraService.focusAt(normPoint.x, normPoint.y, previewView)
    }

    fun unlockAEAF() {
        _isAEAFLocked.value = false
        _focusPoint.value = null
    }

    fun adjustSunExposure(delta: Float) {
        val newBias = (_sunExposureBias.value + delta).coerceIn(-2.0f, 2.0f)
        _sunExposureBias.value = newBias
        cameraService.setExposureCompensation(newBias)
    }

    fun setZoom(ratio: Float) {
        cameraService.setZoom(ratio)
    }

    fun toggleFlash() {
        val next = when (_flashMode.value) {
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
        _flashMode.value = next
        cameraService.setFlashMode(next)
    }

    fun setCompositionRule(rule: CompositionRule) {
        _activeCompositionRule.value = rule
    }

    fun setFilmPreset(preset: FilmPreset) {
        _activeFilmPreset.value = preset
    }

    fun toggleFilmDrawer() {
        _isShowingFilmDrawer.value = !_isShowingFilmDrawer.value
    }

    fun toggleProDrawer() {
        _isProDrawerExpanded.value = !_isProDrawerExpanded.value
    }

    fun selectProTab(tab: ProVideoParameterTab) {
        _selectedProTab.value = tab
        _isProDrawerExpanded.value = true
    }

    fun setSettingsPresented(show: Boolean) {
        _isShowingSettings.value = show
    }

    fun setCompositionRuleSheetPresented(show: Boolean) {
        _isCompositionRuleSheetPresented.value = show
    }

    fun toggleVideoFormat() {
        val formats = VideoFormatOption.values()
        val nextIndex = (formats.indexOf(_videoFormat.value) + 1) % formats.size
        _videoFormat.value = formats[nextIndex]
    }
}

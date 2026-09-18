package com.alignai.camera.service

import android.graphics.PointF
import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.alignai.camera.data.models.DetectedSceneType
import com.alignai.camera.data.models.SmartFocusType
import com.alignai.camera.data.models.SubjectDetectionResult
import com.alignai.camera.domain.SpatialTrackingEngine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean

class AIVisionEngine {
    companion object {
        val shared = AIVisionEngine()
    }

    private val isProcessing = AtomicBoolean(false)

    // ML Kit Face Detector with landmarks and classification
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.10f)
            .build()
    )

    // ML Kit Object Detector with Stream Mode
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    // ML Kit Image Labeler for Scene Classification
    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.55f)
            .build()
    )

    var onDetectionCompleted: ((SubjectDetectionResult) -> Unit)? = null
    var onSmartFocusCalculated: ((PointF, SmartFocusType) -> Unit)? = null

    private var frameCounter = 0

    @OptIn(ExperimentalGetImage::class)
    fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || !isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        val imageWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width
        val imageHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height

        val result = SubjectDetectionResult()

        // 1. Detect Faces
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                val faceRects = faces.map { face ->
                    val box = face.boundingBox
                    RectF(
                        (box.left.toFloat() / imageWidth).coerceIn(0f, 1f),
                        (box.top.toFloat() / imageHeight).coerceIn(0f, 1f),
                        (box.right.toFloat() / imageWidth).coerceIn(0f, 1f),
                        (box.bottom.toFloat() / imageHeight).coerceIn(0f, 1f)
                    )
                }
                result.faceRectangles = faceRects

                if (faces.isNotEmpty()) {
                    val primaryFace = faces[0]
                    val primaryBox = faceRects[0]
                    result.dominantSubjectRect = primaryBox
                    result.confidence = 0.95f
                    result.detectedScene = DetectedSceneType.PORTRAIT

                    // Eye position
                    val leftEye = primaryFace.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)
                    val rightEye = primaryFace.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)
                    if (leftEye != null && rightEye != null) {
                        val eyeX = ((leftEye.position.x + rightEye.position.x) / 2.0f / imageWidth).coerceIn(0f, 1f)
                        val eyeY = ((leftEye.position.y + rightEye.position.y) / 2.0f / imageHeight).coerceIn(0f, 1f)
                        result.primaryEyePosition = PointF(eyeX, eyeY)
                    } else {
                        result.primaryEyePosition = PointF(primaryBox.centerX(), primaryBox.centerY())
                    }

                    // Feed optical tracking to spatial engine if active
                    SpatialTrackingEngine.shared.updateWithOpticalDetection(
                        result.primaryEyePosition ?: PointF(primaryBox.centerX(), primaryBox.centerY()),
                        0.95f
                    )

                    onSmartFocusCalculated?.invoke(
                        result.primaryEyePosition ?: PointF(primaryBox.centerX(), primaryBox.centerY()),
                        SmartFocusType.FACE
                    )
                }

                // 2. Detect Objects (if no faces or for general framing)
                if (faces.isEmpty()) {
                    objectDetector.process(image)
                        .addOnSuccessListener { objects ->
                            if (objects.isNotEmpty()) {
                                val topObj = objects.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                                topObj?.let { obj ->
                                    val b = obj.boundingBox
                                    val normRect = RectF(
                                        (b.left.toFloat() / imageWidth).coerceIn(0f, 1f),
                                        (b.top.toFloat() / imageHeight).coerceIn(0f, 1f),
                                        (b.right.toFloat() / imageWidth).coerceIn(0f, 1f),
                                        (b.bottom.toFloat() / imageHeight).coerceIn(0f, 1f)
                                    )
                                    result.dominantSubjectRect = normRect
                                    result.confidence = 0.85f

                                    val center = PointF(normRect.centerX(), normRect.centerY())
                                    SpatialTrackingEngine.shared.updateWithOpticalDetection(center, 0.85f)
                                    onSmartFocusCalculated?.invoke(center, SmartFocusType.SALIENT_OBJECT)
                                }
                            }
                        }
                }

                // 3. Periodic Scene Classification (every 15 frames)
                frameCounter++
                if (frameCounter % 15 == 0 && faces.isEmpty()) {
                    imageLabeler.process(image)
                        .addOnSuccessListener { labels ->
                            for (label in labels) {
                                val text = label.text.lowercase()
                                val matched = when {
                                    text.contains("dog") || text.contains("cat") || text.contains("pet") || text.contains("animal") -> DetectedSceneType.PET
                                    text.contains("sunset") || text.contains("sunrise") || text.contains("dusk") -> DetectedSceneType.SUNSET
                                    text.contains("mountain") || text.contains("landscape") || text.contains("hill") -> DetectedSceneType.LANDSCAPE
                                    text.contains("building") || text.contains("architecture") || text.contains("city") -> DetectedSceneType.ARCHITECTURE
                                    text.contains("sky") || text.contains("cloud") -> DetectedSceneType.SKY
                                    text.contains("water") || text.contains("sea") || text.contains("ocean") || text.contains("beach") -> DetectedSceneType.WATER
                                    text.contains("flower") || text.contains("plant") || text.contains("tree") -> DetectedSceneType.FOLIAGE
                                    text.contains("night") || text.contains("dark") -> DetectedSceneType.NIGHT
                                    text.contains("food") || text.contains("meal") || text.contains("dish") -> DetectedSceneType.FOOD
                                    text.contains("street") || text.contains("road") -> DetectedSceneType.STREET
                                    else -> null
                                }
                                if (matched != null) {
                                    result.detectedScene = matched
                                    break
                                }
                            }
                        }
                }

                onDetectionCompleted?.invoke(result)
            }
            .addOnCompleteListener {
                isProcessing.set(false)
                imageProxy.close()
            }
    }
}

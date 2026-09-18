package com.alignai.camera.domain

import android.graphics.PointF
import android.graphics.RectF
import com.alignai.camera.data.models.CompositionRule
import com.alignai.camera.data.models.FramingTargetResult
import com.alignai.camera.data.models.SubjectDetectionResult
import com.alignai.camera.data.models.DetectedSceneType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class CompositionCalculator {
    companion object {
        val shared = CompositionCalculator()
    }

    private val phiRatio = 0.61803398875f
    private val phiInverseRatio = 0.38196601125f
    var alignmentTolerance = 0.038f

    fun calculateTarget(
        detection: SubjectDetectionResult,
        rule: CompositionRule,
        currentZoom: Float = 1.0f
    ): FramingTargetResult {
        val center = PointF(0.5f, 0.5f)
        val resolvedRule = resolveDynamicRule(rule, detection)

        val targetPoint: PointF
        var advice: String
        var recommendedZoom = currentZoom

        when (resolvedRule) {
            CompositionRule.RULE_OF_THIRDS -> {
                val pair = computeRuleOfThirdsTarget(detection)
                targetPoint = pair.first
                advice = pair.second
            }
            CompositionRule.GOLDEN_RATIO -> {
                val pair = computeGoldenRatioTarget(detection)
                targetPoint = pair.first
                advice = pair.second
            }
            CompositionRule.GOLDEN_SPIRAL -> {
                val pair = computeGoldenSpiralTarget(detection)
                targetPoint = pair.first
                advice = pair.second
            }
            CompositionRule.CENTER_SYMMETRY -> {
                targetPoint = PointF(0.5f, 0.5f)
                advice = "Giữ chủ thể đối xứng ngay chính giữa khung hình"
            }
            CompositionRule.DYNAMIC_AI -> {
                val pair = computeRuleOfThirdsTarget(detection)
                targetPoint = pair.first
                advice = pair.second
            }
        }

        // Auto-zoom recommendation
        val isGroup = detection.faceRectangles.size > 1
        detection.dominantSubjectRect?.let { dominant ->
            recommendedZoom = computeOptimalZoom(dominant, currentZoom, isGroup)
        } ?: detection.faceRectangles.firstOrNull()?.let { face ->
            recommendedZoom = computeOptimalZoom(face, currentZoom, isGroup)
        }

        val dx = targetPoint.x - center.x
        val dy = targetPoint.y - center.y
        val distance = hypot(dx, dy)
        val radians = atan2(dy, dx)
        var degrees = (radians * 180.0 / Math.PI).toFloat()
        if (degrees < 0) degrees += 360f

        val maxSearchRadius = 0.40f
        val rawScore = max(0.0, 1.0 - (distance / maxSearchRadius).toDouble())
        val alignmentScore = min(1.0, rawScore)
        val isAligned = distance <= alignmentTolerance

        if (isAligned) {
            advice = "Bố cục hoàn hảo! Chạm nút chụp ngay"
        }

        return FramingTargetResult(
            targetPoint = targetPoint,
            currentCenter = center,
            offsetVector = PointF(dx, dy),
            distance = distance,
            angleDegrees = degrees,
            alignmentScore = alignmentScore,
            isAligned = isAligned,
            recommendedZoomFactor = recommendedZoom,
            optimalRule = resolvedRule,
            guideDescription = advice
        )
    }

    private fun resolveDynamicRule(
        requestedRule: CompositionRule,
        detection: SubjectDetectionResult
    ): CompositionRule {
        if (requestedRule != CompositionRule.DYNAMIC_AI) return requestedRule
        return when (detection.detectedScene) {
            DetectedSceneType.PORTRAIT, DetectedSceneType.PET -> CompositionRule.GOLDEN_RATIO
            DetectedSceneType.LANDSCAPE, DetectedSceneType.SUNSET, DetectedSceneType.MACRO,
            DetectedSceneType.SKY, DetectedSceneType.WATER, DetectedSceneType.FOLIAGE -> CompositionRule.RULE_OF_THIRDS
            DetectedSceneType.ARCHITECTURE, DetectedSceneType.FOOD -> CompositionRule.CENTER_SYMMETRY
            DetectedSceneType.STREET, DetectedSceneType.NIGHT, DetectedSceneType.GENERAL -> {
                if (detection.faceRectangles.isEmpty()) CompositionRule.RULE_OF_THIRDS else CompositionRule.GOLDEN_RATIO
            }
        }
    }

    private fun computeRuleOfThirdsTarget(detection: SubjectDetectionResult): Pair<PointF, String> {
        val thirdsX = floatArrayOf(1.0f / 3.0f, 2.0f / 3.0f)
        val thirdsY = floatArrayOf(1.0f / 3.0f, 2.0f / 3.0f)

        val subject = detection.dominantSubjectRect
            ?: return Pair(PointF(2.0f / 3.0f, 1.0f / 3.0f), "Hướng góc chụp về điểm 1/3 góc trên")

        val subjectCenter = detection.primaryEyePosition ?: PointF(subject.centerX(), subject.centerY())

        val preferredX = if (abs(detection.lookingDirection.x) > 0.15f) {
            if (detection.lookingDirection.x > 0) thirdsX[1] else thirdsX[0]
        } else {
            thirdsX.minByOrNull { abs(it - subjectCenter.x) } ?: thirdsX[0]
        }
        val preferredY = thirdsY.minByOrNull { abs(it - subjectCenter.y) } ?: thirdsY[0]

        val advice = if (abs(detection.lookingDirection.x) > 0.15f) {
            "Đưa tâm trắng để chừa khoảng trống phía chủ thể đang nhìn"
        } else {
            "Đưa tâm trắng vào giao điểm 1/3 gần chủ thể nhất"
        }
        return Pair(PointF(preferredX, preferredY), advice)
    }

    private fun computeGoldenRatioTarget(detection: SubjectDetectionResult): Pair<PointF, String> {
        val goldenX = floatArrayOf(phiInverseRatio, phiRatio)
        val goldenY = floatArrayOf(phiInverseRatio, phiRatio)

        val subject = detection.dominantSubjectRect
            ?: return Pair(PointF(phiRatio, phiInverseRatio), "Căn chỉnh theo tỷ lệ vàng 1.618")

        val subjectCenter = detection.primaryEyePosition ?: PointF(subject.centerX(), subject.centerY())

        val preferredX = if (abs(detection.lookingDirection.x) > 0.15f) {
            if (detection.lookingDirection.x > 0) goldenX[1] else goldenX[0]
        } else {
            goldenX.minByOrNull { abs(it - subjectCenter.x) } ?: goldenX[0]
        }
        val preferredY = goldenY.minByOrNull { abs(it - subjectCenter.y) } ?: goldenY[0]

        return Pair(PointF(preferredX, preferredY), "Đưa tâm trắng vào điểm vàng gần chủ thể nhất")
    }

    private fun computeGoldenSpiralTarget(detection: SubjectDetectionResult): Pair<PointF, String> {
        val spiralFoci = listOf(
            PointF(phiRatio, phiInverseRatio),
            PointF(phiInverseRatio, phiInverseRatio),
            PointF(phiRatio, phiRatio),
            PointF(phiInverseRatio, phiRatio)
        )
        val subject = detection.dominantSubjectRect
            ?: return Pair(spiralFoci[0], "Uốn lượn bố cục theo xoắn ốc Fibonacci")

        val subjectCenter = detection.primaryEyePosition ?: PointF(subject.centerX(), subject.centerY())
        val nearest = spiralFoci.minByOrNull {
            hypot(it.x - subjectCenter.x, it.y - subjectCenter.y)
        } ?: spiralFoci[0]

        return Pair(nearest, "Đưa tâm trắng vào tiêu điểm xoắn ốc Fibonacci gần chủ thể nhất")
    }

    private fun computeOptimalZoom(subjectRect: RectF, currentZoom: Float, isGroup: Boolean = false): Float {
        if (isGroup) return 1.0f
        val subjectArea = subjectRect.width() * subjectRect.height()
        return when {
            subjectArea < 0.035f -> 2.5f
            subjectArea < 0.09f -> 2.0f
            subjectArea < 0.18f -> 1.6f
            subjectArea < 0.32f -> 1.3f
            else -> 1.0f
        }
    }
}

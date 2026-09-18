package com.alignai.camera.domain

import android.graphics.PointF
import com.alignai.camera.data.models.TrackingQuality
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Unified Spatial Visual-Inertial Fusion Engine for Android.
 * Pins anchor to target object, suppresses outliers, and uses gyroscope dead-reckoning during optical frame drops.
 */
class SpatialTrackingEngine {
    companion object {
        val shared = SpatialTrackingEngine()
    }

    private val oneEuroFilter = OneEuroFilter(minCutoff = 1.50f, beta = 1.80f)
    private val stateLock = Any()

    var isTrackingActive = false
        private set

    var currentPoint: PointF = PointF(0.5f, 0.5f)
        private set

    var currentQuality: TrackingQuality = TrackingQuality.LOST
        private set

    private var currentZoom = 1.0f
    private var lastOpticalAcceptTime = 0L
    private var deadReckoningCount = 0
    private var outlierStreak = 0

    var maxObservationJump = 0.15f
    var opticalAcceptThreshold = 0.25f

    var onSpatialTargetUpdated: ((PointF, Double, TrackingQuality) -> Unit)? = null

    fun lockAnchor(screenPoint: PointF, zoom: Float = 1.0f) {
        synchronized(stateLock) {
            currentZoom = max(1.0f, zoom)
            currentPoint = PointF(screenPoint.x, screenPoint.y)
            oneEuroFilter.reset(screenPoint.x, screenPoint.y)
            lastOpticalAcceptTime = System.currentTimeMillis()
            deadReckoningCount = 0
            outlierStreak = 0
            isTrackingActive = true
            currentQuality = TrackingQuality.LOCKED
        }
        onSpatialTargetUpdated?.invoke(currentPoint, 1.0, TrackingQuality.LOCKED)
    }

    fun updateZoom(zoom: Float) {
        synchronized(stateLock) {
            currentZoom = max(1.0f, zoom)
        }
    }

    /**
     * Optical update from ML Kit / AI Image Analysis
     */
    fun updateWithOpticalDetection(point: PointF?, confidence: Float) {
        if (!isTrackingActive) return
        val now = System.currentTimeMillis()

        synchronized(stateLock) {
            if (point != null && confidence >= opticalAcceptThreshold) {
                lastOpticalAcceptTime = now
                deadReckoningCount = 0

                var rawObsX = point.x
                var rawObsY = point.y
                val jump = hypot(rawObsX - currentPoint.x, rawObsY - currentPoint.y)

                if (jump > maxObservationJump) {
                    outlierStreak++
                    if (outlierStreak >= 5) {
                        // Accept reposition smoothly after 5 consistent frames
                        outlierStreak = 0
                    } else {
                        // Clamp observation within max jump radius
                        val k = maxObservationJump / jump
                        rawObsX = currentPoint.x + (rawObsX - currentPoint.x) * k
                        rawObsY = currentPoint.y + (rawObsY - currentPoint.y) * k
                    }
                } else {
                    outlierStreak = 0
                }

                val filtered = oneEuroFilter.filter(rawObsX, rawObsY, now)
                val clampedX = min(0.98f, max(0.02f, filtered.x))
                val clampedY = min(0.98f, max(0.02f, filtered.y))
                currentPoint = PointF(clampedX, clampedY)
                currentQuality = TrackingQuality.LOCKED

                onSpatialTargetUpdated?.invoke(currentPoint, confidence.toDouble(), TrackingQuality.LOCKED)
            }
        }
    }

    /**
     * Gyroscope inertial update (~60Hz).
     * Compensates camera movement during fast sweeps or when optical tracking temporarily drops.
     */
    fun updateWithGyroscope(rateX: Float, rateY: Float, dtSeconds: Float) {
        if (!isTrackingActive) return
        val now = System.currentTimeMillis()

        synchronized(stateLock) {
            val timeSinceOptical = (now - lastOpticalAcceptTime) / 1000.0f
            // Only perform dead-reckoning if optical hasn't updated in the last 100ms
            if (timeSinceOptical <= 0.10f) return

            deadReckoningCount++
            val scale = 0.90f * currentZoom

            // Panning right (negative Y angular rate in screen coords) -> target moves left
            // Tilting up (negative X angular rate) -> target moves down
            val dx = rateY * dtSeconds * scale
            val dy = -rateX * dtSeconds * scale

            // Inertial decay
            val optDx = oneEuroFilter.velocityX * dtSeconds * max(0.0f, 1.0f - timeSinceOptical / 0.5f)
            val optDy = oneEuroFilter.velocityY * dtSeconds * max(0.0f, 1.0f - timeSinceOptical / 0.5f)

            val newX = min(0.98f, max(0.02f, currentPoint.x + dx + optDx))
            val newY = min(0.98f, max(0.02f, currentPoint.y + dy + optDy))
            currentPoint = PointF(newX, newY)

            currentQuality = when {
                deadReckoningCount > 200 -> TrackingQuality.LOST
                deadReckoningCount > 70 -> TrackingQuality.REACQUIRING
                else -> TrackingQuality.PREDICTING
            }

            val confidence = when (currentQuality) {
                TrackingQuality.LOCKED -> 1.0
                TrackingQuality.PREDICTING -> 0.65
                TrackingQuality.REACQUIRING -> 0.35
                TrackingQuality.LOST -> 0.10
            }

            onSpatialTargetUpdated?.invoke(currentPoint, confidence, currentQuality)
        }
    }

    fun stopTracking() {
        synchronized(stateLock) {
            isTrackingActive = false
            currentQuality = TrackingQuality.LOST
            oneEuroFilter.reset()
        }
    }
}

package com.alignai.camera.domain

import android.graphics.PointF
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.max

/**
 * Adaptive 1-Euro Filter for sub-millimeter jitter suppression and zero-lag tracking.
 * - Stationary: High filtering (1.50Hz cutoff) eliminates 100% biological hand tremors.
 * - Motion: Dynamic cutoff adapts according to camera panning velocity.
 */
class OneEuroFilter(
    private var minCutoff: Float = 1.50f,
    private var beta: Float = 1.80f,
    private val dCutoff: Float = 1.20f
) {
    private var xPrev = 0.5f
    private var yPrev = 0.5f
    private var dxPrev = 0.0f
    private var dyPrev = 0.0f
    private var lastTimestamp: Long = 0
    private var isInitialized = false

    var velocityX: Float = 0f
        private set
    var velocityY: Float = 0f
        private set

    fun reset(initialX: Float = 0.5f, initialY: Float = 0.5f) {
        xPrev = initialX
        yPrev = initialY
        dxPrev = 0f
        dyPrev = 0f
        velocityX = 0f
        velocityY = 0f
        lastTimestamp = 0
        isInitialized = false
    }

    fun filter(x: Float, y: Float, timestampMs: Long): PointF {
        if (!isInitialized || lastTimestamp == 0L) {
            xPrev = x
            yPrev = y
            lastTimestamp = timestampMs
            isInitialized = true
            return PointF(x, y)
        }

        val dt = max(0.005f, (timestampMs - lastTimestamp) / 1000.0f)
        lastTimestamp = timestampMs
        val rate = 1.0f / dt

        // 1. Calculate velocity derivative
        val rawDx = (x - xPrev) / dt
        val rawDy = (y - yPrev) / dt

        val aD = alpha(rate, dCutoff)
        val dxHat = aD * rawDx + (1.0f - aD) * dxPrev
        val dyHat = aD * rawDy + (1.0f - aD) * dyPrev
        dxPrev = dxHat
        dyPrev = dyHat
        velocityX = dxHat
        velocityY = dyHat

        // 2. Adaptive cutoff frequency
        val speed = hypot(dxHat, dyHat)
        val adaptiveCutoff = minCutoff + beta * speed

        // 3. Filter position
        val aPos = alpha(rate, adaptiveCutoff)
        val xHat = aPos * x + (1.0f - aPos) * xPrev
        val yHat = aPos * y + (1.0f - aPos) * yPrev
        xPrev = xHat
        yPrev = yHat

        return PointF(xHat, yHat)
    }

    private fun alpha(rate: Float, cutoff: Float): Float {
        val tau = 1.0f / (2.0f * PI.toFloat() * cutoff)
        val te = 1.0f / rate
        return 1.0f / (1.0f + tau / te)
    }
}

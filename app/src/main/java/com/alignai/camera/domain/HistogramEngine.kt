package com.alignai.camera.domain

import androidx.compose.ui.graphics.Color
import com.alignai.camera.data.models.HistogramBarData
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * Real-time RGB and Luminance Histogram Calculator.
 */
class HistogramEngine {
    companion object {
        val shared = HistogramEngine()
    }

    private val binCount = 32

    fun computeFromRgbaBuffer(buffer: ByteBuffer, width: Int, height: Int): List<HistogramBarData> {
        val rBins = IntArray(binCount)
        val gBins = IntArray(binCount)
        val bBins = IntArray(binCount)
        val lumaBins = IntArray(binCount)

        val totalPixels = width * height
        val step = max(1, totalPixels / 10000) // Sample ~10,000 pixels for 60fps performance

        buffer.rewind()
        val capacity = buffer.remaining()

        var sampleCount = 0
        var i = 0
        while (i < capacity - 4) {
            val r = buffer.get(i).toInt() and 0xFF
            val g = buffer.get(i + 1).toInt() and 0xFF
            val b = buffer.get(i + 2).toInt() and 0xFF

            val luma = (0.2126f * r + 0.7152f * g + 0.0722f * b).toInt().coerceIn(0, 255)

            val rIndex = (r * binCount / 256).coerceIn(0, binCount - 1)
            val gIndex = (g * binCount / 256).coerceIn(0, binCount - 1)
            val bIndex = (b * binCount / 256).coerceIn(0, binCount - 1)
            val lumaIndex = (luma * binCount / 256).coerceIn(0, binCount - 1)

            rBins[rIndex]++
            gBins[gIndex]++
            bBins[bIndex]++
            lumaBins[lumaIndex]++

            sampleCount++
            i += step * 4
        }

        if (sampleCount == 0) return emptyList()

        val maxCount = (lumaBins.maxOrNull() ?: 1).toFloat()

        return (0 until binCount).map { index ->
            val height = (lumaBins[index].toFloat() / maxCount).coerceIn(0.05f, 1.0f)
            val color = when {
                index < binCount / 3 -> Color(0xFF34C759) // Shadows
                index < binCount * 2 / 3 -> Color(0xFFFFCC00) // Midtones
                else -> Color(0xFFFF3B30) // Highlights
            }
            HistogramBarData(
                id = index,
                height = height,
                color = color
            )
        }
    }
}

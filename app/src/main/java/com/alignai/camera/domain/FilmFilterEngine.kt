package com.alignai.camera.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.alignai.camera.data.models.AIColorParameters
import com.alignai.camera.data.models.FilmPreset

/**
 * Film Simulation & Studio Color Science Engine for Android.
 * Implements 18 film looks and AI full color pipeline using GPU ColorMatrix transformation.
 */
class FilmFilterEngine {
    companion object {
        val shared = FilmFilterEngine()
    }

    fun applyPreset(source: Bitmap, preset: FilmPreset): Bitmap {
        if (preset == FilmPreset.STANDARD || preset == FilmPreset.AI_FULL_AUTO) {
            return source
        }
        val matrix = getPresetColorMatrix(preset)
        return applyColorMatrix(source, matrix)
    }

    fun applyPresetAndAIParameters(
        source: Bitmap,
        preset: FilmPreset,
        params: AIColorParameters?
    ): Bitmap {
        var result = applyPreset(source, preset)
        params?.let {
            result = applyAIColorParameters(result, it)
        }
        return result
    }

    fun applyAIColorParameters(source: Bitmap, params: AIColorParameters): Bitmap {
        val matrix = ColorMatrix()

        // 1. Exposure Bias
        if (params.exposureBias != 0.0f) {
            val bias = params.exposureBias * 35.0f
            val expMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, bias,
                0f, 1f, 0f, 0f, bias,
                0f, 0f, 1f, 0f, bias,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(expMatrix)
        }

        // 2. Warmth and Tint
        if (params.warmthShift != 0.0f || params.tintShift != 0.0f) {
            val rShift = params.warmthShift * 25.0f
            val bShift = -params.warmthShift * 25.0f
            val gShift = params.tintShift * 20.0f
            val wbMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, rShift,
                0f, 1f, 0f, 0f, gShift,
                0f, 0f, 1f, 0f, bShift,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(wbMatrix)
        }

        // 3. Contrast Curve
        if (params.contrastCurve != 1.0f) {
            val scale = params.contrastCurve
            val translate = (-0.5f * scale + 0.5f) * 255.0f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(contrastMatrix)
        }

        // 4. Saturation
        if (params.saturationBoost != 1.0f) {
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(params.saturationBoost)
            matrix.postConcat(satMatrix)
        }

        return applyColorMatrix(source, matrix)
    }

    private fun applyColorMatrix(source: Bitmap, matrix: ColorMatrix): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    fun getPresetColorMatrix(preset: FilmPreset): ColorMatrix {
        val matrix = ColorMatrix()
        when (preset) {
            FilmPreset.STANDARD, FilmPreset.AI_FULL_AUTO -> {
                // Identity
            }
            FilmPreset.FUJI_PRO_400H -> {
                // Fuji Pastel: subtle cyan-green tint, lifted shadows, soft contrast, boosted skin warmth
                matrix.set(floatArrayOf(
                    1.02f, 0f, 0f, 0f, 8f,
                    0f, 1.04f, 0f, 0f, 12f,
                    0f, 0f, 0.98f, 0f, 6f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilmPreset.KODAK_PORTRA_400 -> {
                // Warm golden, smooth skin tones, gentle highlights
                matrix.set(floatArrayOf(
                    1.08f, 0f, 0f, 0f, 14f,
                    0f, 1.02f, 0f, 0f, 6f,
                    0f, 0f, 0.92f, 0f, -8f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilmPreset.CLASSIC_CHROME -> {
                // Documentary film look: muted saturation, deep shadows
                val m = ColorMatrix()
                m.setSaturation(0.82f)
                val c = ColorMatrix(floatArrayOf(
                    1.12f, 0f, 0f, 0f, -10f,
                    0f, 1.10f, 0f, 0f, -8f,
                    0f, 0f, 1.06f, 0f, -4f,
                    0f, 0f, 0f, 1f, 0f
                ))
                m.postConcat(c)
                matrix.set(m)
            }
            FilmPreset.CINEMA_TEAL_ORANGE -> {
                // Hollywood block-buster: warm highlights, teal shadows
                matrix.set(floatArrayOf(
                    1.18f, 0f, 0f, 0f, 15f,
                    0f, 1.00f, 0f, 0f, 5f,
                    0f, 0f, 1.25f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilmPreset.VELVIA_50 -> {
                // Saturated nature, deep blues and rich foliage greens
                val m = ColorMatrix()
                m.setSaturation(1.38f)
                val c = ColorMatrix(floatArrayOf(
                    1.08f, 0f, 0f, 0f, -5f,
                    0f, 1.15f, 0f, 0f, -5f,
                    0f, 0f, 1.18f, 0f, -2f,
                    0f, 0f, 0f, 1f, 0f
                ))
                m.postConcat(c)
                matrix.set(m)
            }
            FilmPreset.SUNSET_GLOW -> {
                // Golden hour orange and amber glow
                matrix.set(floatArrayOf(
                    1.28f, 0f, 0f, 0f, 25f,
                    0f, 1.05f, 0f, 0f, 10f,
                    0f, 0f, 0.82f, 0f, -18f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilmPreset.TOKYO_AIRY -> {
                // Japanese airy style: lifted shadows, light pastel, clean whites
                matrix.set(floatArrayOf(
                    1.00f, 0f, 0f, 0f, 18f,
                    0f, 1.04f, 0f, 0f, 20f,
                    0f, 0f, 1.08f, 0f, 22f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilmPreset.HK_CINEMA_90S -> {
                // Emerald shadows, golden warm incandescent highlights (Wong Kar-wai)
                matrix.set(floatArrayOf(
                    1.08f, 0f, 0f, 0f, 10f,
                    0f, 1.15f, 0f, 0f, 14f,
                    0f, 0f, 0.85f, 0f, -15f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilmPreset.CINESTILL_800T -> {
                // Tungsten night movie film, halo glow around lamps
                matrix.set(floatArrayOf(
                    0.96f, 0f, 0f, 0f, 5f,
                    0f, 1.00f, 0f, 0f, 2f,
                    0f, 0f, 1.22f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilmPreset.LEICA_MONOCHROM -> {
                // Pure silver grayscale with smooth gradient
                val m = ColorMatrix()
                m.setSaturation(0.0f)
                val c = ColorMatrix(floatArrayOf(
                    1.05f, 0f, 0f, 0f, 4f,
                    0f, 1.05f, 0f, 0f, 4f,
                    0f, 0f, 1.05f, 0f, 4f,
                    0f, 0f, 0f, 1f, 0f
                ))
                m.postConcat(c)
                matrix.set(m)
            }
            FilmPreset.MONOCHROME_NOIR -> {
                // High contrast dramatic black and white
                val m = ColorMatrix()
                m.setSaturation(0.0f)
                val c = ColorMatrix(floatArrayOf(
                    1.35f, 0f, 0f, 0f, -35f,
                    0f, 1.35f, 0f, 0f, -35f,
                    0f, 0f, 1.35f, 0f, -35f,
                    0f, 0f, 0f, 1f, 0f
                ))
                m.postConcat(c)
                matrix.set(m)
            }
            FilmPreset.TRI_X_400 -> {
                // Photojournalism black & white with punchy mids
                val m = ColorMatrix()
                m.setSaturation(0.0f)
                val c = ColorMatrix(floatArrayOf(
                    1.20f, 0f, 0f, 0f, -18f,
                    0f, 1.20f, 0f, 0f, -18f,
                    0f, 0f, 1.20f, 0f, -18f,
                    0f, 0f, 0f, 1f, 0f
                ))
                m.postConcat(c)
                matrix.set(m)
            }
            FilmPreset.VINTAGE_WARM -> {
                // 1970s warm retro fade
                matrix.set(floatArrayOf(
                    1.12f, 0f, 0f, 0f, 18f,
                    0f, 1.04f, 0f, 0f, 12f,
                    0f, 0f, 0.86f, 0f, -12f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilmPreset.STREET_CLASSIC -> {
                // Snappy street photography, rich micro-contrast
                matrix.set(floatArrayOf(
                    1.14f, 0f, 0f, 0f, -8f,
                    0f, 1.10f, 0f, 0f, -6f,
                    0f, 0f, 1.08f, 0f, -5f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilmPreset.NORDIC_COLD -> {
                // Cool Scandinavian minimal desaturated look
                val m = ColorMatrix()
                m.setSaturation(0.75f)
                val c = ColorMatrix(floatArrayOf(
                    0.92f, 0f, 0f, 0f, -4f,
                    0f, 0.98f, 0f, 0f, 0f,
                    0f, 0f, 1.15f, 0f, 12f,
                    0f, 0f, 0f, 1f, 0f
                ))
                m.postConcat(c)
                matrix.set(m)
            }
            FilmPreset.EKTAR_100 -> {
                // Ultra vivid red & blue saturation
                val m = ColorMatrix()
                m.setSaturation(1.30f)
                val c = ColorMatrix(floatArrayOf(
                    1.12f, 0f, 0f, 0f, 4f,
                    0f, 1.06f, 0f, 0f, 0f,
                    0f, 0f, 1.18f, 0f, 6f,
                    0f, 0f, 0f, 1f, 0f
                ))
                m.postConcat(c)
                matrix.set(m)
            }
            FilmPreset.NEON_CYBERPUNK -> {
                // Neon magenta highlights and deep purple shadows
                matrix.set(floatArrayOf(
                    1.25f, 0f, 0f, 0f, 20f,
                    0f, 0.88f, 0f, 0f, -10f,
                    0f, 0f, 1.40f, 0f, 25f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
        }
        return matrix
    }
}

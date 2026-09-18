package com.alignai.camera.presentation.ui

import android.graphics.PointF
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alignai.camera.data.models.AISessionState
import com.alignai.camera.data.models.CompositionRule
import com.alignai.camera.data.models.TrackingQuality
import com.alignai.camera.presentation.theme.AmberAccent
import com.alignai.camera.presentation.theme.GreenAligned
import com.alignai.camera.presentation.viewmodel.CameraViewModel
import kotlin.math.*

@Composable
fun ARFramingOverlay(
    viewModel: CameraViewModel,
    previewView: PreviewView?,
    modifier: Modifier = Modifier
) {
    val aiState by viewModel.aiSessionState.collectAsState()
    val targetPoint by viewModel.currentTargetPoint.collectAsState()
    val trackingQuality by viewModel.trackingQuality.collectAsState()
    val isAligned by viewModel.isPerfectAlignment.collectAsState()
    val countdown by viewModel.autoCaptureCountdown.collectAsState()
    val activeRule by viewModel.activeCompositionRule.collectAsState()
    val faces by viewModel.detectedFaces.collectAsState()
    val focusPoint by viewModel.focusPoint.collectAsState()
    val isLocked by viewModel.isAEAFLocked.collectAsState()
    val rollDegrees by viewModel.sensorService.rollDegrees.collectAsState()
    val isLevel by viewModel.sensorService.isLevel.collectAsState()

    // Radar pulse animation for target gold circle
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulse"
    )
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        val newZoom = viewModel.cameraService.zoomRatio.value * zoom
                        viewModel.setZoom(newZoom)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val norm = PointF(offset.x / size.width, offset.y / size.height)
                        if (isLocked) {
                            viewModel.unlockAEAF()
                        } else if (aiState is AISessionState.TargetPlaced) {
                            viewModel.pinTarget(norm)
                        } else {
                            previewView?.let { pv ->
                                viewModel.tapToFocus(norm, pv)
                            }
                        }
                    },
                    onLongPress = { offset ->
                        val norm = PointF(offset.x / size.width, offset.y / size.height)
                        previewView?.let { pv ->
                            viewModel.lockAEAF(norm, pv)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.5f, h * 0.5f)

            // 1. Composition Grid Lines
            if (aiState.isSessionActive) {
                drawCompositionGrid(activeRule, w, h)
            }

            // 2. Detected Face Boxes
            for (face in faces) {
                drawRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(face.left * w, face.top * h),
                    size = Size(face.width() * w, face.height() * h),
                    style = Stroke(width = 1.5f.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                )
            }

            // 3. Virtual Horizon Leveler
            if (!aiState.isSessionActive) {
                val levelColor = if (isLevel) GreenAligned else Color.White.copy(alpha = 0.6f)
                val lineLength = 70.dp.toPx()
                val rad = Math.toRadians(rollDegrees.toDouble())
                val dx = (cos(rad) * lineLength).toFloat()
                val dy = (sin(rad) * lineLength).toFloat()
                drawLine(
                    color = levelColor,
                    start = Offset(center.x - dx, center.y - dy),
                    end = Offset(center.x + dx, center.y + dy),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = levelColor,
                    radius = 3.dp.toPx(),
                    center = center
                )
            }

            // 4. Guidance Ray Line from Screen Center -> Target Point
            targetPoint?.let { target ->
                val targetScreen = Offset(target.x * w, target.y * h)
                val rayColor = if (isAligned) GreenAligned else AmberAccent.copy(alpha = 0.7f)

                drawLine(
                    color = rayColor,
                    start = center,
                    end = targetScreen,
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )

                // Target Gold Circle
                val ringColor = when (trackingQuality) {
                    TrackingQuality.LOCKED -> if (isAligned) GreenAligned else AmberAccent
                    TrackingQuality.PREDICTING, TrackingQuality.REACQUIRING -> Color(0xFFFF9500)
                    TrackingQuality.LOST -> Color.Red
                }

                // Pulsing radar ring
                if (!isAligned) {
                    drawCircle(
                        color = ringColor.copy(alpha = radarAlpha),
                        radius = 36.dp.toPx() * radarPulse,
                        center = targetScreen,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Core target circle
                drawCircle(
                    color = ringColor,
                    radius = 32.dp.toPx(),
                    center = targetScreen,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = ringColor,
                    radius = 5.dp.toPx(),
                    center = targetScreen
                )

                // 5. Center Optical Crosshair (Only shown when target is placed)
                val crosshairColor = if (isAligned) GreenAligned else Color.White
                val chSize = 16.dp.toPx()
                drawLine(
                    color = crosshairColor,
                    start = Offset(center.x - chSize, center.y),
                    end = Offset(center.x + chSize, center.y),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = crosshairColor,
                    start = Offset(center.x, center.y - chSize),
                    end = Offset(center.x, center.y + chSize),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = crosshairColor,
                    radius = 18.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 6. Tap-to-Focus Yellow Box
            focusPoint?.let { fp ->
                val boxCenter = Offset(fp.x * w, fp.y * h)
                val boxSize = 56.dp.toPx()
                val focusColor = if (isLocked) AmberAccent else Color.White
                drawRect(
                    color = focusColor,
                    topLeft = Offset(boxCenter.x - boxSize / 2, boxCenter.y - boxSize / 2),
                    size = Size(boxSize, boxSize),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // 7. Countdown Overlay when aligned
        countdown?.let { count ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$count",
                    color = GreenAligned,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCompositionGrid(
    rule: CompositionRule,
    w: Float,
    h: Float
) {
    val gridColor = Color.White.copy(alpha = 0.35f)
    val stroke = Stroke(width = 1.dp.toPx())

    when (rule) {
        CompositionRule.RULE_OF_THIRDS, CompositionRule.DYNAMIC_AI -> {
            drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = stroke.width)
            drawLine(gridColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), strokeWidth = stroke.width)
            drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = stroke.width)
            drawLine(gridColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), strokeWidth = stroke.width)
        }
        CompositionRule.GOLDEN_RATIO -> {
            val phiInv = 0.381966f
            val phi = 0.618034f
            drawLine(gridColor, Offset(w * phiInv, 0f), Offset(w * phiInv, h), strokeWidth = stroke.width)
            drawLine(gridColor, Offset(w * phi, 0f), Offset(w * phi, h), strokeWidth = stroke.width)
            drawLine(gridColor, Offset(0f, h * phiInv), Offset(w, h * phiInv), strokeWidth = stroke.width)
            drawLine(gridColor, Offset(0f, h * phi), Offset(w, h * phi), strokeWidth = stroke.width)
        }
        CompositionRule.GOLDEN_SPIRAL -> {
            val path = Path()
            path.moveTo(0f, h)
            path.cubicTo(w * 0.618f, h, w, h * 0.618f, w, 0f)
            drawPath(path, gridColor, style = stroke)
        }
        CompositionRule.CENTER_SYMMETRY -> {
            drawLine(gridColor, Offset(w / 2f, 0f), Offset(w / 2f, h), strokeWidth = stroke.width)
            drawLine(gridColor, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = stroke.width)
        }
    }
}

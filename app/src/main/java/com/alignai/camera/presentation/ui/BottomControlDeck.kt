package com.alignai.camera.presentation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alignai.camera.R
import com.alignai.camera.data.models.CameraCaptureMode
import com.alignai.camera.presentation.theme.AmberAccent
import com.alignai.camera.presentation.theme.GlassSurface
import com.alignai.camera.presentation.viewmodel.CameraViewModel
import kotlin.math.roundToInt

@Composable
fun BottomControlDeck(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val captureMode by viewModel.captureMode.collectAsState()
    val zoomRatio by viewModel.cameraService.zoomRatio.collectAsState()
    val isRecording by viewModel.cameraService.isRecording.collectAsState()
    val latestPhoto by viewModel.latestCapturedPhoto.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                )
            )
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Zoom Selector Pills (0.5x, 1x, 2x, 5x)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            val zoomOptions = listOf(0.5f, 1.0f, 2.0f, 5.0f)
            zoomOptions.forEach { z ->
                val isSelected = kotlin.math.abs(zoomRatio - z) < 0.2f
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isSelected) AmberAccent else GlassSurface,
                            CircleShape
                        )
                        .clickable { viewModel.setZoom(z) },
                    contentAlignment = Alignment.Center
                ) {
                    val label = if (z == 0.5f) ".5" else "${z.toInt()}"
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // 2. Segmented Mode Switcher (Ảnh / Video / Pro)
        Row(
            modifier = Modifier
                .background(GlassSurface, RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CameraCaptureMode.values().forEach { mode ->
                val isSelected = captureMode == mode
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) AmberAccent else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.setCaptureMode(mode) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.title,
                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Main Bottom Shutter Row: [Gallery] — [Shutter with Swipe-to-AI] — [Film Presets]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Gallery Thumbnail Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clickable { /* Open gallery */ },
                contentAlignment = Alignment.Center
            ) {
                latestPhoto?.bitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Recent Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Center: Central Shutter Button (with Swipe-to-AI dock in photo mode)
            if (captureMode.isVideo) {
                // Video Record Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clickable { viewModel.toggleVideoRecording() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isRecording) 32.dp else 56.dp)
                            .background(
                                Color.Red,
                                if (isRecording) RoundedCornerShape(8.dp) else CircleShape
                            )
                    )
                }
            } else {
                // Photo Mode: Central Shutter with Swipe-to-AI Dock
                var dragOffsetX by remember { mutableStateOf(0f) }
                val animatedDragX by animateFloatAsState(targetValue = dragOffsetX, label = "ShutterDrag")

                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Left AI Dock Target
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(44.dp)
                            .background(GlassSurface, CircleShape)
                            .border(1.dp, AmberAccent.copy(alpha = 0.6f), CircleShape)
                            .clickable { viewModel.triggerAISession() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Framing",
                            tint = AmberAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Shutter Button with Drag-to-left gesture
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(animatedDragX.roundToInt(), 0) }
                            .size(72.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(5.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        if (dragOffsetX < -35f) {
                                            viewModel.triggerAISession()
                                        }
                                        dragOffsetX = 0f
                                    },
                                    onDragCancel = { dragOffsetX = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val newX = (dragOffsetX + dragAmount.x).coerceIn(-48f, 0f)
                                        dragOffsetX = newX
                                    }
                                )
                            }
                            .clickable { viewModel.takePhoto() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }

            // Right: Film Presets Toggle Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(GlassSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable { viewModel.toggleFilmDrawer() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = "Film Presets",
                    tint = AmberAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

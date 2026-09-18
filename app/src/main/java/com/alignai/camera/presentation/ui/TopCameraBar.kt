package com.alignai.camera.presentation.ui

import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alignai.camera.data.models.CameraCaptureMode
import com.alignai.camera.presentation.theme.AmberAccent
import com.alignai.camera.presentation.theme.GlassSurface
import com.alignai.camera.presentation.viewmodel.CameraViewModel

@Composable
fun TopCameraBar(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val captureMode by viewModel.captureMode.collectAsState()
    val flashMode by viewModel.flashMode.collectAsState()
    val activeRule by viewModel.activeCompositionRule.collectAsState()
    val aiState by viewModel.aiSessionState.collectAsState()
    val isRecording by viewModel.cameraService.isRecording.collectAsState()
    val durationSec by viewModel.videoDurationSeconds.collectAsState()
    val videoFormat by viewModel.videoFormat.collectAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Flash Button
        IconButton(
            onClick = { viewModel.toggleFlash() },
            modifier = Modifier
                .size(40.dp)
                .background(GlassSurface, CircleShape)
        ) {
            val flashIcon = when (flashMode) {
                ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                else -> Icons.Default.FlashOff
            }
            val flashTint = if (flashMode == ImageCapture.FLASH_MODE_OFF) Color.White.copy(alpha = 0.8f) else AmberAccent
            Icon(
                imageVector = flashIcon,
                contentDescription = "Flash",
                tint = flashTint,
                modifier = Modifier.size(20.dp)
            )
        }

        // Center: Composition Rule Picker / Video Timer
        if (captureMode.isVideo) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Recording Timer Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(GlassSurface, RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (isRecording) Color.Red else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val minutes = durationSec / 60
                    val seconds = durationSec % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Video Resolution / FPS Toggle Button
                Box(
                    modifier = Modifier
                        .background(GlassSurface, RoundedCornerShape(16.dp))
                        .clickable(!isRecording) { viewModel.toggleVideoFormat() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = videoFormat.title,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            // Photo Mode: Smart Rule Picker Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(GlassSurface, RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        if (aiState.isSessionActive) Color(0xFF34C759).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { viewModel.setCompositionRuleSheetPresented(true) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Grid3x3,
                    contentDescription = null,
                    tint = if (aiState.isSessionActive) Color(0xFF34C759) else AmberAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = activeRule.displayNameVietnamese,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Right: Settings Button
        IconButton(
            onClick = { viewModel.setSettingsPresented(true) },
            modifier = Modifier
                .size(40.dp)
                .background(GlassSurface, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

package com.alignai.camera.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alignai.camera.data.models.AISessionState
import com.alignai.camera.data.models.ActiveAIIndicatorType
import com.alignai.camera.presentation.theme.AmberAccent
import com.alignai.camera.presentation.theme.GlassSurface
import com.alignai.camera.presentation.viewmodel.CameraViewModel

@Composable
fun AIStatusHUD(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val aiState by viewModel.aiSessionState.collectAsState()
    val indicatorType by viewModel.activeAIIndicator.collectAsState()

    AnimatedVisibility(
        visible = aiState !is AISessionState.Idle,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(GlassSurface, RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // State Icon
            val icon = when (aiState) {
                is AISessionState.Idle -> Icons.Default.Camera
                is AISessionState.Analyzing -> Icons.Default.AutoAwesome
                is AISessionState.TargetPlaced -> Icons.Default.Adjust
                is AISessionState.AlignmentPerfect -> Icons.Default.CheckCircle
                is AISessionState.Capturing -> Icons.Default.CameraAlt
                is AISessionState.Done -> Icons.Default.Done
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = aiState.accentColor,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // State Message
            Text(
                text = aiState.displayMessage,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            // Local/Cloud Badge
            if (indicatorType != ActiveAIIndicatorType.NONE) {
                Spacer(modifier = Modifier.width(8.dp))
                val badgeColor = if (indicatorType == ActiveAIIndicatorType.CLOUD) AmberAccent else Color.Red
                val badgeText = if (indicatorType == ActiveAIIndicatorType.CLOUD) "CLOUD" else "LOCAL"

                Box(
                    modifier = Modifier
                        .background(badgeColor, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = if (indicatorType == ActiveAIIndicatorType.CLOUD) Color.Black else Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

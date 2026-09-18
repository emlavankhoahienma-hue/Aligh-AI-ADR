package com.alignai.camera.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.alignai.camera.data.models.FilmPreset
import com.alignai.camera.presentation.theme.AmberAccent
import com.alignai.camera.presentation.theme.GlassSurface
import com.alignai.camera.presentation.viewmodel.CameraViewModel

@Composable
fun FilmPresetDrawer(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val isShowing by viewModel.isShowingFilmDrawer.collectAsState()
    val activePreset by viewModel.activeFilmPreset.collectAsState()

    AnimatedVisibility(
        visible = isShowing,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassSurface, RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Text(
                text = "18 BỘ MÀU FILM CLASSIC (LEICA / HASSELBLAD)",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilmPreset.values().forEach { preset ->
                    val isSelected = activePreset == preset
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(
                                if (isSelected) AmberAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) AmberAccent else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.setFilmPreset(preset) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset.shortTitle,
                            color = if (isSelected) AmberAccent else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preset.displayName,
                            color = if (isSelected) AmberAccent.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

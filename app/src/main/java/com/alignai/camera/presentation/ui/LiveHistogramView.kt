package com.alignai.camera.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alignai.camera.presentation.theme.GlassSurface
import com.alignai.camera.presentation.viewmodel.CameraViewModel

@Composable
fun LiveHistogramView(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val bars by viewModel.histogramBars.collectAsState()

    Box(
        modifier = modifier
            .width(130.dp)
            .height(50.dp)
            .background(GlassSurface, RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (bars.isEmpty()) {
                // Default subtle curve if buffer hasn't calculated yet
                val count = 24
                val barWidth = size.width / count
                for (i in 0 until count) {
                    val h = kotlin.math.sin((i.toFloat() / count) * Math.PI).toFloat() * size.height * 0.7f
                    drawRect(
                        color = Color.White.copy(alpha = 0.4f),
                        topLeft = Offset(i * barWidth, size.height - h),
                        size = Size(barWidth - 1f, h)
                    )
                }
            } else {
                val barWidth = size.width / bars.size
                bars.forEachIndexed { index, bar ->
                    val h = bar.height * size.height
                    drawRect(
                        color = bar.color.copy(alpha = 0.85f),
                        topLeft = Offset(index * barWidth, size.height - h),
                        size = Size(barWidth - 1f, h)
                    )
                }
            }
        }
    }
}

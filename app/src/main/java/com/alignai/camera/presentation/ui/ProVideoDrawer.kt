package com.alignai.camera.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alignai.camera.data.models.ProVideoParameterTab
import com.alignai.camera.presentation.theme.AmberAccent
import com.alignai.camera.presentation.theme.GlassSurface
import com.alignai.camera.presentation.viewmodel.CameraViewModel

@Composable
fun ProVideoDrawer(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val proControls = viewModel.proControls
    val selectedTab by viewModel.selectedProTab.collectAsState()
    val isExpanded by viewModel.isProDrawerExpanded.collectAsState()

    val isAutoISO by proControls.isAutoISO.collectAsState()
    val currentISO by proControls.currentISO.collectAsState()

    val isAutoShutter by proControls.isAutoShutter.collectAsState()
    val currentShutter by proControls.currentShutterSpeed.collectAsState()

    val isAutoEV by proControls.isAutoEV.collectAsState()
    val currentEV by proControls.currentEVBias.collectAsState()

    val isAutoWB by proControls.isAutoWB.collectAsState()
    val currentKelvin by proControls.currentKelvin.collectAsState()

    val isAutoFocus by proControls.isAutoFocus.collectAsState()
    val currentFocus by proControls.currentLensPosition.collectAsState()

    val isPeaking by proControls.isFocusPeakingEnabled.collectAsState()
    val peakingColor by proControls.focusPeakingColor.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        // Floating Top Tabs Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassSurface, RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ISO Tab
                ProTabItem(
                    tab = ProVideoParameterTab.ISO,
                    title = "ISO",
                    valueString = if (isAutoISO) "AUTO" else "${currentISO.toInt()}",
                    isAuto = isAutoISO,
                    isSelected = selectedTab == ProVideoParameterTab.ISO,
                    onClick = { viewModel.selectProTab(ProVideoParameterTab.ISO) }
                )

                // Shutter Tab
                ProTabItem(
                    tab = ProVideoParameterTab.SHUTTER,
                    title = "SEC",
                    valueString = if (isAutoShutter) "AUTO" else "1/${currentShutter}s",
                    isAuto = isAutoShutter,
                    isSelected = selectedTab == ProVideoParameterTab.SHUTTER,
                    onClick = { viewModel.selectProTab(ProVideoParameterTab.SHUTTER) }
                )

                // EV Tab
                ProTabItem(
                    tab = ProVideoParameterTab.APERTURE_EV,
                    title = "EV",
                    valueString = if (isAutoEV) "0.0 EV" else String.format("%+.1f EV", currentEV),
                    isAuto = isAutoEV,
                    isSelected = selectedTab == ProVideoParameterTab.APERTURE_EV,
                    onClick = { viewModel.selectProTab(ProVideoParameterTab.APERTURE_EV) }
                )

                // WB Tab
                ProTabItem(
                    tab = ProVideoParameterTab.WB,
                    title = "WB",
                    valueString = if (isAutoWB) "AWB" else "${currentKelvin}K",
                    isAuto = isAutoWB,
                    isSelected = selectedTab == ProVideoParameterTab.WB,
                    onClick = { viewModel.selectProTab(ProVideoParameterTab.WB) }
                )

                // Focus Tab
                ProTabItem(
                    tab = ProVideoParameterTab.FOCUS,
                    title = "FOCUS",
                    valueString = if (isAutoFocus) "AF" else "MF ${(currentFocus * 100).toInt()}",
                    isAuto = isAutoFocus,
                    isSelected = selectedTab == ProVideoParameterTab.FOCUS,
                    onClick = { viewModel.selectProTab(ProVideoParameterTab.FOCUS) }
                )

                // Peaking Quick Toggle
                Row(
                    modifier = Modifier
                        .background(
                            if (isPeaking) peakingColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { proControls.toggleFocusPeaking() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isPeaking) peakingColor else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PEAK",
                        color = if (isPeaking) peakingColor else Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Expand / Collapse Chevron Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable { viewModel.toggleProDrawer() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = "Expand",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Expandable Parameter Control Panel
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(Color.Black.copy(alpha = 0.90f), RoundedCornerShape(16.dp))
                    .border(1.dp, AmberAccent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                when (selectedTab) {
                    ProVideoParameterTab.ISO -> {
                        IsoControlPanel(
                            isAuto = isAutoISO,
                            current = currentISO,
                            onToggleAuto = {
                                proControls.setAutoISO(it)
                                viewModel.cameraService.setManualISO(if (it) null else currentISO.toInt())
                            },
                            onSelectPreset = {
                                proControls.setManualISO(it)
                                viewModel.cameraService.setManualISO(it.toInt())
                            }
                        )
                    }
                    ProVideoParameterTab.SHUTTER -> {
                        ShutterControlPanel(
                            isAuto = isAutoShutter,
                            current = currentShutter,
                            onToggleAuto = {
                                proControls.setAutoShutter(it)
                                viewModel.cameraService.setManualShutterSpeed(if (it) null else currentShutter)
                            },
                            onSelectPreset = {
                                proControls.setManualShutter(it)
                                viewModel.cameraService.setManualShutterSpeed(it)
                            }
                        )
                    }
                    ProVideoParameterTab.APERTURE_EV -> {
                        EvControlPanel(
                            isAuto = isAutoEV,
                            current = currentEV,
                            onToggleAuto = {
                                proControls.setAutoEV(it)
                                viewModel.cameraService.setExposureCompensation(0f)
                            },
                            onValueChange = {
                                proControls.setManualEV(it)
                                viewModel.cameraService.setExposureCompensation(it)
                            }
                        )
                    }
                    ProVideoParameterTab.WB -> {
                        WbControlPanel(
                            isAuto = isAutoWB,
                            currentKelvin = currentKelvin,
                            onToggleAuto = { proControls.setAutoWB(it) },
                            onValueChange = { proControls.setManualKelvin(it) }
                        )
                    }
                    ProVideoParameterTab.FOCUS -> {
                        FocusControlPanel(
                            isAuto = isAutoFocus,
                            current = currentFocus,
                            onToggleAuto = {
                                proControls.setAutoFocus(it)
                                viewModel.cameraService.setManualFocusDistance(if (it) null else currentFocus)
                            },
                            onValueChange = {
                                proControls.setManualFocusPosition(it)
                                viewModel.cameraService.setManualFocusDistance(it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProTabItem(
    tab: ProVideoParameterTab,
    title: String,
    valueString: String,
    isAuto: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                if (isSelected) AmberAccent.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isSelected) AmberAccent else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = if (isSelected) AmberAccent else Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (isAuto) {
                Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .background(Color.Cyan.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "A",
                        color = Color.Cyan,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        Text(
            text = valueString,
            color = if (isSelected) AmberAccent else Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// 1. ISO Control Panel
@Composable
private fun IsoControlPanel(
    isAuto: Boolean,
    current: Float,
    onToggleAuto: (Boolean) -> Unit,
    onSelectPreset: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ĐỘ NHẠY SÁNG ISO", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            AutoPillButton(isAuto = isAuto, onToggle = onToggleAuto)
        }
        Spacer(modifier = Modifier.height(10.dp))
        val presets = listOf(50f, 100f, 200f, 400f, 800f, 1600f, 3200f)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { preset ->
                val isCur = !isAuto && kotlin.math.abs(current - preset) < 10f
                PresetChip(
                    text = "${preset.toInt()}",
                    isSelected = isCur,
                    onClick = { onSelectPreset(preset) }
                )
            }
        }
    }
}

// 2. Shutter Speed Panel
@Composable
private fun ShutterControlPanel(
    isAuto: Boolean,
    current: Long,
    onToggleAuto: (Boolean) -> Unit,
    onSelectPreset: (Long) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TỐC ĐỘ MÀN TRẬP", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            AutoPillButton(isAuto = isAuto, onToggle = onToggleAuto)
        }
        Spacer(modifier = Modifier.height(10.dp))
        val presets = listOf(30L, 60L, 120L, 250L, 500L, 1000L)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { preset ->
                val isCur = !isAuto && current == preset
                PresetChip(
                    text = "1/${preset}s",
                    isSelected = isCur,
                    onClick = { onSelectPreset(preset) }
                )
            }
        }
    }
}

// 3. EV Panel
@Composable
private fun EvControlPanel(
    isAuto: Boolean,
    current: Float,
    onToggleAuto: (Boolean) -> Unit,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("BÙ SÁNG PHƠI SÁNG (EV)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            AutoPillButton(isAuto = isAuto, onToggle = onToggleAuto)
        }
        Slider(
            value = current,
            onValueChange = onValueChange,
            valueRange = -2.0f..2.0f,
            colors = SliderDefaults.colors(
                thumbColor = AmberAccent,
                activeTrackColor = AmberAccent
            )
        )
    }
}

// 4. White Balance Panel
@Composable
private fun WbControlPanel(
    isAuto: Boolean,
    currentKelvin: Int,
    onToggleAuto: (Boolean) -> Unit,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CÂN BẰNG TRẮNG (WB)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            AutoPillButton(isAuto = isAuto, onToggle = onToggleAuto)
        }
        Slider(
            value = currentKelvin.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 2700f..8000f,
            colors = SliderDefaults.colors(
                thumbColor = AmberAccent,
                activeTrackColor = AmberAccent
            )
        )
    }
}

// 5. Focus Panel
@Composable
private fun FocusControlPanel(
    isAuto: Boolean,
    current: Float,
    onToggleAuto: (Boolean) -> Unit,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LẤY NÉT THỦ CÔNG (LENS FOCUS)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            AutoPillButton(isAuto = isAuto, onToggle = onToggleAuto)
        }
        Slider(
            value = current,
            onValueChange = onValueChange,
            valueRange = 0.0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = AmberAccent,
                activeTrackColor = AmberAccent
            )
        )
    }
}

@Composable
private fun AutoPillButton(
    isAuto: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isAuto) Color(0xFF34C759).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .clickable { onToggle(!isAuto) }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (isAuto) "AUTO: BẬT" else "AUTO: TẮT",
            color = if (isAuto) Color(0xFF34C759) else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PresetChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) AmberAccent else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

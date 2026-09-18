package com.alignai.camera.presentation.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.alignai.camera.data.models.CameraCaptureMode
import com.alignai.camera.presentation.viewmodel.CameraViewModel

@Composable
fun CameraMainScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val captureMode by viewModel.captureMode.collectAsState()
    val isShowingSettings by viewModel.isShowingSettings.collectAsState()
    val isShowingRuleSheet by viewModel.isCompositionRuleSheetPresented.collectAsState()

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    // Re-bind camera use cases when capture mode changes (Photo 4:3 vs Video 16:9)
    LaunchedEffect(captureMode) {
        viewModel.cameraService.bindCameraUseCases(lifecycleOwner, previewView, captureMode)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── LAYER 0: CỐ ĐỊNH Ở ĐÁY HIERARCHY — KÍNH NGẮM LIVE VIEW (PREVIEWVIEW) ──
        // Duy trì tuyệt đối tỷ lệ 4:3 (Photo) hoặc 16:9 (Video), không bị méo, không bị dịch chuyển
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val aspect = if (captureMode.isVideo) 9f / 16f else 3f / 4f
            Box(
                modifier = Modifier
                    .aspectRatio(aspect)
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                // CameraX Preview
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // AR Overlay trùng tọa độ 1:1 với Preview
                ARFramingOverlay(
                    viewModel = viewModel,
                    previewView = previewView,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ── LAYER 1: FLOATING TOP BAR & DYNAMIC AI HUD PILL ──
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopCameraBar(viewModel = viewModel)
            Spacer(modifier = Modifier.height(2.dp))
            AIStatusHUD(viewModel = viewModel)
        }

        // ── LAYER 2: FLOATING REALTIME COLOR HISTOGRAM HUD (PRO MODE) ──
        if (captureMode == CameraCaptureMode.PRO_VIDEO) {
            LiveHistogramView(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 220.dp)
            )
        }

        // ── LAYER 3: FLOATING PRO DRAWER (FLOATING OVERLAY TRÊN MẶT KÍNH NGẮM) ──
        // Thao tác đóng/mở Pro Drawer tuyệt đối không làm co nhỏ hay bóp méo Live View
        if (captureMode == CameraCaptureMode.PRO_VIDEO) {
            ProVideoDrawer(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 150.dp)
            )
        }

        // ── LAYER 4: FLOATING FILM PRESET DRAWER ──
        FilmPresetDrawer(
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 160.dp)
        )

        // ── LAYER 5: BOTTOM CONTROL DECK (ZOOM PILLS, SHUTTER, MODE SWITCHER) ──
        BottomControlDeck(
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )

        // ── DIALOGS / SHEETS ──
        if (isShowingSettings || isShowingRuleSheet) {
            SettingsSheet(
                viewModel = viewModel,
                onDismiss = {
                    viewModel.setSettingsPresented(false)
                    viewModel.setCompositionRuleSheetPresented(false)
                }
            )
        }
    }
}

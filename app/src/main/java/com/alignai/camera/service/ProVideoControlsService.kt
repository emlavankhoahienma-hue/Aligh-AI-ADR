package com.alignai.camera.service

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProVideoControlsService {
    companion object {
        val shared = ProVideoControlsService()
    }

    // ISO State
    private val _isAutoISO = MutableStateFlow(true)
    val isAutoISO: StateFlow<Boolean> = _isAutoISO.asStateFlow()

    private val _currentISO = MutableStateFlow(100f)
    val currentISO: StateFlow<Float> = _currentISO.asStateFlow()

    // Shutter State
    private val _isAutoShutter = MutableStateFlow(true)
    val isAutoShutter: StateFlow<Boolean> = _isAutoShutter.asStateFlow()

    private val _currentShutterSpeed = MutableStateFlow(60L) // 1/60s
    val currentShutterSpeed: StateFlow<Long> = _currentShutterSpeed.asStateFlow()

    // EV Bias State
    private val _isAutoEV = MutableStateFlow(true)
    val isAutoEV: StateFlow<Boolean> = _isAutoEV.asStateFlow()

    private val _currentEVBias = MutableStateFlow(0.0f)
    val currentEVBias: StateFlow<Float> = _currentEVBias.asStateFlow()

    // White Balance State
    private val _isAutoWB = MutableStateFlow(true)
    val isAutoWB: StateFlow<Boolean> = _isAutoWB.asStateFlow()

    private val _currentKelvin = MutableStateFlow(5500)
    val currentKelvin: StateFlow<Int> = _currentKelvin.asStateFlow()

    // Focus State
    private val _isAutoFocus = MutableStateFlow(true)
    val isAutoFocus: StateFlow<Boolean> = _isAutoFocus.asStateFlow()

    private val _currentLensPosition = MutableStateFlow(0.5f) // 0.0 (inf) to 1.0 (macro)
    val currentLensPosition: StateFlow<Float> = _currentLensPosition.asStateFlow()

    // Focus Peaking State
    private val _isFocusPeakingEnabled = MutableStateFlow(false)
    val isFocusPeakingEnabled: StateFlow<Boolean> = _isFocusPeakingEnabled.asStateFlow()

    private val _focusPeakingColor = MutableStateFlow(Color(0xFF34C759)) // Green
    val focusPeakingColor: StateFlow<Color> = _focusPeakingColor.asStateFlow()

    fun setAutoISO(auto: Boolean) {
        _isAutoISO.value = auto
    }

    fun setManualISO(iso: Float) {
        _isAutoISO.value = false
        _currentISO.value = iso
    }

    fun setAutoShutter(auto: Boolean) {
        _isAutoShutter.value = auto
    }

    fun setManualShutter(denominator: Long) {
        _isAutoShutter.value = false
        _currentShutterSpeed.value = denominator
    }

    fun setAutoEV(auto: Boolean) {
        _isAutoEV.value = auto
        if (auto) _currentEVBias.value = 0.0f
    }

    fun setManualEV(ev: Float) {
        _isAutoEV.value = false
        _currentEVBias.value = ev
    }

    fun setAutoWB(auto: Boolean) {
        _isAutoWB.value = auto
    }

    fun setManualKelvin(kelvin: Int) {
        _isAutoWB.value = false
        _currentKelvin.value = kelvin
    }

    fun setAutoFocus(auto: Boolean) {
        _isAutoFocus.value = auto
    }

    fun setManualFocusPosition(pos: Float) {
        _isAutoFocus.value = false
        _currentLensPosition.value = pos.coerceIn(0f, 1f)
    }

    fun toggleFocusPeaking() {
        _isFocusPeakingEnabled.value = !_isFocusPeakingEnabled.value
    }

    fun setFocusPeakingColor(color: Color) {
        _focusPeakingColor.value = color
    }
}

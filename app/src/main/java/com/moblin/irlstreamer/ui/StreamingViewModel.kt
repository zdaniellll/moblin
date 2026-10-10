package com.moblin.irlstreamer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moblin.irlstreamer.camera.CameraHelper
import com.moblin.irlstreamer.camera.DeviceCapabilityReport
import com.moblin.irlstreamer.model.StreamStats
import com.moblin.irlstreamer.model.StreamingConfig
import com.moblin.irlstreamer.network.NetworkStreamer
import com.moblin.irlstreamer.service.StreamingForegroundService
import com.pedro.library.view.OpenGlView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado general de la UI y los componentes de streaming.
 */
data class UiState(
    val config: StreamingConfig = StreamingConfig(),
    val stats: StreamStats = StreamStats(),
    val hardwareReport: DeviceCapabilityReport? = null,
    val isSettingsOpen: Boolean = false,
    val isTorchActive: Boolean = false,
    val isMicMuted: Boolean = false,
    val zoomLevel: Float = 1.0f,
    val fallbackMessage: String? = null
)

/**
 * StreamingViewModel: Orquesta la interacción entre Jetpack Compose, Camera2 API
 * y el motor de red SRT.
 */
class StreamingViewModel(application: Application) : AndroidViewModel(application) {

    private val cameraHelper = CameraHelper(application)
    private val networkStreamer = NetworkStreamer(application, viewModelScope)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadHardwareCapabilities()
        observeStats()
    }

    private fun loadHardwareCapabilities() {
        val report = cameraHelper.queryHardwareCapabilities()
        _uiState.update { it.copy(hardwareReport = report) }
    }

    private fun observeStats() {
        viewModelScope.launch {
            networkStreamer.stats.collect { liveStats ->
                _uiState.update { it.copy(stats = liveStats) }
            }
        }
    }

    /**
     * Vincula la superficie OpenGL de la UI con la cámara.
     */
    fun attachPreview(openGlView: OpenGlView) {
        networkStreamer.attachPreview(openGlView)
    }

    /**
     * Alterna la transmisión en vivo sobre SRT.
     */
    fun toggleStreaming() {
        val currentStats = _uiState.value.stats
        if (currentStats.isStreaming) {
            networkStreamer.stopStreaming()
            if (!currentStats.isRecording) {
                StreamingForegroundService.stop(getApplication())
            }
        } else {
            // Aplicar negociación de hardware y fallback graceful antes de lanzar el stream
            val negotiated = cameraHelper.negotiateGracefulFallback(_uiState.value.config)

            if (negotiated.wasFallbackTriggered) {
                _uiState.update {
                    it.copy(
                        config = negotiated.effectiveConfig,
                        fallbackMessage = negotiated.reason
                    )
                }
            }

            networkStreamer.startStreaming(negotiated.effectiveConfig)
            StreamingForegroundService.start(getApplication())
        }
    }

    /**
     * Alterna la grabación local de alta fidelidad (MP4).
     */
    fun toggleRecording() {
        val currentStats = _uiState.value.stats
        if (currentStats.isRecording) {
            networkStreamer.stopRecording()
            if (!currentStats.isStreaming) {
                StreamingForegroundService.stop(getApplication())
            }
        } else {
            networkStreamer.startRecording()
            StreamingForegroundService.start(getApplication())
        }
    }

    /**
     * Actualiza la configuración seleccionada por el usuario en la pantalla de ajustes.
     */
    fun updateConfig(newConfig: StreamingConfig) {
        val negotiated = cameraHelper.negotiateGracefulFallback(newConfig)
        _uiState.update {
            it.copy(
                config = negotiated.effectiveConfig,
                fallbackMessage = if (negotiated.wasFallbackTriggered) negotiated.reason else null
            )
        }
        // Si no estamos transmitiendo, preparamos de antemano los encoders con la nueva configuración
        if (!_uiState.value.stats.isStreaming && !_uiState.value.stats.isRecording) {
            networkStreamer.prepareEncoders(negotiated.effectiveConfig)
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun dismissFallbackMessage() {
        _uiState.update { it.copy(fallbackMessage = null) }
    }

    fun switchCamera() {
        networkStreamer.switchCamera()
        loadHardwareCapabilities()
    }

    fun toggleTorch() {
        val newTorchState = !_uiState.value.isTorchActive
        networkStreamer.toggleTorch(newTorchState)
        _uiState.update { it.copy(isTorchActive = newTorchState) }
    }

    fun toggleMute() {
        val newMuteState = !_uiState.value.isMicMuted
        networkStreamer.muteMicrophone(newMuteState)
        _uiState.update { it.copy(isMicMuted = newMuteState) }
    }

    fun setZoom(level: Float) {
        networkStreamer.setZoom(level)
        _uiState.update { it.copy(zoomLevel = level) }
    }

    override fun onCleared() {
        super.onCleared()
        networkStreamer.release()
    }
}

package com.moblin.irlstreamer

import android.Manifest
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moblin.irlstreamer.ui.MoblinHud
import com.moblin.irlstreamer.ui.SettingsSheet
import com.moblin.irlstreamer.ui.StreamingViewModel
import com.pedro.library.view.OpenGlView

/**
 * MainActivity: Punto de entrada de la aplicación de IRL Streaming.
 * - Modo Inmersivo de Pantalla Completa (Estilo Moblin).
 * - Vista de cámara OpenGL de 60 FPS acelerada por GPU.
 * - HUD semitransparente con telemetría en tiempo real y panel de configuración de red SRT.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: StreamingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configurar orientación horizontal y mantener pantalla encendida
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2. Ocultar barras del sistema para experiencia inmersiva
        hideSystemUi()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    IrlStreamingScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

@Composable
fun IrlStreamingScreen(viewModel: StreamingViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var hasPermissions by remember { mutableStateOf(false) }

    // Gestor de permisos en tiempo de ejecución
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermissions = result.values.all { it }
    }

    LaunchedEffect(Unit) {
        val required = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(required.toTypedArray())
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ==========================================
        // CAPA 1: Vista OpenGL de la Cámara (Full Screen)
        // ==========================================
        if (hasPermissions) {
            AndroidView(
                factory = { context ->
                    OpenGlView(context).apply {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                viewModel.attachPreview(this@apply)
                            }
                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                            override fun surfaceDestroyed(holder: SurfaceHolder) {}
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }

        // ==========================================
        // CAPA 2: HUD Overlay estilo Moblin
        // ==========================================
        MoblinHud(
            stats = uiState.stats,
            isTorchActive = uiState.isTorchActive,
            isMicMuted = uiState.isMicMuted,
            zoomLevel = uiState.zoomLevel,
            fallbackMessage = uiState.fallbackMessage,
            onToggleLive = { viewModel.toggleStreaming() },
            onToggleRecord = { viewModel.toggleRecording() },
            onSwitchCamera = { viewModel.switchCamera() },
            onToggleTorch = { viewModel.toggleTorch() },
            onToggleMute = { viewModel.toggleMute() },
            onSetZoom = { viewModel.setZoom(it) },
            onOpenSettings = { viewModel.openSettings() },
            onDismissFallback = { viewModel.dismissFallbackMessage() }
        )

        // ==========================================
        // CAPA 3: Pantalla de Ajustes (Modal / Overlay)
        // ==========================================
        AnimatedVisibility(
            visible = uiState.isSettingsOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SettingsSheet(
                initialConfig = uiState.config,
                hardwareReport = uiState.hardwareReport,
                onSaveConfig = { updated -> viewModel.updateConfig(updated) },
                onDismiss = { viewModel.closeSettings() }
            )
        }
    }
}

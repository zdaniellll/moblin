package com.moblin.irlstreamer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.irlstreamer.model.ConnectionState
import com.moblin.irlstreamer.model.StreamStats
import java.util.Locale

/**
 * HUD estilo Moblin para transmisión IRL:
 * - Indicadores semitransparentes oscuros tipo 'Pill'
 * - Monitoreo en tiempo real: Bitrate dinámico, Ping/RTT (ms), FPS, Códec y VU meter
 * - Barra de acciones rápidas para streamer (LIVE, REC, Switch, Flash, Mute, Zoom)
 */
@Composable
fun MoblinHud(
    stats: StreamStats,
    isTorchActive: Boolean,
    isMicMuted: Boolean,
    zoomLevel: Float,
    fallbackMessage: String?,
    onToggleLive: () -> Unit,
    onToggleRecord: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleMute: () -> Unit,
    onSetZoom: (Float) -> Unit,
    onOpenSettings: () -> Unit,
    onDismissFallback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {

        // ==========================================
        // 1. Barra Superior: Métricas y Telemetría
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Notificación de Fallback Graceful de Hardware (si aplica)
            AnimatedVisibility(
                visible = fallbackMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                fallbackMessage?.let { msg ->
                    FallbackAlertBanner(
                        message = msg,
                        onDismiss = onDismissFallback
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicadores de Estado: LIVE y REC
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiveBadge(
                        isStreaming = stats.isStreaming,
                        connectionState = stats.connectionState,
                        durationSeconds = stats.streamDurationSeconds
                    )

                    RecBadge(
                        isRecording = stats.isRecording,
                        durationSeconds = stats.recordDurationSeconds
                    )
                }

                // Píldoras de Telemetría (Bitrate, RTT, FPS, Codec)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Píldora de Bitrate (Color adaptativo según ancho de banda)
                    BitratePill(bitrateKbps = stats.currentBitrateKbps)

                    // Píldora de Ping / Latencia RTT de SRT
                    RttPill(rttMs = stats.networkLatencyMs)

                    // Píldora de FPS
                    FpsPill(fps = stats.currentFps)

                    // Píldora de Resolución / Códec
                    ResolutionCodecPill(
                        resolutionLabel = stats.activeResolution.label.split(" ")[0],
                        codecLabel = if (stats.activeCodec.name.contains("H265")) "HEVC" else "AVC"
                    )

                    // Barra VU de audio del micrófono
                    AudioVuMeter(level = stats.micVolumeLevel, isMuted = isMicMuted)
                }
            }
        }

        // ==========================================
        // 2. Barra Inferior: Controles de Acción Rápida
        // ==========================================
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grupo Izquierdo: Zoom presets
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ZoomChip(label = "0.5x", isSelected = zoomLevel == 0.5f) { onSetZoom(0.5f) }
                ZoomChip(label = "1.0x", isSelected = zoomLevel == 1.0f) { onSetZoom(1.0f) }
                ZoomChip(label = "2.0x", isSelected = zoomLevel == 2.0f) { onSetZoom(2.0f) }
            }

            // Grupo Central: Botones principales LIVE y REC
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Master REC (Grabación local independiente en MP4)
                RecordActionButton(
                    isRecording = stats.isRecording,
                    onClick = onToggleRecord
                )

                // Botón Master LIVE (Transmisión SRT hacia OBS Studio)
                LiveActionButton(
                    isStreaming = stats.isStreaming,
                    onClick = onToggleLive
                )
            }

            // Grupo Derecho: Atajos de Hardware y Ajustes
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickIconButton(
                    icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    isActive = isMicMuted,
                    activeColor = Color(0xFFEF4444),
                    onClick = onToggleMute
                )

                QuickIconButton(
                    icon = Icons.Default.FlipCameraAndroid,
                    onClick = onSwitchCamera
                )

                QuickIconButton(
                    icon = Icons.Default.FlashOn,
                    isActive = isTorchActive,
                    activeColor = Color(0xFFFBBF24),
                    onClick = onToggleTorch
                )

                QuickIconButton(
                    icon = Icons.Default.Settings,
                    onClick = onOpenSettings
                )
            }
        }
    }
}

// ==========================================
// Componentes Atómicos del HUD
// ==========================================

@Composable
fun LiveBadge(
    isStreaming: Boolean,
    connectionState: ConnectionState,
    durationSeconds: Long
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )

    val (bgColor, text, showPulse) = when {
        !isStreaming -> Triple(Color(0x991F2937), "OFFLINE", false)
        connectionState is ConnectionState.Connecting -> Triple(Color(0xCCF59E0B), "CONNECTING", true)
        connectionState is ConnectionState.Reconnecting -> Triple(Color(0xCCEF4444), "RETRY ${connectionState.attempt}", true)
        else -> Triple(Color(0xCCE11D48), formatDuration(durationSeconds), true)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showPulse) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha))
            )
        }
        Text(
            text = "LIVE $text",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun RecBadge(isRecording: Boolean, durationSeconds: Long) {
    if (!isRecording) return

    val infiniteTransition = rememberInfiniteTransition(label = "RecPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RecAlpha"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xCCDC2626))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = alpha))
        )
        Text(
            text = "REC ${formatDuration(durationSeconds)}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun BitratePill(bitrateKbps: Long) {
    val color = when {
        bitrateKbps >= 6000 -> Color(0xFF10B981) // Verde: Óptimo
        bitrateKbps in 3000..5999 -> Color(0xFFFBBF24) // Amarillo: Moderado
        bitrateKbps in 1..2999 -> Color(0xFFEF4444) // Rojo: Inestable
        else -> Color(0xFF9CA3AF) // Gris: Inactivo
    }

    val display = if (bitrateKbps >= 1000) {
        String.format(Locale.US, "%.1f Mb/s", bitrateKbps / 1000.0)
    } else {
        "$bitrateKbps kb/s"
    }

    PillContainer {
        Text(
            text = display,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun RttPill(rttMs: Long) {
    val color = when {
        rttMs in 1..60 -> Color(0xFF10B981)
        rttMs in 61..150 -> Color(0xFFFBBF24)
        rttMs > 150 -> Color(0xFFEF4444)
        else -> Color(0xFF9CA3AF)
    }

    PillContainer {
        Text(
            text = if (rttMs > 0) "$rttMs ms" else "--- ms",
            color = color,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun FpsPill(fps: Double) {
    PillContainer {
        Text(
            text = String.format(Locale.US, "%.1f FPS", fps),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ResolutionCodecPill(resolutionLabel: String, codecLabel: String) {
    PillContainer {
        Text(
            text = "$resolutionLabel · $codecLabel",
            color = Color(0xFF93C5FD),
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
    }
}

@Composable
fun AudioVuMeter(level: Float, isMuted: Boolean) {
    val height = 16.dp
    val barColor = if (isMuted) Color(0xFFEF4444) else Color(0xFF10B981)

    PillContainer {
        Row(
            modifier = Modifier.height(height),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = null,
                tint = if (isMuted) Color(0xFFEF4444) else Color.White,
                modifier = Modifier.size(12.dp)
            )
            // 4 Segmentos de barra LED simulados según volumen
            for (i in 1..4) {
                val threshold = i * 0.25f
                val active = !isMuted && level >= threshold
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height((6 + (i * 2.5f)).dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (active) barColor else Color(0x44FFFFFF))
                )
            }
        }
    }
}

@Composable
fun PillContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x99111827))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun LiveActionButton(isStreaming: Boolean, onClick: () -> Unit) {
    val animatedBg by animateColorAsState(
        targetValue = if (isStreaming) Color(0xFFDC2626) else Color(0xFF2563EB),
        label = "LiveBtnBg"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(animatedBg)
            .border(2.dp, Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Radio,
                contentDescription = "LIVE",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = if (isStreaming) "END" else "LIVE",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun RecordActionButton(isRecording: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(if (isRecording) Color(0xFFEF4444) else Color(0x881F2937))
            .border(2.dp, if (isRecording) Color.White else Color(0xFF9CA3AF), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.FiberManualRecord,
            contentDescription = "REC",
            tint = if (isRecording) Color.White else Color(0xFFEF4444),
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun QuickIconButton(
    icon: ImageVector,
    isActive: Boolean = false,
    activeColor: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isActive) activeColor.copy(alpha = 0.25f) else Color(0x99111827))
            .border(1.dp, if (isActive) activeColor else Color(0x33FFFFFF), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) activeColor else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ZoomChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xDD3B82F6) else Color(0x99111827))
            .border(1.dp, if (isSelected) Color(0xFF60A5FA) else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun FallbackAlertBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xE67C2D12))
            .border(1.dp, Color(0xFFF97316), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFDBA74),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                color = Color(0xFFFFEDD5),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

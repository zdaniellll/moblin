package com.moblin.irlstreamer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.irlstreamer.camera.DeviceCapabilityReport
import com.moblin.irlstreamer.model.StreamFps
import com.moblin.irlstreamer.model.StreamResolution
import com.moblin.irlstreamer.model.StreamingConfig
import com.moblin.irlstreamer.model.VideoCodecType

/**
 * Pantalla de Configuración Completa para IRL Streaming (Estilo Moblin):
 * - Parámetros de Red SRT (Modo Caller para NAT Traversal)
 * - Selectores de Hardware de Video (4K/1080p/720p, 60/30/24 FPS, 4000 a 35000 kbps)
 * - Inspección de capacidades detectadas por Camera2 / MediaCodec
 */
@Composable
fun SettingsSheet(
    initialConfig: StreamingConfig,
    hardwareReport: DeviceCapabilityReport?,
    onSaveConfig: (StreamingConfig) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var srtHost by remember { mutableStateOf(initialConfig.srtHost) }
    var srtPort by remember { mutableStateOf(initialConfig.srtPort.toString()) }
    var streamId by remember { mutableStateOf(initialConfig.streamId) }
    var latencyMs by remember { mutableFloatStateOf(initialConfig.latencyMs.toFloat()) }
    var passphrase by remember { mutableStateOf(initialConfig.passphrase) }

    var selectedResolution by remember { mutableStateOf(initialConfig.resolution) }
    var selectedFps by remember { mutableStateOf(initialConfig.fps) }
    var videoBitrateKbps by remember { mutableFloatStateOf(initialConfig.videoBitrateKbps.toFloat()) }
    var selectedCodec by remember { mutableStateOf(initialConfig.codec) }
    var autoRecord by remember { mutableStateOf(initialConfig.autoRecordOnLive) }

    // Generar vista previa dinámica de la URL SRT
    val previewConfig = StreamingConfig(
        srtHost = srtHost,
        srtPort = srtPort.toIntOrNull() ?: 5000,
        streamId = streamId,
        latencyMs = latencyMs.toInt(),
        passphrase = passphrase,
        resolution = selectedResolution,
        fps = selectedFps,
        videoBitrateKbps = videoBitrateKbps.toInt(),
        codec = selectedCodec,
        autoRecordOnLive = autoRecord
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF00F172A))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Cabecera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONFIGURACIÓN DE TRANSMISIÓN IRL",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // SECCIÓN 1: Red y Conexión SRT (Caller)
            // ==========================================
            SectionTitle(title = "1. Conexión SRT (Modo Caller)", icon = Icons.Default.NetworkCheck)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DarkTextField(
                    value = srtHost,
                    onValueChange = { srtHost = it },
                    label = "IP Remota / Host (VPS o Tailscale)",
                    placeholder = "ej. 100.90.12.34 o vps.midominio.com",
                    modifier = Modifier.weight(2f)
                )

                DarkTextField(
                    value = srtPort,
                    onValueChange = { srtPort = it },
                    label = "Puerto SRT",
                    placeholder = "5000",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DarkTextField(
                    value = streamId,
                    onValueChange = { streamId = it },
                    label = "Stream ID (OBS / MediaMTX / Belabox)",
                    placeholder = "publish/live/streamkey",
                    modifier = Modifier.weight(2f)
                )

                DarkTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = "Passphrase AES (Opcional)",
                    placeholder = "Sin clave",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slider de Latencia SRT Buffer
            Text(
                text = "Buffer de Latencia SRT: ${latencyMs.toInt()} ms (Recomendado 4G/5G: 1500ms - 2500ms)",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = latencyMs,
                onValueChange = { latencyMs = it },
                valueRange = 500f..4000f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF38BDF8),
                    activeTrackColor = Color(0xFF0284C7)
                )
            )

            // Vista Previa de la URL SRT
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "URL Generada para Handshake SRT:",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                    Text(
                        text = previewConfig.buildSrtUrl(),
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // SECCIÓN 2: Detección de Hardware & Video
            // ==========================================
            SectionTitle(title = "2. Video & Detección de Hardware", icon = Icons.Default.Videocam)

            // Tarjeta de diagnóstico de Hardware
            hardwareReport?.let { report ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.padding(4.dp)
                        )
                        Column {
                            Text(
                                text = "Diagnóstico Camera2 & MediaCodec:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "4K@60fps: ${if (report.supports4K60Fps) "SOPORTADO" else "NO SOPORTADO (Fallback automático activo)"} | " +
                                        "1080p@60fps: ${if (report.supports1080p60Fps) "SÍ" else "NO"} | " +
                                        "HEVC Hardware: ${if (report.supportsHevcEncoder) "DISPONIBLE" else "NO"}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Selector de Resolución
            Text(
                text = "Resolución de Salida:",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StreamResolution.entries.forEach { res ->
                    val isSupported = hardwareReport?.supportedResolutions?.contains(res) ?: true
                    SelectableChip(
                        label = res.label,
                        isSelected = selectedResolution == res,
                        isEnabled = isSupported,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedResolution = res
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selector de FPS
            Text(
                text = "Tasa de Cuadros por Segundo (FPS):",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StreamFps.entries.forEach { fps ->
                    val isSupported = hardwareReport?.supportedFps?.contains(fps) ?: true
                    SelectableChip(
                        label = fps.label,
                        isSelected = selectedFps == fps,
                        isEnabled = isSupported,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedFps = fps
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slider de Bitrate de Video (4000 a 35000 kbps)
            Text(
                text = "Bitrate de Video: ${videoBitrateKbps.toInt()} kbps (${String.format("%.1f", videoBitrateKbps / 1000f)} Mbps)",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = videoBitrateKbps,
                onValueChange = { videoBitrateKbps = it },
                valueRange = 4000f..35000f,
                steps = 30,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF10B981),
                    activeTrackColor = Color(0xFF059669)
                )
            )

            // Botones de presets de Bitrate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PresetBitrateChip("4 Mbps (4G)", 4000f) { videoBitrateKbps = it }
                PresetBitrateChip("8 Mbps (FHD)", 8000f) { videoBitrateKbps = it }
                PresetBitrateChip("15 Mbps (5G)", 15000f) { videoBitrateKbps = it }
                PresetBitrateChip("30 Mbps (4K HQ)", 30000f) { videoBitrateKbps = it }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selector de Códec
            Text(
                text = "Códec de Compresión:",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoCodecType.entries.forEach { codec ->
                    SelectableChip(
                        label = codec.label,
                        isSelected = selectedCodec == codec,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedCodec = codec
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // SECCIÓN 3: Grabación Local Desacoplada
            // ==========================================
            SectionTitle(title = "3. Pipeline Dual & Grabación Local", icon = Icons.Default.Info)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-grabar master en MP4 al iniciar LIVE",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Almacena en /Movies/MoblinIRL/. No se corta si la red 4G/5G cae.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = autoRecord,
                    onCheckedChange = { autoRecord = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF10B981),
                        checkedTrackColor = Color(0xFF065F46)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón de Guardar
            Button(
                onClick = {
                    onSaveConfig(previewConfig)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "GUARDAR Y APLICAR CAMBIOS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF38BDF8)
        )
        Text(
            text = title,
            color = Color(0xFF38BDF8),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        placeholder = { Text(placeholder, fontSize = 12.sp, color = Color(0xFF475569)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF38BDF8),
            unfocusedBorderColor = Color(0xFF334155),
            focusedLabelColor = Color(0xFF38BDF8),
            unfocusedLabelColor = Color(0xFF94A3B8)
        ),
        modifier = modifier
    )
}

@Composable
fun SelectableChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg = when {
        !isEnabled -> Color(0xFF1E293B).copy(alpha = 0.4f)
        isSelected -> Color(0xFF0284C7)
        else -> Color(0xFF1E293B)
    }
    val border = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
    val textCol = when {
        !isEnabled -> Color(0xFF475569)
        isSelected -> Color.White
        else -> Color(0xFFCBD5E1)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(enabled = isEnabled) { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textCol,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun PresetBitrateChip(label: String, bitrate: Float, onSelect: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF334155))
            .clickable { onSelect(bitrate) }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

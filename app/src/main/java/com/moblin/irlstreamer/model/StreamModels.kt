package com.moblin.irlstreamer.model

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Resoluciones de video estándar para producción y transmisión IRL.
 */
enum class StreamResolution(
    val width: Int,
    val height: Int,
    val label: String
) {
    UHD_4K(3840, 2160, "4K UHD (2160p)"),
    FHD_1080P(1920, 1080, "1080p FHD"),
    HD_720P(1280, 720, "720p HD");

    override fun toString(): String = label
}

/**
 * Tasas de cuadros por segundo (FPS) soportadas.
 */
enum class StreamFps(val value: Int, val label: String) {
    FPS_60(60, "60 FPS (Fluido / Acción)"),
    FPS_30(30, "30 FPS (Estándar Web)"),
    FPS_24(24, "24 FPS (Cinemático)");

    override fun toString(): String = label
}

/**
 * Códecs de compresión de video acelerados por hardware.
 */
enum class VideoCodecType(val mimeType: String, val label: String) {
    H265_HEVC("video/hevc", "H.265 (HEVC - Máxima Eficiencia)"),
    H264_AVC("video/avc", "H.264 (AVC - Alta Compatibilidad)");

    override fun toString(): String = label
}

/**
 * Códecs de audio para transmisión y master recording.
 */
enum class AudioBitrate(val kbps: Int, val label: String) {
    AAC_128(128, "128 kbps"),
    AAC_192(192, "192 kbps (Recomendado)"),
    AAC_256(256, "256 kbps (HQ)");

    override fun toString(): String = label
}

/**
 * Estado de la conexión de red SRT.
 */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val endpoint: String) : ConnectionState
    data class Reconnecting(val attempt: Int, val reason: String) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

/**
 * Configuración completa de la aplicación para red SRT, compresión y grabación local.
 */
data class StreamingConfig(
    // Parámetros de Red SRT (Modo Caller)
    val srtHost: String = "100.100.100.1", // IP de OBS / VPS / Tailscale
    val srtPort: Int = 5000,
    val streamId: String = "publish/live/streamkey",
    val latencyMs: Int = 2000, // 2000ms buffer óptimo para saltos de torres celulares 4G/5G
    val passphrase: String = "",

    // Parámetros de Video
    val resolution: StreamResolution = StreamResolution.FHD_1080P,
    val fps: StreamFps = StreamFps.FPS_60,
    val videoBitrateKbps: Int = 8000, // Rango de 4000 a 35000 kbps
    val codec: VideoCodecType = VideoCodecType.H265_HEVC,

    // Parámetros de Audio
    val audioBitrate: AudioBitrate = AudioBitrate.AAC_192,
    val audioSampleRate: Int = 48000, // 48kHz para broadcast profesional
    val isStereo: Boolean = true,

    // Pipeline Dual
    val autoRecordOnLive: Boolean = false,
    val highQualityLocalRecord: Boolean = true
) {
    /**
     * Construye la URL canónica de SRT en modo Caller con parámetros optimizados.
     * OBS Studio y relays SRT requieren 'mode=caller' y 'streamid' para el handshake sobre NAT.
     */
    fun buildSrtUrl(): String {
        val queryParams = mutableListOf<String>()
        queryParams.add("mode=caller")
        queryParams.add("latency=$latencyMs")

        if (streamId.isNotBlank()) {
            val encodedStreamId = URLEncoder.encode(streamId, StandardCharsets.UTF_8.name())
            queryParams.add("streamid=$encodedStreamId")
        }

        if (passphrase.isNotBlank()) {
            queryParams.add("passphrase=$passphrase")
            queryParams.add("pbkeylen=16") // AES-128
        }

        val queryString = queryParams.joinToString("&")
        return "srt://$srtHost:$srtPort?$queryString"
    }
}

/**
 * Métricas en tiempo real proyectadas en el HUD de Moblin.
 */
data class StreamStats(
    val isStreaming: Boolean = false,
    val isRecording: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val currentBitrateKbps: Long = 0L,
    val currentFps: Double = 0.0,
    val networkLatencyMs: Long = 0L, // RTT (Round Trip Time) medido por libsrt
    val droppedPackets: Long = 0L,
    val streamDurationSeconds: Long = 0L,
    val recordDurationSeconds: Long = 0L,
    val micVolumeLevel: Float = 0f, // 0.0f a 1.0f para barra VU
    val activeResolution: StreamResolution = StreamResolution.FHD_1080P,
    val activeFps: Int = 60,
    val activeCodec: VideoCodecType = VideoCodecType.H265_HEVC,
    val currentRecordPath: String? = null
)

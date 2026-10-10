package com.moblin.irlstreamer.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Range
import android.util.Size
import com.moblin.irlstreamer.model.StreamFps
import com.moblin.irlstreamer.model.StreamResolution
import com.moblin.irlstreamer.model.StreamingConfig
import com.moblin.irlstreamer.model.VideoCodecType

/**
 * Reporte exhaustivo de capacidades de hardware del dispositivo móvil.
 */
data class DeviceCapabilityReport(
    val cameraId: String,
    val isBackCamera: Boolean,
    val sensorOrientation: Int,
    val supportedResolutions: List<StreamResolution>,
    val supportedFps: List<StreamFps>,
    val supports4K: Boolean,
    val supports4K60Fps: Boolean,
    val supports1080p60Fps: Boolean,
    val supportsHevcEncoder: Boolean,
    val maxVideoBitrateBps: Int,
    val availableFpsRanges: List<Range<Int>>,
    val rawSupportedSizes: List<Size>
)

/**
 * Resultado de la negociación de configuración con fallback inteligente.
 */
data class FallbackDecision(
    val effectiveConfig: StreamingConfig,
    val wasFallbackTriggered: Boolean,
    val reason: String? = null
)

/**
 * CameraHelper: Especialista en inspección de hardware mediante Camera2 API
 * y MediaCodecList. Garantiza la selección óptima de resolución y cuadros por segundo,
 * implementando negociación y fallback graceful cuando el silicio del dispositivo
 * no soporta 4K a 60 FPS.
 */
class CameraHelper(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    /**
     * Obtiene el ID de la cámara principal trasera.
     */
    fun getBackCameraId(): String {
        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                return id
            }
        }
        return cameraManager.cameraIdList.firstOrNull() ?: "0"
    }

    /**
     * Obtiene el ID de la cámara frontal (selfie).
     */
    fun getFrontCameraId(): String? {
        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return id
            }
        }
        return null
    }

    /**
     * Escanea a fondo el hardware del dispositivo combinando las capacidades
     * del sensor óptico (Camera2) con las capacidades del decodificador/codificador (MediaCodec).
     */
    fun queryHardwareCapabilities(targetCameraId: String = getBackCameraId()): DeviceCapabilityReport {
        val characteristics = cameraManager.getCameraCharacteristics(targetCameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90

        // 1. Obtener resoluciones soportadas por el sensor para grabación y preview
        val surfaceSizes = map?.getOutputSizes(SurfaceTexture::class.java)?.toList() ?: emptyList()
        val mediaCodecSizes = map?.getOutputSizes(MediaCodec::class.java)?.toList() ?: emptyList()
        val rawSizes = (surfaceSizes + mediaCodecSizes).distinct()

        val has4KSensor = rawSizes.any { it.width >= 3840 && it.height >= 2160 }
        val has1080pSensor = rawSizes.any { it.width >= 1920 && it.height >= 1080 }
        val has720pSensor = rawSizes.any { it.width >= 1280 && it.height >= 720 }

        // 2. Rangos de FPS soportados por la cámara
        val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)?.toList()
            ?: emptyList()

        val cameraSupports60Fps = fpsRanges.any { it.upper >= 60 }
        val cameraSupports30Fps = fpsRanges.any { it.upper >= 30 }
        val cameraSupports24Fps = fpsRanges.any { it.upper >= 24 }

        // 3. Inspeccionar codificador de hardware (HEVC / H.265 y AVC / H.264)
        val hevcEncoder = findHardwareEncoder(MediaFormat.MIMETYPE_VIDEO_HEVC)
        val avcEncoder = findHardwareEncoder(MediaFormat.MIMETYPE_VIDEO_AVC)
        val hasHevc = hevcEncoder != null

        // Verificar si el codificador HEVC o AVC soporta 4K a 60 FPS
        val activeEncoder = hevcEncoder ?: avcEncoder
        var encoderSupports4K60 = false
        var encoderSupports1080p60 = false
        var maxBitrate = 20_000_000 // 20 Mbps por defecto

        activeEncoder?.let { codecInfo ->
            val mime = if (hevcEncoder != null) MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC
            val capabilities = codecInfo.getCapabilitiesForType(mime)
            val videoCaps = capabilities.videoCapabilities

            encoderSupports4K60 = videoCaps.isSizeSupported(3840, 2160) &&
                    videoCaps.areSizeAndRateSupported(3840, 2160, 60.0)

            encoderSupports1080p60 = videoCaps.isSizeSupported(1920, 1080) &&
                    videoCaps.areSizeAndRateSupported(1920, 1080, 60.0)

            videoCaps.bitrateRange?.upper?.let {
                maxBitrate = it
            }
        }

        // Combinar sensor óptico + codificador para determinar soporte real de extremo a extremo
        val fullSupports4K = has4KSensor && (activeEncoder?.let {
            val mime = if (hevcEncoder != null) MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC
            it.getCapabilitiesForType(mime).videoCapabilities.isSizeSupported(3840, 2160)
        } ?: false)

        val fullSupports4K60 = fullSupports4K && cameraSupports60Fps && encoderSupports4K60
        val fullSupports1080p60 = has1080pSensor && cameraSupports60Fps && encoderSupports1080p60

        val supportedResolutions = mutableListOf<StreamResolution>()
        if (fullSupports4K) supportedResolutions.add(StreamResolution.UHD_4K)
        if (has1080pSensor) supportedResolutions.add(StreamResolution.FHD_1080P)
        if (has720pSensor) supportedResolutions.add(StreamResolution.HD_720P)

        val supportedFps = mutableListOf<StreamFps>()
        if (cameraSupports60Fps) supportedFps.add(StreamFps.FPS_60)
        if (cameraSupports30Fps) supportedFps.add(StreamFps.FPS_30)
        if (cameraSupports24Fps) supportedFps.add(StreamFps.FPS_24)

        return DeviceCapabilityReport(
            cameraId = targetCameraId,
            isBackCamera = facing == CameraCharacteristics.LENS_FACING_BACK,
            sensorOrientation = sensorOrientation,
            supportedResolutions = supportedResolutions,
            supportedFps = supportedFps,
            supports4K = fullSupports4K,
            supports4K60Fps = fullSupports4K60,
            supports1080p60Fps = fullSupports1080p60,
            supportsHevcEncoder = hasHevc,
            maxVideoBitrateBps = maxBitrate,
            availableFpsRanges = fpsRanges,
            rawSupportedSizes = rawSizes
        )
    }

    /**
     * Negocia la configuración óptima solicitada por el usuario.
     * REQUISITO ESENCIAL: Si el hardware no soporta 4K a 60 FPS, realiza un
     * fallback graceful a 1080p 60 FPS.
     */
    fun negotiateGracefulFallback(
        requested: StreamingConfig,
        cameraId: String = getBackCameraId()
    ): FallbackDecision {
        val report = queryHardwareCapabilities(cameraId)

        var finalResolution = requested.resolution
        var finalFps = requested.fps
        var finalCodec = requested.codec
        var finalBitrate = requested.videoBitrateKbps
        var fallbackTriggered = false
        var fallbackReason: String? = null

        // 1. Verificación del Códec (HEVC a AVC si el silicio no lo soporta)
        if (requested.codec == VideoCodecType.H265_HEVC && !report.supportsHevcEncoder) {
            finalCodec = VideoCodecType.H264_AVC
            fallbackTriggered = true
            fallbackReason = "Codificador de hardware HEVC/H.265 no disponible. Ajustado a H.264/AVC."
        }

        // 2. REQUISITO CLAVE: Fallback de 4K a 60 FPS
        if (requested.resolution == StreamResolution.UHD_4K && requested.fps == StreamFps.FPS_60) {
            if (!report.supports4K60Fps) {
                fallbackTriggered = true
                if (report.supports1080p60Fps) {
                    finalResolution = StreamResolution.FHD_1080P
                    finalFps = StreamFps.FPS_60
                    // Si el usuario tenía configurado un bitrate excesivo para 4K (>20 Mbps), lo optimizamos para 1080p
                    if (finalBitrate > 15000) {
                        finalBitrate = 12000
                    }
                    fallbackReason = "El hardware no soporta codificación 4K a 60 FPS. Se aplicó fallback graceful a 1080p @ 60 FPS para garantizar la fluidez de transmisión."
                } else if (report.supports4K) {
                    // Si la cámara aguanta 4K pero solo a 30 FPS
                    finalResolution = StreamResolution.UHD_4K
                    finalFps = StreamFps.FPS_30
                    fallbackReason = "Codificación 4K limitada por hardware a 30 FPS. Fallback aplicado a 4K @ 30 FPS."
                } else {
                    // Fallback a 1080p 30 FPS
                    finalResolution = StreamResolution.FHD_1080P
                    finalFps = StreamFps.FPS_30
                    fallbackReason = "4K no soportado en este dispositivo. Fallback aplicado a 1080p @ 30 FPS."
                }
            }
        } else if (requested.resolution == StreamResolution.UHD_4K && !report.supports4K) {
            // El usuario pidió 4K pero el dispositivo no soporta 4K en absoluto
            finalResolution = StreamResolution.FHD_1080P
            fallbackTriggered = true
            fallbackReason = "Resolución 4K UHD no soportada por el sensor/codificador. Fallback a 1080p FHD."
        } else if (requested.fps == StreamFps.FPS_60 && !report.supportedFps.contains(StreamFps.FPS_60)) {
            // Se pidió 60 FPS pero el sensor solo entrega 30 FPS
            finalFps = StreamFps.FPS_30
            fallbackTriggered = true
            fallbackReason = "Tasa de 60 FPS no soportada por el sensor. Ajustado a 30 FPS."
        }

        val negotiatedConfig = requested.copy(
            resolution = finalResolution,
            fps = finalFps,
            codec = finalCodec,
            videoBitrateKbps = finalBitrate
        )

        return FallbackDecision(
            effectiveConfig = negotiatedConfig,
            wasFallbackTriggered = fallbackTriggered,
            reason = fallbackReason
        )
    }

    /**
     * Localiza un codificador acelerado por hardware para el formato especificado.
     */
    private fun findHardwareEncoder(mimeType: String): MediaCodecInfo? {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in codecList.codecInfos) {
            if (!info.isEncoder) continue
            val types = info.supportedTypes
            for (type in types) {
                if (type.equals(mimeType, ignoreCase = true)) {
                    return info
                }
            }
        }
        return null
    }
}

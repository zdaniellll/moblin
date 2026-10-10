# Moblin IRL Streamer (Android / Kotlin)

Una aplicación de streaming móvil de alto rendimiento inspirada en **Moblin**, diseñada específicamente para creadores de contenido IRL (In Real Life) en Twitch, Kick y YouTube. Permite transmitir video por el protocolo **SRT (Secure Reliable Transport)** en modo **Caller** desde cualquier conexión remota (red móvil 4G/5G, Wi-Fi o túneles privados como Tailscale/ZeroTier) directamente a OBS Studio, con **grabación local de alta fidelidad** desacoplada e inmune a cortes de red.

---

## 🚀 Características Principales

### 1. Protocolo SRT en Modo 'Caller' (NAT Traversal)
- **Atravesamiento de NAT Celular:** Al operar como `caller` (cliente), el teléfono inicia la conexión hacia la IP pública de tu VPS o la IP virtual de tu red mesh (**Tailscale** o **ZeroTier**), eliminando problemas de CGNAT en operadores móviles.
- **Configuración de OBS Studio / SLS / MediaMTX:** Diseñado para conectarse a un `srt://IP:PORT?mode=listener`.
- **Buffer de Latencia Ajustable:** Configurable entre 500ms y 4000ms (2000ms recomendado para transiciones fluidas entre torres celulares 4G y 5G).
- **Reconexión Automática con Exponential Backoff:** Si el streamer entra en un túnel o elevador, el motor intenta reconectar automáticamente sin congelar la app.

### 2. Detección Dinámica de Hardware (Camera2 API & MediaCodec)
- **Consulta de Capacidades en Silicio:** Inspecciona tanto el sensor óptico (`CameraCharacteristics`) como el codificador de video de hardware (`MediaCodecList`).
- **Resoluciones Soportadas:** 4K UHD (3840x2160), 1080p FHD (1920x1080) y 720p HD (1280x720).
- **Tasas de Refresco:** 60 FPS, 30 FPS y 24 FPS.
- **Graceful Fallback Automático:** Si el usuario selecciona **4K a 60 FPS** pero el hardware del teléfono o el codificador solo admiten 4K a 30 FPS o 1080p a 60 FPS, el sistema degrada suavemente a **1080p a 60 FPS** para preservar la fluidez de movimiento.

### 3. Pipeline Dual Desacoplado (Streaming + Grabación Master)
- **Salida 1 (Streaming SRT):** Flujo H.265 (HEVC) o H.264 (AVC) transmitido en tiempo real por UDP/SRT hacia el destino remoto.
- **Salida 2 (Grabación Local MP4):** Guardado directo en `/Movies/MoblinIRL/`.
- **Aislamiento de Fallos:** Si la conexión 4G/5G se cae por completo, **la grabación local MP4 NO se corta ni se corrompe**. El multiplexor local escribe de forma continua e independiente.

### 4. Interfaz de Usuario Jetpack Compose (Estilo Moblin)
- **Vista de Cámara Edge-to-Edge:** Renderizado directo mediante `OpenGlView` acelerado por GPU.
- **HUD Minimalista Semitransparente:**
  - Badge de estado: `LIVE` (indicador pulsante) y `REC`.
  - Bitrate en tiempo real con código de color dinámico (Verde > 6 Mbps, Amarillo 3-6 Mbps, Rojo < 3 Mbps).
  - Ping / Latencia de red SRT RTT en milisegundos (`RTT: 35 ms`).
  - FPS en tiempo real (`60.0 FPS`).
  - Píldora de resolución y códec (`1080p · HEVC`).
  - Barra medidora de volumen de audio (VU meter).
- **Controles Rápidos:**
  - Botón Master LIVE y Botón Master REC.
  - Conmutador de cámara (Trasera / Frontal).
  - Linterna / Flashlight toggle.
  - Silenciador de micrófono (Mute).
  - Selector de zoom (0.5x Gran angular, 1.0x, 2.0x).
- **Pantalla de Configuración Completa:**
  - Configuración de Red: Host/IP, Puerto, Stream ID, Buffer de Latencia, Passphrase AES.
  - Configuración de Video: Selector de Resolución, FPS, Bitrate (4,000 kbps a 35,000 kbps) y Códec.
  - Resumen de diagnóstico de hardware en tiempo real.

### 5. Foreground Service & WakeLock
- Incorpora `StreamingForegroundService` con tipos de servicio `camera`, `microphone` y `connectedDevice` para Android 14+ (API 34/35), garantizando que el streaming continúe incluso si la pantalla se bloquea o el streamer abre el chat de Twitch/Kick.

---

## 📁 Estructura del Código

```
MoblinIRLStreamer/
├── app/
│   ├── src/main/
│   │   ├── java/com/moblin/irlstreamer/
│   │   │   ├── MainActivity.kt               # Contenedor Compose, SurfaceView OpenGL y ciclo de vida
│   │   │   ├── MoblinApp.kt                  # Inicialización de canales de notificación
│   │   │   ├── camera/
│   │   │   │   └── CameraHelper.kt           # Detección Camera2 + MediaCodec y Fallback 4K@60 -> 1080p@60
│   │   │   ├── model/
│   │   │   │   └── StreamModels.kt           # Data classes de configuración, métricas y resoluciones
│   │   │   ├── network/
│   │   │   │   └── NetworkStreamer.kt        # Motor SRT Caller, Pipeline dual desacoplado y telemetría
│   │   │   ├── service/
│   │   │   │   └── StreamingForegroundService.kt # Foreground service y WakeLock para IRL
│   │   │   └── ui/
│   │   │       ├── MoblinHud.kt              # HUD overlay estilo Moblin con métricas en tiempo real
│   │   │       ├── SettingsSheet.kt          # Hoja de ajustes (SRT, Bitrate, FPS, Res)
│   │   │       └── StreamingViewModel.kt     # StateFlow reactivo y orquestación
│   │   ├── res/
│   │   │   └── values/
│   │   │       ├── strings.xml
│   │   │       └── themes.xml
│   │   └── AndroidManifest.xml               # Permisos de red, multimedia y servicios
│   ├── build.gradle.kts                      # Dependencias del módulo app
│   └── proguard-rules.pro                    # Reglas de ofuscación para libsrt
├── gradle/
│   ├── libs.versions.toml                    # Catálogo de versiones centralizado
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts                          # Configuración raíz
└── settings.gradle.kts                       # Repositorios (JitPack, Google, MavenCentral)
```

---

## 🛠️ Cómo Configurar OBS Studio para Recibir el Stream

Para recibir la transmisión desde tu teléfono mediante SRT Caller:

1. Abre **OBS Studio**.
2. Añade una nueva fuente de tipo **Fuente multimedia** (Media Source).
3. Desmarca la casilla **Archivo local** (Local File).
4. En el campo **Entrada** (Input), escribe la URL en modo **Listener**:
   ```
   srt://0.0.0.0:5000?mode=listener&latency=2000
   ```
   *(Asegúrate de que el puerto UDP 5000 esté abierto en el firewall de tu router/VPS o estés usando la IP de Tailscale).*
5. En la aplicación móvil **Moblin IRL Streamer**:
   - En **Host / IP**, coloca la IP pública de tu PC/VPS o la IP de Tailscale (ej. `100.90.12.34`).
   - En **Puerto**, coloca `5000`.
   - En **Latencia**, mantén `2000 ms`.
6. Presiona el botón **LIVE**. En OBS Studio verás aparecer el feed H.265 en cuestión de segundos.

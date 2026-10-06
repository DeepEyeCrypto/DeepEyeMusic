# Plan: HERMES VISUALIZER-OMEGA PROTOCOL (AGSL ENGINE RESCUE)

## Goal
Rescue and overhaul the Jetpack Compose AGSL visualizer engine by implementing a zero-allocation 256x1 FFT audio bitmap bridge (`VisualizerDataBridge.kt`), a dynamic GLSL-to-AGSL translation compiler (`AgslShaderEngine.kt`), and a locked 60/120 FPS hardware VSYNC render loop with `iChannel0` audio texture binding.

## Context & Problem Definition
- **GLSL vs AGSL Syntax Incompatibility**: WebGL/Shadertoy/VVavy shaders use `void mainImage(out vec4 fragColor, in vec2 fragCoord)`, `vec2`/`vec3`/`vec4`, `texture(iChannel0, uv)`, and GLSL precision qualifiers that fail to compile under Android 13+ SkSL/AGSL `RuntimeShader`.
- **Audio Texture Gap (`iChannel0`)**: AGSL `RuntimeShader` does not accept raw dynamic float/byte arrays for audio spectrum data; it expects a child `shader` (`uniform shader iChannel0`).
- **Render Loop Stalling**: Without an explicit `produceState` / `withFrameNanos` frame ticker, Compose skips Canvas draw passes when audio FFT updates arrive without triggering layout recomposition.

## Proposed Architecture
```
┌───────────────────────────┐      ┌──────────────────────────┐
│ ViPER4Android / ExoPlayer │ ───► │ VisualizerAudioProcessor │
└───────────────────────────┘      └────────────┬─────────────┘
                                                │ (Raw PCM / FFT Byte Bins)
                                                ▼
┌───────────────────────────┐      ┌──────────────────────────┐
│   VisualizerDataBridge    │ ◄─── │ VisualizerEngine         │
│ (256x1 ARGB_8888 Bitmap   │      └──────────────────────────┘
│  + Cached BitmapShader)   │
└─────────────┬─────────────┘
              │ shader.setInputShader("iChannel0", bitmapShader)
              ▼
┌───────────────────────────┐      ┌──────────────────────────┐
│    AgslShaderEngine       │ ───► │   RuntimeShader (AGSL)   │
│ (Regex GLSL -> AGSL Dialect│      └────────────┬─────────────┘
│  Transpiler + Uniforms)   │                   │
└───────────────────────────┘                   │ ShaderBrush
                                                ▼
                                   ┌──────────────────────────┐
                                   │  Compose Canvas / Visual │
                                   │  (Locked 60/120 FPS      │
                                   │   withFrameNanos Ticker) │
                                   └──────────────────────────┘
```

## Step-by-Step Implementation Tasks

### Task 1: Create `VisualizerDataBridge.kt` (The FFT Bitmap Engine)
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/agsl/VisualizerDataBridge.kt`
- Create a high-performance, zero-allocation FFT-to-Bitmap converter.
- Pre-allocate a `Bitmap` of size `256 x 1` (`Bitmap.Config.ARGB_8888`).
- Pre-allocate an `IntArray(256)` pixel buffer.
- Map FFT magnitudes (`0..255` or normalized floats `0.0f..1.0f`) to ARGB color components:
  - Alpha = `0xFF`
  - Red = FFT magnitude (Frequency domain)
  - Green = Waveform amplitude (Time domain)
  - Blue = Spectral flux / transient peak
- Use `bitmap.setPixels(pixels, 0, 256, 0, 0, 256, 1)` and wrap in a cached `BitmapShader` with `Shader.TileMode.CLAMP`.

### Task 2: Create `AgslShaderEngine.kt` (The Dialect Transpiler)
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/agsl/AgslShaderEngine.kt`
- Implement an automated regex transpiler that converts GLSL / WebGL / Shadertoy shader strings into valid SkSL / AGSL syntax for Android 13+ (`RuntimeShader`):
  1. Replace `void mainImage\s*\(\s*out\s+vec4\s+(\w+)\s*,\s*in\s+vec2\s+(\w+)\s*\)` with `half4 main(float2 $2)`.
  2. Replace `$1\s*=\s*([^;]+);` inside the main function with `return half4($1);` or direct return statements.
  3. Replace `texture\s*\(\s*iChannel0\s*,\s*([^)]+)\)` with `iChannel0.eval(($1) * iResolution)`.
  4. Replace `texture2D\s*\(\s*iChannel0\s*,\s*([^)]+)\)` with `iChannel0.eval(($1) * iResolution)`.
  5. Replace GLSL types: `vec2` -> `float2`, `vec3` -> `float3`, `vec4` -> `float4`, `mat2` -> `float2x2`, `mat3` -> `float3x3`, `mat4` -> `float4x4`.
  6. Strip precision specifiers: `precision\s+(highp|mediump|lowp)\s+float;`.
  7. Prepend standard uniform headers:
     ```agsl
     uniform float2 iResolution;
     uniform float iTime;
     uniform float iTimeDelta;
     uniform float4 iMouse;
     uniform shader iChannel0;
     uniform float4 iColorPrimary;
     uniform float4 iColorSecondary;
     uniform float iBass;
     uniform float iMid;
     uniform float iTreble;
     uniform float iPeak;
     ```
  8. Provide `compileShader(rawGlsl: String): RuntimeShader?` with `runCatching` safe fallback and error logging.

### Task 3: Upgrade `AgslVisualizer.kt` & `VisualizerCanvas.kt` (The Compose Render Loop)
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/agsl/AgslVisualizer.kt`
- Integrate `VisualizerDataBridge` and `AgslShaderEngine`.
- Bind dynamic uniforms on every frame:
  - `iResolution` -> `size.width`, `size.height`
  - `iTime` -> `timeSec`
  - `iChannel0` -> `runtimeShader.setInputShader("iChannel0", dataBridge.getAudioBitmapShader())`
  - `iColorPrimary`, `iColorSecondary` -> Monet dynamic palette
  - `iBass`, `iMid`, `iTreble`, `iPeak` -> Acoustic envelopes
- Render with `drawRect(brush = ShaderBrush(runtimeShader))`.

### Task 4: Unit Testing & Transpiler Validation
- **File**: `app/src/test/java/com/deepeye/musicpro/ui/player/visualizer/AgslShaderEngineTest.kt`
- Write unit tests verifying GLSL-to-AGSL transpilation:
  - Test `mainImage` conversion.
  - Test `iChannel0` texture sampling conversion to `eval()`.
  - Test `vec` to `float` vector type conversion.
  - Test precision stripping and uniform header injection.
- Run `./gradlew --no-daemon testDebugUnitTest`.

### Task 5: Build Debug APK & On-Device Validation
- Run `./gradlew --no-daemon assembleDebug`.
- Sideload to Realme RMX3945 (Android 16, MT6835).
- Verify logcat for SkiaShader compilation logs and verify 60 FPS locked animation.
- Capture screen forensics.

### Task 6: Bump Version & Release `v3.0.1.63`
- Bump version to `versionCode = 30073`, `versionName = "3.0.1.63"`.
- Commit, tag `v3.0.1.63`, and push to GitHub.

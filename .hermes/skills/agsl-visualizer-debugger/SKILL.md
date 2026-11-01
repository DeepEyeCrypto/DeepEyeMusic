# AGSL Visualizer Debugger Skill

## Skill Overview
Debug and recover AGSL (Android Graphics Shading Language) visualizer state on Android devices, with focus on MediaTek MT6835 / Android 16 platforms. This skill covers the full workflow from permissioning through shader compile verification to scene dispatch, with guards against the MT6835 SIGABRT pathway.

## Always-On Rules (SKILL.md body)

### 1. Permission gate first
- **Rule**: Always grant `android.permission.RECORD_AUDIO` before any visualizer work. The `android.media.audiofx.Visualizer` fallback requires this permission; without it, `frequencyBands` / `fftSpectrum` StateFlows remain zero and the visualizer appears dead.
- **Command**: `adb -s <device> shell pm grant <pkg> android.permission.RECORD_AUDIO`
- **Fallback**: If adb grant fails (Android 16 enterprise lockdown), the PCM pipeline via `VisualizerAudioProcessor` → `VisualizerEngine.feedPcmData()` does NOT need `RECORD_AUDIO` — it sits inside ExoPlayer's audio processor. Distinguish the two pathways.

### 2. Data-flow verification before shader work
- **Rule**: Before touching any AGSL, confirm the PCM pipeline is alive. Look for `PCM_PIPELINE_ACTIVE` in `logcat -s VisualizerEngine` or `frequencyBands LIVE` in `logcat -s PlayerViewModel`. If these don't appear within 3s of playback start, the visualizer cannot render — fix the pipeline first.
- **Diagnostic log injection** (one-shot, do not ship to prod):
  ```kotlin
  // In VisualizerEngine.feedPcmData(), after maxMagnitude calc:
  diagnosticLogged = maxMagnitude > 0.1f
  if (diagnosticLogged) Log.i(TAG, "PCM_PIPELINE_ACTIVE maxMag=$maxMagnitude bass=${smoothedBands[0]}")
  ```
- **Diagnostic collection** (PlayerViewModel, init block):
  ```kotlin
  viewModelScope.launch {
      visualizerEngine.frequencyBands
          .distinctUntilChanged { a, b -> a.contentEquals(b) }
          .collect { bands ->
              if (bands.any { it > 0.05f }) Log.i("PlayerViewModel", "frequencyBands LIVE: ${bands.joinToString(", ") { "%.2f".format(it) }})
          }
  }
  ```

### 3. MT6835 SIGABRT guard
- **Rule**: On Realme RMX35 (MT6835), binding undeclared uniforms (`iMouse`, `iColorPrimary`, `iColorSecondary`) to AGSL shaders that don't declare them triggers `libhwui` native path that reads freed buffers and fails CheckJNI UTF-8 validation → SIGABRT. Three extensions prevent this:
  - `AgslScene.declaresMouse`: true for FLUID_PULSE, AUDIO_FRACTAL, NEON_GRID, AURORA_FLUX, LIQUID_CHROME, NEBULA_BLOOM, PRISM_TUNNEL, KINETIC_MANDALA, PULSE_GEOMETRY
  - `AgslScene.declaresColorUniforms`: true only for NEBULA_BLOOM, PRISM_TUNNEL, KINETIC_MANDALA, PULSE_GEOMETRY
  - Always guard uniform binding behind these extensions; never bind `iMouse` / `iColorPrimary` / `iColorSecondary` unconditionally.

### 4. Scene dispatch exhaustiveness
- **Rule**: `VisualizerHost.kt` `when (sceneId)` must cover ALL 14 `VisualizerSceneId` entries. Missing any branch is a compile error. The 3 Batch 1 scenes (AURORA_FLUX, LIQUID_CHROME, NEBULA_BLOOM) were originally absent and cause build failure.
- **Fix pattern**: Add each missing branch as:
  ```kotlin
  VisualizerSceneId.AURORA_FLUX -> com.deepeye.musicpro.ui.player.visualizer.agsl.AgslVisualizer(
      fftSpectrum = fftSpectrum,
      frequencyBands = frequencyBands,
      scene = com.deepeye.musicpro.ui.player.visualizer.agsl.AgslScene.AURORA_FLUX,
      accentColor = accentColor, primaryColor = primaryColor, secondaryColor = secondaryColor,
      modifier = modifier
  )
  ```

### 5. VisualizerStudioScreen Compose guard
- **Rule**: `Column(Modifier.align(Alignment.BottomStart))` is unavailable in the Compose version used by this project. Replace with:
  ```kotlin
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) { Column {...} }
  ```

## Verification Scripts (scripts/ directory)

### verify-pcm-pipeline.sh
```bash
#!/bin/bash
# Verify PCM pipeline is alive before visualizer debugging
# Usage: ./verify-pcm-pipeline.sh <logcat-tag> <timeout-seconds>
TAG=$1
TIMEOUT=$2
adb logcat -c
adb shell am start -n com.deepeye.musicpro/.MainActivity
sleep 2
timeout $TIMEOUT adb logcat -s $TAG | grep -q "PCM_PIPELINE_ACTIVE" && echo "PCM pipeline ACTIVE" || echo "PCM pipeline INACTIVE"
```

### verify-shader-compile.sh
```bash
#!/bin/bash
# Verify AGSL shaders compile without MT6835 SIGABRT
# Usage: ./verify-shader-compile.sh <package> <duration-minutes>
PACKAGE=$1
DUR=$2
adb -s LZN7EERSZPS4VSUG install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
adb -s LZN7EERSZPS4VSUG logcat -c
adb -s LZN7EERSZPS4VSUG shell am start -n com.deepeye.musicpro/.MainActivity >/dev/null
sleep 3
timeout $((DUR*60)) adb logcat -s AgslVisualizer | grep -ci "compile failed" | grep -q "0" && echo "SHADER COMPILE OK" || echo "SHADER COMPILE FAILED"
```

## References (references/ directory)

### references/uniform-declarations.md
Uniform declaration patterns per AGSL scene, verified against `AgslShaders.kt`:
- LIQUID_PLASMA / CRYSTAL_TUNNEL / AURA_ORB / CYBER_GRID: declares `iResolution, iTime, iBass, iMid, iTreble, iPeak, iChannel0, iAccentColor, iColorPrimary, iColorSecondary`
- NEON_TRIANGLE_GRID: same minus `iChannel0` and `iAccentColor`
- (no shader declares `iTimeDelta` — that call was removed)
- FLUID_PULSE / AUDIO_FRACTAL / NEON_GRID: full catalog uniform contract (`iAudioBass/iAudioMids/iAudioTreble/iAudioEnergy/iBeatPulse/iSceneTransition/iIntensity/iAspect`) plus `iMouse` where declared
- AURORA_FLUX / LIQUID_CHROME / NEBULA_BLOOM: same catalog contract + `iMouse` + `iColorPrimary`/`iColorSecondary`

### references/scene-mapping.md
VisualizerStudioScreen scene→AgslScene mapping table:
| VisualizerSceneId | AgslScene |
|---|---|
| NEON_TRIANGLE_GRID | NEON_TRIANGLE_GRID |
| LIQUID_PLASMA | LIQUID_PLASMA |
| CRYSTAL_TUNNEL | CRYSTAL_TUNNEL |
| CYBER_GRID | CYBER_GRID |
| AURA_ORB | AURA_ORB |
| FLUID_PULSE | FLUID_PULSE |
| AUDIO_FRACTAL | AUDIO_FRACTAL |
| NEON_GRID | NEON_GRID |
| AURORA_FLUX | AURORA_FLUX (Batch 1 — was missing) |
| LIQUID_CHROME | LIQUID_CHROME (Batch 1 — was missing) |
| NEBULA_BLOOM | NEBULA_BLOOM (Batch 1 — was missing) |
| PRISM_TUNNEL | PRISM_TUNNEL (Batch 1 — was missing) |
| KINETIC_MANDALA | KINETIC_MANDALA (Batch 1 — was missing) |
| PULSE_GEOMETRY | PULSE_GEOMETRY (Batch 1 — was missing) |

## Templates (templates/ directory)

### templates/visualizer-studio-screen.kt
Starter template for VisualizerStudioScreen with all 14 scene branches wired.

### templates/visualizer-host.kt
Starter template for VisualizerHost with all 14 scene branches in the `when` expression.

## Scripts (scripts/ directory)

### scripts/verify-full.sh
Full end-to-end verification:
```bash
#!/bin/bash
set -e
cd /Users/enayat/Documents/DeepEyeMusicPro
./gradlew :app:assembleDebug >/dev/null
adb -s LZN7EERSZPS4VSUG install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
adb -s LZN7EERSZPS4VSUG shell am start -n com.deepeye.musicpro/.MainActivity >/dev/null
sleep 3
# Check 1: Permission
adb -s LZN7EERSZPS4VSUG shell pm list permissions | grep -q RECORD_AUDIO && echo "PASS: Permission granted" || echo "FAIL: Permission missing"
# Check 2: PCM pipeline
adb -s LZN7EERSZPS4VSUG logcat -c
timeout 15 adb logcat -s VisualizerEngine | grep -q "PCM_PIPELINE_ACTIVE" && echo "PASS: PCM pipeline active" || echo "FAIL: PCM pipeline inactive"
# Check 3: Shader compile
timeout 20 adb logcat -s AgslVisualizer | grep -q "compile failed" && echo "FAIL: Shader compile error" || echo "PASS: Shader compile OK"
echo "VERIFICATION COMPLETE"
```
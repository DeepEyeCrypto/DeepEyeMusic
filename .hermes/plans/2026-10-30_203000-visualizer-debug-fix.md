# Visualizer Debug & Fix Plan — DeepEyeMusicPro

**Goal**: Get AGSL visualizer rendering on Realme RMX3945 (MT6835, Android 16) via adb install, debug why visuals don't appear, and fix the root cause.

---

## Current Context / Assumptions

| Item | Status |
|------|--------|
| Build | `./gradlew assembleDebug` → SUCCESS, APK installed via `adb install -r` |
| Device | Realme RMX3945, serial `LZN7EERSZPS4VSUG`, Android 16 (API 35), MT6835 (MediaTek Dimensity) |
| App process | Running (PID 19090), `topResumedActivity = MainActivity` |
| Visualizer code | **Two parallel paths exist**:<br>1. **AGSL** (`AgslVisualizer.kt`) — GPU RuntimeShader, 14 scenes incl. AURORA_FLUX, LIQUID_CHROME, NEBULA_BLOOM (Batch 1)<br>2. **Canvas** (`VisualizerCanvas.kt`) — CPU Compose Canvas, 4 legacy scenes |
| Data source | `VisualizerAudioProcessor` (ExoPlayer in-pipeline) → `VisualizerEngine.feedPcmData()` → `StateFlow<FloatArray>` for `fftSpectrum` (32-bin) & `frequencyBands` (6-band) |
| Permission | **`RECORD_AUDIO` NOT granted** (missing from `dumpsys package` output). App requests it in `NowPlayingScreen` but user hasn't allowed it. |
| Logs | No `Visualizer|AGSL|RuntimeShader` tags in logcat → visualizer code path not executing or failing silently |
| Scene selection | Persisted in DataStore via `VisualizerPreferences`; default scene = `VisualizerSceneId.NEON_TRIANGLE_GRID` |
| MT6835 guard | `AgslVisualizer` has `declaresMouse` / `declaresColorUniforms` extensions to avoid SIGABRT on undeclared uniforms |

---

## Architecture / Approach (2-3 sentences)

The visualizer receives real-time PCM data from ExoPlayer's audio pipeline via `VisualizerAudioProcessor` → `VisualizerEngine`. This produces two `StateFlow<FloatArray>` streams (`fftSpectrum`, `frequencyBands`) that the UI layer (`VisualizerHost` → `AgslVisualizer` / `VisualizerCanvas`) consumes inside Compose `Canvas` draw lambdas (zero-recomposition). The most likely blockers are: (1) missing `RECORD_AUDIO` permission preventing the fallback `android.media.audiofx.Visualizer` from attaching, (2) AGSL `RuntimeShader` compilation failure on MT6835 Mali GPU, or (3) scene ID mismatch between persisted preference and `AgslScene` enum. We will verify each layer bottom-up: permission → data flow → shader compile → scene wiring.

---

## Step-by-Step Tasks

Each task is 2-5 minutes, names exact file paths, includes copy-pasteable commands/code, and expected verification output.

### Phase 0 — Permission & Environment (prerequisite)

#### Task 0.1 — Grant RECORD_AUDIO permission on device
**Why**: `VisualizerEngine.start()` uses `android.media.audiofx.Visualizer` as fallback when PCM pipeline isn't ready. Without `RECORD_AUDIO`, it fails silently and `frequencyBands` / `fftSpectrum` stay zero.

**Command**:
```bash
adb -s LZN7EERSZPS4VSUG shell pm grant com.deepeye.musicpro android.permission.RECORD_AUDIO
```
**Expected**: `Success` (exit 0). If it fails with "Neither user 2000 nor current process has GRANT_RUNTIME_PERMISSIONS", use the UI grant flow below.

**Alternative (UI grant)**:
```bash
# Launch permission screen directly
adb -s LZN7EERSZPS4VSUG shell am start -n com.android.permissioncontroller/com.android.permissioncontroller.permission.ui.GrantPermissionsActivity --es package_name com.deepeye.musicpro --esa permissions android.permission.RECORD_AUDIO
```
Then tap "Allow" on device.

**Verification**:
```bash
adb -s LZN7EERSZPS4VSUG shell dumpsys package com.deepeye.musicpro | grep -A 30 "requested permissions:" | grep RECORD_AUDIO
```
Should show `android.permission.RECORD_AUDIO: granted=true`.

---

### Phase 1 — Verify Data Flow (PCM → StateFlow)

#### Task 1.1 — Add diagnostic logging to VisualizerEngine
**File**: `/Users/enayat/Documents/DeepEyeMusicPro/app/src/main/java/com/deepeye/musicpro/player/visualizer/VisualizerEngine.kt`

**Change**: Add a one-time log when `feedPcmData` receives non-zero data (confirms PCM pipeline is live).

```kotlin
// In feedPcmData(), after line 94 (_fftSpectrum.value = ...)
if (maxMagnitude > 0.1f && !diagnosticLogged) {
    diagnosticLogged = true
    Log.i(TAG, "[VisualizerEngine] PCM_PIPELINE_ACTIVE maxMag=$maxMagnitude bass=${smoothedBands[0]} mid=${smoothedBands[2]} treble=${smoothedBands[4]}")
}
```
Add `private var diagnosticLogged = false` at class top (line ~50).

**Verification**: Rebuild & reinstall, play a track, check logcat:
```bash
adb -s LZN7EERSZPS4VSUG logcat -s VisualizerManager
```
**Expected**: `PCM_PIPELINE_ACTIVE maxMag=...` within 2-3 seconds of playback start.

#### Task 1.2 — Verify StateFlow emission in PlayerViewModel
**File**: `/Users/enayat/Documents/DeepEyeMusicPro/app/src/main/java/com/deepeye/musicpro/ui/player/PlayerViewModel.kt`

**Change**: Temporary log in `init` block (after line 140) to confirm flows are non-empty:
```kotlin
viewModelScope.launch {
    visualizerEngine.frequencyBands
        .distinctUntilChanged { a, b -> a.contentEquals(b) }
        .collect { bands ->
            if (bands.any { it > 0.05f }) {
                Log.i("PlayerViewModel", "frequencyBands LIVE: ${bands.joinToString(", ") { "%.2f".format(it) }}")
            }
        }
}
```

**Verification**: Same logcat, filter `PlayerViewModel`:
```bash
adb -s LZN7EERSZPS4VSUG logcat -s PlayerViewModel
```
**Expected**: `frequencyBands LIVE: 0.12, 0.05, 0.34, 0.08, 0.02, 0.41` (non-zero values pulsing).

---

### Phase 2 — AGSL Shader Compile & Runtime (MT6835)

#### Task 2.1 — Force AGSL path & log shader creation
**File**: `/Users/enayat/Documents/DeepEyeMusicPro/app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/agsl/AgslVisualizer.kt`

**Change**: In `AgslRuntimeShaderRenderer`, wrap each `RuntimeShader` creation in `runCatching` with log (around lines 185-203):
```kotlin
val auroraFluxShader = remember { 
    runCatching { RuntimeShader(AURORA_FLUX) }
        .onFailure { Log.e("AgslVisualizer", "AURORA_FLUX compile failed", it) }
        .getOrNull() ?: RuntimeShader(AgslShaders.LIQUID_PLASMA) // fallback
}
```
Do this for all 9 catalog shaders (lines 195-203). Keep the original `liquidShader` etc. as fallbacks.

**Verification**: Rebuild, reinstall, launch app, check logcat:
```bash
adb -s LZN7EERSZPS4VSUG logcat -s AgslVisualizer
```
**Expected**: No "compile failed" errors. If any shader fails, that specific shader name will appear.

#### Task 2.2 — Verify uniform binding at draw time
**File**: Same `AgslVisualizer.kt`, in the draw block (around line 330), add:
```kotlin
if (isCatalogScene) {
    // ... existing uniform sets ...
    // Add after iAspect binding:
    runCatching {
        Log.d("AgslVisualizer", "DRAW scene=$scene bass=$bass mids=$mids treble=$treble energy=$energy")
    }
}
```

**Verification**: Logcat `AgslVisualizer` should show `DRAW scene=AURORA_FLUX bass=...` every frame (throttled by thermal tier).

#### Task 2.3 — Fallback to Canvas path if AGSL fails
**File**: `/Users/enayat/Documents/DeepEyeMusicPro/app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/agsl/AgslVisualizer.kt`

**Change**: In `AgslVisualizer` composable (lines 74-95), add explicit fallback logging:
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    val agslSupported = remember { 
        // Quick probe: try compiling a minimal shader
        runCatching { RuntimeShader("half4 main(float2 fc) { return half4(1,0,0,1); }") }.isSuccess
    }
    if (agslSupported) {
        AgslRuntimeShaderRenderer(...)
    } else {
        Log.w("AgslVisualizer", "AGSL probe failed, falling back to VisualizerCanvas")
        VisualizerCanvas(...)
    }
} else {
    VisualizerCanvas(...)
}
```

**Verification**: If AGSL probe fails on MT6835, you'll see the warning and Canvas path will render (4 scenes work on CPU).

---

### Phase 3 — Scene Selection & Persistence

#### Task 3.1 — Verify current scene ID matches AgslScene enum
**File**: `/Users/enayat/Documents/DeepEyeMusicPro/app/src/main/java/com/deepeye/musicpro/features/visualizer/studio/VisualizerStudioScreen.kt`

**Check**: The `when (activeScene)` mapping (lines 51-61) maps only 5 IDs to `AgslScene`. The other 9 (including Batch 1: AURORA_FLUX, LIQUID_CHROME, NEBULA_BLOOM) fall through to `AgslScene.NEON_TRIANGLE_GRID`.

**Fix**: Add explicit mappings for the 3 Batch 1 scenes:
```kotlin
VisualizerSceneId.AURORA_FLUX -> AgslScene.AURORA_FLUX
VisualizerSceneId.LIQUID_CHROME -> AgslScene.LIQUID_CHROME
VisualizerSceneId.NEBULA_BLOOM -> AgslScene.NEBULA_BLOOM
```
Also add the remaining 6: PRISM_TUNNEL, KINETIC_MANDALA, PULSE_GEOMETRY.

**Verification**: Open Visualizer Studio screen (tap tune icon on NowPlaying), select "Aurora Flux (Studio)" → should render that shader, not NEON_TRIANGLE_GRID.

#### Task 3.2 — Ensure default scene is an AGSL catalog scene
**File**: `/Users/enayat/Documents/DeepEyeMusicPro/app/src/main/java/com/deepeye/musicpro/data/prefs/VisualizerPreferences.kt` (or wherever default is defined)

**Check**: Default `sceneId` should be one of the catalog scenes (e.g., `FLUID_PULSE`) not a legacy scene.

**Fix**: If default is `NEON_TRIANGLE_GRID`, change to `FLUID_PULSE` (first catalog scene).

---

### Phase 4 — End-to-End Verification

#### Task 4.1 — Full rebuild & device test
```bash
cd /Users/enayat/Documents/DeepEyeMusicPro
./gradlew :app:assembleDebug
adb -s LZN7EERSZPS4VSUG install -r app/build/outputs/apk/debug/app-debug.apk
adb -s LZN7EERSZPS4VSUG shell am start -n com.deepeye.musicpro/.MainActivity
```
Play a YouTube track (audio, not video) → wait 5s.

#### Task 4.2 — Logcat capture for evidence
```bash
adb -s LZN7EERSZPS4VSUG logcat -d -t 500 | grep -iE "Visualizer|AGSL|RuntimeShader|PCM_PIPELINE|frequencyBands|DRAW scene|compile" > /tmp/visualizer_evidence.log
cat /tmp/visualizer_evidence.log
```

**Success criteria** (all must pass):
1. `RECORD_AUDIO` granted ✓
2. `PCM_PIPELINE_ACTIVE` log appears ✓
3. `frequencyBands LIVE` shows pulsing non-zero values ✓
4. No `AgslVisualizer` "compile failed" errors ✓
5. `DRAW scene=` logs appear every frame ✓
6. Visual output visible on device (no black screen) ✓

---

## Tests / Validation (TDD per task)

| Task | Test Command | Pass Condition |
|------|--------------|----------------|
| 0.1 | `adb shell pm grant ...` | Exit 0, permission shows granted |
| 1.1 | `logcat -s VisualizerManager` | `PCM_PIPELINE_ACTIVE` within 3s of play |
| 1.2 | `logcat -s PlayerViewModel` | `frequencyBands LIVE` non-zero pulsing |
| 2.1 | `logcat -s AgslVisualizer` | Zero "compile failed" lines |
| 2.2 | `logcat -s AgslVisualizer` | `DRAW scene=` every ~16ms (60fps) |
| 3.1 | UI: select Aurora Flux in Studio | Shader changes, not fallback |
| 4.1-4.2 | Full run + evidence log | All 6 success criteria ✓ |

---

## Risks, Tradeoffs, Open Questions

| Risk | Mitigation |
|------|------------|
| **MT6835 Mali GPU doesn't support some AGSL ops** | Task 2.3 fallback to Canvas path; log which shader fails |
| **RECORD_AUDIO grant fails on Android 16** | Use UI grant flow; if still fails, PCM pipeline (VisualizerAudioProcessor) should still work — it doesn't need the permission |
| **VisualizerAudioProcessor not in ExoPlayer chain** | Verified in `PlayerModule.kt` (DI) and `PlayerController` init — it's wired. Task 1.1 confirms. |
| **Scene ID persisted as legacy enum value** | Task 3.1 fixes mapping; Task 3.2 fixes default |
| **Thermal governor capping FPS to 30 (THROTTLED tier)** | Logs will show `DRAW` at lower rate; visual still works. Check `ThermalGovernor.kt` tier. |
| **AGSL `iMouse` / `iColorPrimary` SIGABRT on MT6835** | Already guarded by `declaresMouse` / `declaresColorUniforms` extensions. Verify in Task 2.2. |

**Open Questions**:
1. Does `VisualizerAudioProcessor` actually receive PCM data on this device/ExoPlayer version? (Task 1.1 answers)
2. Is the user actually on the NowPlaying screen with audio playing (not video)? Video mode uses WebView + different visualizer path.
3. Are the AGSL shaders hitting instruction limits on MT6835? (Task 2.1 compile check answers)

---

## Deliverable

After executing this plan, you will have:
1. `RECORD_AUDIO` granted on device
2. Confirmed PCM → StateFlow data flow working
3. Identified whether AGSL shaders compile/run on MT6835
4. Fixed scene mapping for Batch 1 scenes (AURORA_FLUX, LIQUID_CHROME, NEBULA_BLOOM)
5. Evidence log (`/tmp/visualizer_evidence.log`) showing root cause
6. Visualizer rendering on device (AGSL or Canvas fallback)

---

## Execution Offer

This plan is ready for subagent-driven execution. Each task is independent and verifiable. Would you like me to spawn a subagent to execute Phase 0 → Phase 4 sequentially?
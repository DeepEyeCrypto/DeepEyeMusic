# AGSL Visualizer Studio Skill

Always-on rules and procedural memory for building, extending, and certifying the DeepEyeMusicPro AGSL visualizer system on Android 13+ (API 33+).

## Always-on Rules

1. **Uniform declaration guard**: Never bind a uniform to a shader that does not declare it. On MT6835/Realme RMX3945 (Android 16), binding an undeclared uniform triggers `libhwui` `IllegalArgumentException` → SIGABRT via the "unable to find uniform" native path. The dispatcher must check `AgslScene.declaresMouse` before binding `iMouse`, and skip the legacy `iBass`/`iMid`/`iTreble`/`iPeak`/`iChannel0`/`iColorPrimary`/`iColorSecondary` block for catalog scenes (FLUID_PULSE / AUDIO_FRACTAL / NEON_GRID).

2. **Thermal tier before draw**: The `ThermalGovernor` StateFlow must be collected and its `targetFps` / `audioPostFx` values used to gate the frame loop. On FULL (144 Hz) tier: full post-processing + complex shader. On CAPPED (60 Hz): heavier smoothing to mask frame drops. On THROTTLED (30 Hz): heaviest smoothing, audio post-processing disabled. Tier is re-seeded from `PowerManager.getCurrentThermalStatus` on `ON_RESUME`.

3. **Delta-time clamping**: In `AudioVisualizerStateSmoother.update()`, if `nowNanos - lastNanos` exceeds `MAX_DT_SECONDS` (0.1s), treat `dt` exactly as `MAX_DT_SECONDS`. A negative or NaN `dt` is coerced to `DEFAULT_DT_SECONDS` (1/60s). This prevents backgrounding GC stalls from producing a single huge smoothing jump that reads as a glitch.

4. **First-frame NaN sanitization**: The first-frame snap in `AudioVisualizerStateSmoother.update()` carries raw band values directly into shader uniforms. Sanitize with `sanitize(raw)` before snap: `NaN → 0`, `+Inf → [MAX_BAND]`, `-Inf → 0`. Without this, the very first frame can poison every subsequent smoothing step.

5. **Zero-allocation draw loop**: All per-frame state lives in `remember` holders (`mutableFloatStateOf`, `mutableIntStateOf`) — never re-allocate arrays or objects inside the `Canvas` draw scope. The `AudioFrameInterpolator` and `AudioVisualizerStateSmoother` own their buffers for the instance's lifetime; `reset()` clears them.

6. **iMouse only for declaring scenes**: Only bind `iMouse` for scenes that declare it (`FLUID_PULSE`, `AUDIO_FRACTAL`, `NEON_GRID`). The touch gesture mapping uses `detectDragGestures` with `onDragStart`, `onDrag`, `onDragEnd`, `onDragCancel` — normalized to `(0..1, y-down)` via `viewWidth` / `viewHeight` captured by `onGloballyPositioned`. Never use `onPointerEvent` in this Compose BOM; it is not resolvable.

7. **Shader compilation fallback**: `AgslShaderEngine.compileShader()` wraps `RuntimeShader(agslSource)` in `runCatching`. On failure, log the error with a source preview (first 500 chars) and fall back to the *previous* successfully-bound shader. Never leave the visualizer without an active shader.

8. **No proprietary VVavy code**: Every AGSL source string in `ShaderCatalog` must be an original implementation using only public mathematical techniques (value noise, FBM, SDF, raymarch-style analytic fields). No `vvavy`, `VVavy`, `liquid-carbon-plasma`, `centroid-crystalline-tunnel`, `aura-orb-hyper-geometric`, or `the-infinite-grid` strings may appear. The `ShaderCatalogTest` enforces this via `forbiddenStrings`.

9. **Audio band mapping stability**: `AudioVisualizerStateSmoother` maps raw bands as: `bass ← bands[0]`, `mids ← bands[2]`, `treble ← bands[4]`. If the raw band array is shorter than expected, missing entries default to `0f`. The energy value is `sqrt((bass² + mids² + treble²) / 3)`, clamped `[0,1]`. The beat pulse uses a one-frame transient proxy: `bassDelta = smoothedBass - lastBassForBeat`; if `bassDelta > BEAT_THRESHOLD (0.04)` and positive, pulse energy = `min(1, beatEnergy + bassDelta * BEAT_GAIN (6))`; else decay toward `0` with release τ.

## Pitfalls (with "why")

- **Pitfall: Unbound uniform SIGABRT** — *Why*: MT6835's `libhwui` reads a freed buffer when it cannot locate a uniform name during shader binding. The `AgslScene.declaresMouse` guard and the catalog/legacy dispatch split exist specifically to prevent this.
- **Pitfall: First-frame NaN propagation** — *Why*: The first-frame snap in `AudioVisualizerStateSmoother.update()` carries raw band values directly into shader uniforms without sanitization. `NaN` or `Inf` from a stalled FFT capture thread would survive into every subsequent smoothing step.
- **Pitfall: Thermal-driven frame-drop masking failure** — *Why*: If `ThermalGovernor` tier is not collected before the draw loop, the loop may operate at an uncontrolled FPS, causing the smoothing accumulator to advance by irregular amounts and read as judder rather than smooth decay.
- **Pitfall: onPointerEvent not available in this Compose BOM** — *Why*: Compose BOM `2024.12.01` does not expose `onPointerEvent` as a modifier function; the codebase uses `detectDragGestures` from `androidx.compose.foundation.gestures` instead.

## Procedures

### Spawn a new AGSL scene
1. Add the AGSL source string to `ShaderCatalog` (object `ShaderCatalog` in `AgslShaders.kt`).
2. Add a `SceneInfo` entry to `ShaderCatalog.SCENES` with the source and a human-readable title.
3. Add a test case in `ShaderCatalogTest` that verifies: uniform contract presence, no forbidden strings, valid `half4 main(float2 fragCoord)` signature, and visual distinctness from other scenes.
4. Re-run `:app:testDebugUnitTest --tests "com.deepeye.musicpro.ui.player.visualizer.agsl.ShaderCatalogTest"` to verify all 7 tests pass.

### Update the uniform dispatch in `AgslVisualizer.kt`
1. Add `AgslScene.declaresMouse: Boolean` enum property (scenes that declare `iMouse`).
2. In the `when (scene)` dispatch, add the three catalog scenes (`FLUID_PULSE`, `AUDIO_FRACTAL`, `NEON_GRID`) with their `ShaderBrush` and `RuntimeShader`.
3. After the legacy uniform block, add the catalog uniform block that binds `iAudioBass`, `iAudioMids`, `iAudioTreble`, `iAudioEnergy`, `iBeatPulse`, `iSceneTransition`, `iIntensity`, `iAspect`, and `iMouse` (only if `scene.declaresMouse`).
4. Guard the legacy `iBass`/`iMid`/`iTreble`/`iPeak`/`iChannel0`/`iColorPrimary`/`iColorSecondary` block with `if (!isCatalogScene)` so catalog scenes skip those declarations.
5. Re-run the full suite `:app:testDebugUnitTest` to verify 7/7 tests pass (or 383/383 for the full project).

### Add touch gesture mapping for iMouse
1. In `AgslVisualizer.kt`, add `val mouseX`, `val mouseY`, `val mousePressed`, `val viewWidth`, `val viewHeight` as `remember { mutableFloatStateOf(...) }` / `mutableIntStateOf(...)`.
2. Add `viewWidth` / `viewHeight` capture via `onGloballyPositioned { coords -> ... }` on the `Canvas` modifier.
3. Add `pointerInput(Unit) { detectDragGestures(onDragStart = ..., onDrag = ..., onDragEnd = ..., onDragCancel = ...) }` on the `Canvas` modifier.
4. In `onDragStart`, set `mousePressed = 1f` and normalize `mouseX`, `mouseY` against `viewWidth` / `viewHeight`.
5. In `onDrag`, update `mouseX`, `mouseY` against current pointer position.
6. In `onDragEnd` / `onDragCancel`, set `mousePressed = 0f`.
7. In the uniform dispatch catalog block, bind `iMouse` only if `scene.declaresMouse` is true, using `mouseX.floatValue`, `mouseY.floatValue`, and `if (mousePressed.floatValue >= 0.5f) 1f else 0f`.

### Debug a shader compile failure
1. Run `:app:compileDebugKotlin` and capture the `runCatching` failure log.
2. Inspect `Log.e(TAG, "Shader compilation failed: ..., Source preview: <first 500 chars>")`.
3. Use `AgslShaderEngine.transpileGlslToAgsl(rawGlsl)` to inspect the transpiled output.
4. Apply critical fixes: inject WebGL polyfill header, strip precision specifiers, enforce `float` (not `half`) for intermediates, transpile `texture()` → `iChannel0.eval()`, wrap `mainImage → main` with Y-axis correction.
5. Re-run `:app:compileDebugKotlin` to verify success.

## Support Files (references/)
- `references/agdsl-uniforms.md` — detailed uniform contract per scene
- `references/thermal-tier-policy.md` — ThermalGovernor tier behavior table
- `references/audio-band-mapping.md` — band indexing and energy computation recipe

## Version
1.0.0 — Initial commit. commensurate with AGSL Visualizer Studio v1.0 release.


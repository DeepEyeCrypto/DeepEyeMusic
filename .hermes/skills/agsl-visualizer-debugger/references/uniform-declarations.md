# Uniform Declarations per AGSL Scene

Verified against `AgslShaders.kt`. Each scene declares a stable uniform contract so the dispatcher can bind one set of uniforms regardless of the active scene.

## Core contract (all scenes)
```
uniform float2 iResolution;
uniform float  iTime;
uniform float  iTimeDelta;
uniform float4 iMouse;       // xy = normalized 0..1, zw = pressed state
uniform float  iAudioBass;   // 0..2
uniform float  iAudioMids;   // 0..2
uniform float  iAudioTreble; // 0..2
uniform float  iAudioEnergy; // 0..1
uniform float  iBeatPulse;   // 0..1
uniform float  iSceneTransition; // 0..1 (0 = fully old, 1 = fully new)
uniform float  iIntensity;   // 0.5..1.5
uniform float  iAspect;      // width / height
```

## Scene-specific declarations

### LIQUID_PLASMA / CRYSTAL_TUNNEL / AURA_ORB / CYBER_GRID
Also declare: `iBass, iMid, iTreble, iPeak, iChannel0, iAccentColor, iColorPrimary, iColorSecondary`

### NEON_TRIANGLE_GRID
Same as above minus `iChannel0` and `iAccentColor`

### (no shader declares `iTimeDelta` — that call was removed.)

### FLUID_PULSE / AUDIO_FRACTAL / NEON_GRID (catalog scenes)
Use the full Studio contract above and do **NOT** bind `iBass/iMid/iTreble/iPeak/iChannel0/iColorPrimary` — binding any of those on MT6835 triggers SIGABRT via the "unable to find uniform" native path. Skip the whole legacy block for them; only draw the brush.

### AURORA_FLUX / LIQUID_CHROME / NEBULA_BLOOM (Batch 1 catalog scenes)
Same full catalog contract + `iMouse` where declared. Do NOT bind `iBass/iMid/iTreble/iPeak/iChannel0/iColorPrimary` on MT6835 — SIGABRT.

### DeclaresMouse extension
```kotlin
val AgslScene.declaresMouse: Boolean
    get() = when (this) {
        FLUID_PULSE, AUDIO_FRACTAL, NEON_GRID,
        AURORA_FLUX, LIQUID_CHROME, NEBULA_BLOOM,
        PRISM_TUNNEL, KINETIC_MANDALA, PULSE_GEOMETRY -> true
        else -> false
    }
```

### DeclaresColorUniforms extension
```kotlin
val AgslScene.declaresColorUniforms: Boolean
    get() = when (this) {
        NEBULA_BLOOM, PRISM_TUNNEL,
        KINETIC_MANDALA, PULSE_GEOMETRY -> true
        else -> false
    }
```
# Plan: V4A DSP 10 Premium Presets (5 Deep Bass + 5 Studio/Spatial) & Backend Debug

## Goal
Implement 10 mathematically calibrated ViPER4Android DSP master presets (5 Sub-Bass & Deep Bass presets + 5 Studio, Audiophile, and Spatial presets), purge legacy presets, upgrade preset dispatch logic in `DSPViewModel.kt`, and enhance the `DspDebugCard.kt` developer HUD for real-time telemetry.

## Current Context / Assumptions
- `DSPPreset.kt` currently holds 6 legacy presets (`DEFAULT`, `VIPER_BASS_XHIFI`, `AUDIOPHILE_PURE`, `CYBER_SPATIAL_STAGE`, `WARM_TUBE_ANALOG`, `CLUB_EDM_SUBWOOFER`).
- `DSPPresetSelector.kt` renders preset chips in a horizontal scroll row on `DSPScreen.kt`.
- `DSPViewModel.kt` applies presets via `applyDSPPreset(preset)` and syncs with `DSPEngine`.
- `DspDebugCard.kt` displays debug metrics (Session ID, Route, Gain Budget, Active Modules) when `BuildConfig.DEBUG` is true.

## Deep Research & Audio Engineering Blueprint

### Anti-Clipping & Gain Budget Architecture
ViPER4Android utilizes 64-bit float multi-stage psychoacoustic processing. When boosting sub-bass (+8 to +12 dB) or high clarity (+6 to +8 dB), digital clipping is avoided by:
1. **Pre-Gain Control (PGC):** Negative attenuation headroom (-1.5 dB to -4.0 dB) applied before nonlinear stages (Tube, ViperBass, Dynamic System).
2. **Hard Limiter Safety Ceiling:** Peak threshold at -0.5 dBFS with 4:1 compression ratio.
3. **Harmonic Saturation Compensation:** Triode valve simulation injects musical 2nd-order even harmonics, making sub-bass audible and warm even on smaller drivers without causing intermodulation distortion.

---

### The 10 Master Presets

#### 5 Premium Deep Bass Presets:
1. **`SUBWOOFER_30HZ_INFRA` ("Subwoofer 30Hz Infrasonic")**
   - *Target:* 30Hz–45Hz sub-audible physical vibration, zero vocal muddying.
   - *ViperBass:* Mode = `DYNAMIC`, Freq = 40Hz, Gain = +10.0 dB
   - *Dynamic System:* Mode = `SUBWOOFER`, Strength = 60
   - *ViperClarity:* Mode = `X_HIFI`, Gain = +4.5 dB
   - *PGC Headroom:* -3.5 dB
   - *10-Band EQ:* `[+6.0f, +4.5f, +2.0f, 0f, 0f, 0f, +0.5f, +1.0f, +1.5f, +2.0f]`

2. **`EARTHQUAKE_BASS_MONSTER` ("Earthquake Bass Monster")**
   - *Target:* Heavyweight club EDM, Trap & Phonk chest-thump.
   - *ViperBass:* Mode = `PURE`, Freq = 50Hz, Gain = +12.0 dB
   - *BassBoost:* Enabled, Strength = 450
   - *Tube:* Mode = `TRIODE`, Drive = 15
   - *ViperClarity:* Mode = `NATURAL`, Gain = +5.0 dB
   - *PGC Headroom:* -4.0 dB
   - *10-Band EQ:* `[+7.0f, +5.0f, +3.0f, +1.0f, 0f, 0f, +0.5f, +1.5f, +2.5f, +3.0f]`

3. **`PUNCHY_808_SLAP` ("Punchy 808 & Hip-Hop Kick")**
   - *Target:* Fast transient attack, tight 60Hz–80Hz kick drum impact with zero bloat.
   - *ViperBass:* Mode = `NATURAL`, Freq = 60Hz, Gain = +8.5 dB
   - *Dynamics:* Threshold = -18 dB, Ratio = 3.5:1, Attack = 5ms, Release = 120ms
   - *ViperClarity:* Mode = `OZONE_PLUS`, Gain = +6.0 dB
   - *PGC Headroom:* -2.5 dB
   - *10-Band EQ:* `[+3.0f, +6.0f, +4.0f, 0f, -1.0f, 0f, +1.0f, +2.0f, +2.5f, +2.0f]`

4. **`VELVET_WARM_LOFI_BASS` ("Velvet Warm Lo-Fi Bass")**
   - *Target:* Smooth, thick analog tube-saturated low-end for Lo-Fi, R&B and Soul.
   - *ViperBass:* Mode = `NATURAL`, Freq = 75Hz, Gain = +6.5 dB
   - *Tube:* Mode = `TRIODE`, Drive = 35
   - *Dynamic System:* Mode = `V1`, Strength = 40
   - *ViperClarity:* Mode = `NATURAL`, Gain = +2.0 dB
   - *PGC Headroom:* -2.0 dB
   - *10-Band EQ:* `[+4.0f, +5.0f, +3.5f, +2.0f, +1.0f, 0f, 0f, -0.5f, -1.5f, -2.5f]`

5. **`SUB_HARMONIC_EXCITER` ("Sub-Harmonic Bass Synthesizer")**
   - *Target:* Psychoacoustic synthesis generating upper harmonics of 20Hz–40Hz fundamental sub-bass.
   - *ViperBass:* Mode = `DYNAMIC`, Freq = 55Hz, Gain = +9.0 dB
   - *Dynamic System:* Mode = `V2`, Strength = 75
   - *Field Surround:* Strength = 3, MidImage = 4
   - *ViperClarity:* Mode = `X_HIFI`, Gain = +5.5 dB
   - *PGC Headroom:* -3.0 dB
   - *10-Band EQ:* `[+5.0f, +4.0f, +2.5f, +1.0f, 0f, 0f, +1.0f, +1.5f, +2.0f, +2.5f]`

#### 5 Premium Studio / Audiophile / Spatial Presets:
6. **`AUDIOPHILE_STUDIO_MASTER` ("Audiophile Studio Master")**
   - *Target:* Ultra-flat, phase-coherent studio mastering response with binaural crossfeed.
   - *ViperBass:* Mode = `PURE`, Freq = 40Hz, Gain = +2.0 dB
   - *ViperClarity:* Mode = `OZONE_PLUS`, Gain = +4.0 dB
   - *Crossfeed:* Enabled
   - *Tube:* Mode = `TRIODE`, Drive = 5
   - *PGC Headroom:* -1.0 dB
   - *10-Band EQ:* `[+0.5f, +0.2f, 0f, 0f, 0f, 0f, +0.5f, +0.8f, +1.0f, +1.2f]`

7. **`CRYSTAL_VOCAL_ACOUSTIC_AIR` ("Crystal Vocal & Acoustic Air")**
   - *Target:* Intimate front-stage breathy vocals, acoustic guitar strings and 16kHz airy sparkle.
   - *ViperClarity:* Mode = `X_HIFI`, Gain = +8.0 dB
   - *ViperBass:* Mode = `NATURAL`, Freq = 90Hz, Gain = +3.0 dB
   - *Field Surround:* FieldMidImage = 6 (locks vocal center while widening stage)
   - *Dynamics:* Threshold = -22 dB, Ratio = 2.5:1
   - *PGC Headroom:* -2.0 dB
   - *10-Band EQ:* `[-1.0f, -0.5f, 0f, +1.5f, +3.5f, +4.0f, +3.5f, +4.5f, +6.0f, +7.0f]`

8. **`SPATIAL_3D_HOLOGRAPHIC` ("Spatial 3D Holographic Stage")**
   - *Target:* 360° holographic live concert hall soundstage with HRTF binaural depth.
   - *Field Surround:* Strength = 8, MidImage = 5
   - *Virtualizer:* Enabled, Strength = 650
   - *Reverb:* Preset = `MEDIUM_HALL`, RoomLevel = -600, Decay = 1200ms
   - *ViperBass:* Mode = `DYNAMIC`, Freq = 50Hz, Gain = +5.0 dB
   - *ViperClarity:* Mode = `X_HIFI`, Gain = +6.5 dB
   - *PGC Headroom:* -3.0 dB
   - *10-Band EQ:* `[+2.0f, +1.5f, +1.0f, 0f, 0f, +1.0f, +2.0f, +3.0f, +4.0f, +4.5f]`

9. **`VINTAGE_300B_TRIODE_TUBE` ("Vintage 300B Triode Tube")**
   - *Target:* Legendary vacuum tube amplifier lush midrange, harmonic sweetness and analog warmth.
   - *Tube:* Mode = `TRIODE`, Drive = 52
   - *Dynamic System:* Mode = `V1`, Strength = 55
   - *ViperBass:* Mode = `NATURAL`, Freq = 80Hz, Gain = +4.5 dB
   - *ViperClarity:* Mode = `NATURAL`, Gain = +3.5 dB
   - *Reverb:* Preset = `SMALL_ROOM`, RoomLevel = -1200, Decay = 600ms
   - *PGC Headroom:* -2.5 dB
   - *10-Band EQ:* `[+2.5f, +3.5f, +3.0f, +2.0f, +1.5f, +1.0f, +0.5f, 0f, -1.0f, -2.0f]`

10. **`CINEMA_IMAX_DOLBY_SURROUND` ("Cinema IMAX Dolby Theatre")**
    - *Target:* Cinema dynamic range, explosive LFE sub-rumble, and crystal dialogue.
    - *ViperBass:* Mode = `PURE`, Freq = 35Hz, Gain = +9.5 dB
    - *Loudness:* Enabled, Gain = +4.0 dB, Target = 350 mB
    - *Field Surround:* Strength = 7, MidImage = 7
    - *Dynamics:* Threshold = -26 dB, Ratio = 4:1
    - *ViperClarity:* Mode = `X_HIFI`, Gain = +7.0 dB
    - *PGC Headroom:* -3.5 dB
    - *10-Band EQ:* `[+6.0f, +4.0f, +2.0f, 0f, +1.0f, +2.5f, +2.0f, +3.0f, +4.5f, +5.5f]`

---

## Step-by-Step Implementation Tasks

### Task 1: Create Unit Test Suite for All 10 Presets
- **File**: `app/src/test/java/com/deepeye/musicpro/dsp/DSPPresetTest.kt`
- **Action**: Test each of the 10 presets for anti-clip headroom (`pgcGain <= -1.0f`), valid 10-band EQ array size (10 elements), and non-null descriptions.
- **Verification**: Run `./gradlew testDebugUnitTest --tests "com.deepeye.musicpro.dsp.DSPPresetTest"`.

### Task 2: Rewrite `DSPPreset.kt` with the 10 Presets
- **File**: `app/src/main/java/com/deepeye/musicpro/dsp/model/DSPPreset.kt`
- **Action**: Replace old 6 enum constants with the 10 newly calibrated constants (`requiredRank = 0` for all).

### Task 3: Update `PresetRepository.kt` & `DspModels.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/dsp/data/PresetRepository.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/dsp/model/DspModels.kt`
- **Action**: Sync built-in preset seeding to match the 10 presets.

### Task 4: Upgrade Preset Dispatch in `DSPViewModel.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/dsp/engine/DSPViewModel.kt`
- **Action**:
  - Update `activePreset` default to `DSPPreset.SUBWOOFER_30HZ_INFRA`.
  - In `applyDSPPreset(preset)`:
    - Update `_uiState` params.
    - If `preset.params.eqBands.isNotEmpty()`, update `stagedEqBands` and call `dspEngine.updateEqBands(preset.params.eqBands)`.
    - Persist updated params.

### Task 5: Enhance `DspDebugCard.kt` Developer HUD
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/dsp/components/DspDebugCard.kt`
- **Action**: Add `activePresetName: String` to `DspDebugCard` parameters and display active preset badge, real-time gain headroom meter, and active modules list.

### Task 6: Update `DSPPresetSelector.kt` & `DSPScreen.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/dsp/components/DSPPresetSelector.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/dsp/DSPScreen.kt`
- **Action**: Pass `activePresetName` into `DspDebugCard` and refine preset selector chip cards with vibrant neon borders and badges.

### Task 7: Compile, Deploy & Device Verification
- Execute `./run_deepeye.sh` in background with notify.
- Verify through ADB screencap `dsp_presets_live.png`.

---

## Risks & Tradeoffs
- *Risk:* Old database cache might hold legacy preset names.
  - *Mitigation:* `DSPPreset.entries` is the SSOT for the UI selector; database presets are synced via `PresetRepository.seedBuiltinPresets()`.
- *Risk:* High bass boost causing clipping on low-end hardware.
  - *Mitigation:* Every preset has mandatory PGC pre-attenuation (-1.0 to -4.0 dB) and limiter ceiling (-0.5 dBFS).

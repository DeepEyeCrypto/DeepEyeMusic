# DeepEyeMusicPro — DSP Engine Documentation

## Overview

The V4A DSP Engine is a 14-module audio processing pipeline built on Android's `AudioEffect` API. It attaches to ExoPlayer's audio session and processes audio in real-time.

## Architecture

```
ExoPlayer (audio session)
    │
    ▼
AudioSessionManager (listens for session ID changes)
    │
    ▼
V4AEngine (@Singleton)
    ├── Equalizer (10-band)
    ├── BassBoost
    ├── Virtualizer
    ├── PresetReverb
    ├── LoudnessEnhancer
    ├── DynamicsProcessing*
    ├── Field Surround*
    ├── Convolver (IRS)*
    ├── Tube Simulator*
    ├── Clarity / Exciter*
    ├── HRTF*
    ├── Speaker Protection*
    ├── Noise Gate*
    └── Pre-Gain Control (PGC)
    
    * = Software-implemented (not using system AudioEffect)
```

## Module Reference

### 1. Pre-Gain Control (PGC)
- **Purpose**: Adjusts input gain before any processing
- **Range**: -12 dB to +12 dB
- **Use case**: Normalize volume levels between tracks

### 2. 10-Band Equalizer
- **Bands**: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
- **Range**: -12 dB to +12 dB per band
- **Implementation**: Android `Equalizer` AudioEffect

### 3. Bass Boost
- **Range**: 0–1000 (strength)
- **Implementation**: Android `BassBoost` AudioEffect

### 4. Virtualizer
- **Range**: 0–1000 (strength)
- **Implementation**: Android `Virtualizer` AudioEffect

### 5. Reverb
- **Presets**: None, Small Room, Medium Room, Large Room, Medium Hall, Large Hall, Plate
- **Implementation**: Android `PresetReverb` AudioEffect

### 6. Loudness Enhancer
- **Range**: 0 to +15 dB
- **Implementation**: Android `LoudnessEnhancer` AudioEffect

### 7. Dynamics Processing
- **Compressor**: Threshold (-60 to 0 dB), Ratio (1:1 to 20:1), Attack/Release
- **Limiter**: Threshold, enabled by default at -1 dB

### 8–14. Software Modules
Field Surround, Convolver, Tube Simulator, Clarity, HRTF, Speaker Protection, and Noise Gate are software-implemented for maximum compatibility across Android devices.

## Gain Budget System

The `GainBudget` calculator monitors total accumulated gain across all active modules:

| Risk Level | Headroom   | Color  | Action                    |
|------------|------------|--------|---------------------------|
| SAFE       | > 6 dB     | Green  | No action needed          |
| MODERATE   | 0–6 dB     | Amber  | Warning displayed         |
| DANGER     | < 0 dB     | Red    | Clipping risk, PGC auto-corrects |

## Preset System

- **7 built-in presets**: Flat, Bass Boost, Vocal Clarity, Concert Hall, Night Mode, Headphone Surround, Electronic
- **User presets**: Unlimited save/load/delete
- **Storage**: Room database (`DspDatabase`) with JSON-serialized `DspParams`

## Threading Rules

- All AudioEffect operations run on `Dispatchers.IO`
- UI state updates are collected on Main thread via `StateFlow`
- Session attach/detach is always symmetric to prevent AudioEffect leaks

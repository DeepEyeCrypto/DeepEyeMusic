# 🎵 DeepEye Music Pro

> Premium Android music player with a 14-module Viper4Android-style DSP engine, Jetpack Compose glassmorphic UI, and local library management.

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-brightgreen?style=flat-square&logo=android" alt="Min SDK 26" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-purple?style=flat-square&logo=kotlin" alt="Kotlin 2.0" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-1.7-blue?style=flat-square" alt="Compose" />
  <img src="https://img.shields.io/badge/Architecture-Clean%20%2B%20MVI-orange?style=flat-square" alt="Clean Architecture" />
  <img src="https://img.shields.io/badge/License-Proprietary-red?style=flat-square" alt="License" />
</p>

---

## ✨ Features

### 🎧 Music Player
- **Local library scanning** via MediaStore with Android 8–15 compatibility
- **ExoPlayer / Media3** powered playback with media session support
- **Queue management** with shuffle, repeat (off/all/one), drag-to-reorder
- **Media notification** with playback controls
- **Background playback** via foreground service

### 🔊 V4A DSP Engine (14 Modules)
| Module | Description |
|--------|-------------|
| Pre-Gain Control | Input gain normalization (-12 to +12 dB) |
| 10-Band Equalizer | 31Hz – 16kHz with canvas curve preview |
| Bass Boost | Low-frequency enhancement (0–1000) |
| Virtualizer | Stereo widening (0–1000) |
| Reverb | 6 presets (Small Room → Large Hall) |
| Loudness Enhancer | Output loudness boost (0–15 dB) |
| Dynamics Processing | Compressor + Limiter |
| Field Surround | Spatial audio widening |
| Convolver (IRS) | Impulse response convolution |
| Tube Simulator | Warm analog tube emulation |
| Clarity / Exciter | High-frequency sparkle enhancement |
| HRTF | Head-related transfer function |
| Speaker Protection | Output limiter for speaker safety |
| Noise Gate | Background noise suppression |

- **Gain Budget Meter** — Real-time headroom risk scoring (Safe / Moderate / Danger)
- **7 Built-in Presets** + Unlimited user presets
- **Phase conflict detection** (Surround + Convolver warning)

### 🎨 Premium UI
- **Jetpack Compose** with Material 3 / Material You
- **Glassmorphic GlowCard** components with animated glow borders
- **Audio Visualizer** — Canvas-based FFT bar visualization
- **MarqueeText** — Auto-scrolling long song titles
- **Dark/Light/System** themes with dynamic color support
- **Bottom navigation** with animated visibility

### 📱 Screens
- **Home** — Featured albums carousel, recently added
- **Library** — Songs / Albums / Artists / Genres tabs
- **Now Playing** — Full-bleed album art, blurred background, visualizer
- **Search** — Debounced live search with skeleton loading
- **Playlists** — Create, manage, reorder songs
- **V4A DSP** — All 14 module controls + presets + gain budget
- **Settings** — Theme, audio quality, library management

---

## 🏗️ Architecture

```
UI (Compose) → Domain (Use Cases) → Data (Room + MediaStore)
                                   → DSP (V4AEngine + AudioEffect)
                                   → Player (Media3 + ExoPlayer)
```

- **Clean Architecture** with strict layer separation
- **MVI** pattern with `StateFlow<UiState>` in ViewModels
- **Hilt** dependency injection throughout
- **Room** database as single source of truth
- **DataStore** for settings persistence

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for details.

---

## 🛠️ Build

### Prerequisites
- Android Studio Ladybug (2024.2+)
- JDK 17
- Android SDK 35

### Steps
```bash
git clone https://github.com/deepeye/DeepEyeMusicPro.git
cd DeepEyeMusicPro
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease -Pandroid.injected.signing.store.file=keystore.jks \
  -Pandroid.injected.signing.store.password=*** \
  -Pandroid.injected.signing.key.alias=*** \
  -Pandroid.injected.signing.key.password=***
```

---

## 📦 Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose 1.7 + Material 3 |
| DI | Hilt (Dagger) |
| Database | Room 2.6 |
| Audio | Media3 / ExoPlayer |
| DSP | Android AudioEffect API |
| Preferences | DataStore |
| Image Loading | Coil |
| Async | Kotlin Coroutines + Flow |
| CI/CD | GitHub Actions |

---

## 📄 Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — System architecture
- [`docs/DSP_ENGINE.md`](docs/DSP_ENGINE.md) — DSP engine deep dive
- [`docs/API_CONTRACTS.md`](docs/API_CONTRACTS.md) — Repository & navigation contracts
- [`docs/PLAN.md`](docs/PLAN.md) — Implementation roadmap

---

<p align="center">
  <b>DeepEye Music Pro</b> · deepeye.tech · Build with precision, ship with confidence
</p>

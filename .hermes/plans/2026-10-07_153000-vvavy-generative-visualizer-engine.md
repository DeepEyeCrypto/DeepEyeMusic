# Implementation Plan: VVavy-Grade Generative Multi-Pattern Visualizer Engine

**File:** `.hermes/plans/2026-10-07_153000-vvavy-generative-visualizer-engine.md`  
**Reference:** [VVavy.io Visual=Triangle Engine](https://vvavy.io/app?visual=triangle&source=library&id=aaa_eee_iii)  
**Target Device:** Realme RMX3945 (MediaTek MT6835, Android 16, 60/120Hz)

---

## 🎯 Goal
Upgrade the DeepEyeMusicPro visualizer pipeline with a multi-topology procedural generative engine that dynamically morphs between 5 distinct geometric algorithms (Sierpinski Fractal, Sacred Merkaba, 3D Polyhedron, Orbital Vortex, and Deconstructed Lattice) driven by real-time audio FFT transients and non-repeating random seed mutations.

---

## 📌 Current Context & Problem Statement
* Current `VvavyTriangleVisualizer.kt` only renders a single static 3-triangle subdivision root with fixed rotation speed, causing the visualizer to appear repetitive without emergent random patterns.
* The visualizer lacks procedural topology switching, multi-stage fractal mutation, vertex particle emission on beat drops, and audio-reactive random seed shifts.
* Reference [vvavy.io](https://vvavy.io/app?visual=triangle&source=library&id=aaa_eee_iii) generates continuously evolving geometric structures through algorithmic symmetry shifts, dynamic quadtree depth scaling, vertex displacement noise, and chromatic RGB aberration.

---

## 🏛️ Architecture & Proposed Approach
1. **Multi-Topology Generative Engine (`VvavyTopology.kt`)**: Implements 5 mathematical topology generators (Sierpinski, Merkaba, 3D Polyhedron, Orbital Vortex, and Deconstructed Lattice) with zero-allocation geometry pooling (512 triangles max).
2. **Audio-Driven Pattern Mutator & Morphing Pipeline**: Dynamically interpolates vertex positions and subdivision rules across topology modes on detected beat drops and energy transients using Perlin/Simplex noise seed shifts.
3. **Kinetic FX & Chromatic Aberration Pipeline**: Adds multi-pass neon glow, vertex spark bursts, and high-frequency chromatic aberration (RGB channel separation) on heavy treble/peak transients directly inside hardware-accelerated Compose Canvas & AGSL shaders.

---

## 📋 Step-by-Step Implementation Tasks

### Task 1: Create `VvavyTopology.kt` Domain Model & Procedural Generators
* **Path:** `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/vvavy/VvavyTopology.kt`
* **Purpose:** Define topology modes, vertex data models, and zero-allocation procedural generation algorithms.
* **Code Implementation:**
```kotlin
package com.deepeye.musicpro.ui.player.visualizer.vvavy

import kotlin.math.*
import kotlin.random.Random

enum class VvavyTopologyMode {
    SIERPINSKI_FRACTAL,   // Classic recursive 4-way fractal quadtree
    SACRED_MERKABA,       // Interlocking dual-triangle hexagram with radial expansion
    POLYHEDRON_3D,        // 3D wireframe octahedron/icosahedron projection
    ORBITAL_VORTEX,       // Concentric spiraling logarithmic triangles
    DECONSTRUCTED_LATTICE // Displaced floating vertices reforming on bass drops
}

data class VvavyVertex(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var targetX: Float = 0f,
    var targetY: Float = 0f,
    var targetZ: Float = 0f,
    var u: Float = 0f,
    var v: Float = 0f
)

data class VvavyTriangle(
    val a: VvavyVertex = VvavyVertex(),
    val b: VvavyVertex = VvavyVertex(),
    val c: VvavyVertex = VvavyVertex(),
    var depth: Int = 0,
    var energyBin: Int = 0,
    var alpha: Float = 1f,
    var kickPhase: Float = 0f,
    var kickVx: Float = 0f,
    var kickVy: Float = 0f
)
```

---

### Task 2: Create Unit Test Suite for Multi-Topology Generators (`VvavyTopologyTest.kt`)
* **Path:** `app/src/test/java/com/deepeye/musicpro/ui/player/visualizer/VvavyTopologyTest.kt`
* **Purpose:** Ensure all 5 topology algorithms generate valid vertex boundaries, zero NaN coordinates, and smoothly mutate between modes.
* **TDD Command:**
```bash
./gradlew testDebugUnitTest --tests "com.deepeye.musicpro.ui.player.visualizer.VvavyTopologyTest"
```

---

### Task 3: Implement `VvavyTriangleVisualizer.kt` with Multi-Pattern Engine
* **Path:** `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/VvavyTriangleVisualizer.kt`
* **Key Enhancements:**
  1. **Pattern Mutation Engine:** Switches/interpolates between `VvavyTopologyMode` on beat drops or timer intervals.
  2. **Random Seed Displacement:** Injects pseudo-random displacement noise on triangle vertices weighted by `iBass` and `iMid`.
  3. **Chromatic Aberration Pass:** Renders cyan (RGB -dx) and magenta (RGB +dx) offset passes during treble spikes.
  4. **Spark Particle Bursts:** Spawns glowing particle trails from outer triangle vertices upon transient detection.

---

### Task 4: Upgrade AGSL GPU Shaders with Procedural Multi-Fractal Formulas
* **Path:** `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/agsl/AgslShaders.kt`
* **Key Enhancements:**
  1. Ingest `uniform float iPatternSeed;` and `uniform int iTopologyMode;` into SkSL runtime shaders.
  2. Add procedural noise displacement formulas (Simplex 3D + Voronoi cellular noise) into `LIQUID_PLASMA` and `CRYSTAL_TUNNEL` to generate non-repeating fluid and crystalline geometries.

---

### Task 5: Expose Pattern Selection & Auto-Morphing in Visualizer UI
* **Path:** `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/VisualizerSettingsSheet.kt` & `VisualizerState.kt`
* **Key Enhancements:**
  1. Add "Pattern Morph Mode" toggle (Auto-Morph on Beat vs. Fixed Topology).
  2. Add Chaos/Randomness intensity slider (0.0 to 2.0x).

---

### Task 6: Hardware Verification on Realme RMX3945 (Android 16)
1. **Compilation Check:**
   ```bash
   ./gradlew compileDebugKotlin -x test
   ```
2. **Unit Test Verification:**
   ```bash
   ./gradlew testDebugUnitTest
   ```
3. **APK Build & On-Device Deployment:**
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.deepeye.musicpro/com.deepeye.musicpro.MainActivity
   ```
4. **Logcat & Performance Audit:**
   * Verify 60 FPS VSYNC frame rate (`SurfaceFlinger` steady).
   * Verify zero GC pressure / zero allocations inside Canvas draw loop.
   * Verify audio spectrum reactivity (`VisualizerManager` FFT frames).

---

## ⚠️ Risks & Mitigation Strategies
| Risk | Mitigation |
|---|---|
| Frame drops from geometry re-allocation | Pre-allocate fixed `VvavyTriangle` pools (512 max) and re-use vertices in-place. |
| Harsh/jarring pattern jumps on beat drops | Implement smooth exponential interpolation (`lerp`) across vertex transitions over 300ms. |
| Shader compilation failure on older Adreno/Mali GPUs | Guard AGSL uniform bindings with `runCatching` and fallback to Canvas 2D renderer. |

---

## 🏁 Deliverables
1. `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/vvavy/VvavyTopology.kt`
2. `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/VvavyTriangleVisualizer.kt`
3. `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/agsl/AgslShaders.kt`
4. `app/src/test/java/com/deepeye/musicpro/ui/player/visualizer/VvavyTopologyTest.kt`
5. Verified live on Realme RMX3945 physical device.

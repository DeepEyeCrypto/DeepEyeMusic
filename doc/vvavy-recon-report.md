# VVavy.io Visual Scene Reconnaissance Report
# Extracted: 2026-09-26 from vvavy.io JS bundle (8.7MB)
# For: DeepEyeMusicPro native Android implementation

## SCENE CATALOG (73 unique scene IDs extracted from bundle)

### Geometric / Reactive
1. aura-orb-hyper-geometric  — Concentric glowing orb layers driven by bass/treble
2. beat-drop-grid            — Dot matrix grid pulsing on beat drops
3. spin-contour              — Rotating contour lines
4. glass-facets              — Faceted glass refraction
5. flower-fractal-ripple     — Fractal flower that ripples with audio
6. fractal-symphony-bloom    — Fractal bloom driven by spectral flux

### Triangle / Quadtree (KEY SCENE)
- Rendering: "Adaptive triangle quadtree with four-way subdivision chasing local
  detail while bass, mids, and beats drive depth, drift, edge light, and a held
  drop shuffle that kicks selected triangles outward into brief rotations."
- Subdivision: 4-way (midpoint insertion on each edge)
- Bass → depth extrusion, drift velocity
- Mids → edge glow, subdivision depth, rotation speed
- Beats → shockwave ring, drop-shuffle impulse (kicks triangles outward)
- Treble → chromatic aberration, glitch offset, edge luminance

### Ambient / Fluid
7. cloud-flight              — Volumetric cloud flythrough
8. spectral-fluid-field      — Fluid dynamics driven by spectral data
9. dreamflow                 — Dreamy flowing particles
10. matcha-whisper            — Soft organic movements
11. slow-reggae-portal        — Slow swirling portal
12. liquid-carbon-plasma      — Dark liquid plasma
13. liquid-glass-residue      — Glass-like liquid distortion
14. mercury-core              — Metallic fluid sphere

### Particle
15. drifter                   — Particle drift field
16. echo-halo                 — Concentric echo halos with particles
17. ember-sketch              — Sketched ember particles
18. sonic-relief              — Sound-driven particle relief

### Intense / EDM
19. edm-escapism              — Heavy EDM visual with beat sync
20. bass-punch-camera         — Camera shake on bass punch
21. beat-barrel-roll          — Barrel roll on beat
22. cyber-punk-punch          — Cyberpunk aesthetic with beat reaction
23. dragonspine               — Dragon spine visualization
24. desync-engine             — Desync glitch
25. fel-portal                — Energy portal

### Camera / Capture Effects
26. point-cloud               — 3D point cloud from video
27. reaction-diffusion        — Reaction-diffusion pattern
28. gaussian-capture-clouds   — Gaussian splat capture
29. pixel-time-smear          — Temporal pixel smearing
30. fishbowl-lens-breather    — Lens distortion with breathing

### Retro / CRT
31. crt-physics-engine        — CRT phosphor physics
32. 8bit-showdown             — 8-bit quantized slit-scan

### Post-Effects (composable)
33. bloom-burn                — HDR bloom
34. orbit-drift-camera        — Orbiting camera
35. reactive-mask-router      — Audio-reactive masking
36. chroma-tear               — Chromatic tear
37. edgeflow-pro              — Edge detection flow

## AUDIO DATA PIPELINE (Web Audio API)

### FFT Extraction
- `AnalyserNode.getByteFrequencyData()` — 256-bin uint8 array
- `AnalyserNode.getFloatTimeDomainData()` — float waveform
- Band averaging: `_sumBandAverage(freqData, start, end)` → normalize by /255

### Band Energy Mapping
- `bassEnergy`      → FFT bins [0..5]    (20-150 Hz)
- `lowMidEnergy`    → FFT bins [6..15]   (150-500 Hz)
- `midEnergy`       → FFT bins [16..40]  (500-2kHz)
- `brillianceEnergy` → FFT bins [41..80] (2-8 kHz)
- `spectralFlux`    → frame-to-frame magnitude change

### Beat Detection
- 20-sample history ring buffer
- EMA running average comparison
- `_triggerShockwave()` on bass transient exceeding threshold
- `_triggerDropShuffle()` on held bass drop (duration-aware)

## RENDERING ENGINE

### Architecture: WebGL 2.0 + GLSL Shaders
- 159 `gl_Position` instances (vertex shaders)
- 453 `gl_FragColor` instances (fragment shaders)
- 991 `texture2D` calls (texture sampling)
- 1110 `smoothstep` calls (smooth transitions)
- 284 `noise` functions (procedural noise — Perlin/Simplex)
- 153 `fbm` functions (fractal Brownian motion)

### Key Shader Uniforms (mapped to Android equivalents)
- uBass / uMid / uTreble → frequencyBands[0..4]
- uTime → animateFloat clock
- uResolution → Canvas size
- uBeatPhase → beat detection phase
- uDropAge → time since last drop-shuffle

### Canvas Implementation Notes for Android
1. WebGL shaders → Compose Canvas + Path() + Brush
2. noise/fbm → Kotlin sin/cos approximations (cheaper on mobile GPU)
3. texture2D → Brush.radialGradient / linearGradient
4. smoothstep → Kotlin lerp with clamping
5. Frame budget: 16ms (60fps) — skip noise if MediaTek GPU can't sustain

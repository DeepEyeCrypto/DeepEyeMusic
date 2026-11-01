package com.deepeye.musicpro.ui.player.visualizer

enum class VisualizerSceneId {
    TRIANGLE_REACTIVE,
    SPECTRUM_BARS,
    WAVEFORM,
    RADIAL_PULSE,
    PARTICLE_FIELD,
    LIQUID_PLASMA,
    CRYSTAL_TUNNEL,
    CYBER_GRID,
    NEON_TRIANGLE_GRID,
    FLUID_PULSE,
    AUDIO_FRACTAL,
    NEON_GRID,
    AURA_ORB,
    AURORA_FLUX,
    LIQUID_CHROME,
    NEBULA_BLOOM,
    PRISM_TUNNEL,
    KINETIC_MANDALA,
    PULSE_GEOMETRY
}

data class VisualizerSceneMetadata(
    val id: VisualizerSceneId,
    val title: String,
    val description: String,
    val supportsFft: Boolean,
    val supportsAmplitude: Boolean,
    val supportsReducedMotion: Boolean,
    val tags: Set<String>
)

/**
 * Category rail shown in the visualizer library. Every [VisualizerSceneMetadata.tags]
 * entry must be drawn from this set, otherwise scenes silently disappear from a
 * filtered view.
 */
val VisualizerCategories =
    listOf(
        "All", "Geometric", "Spectrum", "Ambient", "Particle", "3D", "Classic", "Intense", "Waveform"
    )

/**
 * Single source of truth for the scene catalog.
 *
 * Invariant: this list contains exactly one entry per [VisualizerSceneId] member, and
 * every entry has a matching renderer in `VisualizerHost`. Both are enforced by
 * `VisualizerHostTest`; adding an enum member without wiring a renderer here is a
 * compile-time-visible omission, not a silent runtime fallback.
 */
val AvailableVisualizerScenes =
    listOf(
        VisualizerSceneMetadata(
            id = VisualizerSceneId.TRIANGLE_REACTIVE,
            title = "Tumbling Triangle",
            description = "VVavy-inspired 3D audio-reactive quad-tree triangle.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Geometric", "3D", "Intense")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.SPECTRUM_BARS,
            title = "Spectrum Bars",
            description = "Classic frequency spectrum analyzer.",
            supportsFft = true,
            supportsAmplitude = false,
            supportsReducedMotion = true,
            tags = setOf("Spectrum", "Classic")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.WAVEFORM,
            title = "Waveform Ribbon",
            description = "Scrolling oscilloscope trace of the live signal.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Spectrum", "Classic", "Waveform")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.RADIAL_PULSE,
            title = "Radial Pulse",
            description = "Concentric rings driven by bass and treble energy.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Geometric", "Ambient")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.PARTICLE_FIELD,
            title = "Particle Field",
            description = "Orbiting particle swarm reacting to spectral flux.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Particle", "Ambient")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.LIQUID_PLASMA,
            title = "Liquid Plasma (AGSL)",
            description = "Direct GPU-accelerated viscous ferrofluid with Navier-Stokes curl noise.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Ambient", "3D", "Intense")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.CRYSTAL_TUNNEL,
            title = "Crystal Tunnel (AGSL)",
            description = "Raymarched 3D crystalline tunnel with analytical depth fog.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Geometric", "3D", "Intense")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.CYBER_GRID,
            title = "Cyber Synthwave (AGSL)",
            description = "Infinite perspective grid plane with retro sunset horizon.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Spectrum", "Classic", "3D")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.NEON_TRIANGLE_GRID,
            title = "Neon Triangle Grid (AGSL)",
            description = "SDF-based equilateral triangle array driven by real-time audio FFT sampling.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Geometric", "3D", "Intense")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.FLUID_PULSE,
            title = "Fluid Pulse (Studio)",
            description = "Original domain-warped FBM noise field — bass drives scale, mids turbulence, treble shimmer.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Ambient", "3D", "Intense")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.AUDIO_FRACTAL,
            title = "Audio Fractal (Studio)",
            description = "Raymarch-inspired Mandelbrot set — bass depth, mids drift, treble boundary sparkle.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Geometric", "3D", "Intense")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.NEON_GRID,
            title = "Neon Grid (Studio)",
            description = "Perspective tunnel grid with waveform displacement — bass advance, mids line width, treble accents.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Spectrum", "Classic", "3D")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.AURA_ORB,
            title = "Aura Orb (AGSL)",
            description = "Pulsing concentric aura orb with bass-reactive color shift and beat-flash ring.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Ambient", "3D", "Intense")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.AURORA_FLUX,
            title = "Aurora Flux (AGSL)",
            description = "Layered aurora curtain with spectral gradient shift — bass drives wave amplitude, mids color drift, treble shimmer.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Ambient", "3D", "Intense")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.LIQUID_CHROME,
            title = "Liquid Chrome (AGSL)",
            description = "Metallic liquid surface with animated normals — bass ripples the surface, mids modulate metallic sheen, treble highlights.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Ambient", "3D", "Intense")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.NEBULA_BLOOM,
            title = "Nebula Bloom (AGSL)",
            description = "Deep-space nebula bloom — bass expands the gas cloud, mids add color variation, treble adds star sparkle.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Ambient", "3D", "Intense")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.PRISM_TUNNEL,
            title = "Prism Tunnel (AGSL)",
            description = "Raymarched prism tunnel with chromatic aberration — bass drives tunnel depth, mids shift hue, treble adds lens flare.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Geometric", "3D", "Intense")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.KINETIC_MANDALA,
            title = "Kinetic Mandala (AGSL)",
            description = "Symmetric mandala with rotating layers — bass drives rotation speed, mids control layer count, treble adds edge glow.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Geometric", "Ambient", "3D")
        ),
        VisualizerSceneMetadata(
            id = VisualizerSceneId.PULSE_GEOMETRY,
            title = "Pulse Geometry (AGSL)",
            description = "Pulsing geometric field — bass drives the pulse amplitude, mids modulate shape morphing, treble adds vertex highlights.",
            supportsFft = true,
            supportsAmplitude = true,
            supportsReducedMotion = true,
            tags = setOf("Geometric", "3D", "Intense")
        )
    )

/** Human-readable title for a scene, falling back to the enum name if unmapped. */
val VisualizerSceneId.title: String
    get() = AvailableVisualizerScenes.firstOrNull { it.id == this }?.title ?: name

package com.deepeye.musicpro.ui.player.visualizer

enum class VisualizerSceneId {
    TRIANGLE_REACTIVE,
    SPECTRUM_BARS,
    WAVEFORM,
    RADIAL_PULSE,
    PARTICLE_FIELD,
    LIQUID_PLASMA,
    CRYSTAL_TUNNEL,
    CYBER_GRID
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
val VisualizerCategories = listOf(
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
val AvailableVisualizerScenes = listOf(
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
    )
)

/** Human-readable title for a scene, falling back to the enum name if unmapped. */
val VisualizerSceneId.title: String
    get() = AvailableVisualizerScenes.firstOrNull { it.id == this }?.title ?: name

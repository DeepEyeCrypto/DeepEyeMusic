package com.deepeye.musicpro.ui.player.visualizer

enum class VisualizerSceneId {
    TRIANGLE_REACTIVE,
    SPECTRUM_BARS,
    WAVEFORM,
    RADIAL_PULSE,
    PARTICLE_FIELD
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
        tags = setOf("Classic", "Spectrum")
    )
)

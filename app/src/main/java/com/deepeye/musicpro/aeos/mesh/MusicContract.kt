package com.deepeye.musicpro.aeos.mesh

/**
 * AEOS Day-0 Vibe-Coding Genesis
 * Core Domain Logic - Music Intent and State for Phase 4
 */
sealed class MusicIntent {
    object Play : MusicIntent()
    object Pause : MusicIntent()
    object NextTrack : MusicIntent()
    object SimulateChaos : MusicIntent()
}

data class MusicState(
    val currentTrack: String = "No Track", 
    val isPlaying: Boolean = false,
    val isSystemFailure: Boolean = false,
    val errorMessage: String? = null
)

interface MusicContract : CrossDomainContract<MusicIntent, MusicState>

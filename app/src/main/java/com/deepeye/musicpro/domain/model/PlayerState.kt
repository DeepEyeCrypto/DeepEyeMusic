package com.deepeye.musicpro.domain.model

/**
 * Domain model representing the current player state.
 *
 * Emitted by the player layer and observed by UI ViewModels.
 */
data class PlayerState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val position: Long = 0L,          // current playback position in ms
    val duration: Long = 0L,          // total duration of current track in ms
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF,
    val playbackSpeed: Float = 1.0f
)

/**
 * Repeat mode for the player queue.
 */
enum class RepeatMode {
    OFF,      // No repeat — stop after last track
    ONE,      // Repeat current track
    ALL       // Repeat entire queue
}

/**
 * Shuffle mode for the player queue.
 */
enum class ShuffleMode {
    OFF,      // Play in order
    ON        // Shuffled order
}

package com.deepeye.musicpro

import androidx.media3.common.Player
import androidx.media3.common.FlagSet

class TestEvents {
    fun test() {
        val flags = FlagSet.Builder()
            .add(Player.EVENT_IS_PLAYING_CHANGED)
            .add(Player.EVENT_PLAYBACK_STATE_CHANGED)
            .add(Player.EVENT_PLAY_WHEN_READY_CHANGED)
            .build()
        val events = Player.Events(flags)
    }
}

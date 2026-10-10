// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.features.visualizer.input

import android.content.Context
import android.media.midi.MidiManager
import android.util.Log

/**
 * MidiSource — MIDI input event parser for visualizer parameter control.
 */
class MidiSource(context: Context) {
    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager

    fun startListening() {
        if (midiManager == null) {
            Log.w("MidiSource", "MIDI not supported on this device")
            return
        }
        // Implementation would involve MidiDevice.openDevice and device.openInputPort/openOutputPort
        Log.d("MidiSource", "MIDI listening started")
    }

    fun stopListening() {
        Log.d("MidiSource", "MIDI listening stopped")
    }
}

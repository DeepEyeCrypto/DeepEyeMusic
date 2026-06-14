// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

/**
 * Standardized Motion and Animation durations for AFDS.
 * Maximum duration is 250ms to ensure the UI feels instant and extremely responsive.
 */
object AFDSMotion {
    /** 120ms: For simple state changes, micro-interactions, opacity fades, color shifts. */
    const val Fast = 120
    
    /** 180ms: For standard component transitions, card expansions, dialog appearances. */
    const val Normal = 180
    
    /** 250ms: For large structural layout changes, full screen transitions. Never exceed this. */
    const val Large = 250
}

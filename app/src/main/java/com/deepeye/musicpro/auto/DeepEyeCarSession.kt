// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.auto

import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import com.deepeye.musicpro.player.controller.PlayerController

private const val TAG = "AutoSession"

/**
 * Android Auto session.
 *
 * Deliberately NOT annotated `@AndroidEntryPoint`: androidx.car.app.Session is
 * neither an Activity, Service, Fragment, View, nor BroadcastReceiver, so Hilt
 * rejects it as an injection target. The session is instead instantiated by
 * [DeepEyeCarAppService] (which IS a Service and therefore Hilt-eligible) and
 * receives [PlayerController] through the constructor. The singleton is shared
 * with the phone UI, so both surfaces drive one playback engine.
 *
 * Driver-distraction compliance:
 * - Auto renders declarative templates, never Jetpack Compose UI, so there is no
 *   Canvas and no glassmorphism on this surface by construction.
 * - The V4A DSP engine is intentionally unreachable from this package. No screen
 *   here exposes EQ bands, spatial audio, or tuning controls, because those are
 *   text-dense multi-step interactions the distraction guidelines forbid while
 *   the vehicle is moving.
 * - Back navigation is delegated to the head unit's own template affordance
 *   rather than an in-app back button, so the car owns the gesture.
 */
class DeepEyeCarSession(
    private val playerController: PlayerController,
) : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        val destination = intent.getStringExtra(EXTRA_DESTINATION)
        Log.i(TAG, "screen created destination=$destination result=success")
        val carContext: CarContext = carContext
        return if (destination == DESTINATION_QUEUE) {
            QueueScreen(carContext, playerController)
        } else {
            LibraryRootScreen(carContext, playerController)
        }
    }

    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        Log.i(TAG, "car config changed uiMode=${newConfiguration.uiMode}")
        super.onCarConfigurationChanged(newConfiguration)
    }

    companion object {
        const val EXTRA_DESTINATION = "com.deepeye.musicpro.auto.DESTINATION"
        const val DESTINATION_QUEUE = "queue"
    }
}

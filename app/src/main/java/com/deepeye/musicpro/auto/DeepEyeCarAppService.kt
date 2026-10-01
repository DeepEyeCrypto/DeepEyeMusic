// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.auto

import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.deepeye.musicpro.player.controller.PlayerController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "DeepEyeCarAppService"

/**
 * Android Auto entry point.
 *
 * This is a *separate surface* from the phone/tablet app, by design. Auto
 * requires apps to declare a templated UI through [CarAppService] and refuses
 * arbitrary custom views, which is exactly why the glassmorphism Compose tree
 * in `ui/` can never leak into the head unit.
 *
 * Playback itself stays in [com.deepeye.musicpro.player.service.MusicPlayerService]
 * (Media3). Screens mutate [com.deepeye.musicpro.player.controller.PlayerController],
 * the same singleton the phone UI drives, so both surfaces stay in lockstep and
 * the DSP/audio engine is untouched.
 */
@AndroidEntryPoint
class DeepEyeCarAppService : CarAppService() {

    @Inject
    lateinit var playerController: PlayerController

    override fun createHostValidator(): HostValidator =
        // Only the platform-signed Android Auto host may bind. ALLOW_ALL_HOSTS is
        // explicitly rejected in production Play policy, so the default allow-list
        // is used and any non-AAOS host is refused at handshake time.
        HostValidator.Builder(applicationContext).build()

    override fun onCreateSession(): Session = DeepEyeCarSession(playerController)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "car app service created host=${hostInfo?.packageName} result=success")
    }
}

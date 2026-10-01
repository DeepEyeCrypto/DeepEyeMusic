// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.auto

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarText
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Template
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.player.controller.PlayerController
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val TAG = "AutoLibraryScreen"

/**
 * Auto landing screen.
 *
 * Compliance: Auto renders declarative [Template]s, never Compose UI, so there is
 * no Canvas or glassmorphism on this surface by construction. The V4A DSP engine
 * is deliberately unreachable — no row here exposes EQ, spatial audio, or tuning
 * controls, which the distraction guidelines disallow while driving.
 */
class LibraryRootScreen(
    private val carContext: CarContext,
    private val playerController: PlayerController,
) : Screen(carContext) {

    init {
        // invalidate() asks the head unit to re-pull the template. Auto requires
        // this explicit call; it will not observe our state on its own.
        playerController.playerState
            .onEach { state ->
                Log.d(
                    TAG,
                    "state changed playing=${state.isPlaying} queue=${state.queue.size} index=${state.currentIndex} result=success"
                )
                invalidate()
            }
            .launchIn(lifecycle.coroutineScope)
    }

    override fun onGetTemplate(): Template {
        val state = playerController.playerState.value
        return if (state.queue.isEmpty()) emptyTemplate() else libraryTemplate(state)
    }

    private fun emptyTemplate(): ListTemplate =
        ListTemplate.Builder()
            .setTitle("DeepEye Music Pro")
            .setLoading(false)
            .setSingleList(
                ItemList.Builder()
                    .setNoItemsMessage(
                        "Nothing queued yet. Open DeepEye Music Pro on your phone to start listening."
                    )
                    .build()
            )
            .build()

    /**
     * Root template is a [GridTemplate]: album-art tiles are the fastest scannable
     * layout at a glance while driving, and the host enforces a minimum tile size,
     * so touch targets stay compliant without us computing them.
     */
    private fun libraryTemplate(state: PlayerState): GridTemplate {
        val gridBuilder = ItemList.Builder()
            .setOnSelectedListener { index ->
                Log.i(TAG, "grid tile selected index=$index result=success")
                playerController.playQueueItem(index)
            }
            .setNoItemsMessage("Queue is empty.")

        state.queue.forEachIndexed { index, item ->
            gridBuilder.addItem(buildQueueTile(item, index))
        }

        return GridTemplate.Builder()
            .setTitle("DeepEye Music Pro")
            .setLoading(false)
            .setSingleList(gridBuilder.build())
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Queue")
                            .setOnClickListener {
                                Log.i(TAG, "open queue pressed result=success")
                                screenManager.push(QueueScreen(carContext, playerController))
                            }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setTitle(if (state.isPlaying) "Pause" else "Play")
                            .setOnClickListener {
                                Log.i(
                                    TAG,
                                    "toggle pressed playing=${state.isPlaying} result=success"
                                )
                                playerController.togglePlayPause()
                            }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setTitle("Next")
                            .setOnClickListener {
                                Log.i(TAG, "next pressed result=success")
                                playerController.next()
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun buildQueueTile(item: MediaItem, index: Int): GridItem =
        GridItem.Builder()
            .setTitle(CarText.create(item.title))
            .setText(CarText.create(item.artist))
            .setOnClickListener {
                Log.i(TAG, "grid tile tapped index=$index id=${item.id} result=success")
                playerController.playQueueItem(index)
            }
            .build()



}

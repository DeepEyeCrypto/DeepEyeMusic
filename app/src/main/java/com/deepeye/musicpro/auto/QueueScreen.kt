// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.auto

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarText
import androidx.car.app.model.GridItem
import androidx.car.app.model.ItemList
import androidx.car.app.model.Item
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Template
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.player.controller.PlayerController
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val TAG = "AutoQueueScreen"

/**
 * Full play-queue as a [ListTemplate].
 *
 * Rows are [GridItem]s: in androidx.car.app:app 1.2.0 it is the only [Item]
 * implementation (Action is reserved for ActionStrips). The now-playing row is
 * marked with a play glyph so the driver has an unambiguous "where am I" cue.
 */
class QueueScreen(
    private val carContext: CarContext,
    private val playerController: PlayerController,
) : Screen(carContext) {

    init {
        playerController.playerState
            .onEach { invalidate() }
            .launchIn(lifecycle.coroutineScope)
    }

    override fun onGetTemplate(): Template {
        val state = playerController.playerState.value

        val listBuilder = ItemList.Builder()
            .setNoItemsMessage("Queue is empty.")
            .setOnSelectedListener { index ->
                Log.i(TAG, "queue selected index=$index result=success")
                playerController.playQueueItem(index)
            }

        // GridItem is the only Item implementation in androidx.car.app:app 1.2.0
        // (Action is for ActionStrips, not list rows), so rows are GridItems.
        // The now-playing row is prefixed so the driver can tell position at a
        // glance; GridItem exposes no FLAG_PRIMARY equivalent (that is Action-only).
        state.queue.forEachIndexed { index, item ->
            listBuilder.addItem(
                GridItem.Builder()
                    .setTitle(
                        CarText.create(
                            if (index == state.currentIndex) "\u25B6 ${item.title}" else item.title
                        )
                    )
                    .setText(CarText.create(item.artist))
                    .setImage(CarIcon.APP_ICON, GridItem.IMAGE_TYPE_ICON)
                    .setOnClickListener {
                        Log.i(TAG, "queue row tapped index=$index id=${item.id} result=success")
                        playerController.playQueueItem(index)
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("Queue (${state.queue.size})")
            .setLoading(false)
            .setSingleList(listBuilder.build())
            .setActionStrip(
                ActionStrip.Builder()
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
                            .setTitle("Previous")
                            .setOnClickListener {
                                Log.i(TAG, "previous pressed result=success")
                                playerController.previous()
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
}

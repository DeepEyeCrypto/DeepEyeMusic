// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Material3 keeps these metrics on an internal `AlertDialogContent`, so they
// cannot be read from `AlertDialogDefaults`. The values below are the ones
// Material3's alert dialog has shipped with and are restated here so the card
// stays visually close once it is laid out at the app zoom.
private val DialogMinWidth = 280.dp
private val DialogMaxWidth = 560.dp
private val DialogPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp)
private val SlotBottomPadding = 16.dp
private val ButtonsMainAxisSpacing = 8.dp
private val ButtonsCrossAxisSpacing = 4.dp

/**
 * Drop-in replacement for Material3's `AlertDialog` that keeps dialogs on the
 * same scale as the rest of the landscape UI.
 *
 * Every Compose `Dialog` is hosted in its own window, and a dialog window
 * re-provides [androidx.compose.ui.platform.LocalDensity] from the display
 * instead of inheriting the caller's value. Material3's `AlertDialog` is itself
 * a `Dialog` — `javap` on material3 1.3.1 shows it compiling down to
 * `androidx.compose.ui.window.AndroidDialog_androidKt.Dialog(...)` — so it lays
 * its content out at the system density while the screen behind it is
 * composited at [ZoomState.zoom].
 *
 * On the 320dpi landscape target that is density 2.0 behind versus 2.0 * 0.75
 * = 1.5 in front, so an unzoomed `AlertDialog` renders 1.33x too large and
 * overflows the short viewport.
 *
 * This cannot be fixed by wrapping the caller's `title` / `text` / button slots:
 * those are only part of the card, and Material3's own min/max dialog width and
 * padding are read at the unzoomed density, which would leave a system-sized
 * card wrapped around zoom-sized text. The card is therefore rebuilt here from
 * public Material3 tokens so chrome and content are laid out together at the
 * app zoom.
 *
 * Material3's `AlertDialogContent` is `internal`, so `AlertDialog` cannot be
 * composed with a replacement content lambda. [BasicAlertDialog] is public and
 * does take one, and it is the same primitive `AlertDialog` itself delegates
 * to, so this stays behaviourally equivalent apart from the zoom.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
) {
    val buttonContentColor = contentColorFor(containerColor)
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        ProvideAppZoom {
            Surface(
                shape = shape,
                color = containerColor,
                tonalElevation = tonalElevation,
                modifier =
                modifier
                    .widthIn(min = DialogMinWidth, max = DialogMaxWidth)
                    .minimumInteractiveComponentSize(),
            ) {
                Column(modifier = Modifier.padding(DialogPadding)) {
                    if (icon != null) {
                        Box(
                            modifier =
                            Modifier.fillMaxWidth().padding(bottom = SlotBottomPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                                icon()
                            }
                        }
                    }
                    if (title != null) {
                        Box(
                            modifier =
                            Modifier.fillMaxWidth().padding(bottom = SlotBottomPadding),
                        ) {
                            CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                                title()
                            }
                        }
                    }
                    if (text != null) {
                        Box(
                            modifier =
                            Modifier.fillMaxWidth().padding(bottom = SlotBottomPadding),
                        ) {
                            CompositionLocalProvider(LocalContentColor provides textContentColor) {
                                text()
                            }
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                        Arrangement.spacedBy(ButtonsMainAxisSpacing, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(ButtonsCrossAxisSpacing),
                    ) {
                        CompositionLocalProvider(LocalContentColor provides buttonContentColor) {
                            if (dismissButton != null) {
                                dismissButton()
                            }
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}

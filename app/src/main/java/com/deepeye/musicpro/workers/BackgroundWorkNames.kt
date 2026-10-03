// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.workers

/**
 * The unique names of the account-scoped periodic work enqueued by
 * [com.deepeye.musicpro.DeepEyeApp].
 *
 * These are the keys `WorkManager.enqueueUniqueWork` is called with, and
 * therefore the only valid arguments to `cancelUniqueWork`. They are declared
 * once here so that sign-out cannot drift from scheduling: a renamed constant
 * would silently leave the job running against a signed-out user.
 */
object BackgroundWorkNames {
    const val REC_REFRESH = "rec_refresh"
    const val QUEUE_PREFETCH = "queue_prefetch"
    const val CHANNEL_SYNC = "channel_sync"
}
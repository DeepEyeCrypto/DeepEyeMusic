// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.controller

/**
 * Pure functional gate that detects whether incoming Media3 runtime tracks belong to a
 * different media item than the SmartTube resolver catalog currently held in player state.
 *
 * The resolver inventory stored in state is authoritative only for the media item it was
 * resolved for. When Media3 reports tracks that belong to a different item (for example a
 * sparse muxed fallback stream that replaced the expected one), the stale catalog must be
 * reset to a clean/loading state BEFORE [FormatInventoryMergePolicy] runs — otherwise item
 * A's rich format list would be presented as item B's inventory while B is still loading.
 *
 * Media keys are materialized from a media ID plus a coarse playback-position bucket. The
 * position bucket (30s) aligns with the recovery probe's position-restore tolerance, so
 * in-place continuing playback and recovery restores of the SAME media never reset the
 * catalog, while a genuine media-id transition always does (the ID component differs
 * regardless of position bucket).
 */
class FormatInventoryMediaGate {

    private companion object {
        /** Coarse position bucket (ms); recoveries may restore with up to 30s drift. */
        const val POSITION_BUCKET_MS = 30_000L
    }

    /**
     * Materializes a compact, safe media key from a raw media ID.
     *
     * Null/blank media IDs produce a null key (fail-safe: absence of identity evidence can
     * never force a corruption-triggering reset). The returned key is safe to compare and
     * may be logged only via [hashCode] to avoid exposing any media identity.
     */
    fun materializeMediaKey(mediaId: String?, positionMs: Long): String? {
        if (mediaId.isNullOrBlank()) return null
        val bucket = positionMs.coerceAtLeast(0L) / POSITION_BUCKET_MS
        return "$mediaId#$bucket"
    }

    /**
     * Returns true when the incoming runtime tracks belong to a different media item than
     * the authoritative resolver catalog, forcing a stale-inventory reset before merging.
     * Null or blank keys never force a reset (fail safe).
     */
    fun shouldReset(existingMediaKey: String?, incomingMediaKey: String?): Boolean {
        if (existingMediaKey.isNullOrBlank()) return false
        if (incomingMediaKey.isNullOrBlank()) return false
        return existingMediaKey != incomingMediaKey
    }
}
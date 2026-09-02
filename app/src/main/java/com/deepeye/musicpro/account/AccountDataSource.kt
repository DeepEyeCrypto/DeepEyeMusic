// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.account

import kotlinx.coroutines.flow.Flow

/**
 * Minimal abstraction over the underlying account state store.
 *
 * Returns only privacy-safe fields. Access tokens, refresh tokens, email addresses,
 * and raw credentials must NEVER appear in the returned [ConnectedAccountSnapshot].
 *
 * Implementing classes are responsible for:
 * - Deriving a stable, opaque [ConnectedAccountSnapshot.accountKey] (e.g. token hash).
 * - Not exposing raw OAuth state above this interface.
 */
interface AccountDataSource {
    /**
     * Emits [ConnectedAccountSnapshot] when an account is linked, or null when no account
     * is connected. Emits continuously on any account state change.
     */
    val connectedStream: Flow<ConnectedAccountSnapshot?>
}

/**
 * Stable privacy-safe snapshot of a connected account.
 * Contains no credentials, tokens, cookies, or private identifiers.
 */
data class ConnectedAccountSnapshot(
    /** Opaque stable application key derived from account identity (never a raw token). */
    val accountKey: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val channelKey: String? = null,
)

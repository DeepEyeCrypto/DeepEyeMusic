// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.auth

/**
 * Pure OAuth token-expiry math for the YouTube InnerTube device flow.
 *
 * All functions are side-effect free and take the current time as a parameter
 * so unit tests can use a fake clock deterministically. No token values, user
 * identifiers, or expiry values that could identify a session are ever logged
 * from here — callers must log only booleans/enums.
 */
object TokenExpiry {

    /**
     * Safety margin subtracted before treating a token as expired, absorbing
     * client/server clock skew. Configurable per call site for tests.
     */
    const val DEFAULT_SAFETY_MARGIN_SEC = 300L

    /**
     * Upper bound for a sane `expires_in` (90 days). Provider values above this
     * are clamped rather than trusted blindly.
     */
    const val MAX_EXPIRES_IN_SEC = 90L * 24 * 60 * 60

    /**
     * Computes the absolute expiry epoch-second from a provider `expires_in`.
     *
     * @return null when the provider omitted expiry ([expiresInSec] null or
     * non-positive) — callers must then follow the explicit unknown-expiry path
     * (use token as-is; rely on 401 rescue). Never invents an expiry.
     */
    fun computeExpiresAt(nowEpochSec: Long, expiresInSec: Long?): Long? {
        if (expiresInSec == null || expiresInSec <= 0) return null
        val bounded = expiresInSec.coerceAtMost(MAX_EXPIRES_IN_SEC)
        return nowEpochSec + bounded
    }

    /**
     * True when the token should be refreshed before use: expiry is known and
     * now is past `expiry - safetyMargin`. Null (unknown/legacy) expiry returns
     * false — unknown expiry never forces a refresh on its own.
     */
    fun isExpiredOrNear(
        expiresAtEpochSec: Long?,
        nowEpochSec: Long,
        safetyMarginSec: Long = DEFAULT_SAFETY_MARGIN_SEC,
    ): Boolean {
        if (expiresAtEpochSec == null) return false
        val margin = safetyMarginSec.coerceAtLeast(0)
        return nowEpochSec >= expiresAtEpochSec - margin
    }
}

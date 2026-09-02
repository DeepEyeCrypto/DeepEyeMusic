// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.account

/**
 * Functional hook for invalidating cached data tied to a specific account.
 *
 * Used when an account switches or signs out, ensuring that one account's personalized
 * data is never leaked or visible to a subsequent account session.
 *
 * Future phases (e.g. Phase 4 Room personalization cache) will bind implementations.
 */
fun interface AccountScopedCacheInvalidator {
    suspend fun invalidateAccountCache(accountKey: String)
}

// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.auth

/**
 * Explicit authentication/content state for personalized YouTube surfaces
 * (Home, Music, autoplay). Personalized rows are shown only under [Ready] with
 * [PersonalizationStatus.PERSONALIZED]; every other state that carries rows must
 * label them as guest via [GuestFallbackReason]. Generic/guest rows are never
 * presented as personalized.
 */
sealed interface AuthenticatedContentState {

    /** No signed-in account. Only guest/public content may be shown. */
    data object SignedOut : AuthenticatedContentState

    /** Auth state is being determined or a signed-in fetch is in flight. */
    data object Loading : AuthenticatedContentState

    /** Signed-in fetch succeeded; [personalization] says what the rows are. */
    data class Ready(
        val personalization: PersonalizationStatus,
    ) : AuthenticatedContentState

    /** Signed-in fetch failed but guest rows are shown; [reason] is the label. */
    data class GuestFallback(
        val reason: GuestFallbackReason,
    ) : AuthenticatedContentState

    /** Credentials are invalid/revoked; the user must re-authorize. */
    data object ReauthenticationRequired : AuthenticatedContentState

    /** Transient connectivity failure; cached user data must be kept. */
    data object NetworkError : AuthenticatedContentState

    /** Backend reachable but the endpoint contract is unusable. */
    data object ServiceUnavailable : AuthenticatedContentState

    /**
     * The InnerTube `youtubei/v1` surface used here is a private/undocumented
     * interface with no stable authenticated-personalization contract.
     */
    data object UnsupportedPersonalization : AuthenticatedContentState

    /** An authenticated response could not be parsed into rows. */
    data object ParseFailure : AuthenticatedContentState
}

/** What the currently displayed rows actually are. */
enum class PersonalizationStatus {
    /** Rows came from a successful authenticated request. */
    PERSONALIZED,

    /** Rows are guest/public; shown with an explicit guest label. */
    GUEST,

    /** No usable rows; an error/empty state is shown instead. */
    UNAVAILABLE,
}

/** Machine-readable reason a guest fallback is shown. Safe to log and display. */
enum class GuestFallbackReason {
    SIGNED_OUT,
    TOKEN_EXPIRED,
    UNAUTHORIZED,
    REFRESH_UNSUPPORTED,
    NETWORK_UNAVAILABLE,
    EMPTY_PERSONALIZED_RESULTS,
}

/**
 * Typed outcome of an authorized token refresh. Lets callers distinguish
 * "re-authorize now" ([InvalidGrant]) from "keep old state and retry later"
 * ([TransientFailure]) without inspecting secrets or raw HTTP bodies.
 */
sealed interface TokenRefreshOutcome {
    data class Success(val accessToken: String) : TokenRefreshOutcome
    data object NoRefreshToken : TokenRefreshOutcome
    data object TransientFailure : TokenRefreshOutcome
    data object InvalidGrant : TokenRefreshOutcome
}

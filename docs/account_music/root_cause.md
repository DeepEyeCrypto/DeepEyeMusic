# Root Cause — Account-Music Sections

## Why unrelated music appeared
Three compounding defects on the Music tab's FEmusic_home path:

1. **Unauthenticated request permitted (provenance violation).**
   `postMusicBrowse` attached `Authorization` only when a token existed, yet
   `fetchMusicHomeShelves` proceeded with a tokenless request. A tokenless
   FEmusic_home /browse returns HTTP 200 with a **generic guest music feed**
   (popular mixes trending for the region). The parser turned those into
   shelves and MusicViewModel labeled each
   "From your YouTube Music <shelf>" — guest content wearing account provenance.
   Code: InnerTubeRemoteClient.kt (pre-fix 484-486, 646-656), MusicViewModel.kt:112.

2. **Non-auth failures collapsed to silent empty.**
   Any non-401/403 HTTP failure (e.g. the device-observed HTTP 400 refresh/browses)
   made `postMusicBrowse` return `""`; `fetchMusicHomeShelves` returned
   `emptyList()`; the ViewModel rendered zero sections with `globalError = null`.
   No error, no retry affordance, no explanation. Code: 538-544, 650, 121-131.

3. **Auth failures shown as generic error.**
   `InnerTubeAuthException` fell into the generic `catch (e: Exception)` →
   "Unable to load your YouTube Music home", hiding the reconnect action.

## Fixes applied (this round)
- Blank token -> `InnerTubeAuthException` pre-network; no request ever leaves.
- `IOException` -> UNAVAILABLE(NETWORK); blank body -> UNAVAILABLE(INVALID_RESPONSE);
  parse failure -> UNAVAILABLE(INVALID_RESPONSE); residual -> UNAVAILABLE(PROVIDER_UNAVAILABLE).
- `CancellationException` rethrown at every catch on these paths.
- ViewModel: auth -> "Sign in..."; unavailable -> "unavailable..."; valid empty
  shelves -> honest empty (no error).
- `PersonalizedSection.failureReason` added; repo caller returns error-only
  sections with retry rule (`canRetry = reason != EMPTY_ACCOUNT_CONTENT`),
  account-gated (`isAccountRequired = true`).
- Repo `buildYouTubeMusicShelves`: cancellation rethrown; stale builds already
  cancelled by `collectLatest` on the account stream; captured session re-checked.

## Rejected hypotheses
- "Repeat OFF / duplicate filter" — unrelated; loop work settled earlier.
- "Missing RD/RDAMVM radio prefix" — reverted; fabrication caused the /next 400.
- Token expiry as root cause of *content impurity* — expiry causes failure/empty,
  not guest content; the impurity came from permitting the tokenless request.

## Remaining uncertainty
- Exact server-side invalid-field reason for the live HTTP 400 family (no raw
  bodies captured by policy).
- Degree of personalization of device-flow-token FEmusic_home vs the official
  app (report as account-context, not identical).

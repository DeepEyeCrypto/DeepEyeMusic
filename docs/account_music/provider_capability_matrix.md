# Provider Capability Matrix — Account Music Integration

Verified against current tree only. No live probing; no invented browse IDs.

## A. Authenticated transport (evidenced)
- OAuth device flow (RFC 8628) via YouTubeDeviceAuthManager + InnerTubeAuthManager
  (single-flight mutex, persisted expiry, skew margin, bounded 25s refresh,
  invalid_grant-only credential clearing). Source:
  `domain/auth/InnerTubeAuthManager.kt`.
- Bearer injection on ANDROID_MUSIC `/browse` (X-YouTube-Client-Name 67) and
  TVHTML5 `/next`+`/browse` (client name 85). Single bounded 401 refresh+retry;
  401/403 with token -> InnerTubeAuthException. Source:
  `data/source/remote/youtube/InnerTubeRemoteClient.kt`.

## B. Music surfaces (evidenced in code)
| Surface | Request | Auth enforced? | Personalization evidence |
|---|---|---|---|
| FEmusic_home (Music tab) | ANDROID_MUSIC `/browse` browseId=FEmusic_home | YES (this round; previously silent guest) | Shelves carry provider carousel titles ("Listen again", "Quick picks"); 200-vs-empty no longer conflated with auth state |
| Liked / history / playlists / subscriptions | TVHTML5 browse + MusicFilter | token attached when present; blank-token request still possible | Heuristic music classification (`MusicFilter.isMusicTrack`), not a dedicated music surface |
| `/next` radio | TVHTML5 videoId-only | token attached when present | Single-track Up-Next; infinite-queue claim removed (no playlist context fabricated) |

## C. Limitations (no guarantees invented)
- No evidence the device-flow OAuth token yields the same FEmusic_home
  personalization as the official YouTube Music app token; treat results as
  *account-context*, not *identical personalization*.
- HTTP 200 is not proof of personalization; empty/error must surface as
  typed states, never as filler.
- `browseMain FEwhat_to_watch` 401->InnerTubeAuthException observed live on
  device; AuthenticatedYouTubeClient post-refresh retry returned HTTP 400
  live. Same symptom family as the /next 400; browse path untouched this round.
- music.youtube.com private surfaces are undocumented and may change without
  notice; no compatibility guarantees are made.

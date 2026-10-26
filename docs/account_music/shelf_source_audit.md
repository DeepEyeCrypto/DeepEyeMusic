# Shelf Source Audit — Music Sections

HEAD: 6740b663 (main). Current tree only. Synthetic identifiers in examples.

| Shelf | UI owner | Repository | Provider surface | Auth requirement | Cache scope | Fallback (observed) | Provenance | Problem |
|---|---|---|---|---|---|---|---|---|
| Music tab feed | MusicViewModel | InnerTubeRemoteClient.fetchMusicHomeShelves | music.youtube.com `/browse` FEmusic_home, ANDROID_MUSIC client | OAuth Bearer via InnerTubeAuthManager (device-flow) | none (live only) | catch(Exception)->generic error | "From your YouTube Music X shelf" | **D1: token-blank request sent unauthenticated** (guest feed parsed as account shelves); **D2: non-auth failure -> silent empty** (no error state); FIXED this round |
| Personalization Home: YouTube Music shelves | PersonalizationRepositoryImpl.buildYouTubeMusicShelves | AuthenticatedYouTubeClient.getMusicHomeShelves -> same FEmusic_home | same | same | room cache + account invalidation on switch (AccountSessionManager) | catch(Exception)->emptyList, local sections fill the rest | "Live from your connected YouTube Music account" | Missing terminal typed outcome; **FIXED** (error-only section, reason retained, canRetry rule) |
| Home discovery video rails | HomeHubViewModel / VideoRailViewModel | ContentFetcher.search*, getTrendingMusic | TVHTML5 search/trending | none required | none observed | hardcoded seeds + search strings | generic discovery | Separate destination; labels must not claim account provenance (audit-only, untouched) |
| AuthenticatedYouTubeClient music history/likes | various | browseHistory/liked via TVHTML5 + MusicFilter | TV /browse | token from SettingsDataStore | none observed | `search("top hit songs official audio")`, `search("latest hindi songs audio")` on empty (lines 232-253) | mixed | Hardcoded search fallback on signed-in-empty = filler. Audit-only; DO NOT touch this round (separate change, needs product decision) |

## Provenance rules adopted
- `fetchMusicHomeShelves` cannot emit shelves without a token (throws
  InnerTubeAuthException pre-network).
- Every failure/empty outcome on these shelves maps to a typed
  MusicShelfFailureReason + explicit UI state; trending/search/guest
  insertion is prohibited on the `YOUTUBE_MUSIC_SHELF` type.
- Cancellation is never converted to an error outcome on any of these paths.

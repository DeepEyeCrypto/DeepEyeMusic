# SmartTube Personalization Research Manifest

**Research date:** 2026-10-18  
**DeepEye repository:** `/Users/enayat/Documents/DeepEyeMusicPro`  
**DeepEye HEAD:** `6740b663b87dc6a0d948dedfabcd3e9e7e91151a` (`main`, `chore(release): bump version to v3.0.1.95`)  
**SmartTube remote:** `https://github.com/yuliskov/SmartTube`  
**SmartTube inspected SHA:** `ffa16426ea3c7c82077e0f00228818b85f373588`  
**Clone path:** `/tmp/SmartTube-ref`  
**SmartTube root license:** MIT, copyright 2020-present yuliskov.  
**Submodules:** `MediaServiceCore` `7c2451ea9aaff623d53ef1fceaf863a9cfe16cf5`; `SharedModules` `d4f09b11133036d90826baf012e120d06486ae73`. Both initialized at pinned gitlink revisions; nested SharedModules resolves to the same SHA. Submodule root LICENSE files were not present; licenses remain `UNKNOWN`.

## Authorization and actions performed

Phase A was authorized as read-only SmartTube research. SmartTube and pinned submodules were cloned/initialized; source inspection used read-only search and file reads. No SmartTube build, APK install, device action, authenticated network request, credential retrieval, production source change, commit, or push was performed. No production files were modified; only research markdown deliverables were created/updated.

## DeepEye baseline and dirty working tree

Baseline captured before research: HEAD `6740b663...`, branch `main`. The working tree had ten modified production files plus untracked tests/docs. These user changes were preserved and not reset. The relevant modified files include `AuthenticatedYouTubeClient.kt`, `InnerTubeRemoteClient.kt`, `SmartTubeEngine.kt`, `AutoplayRepository.kt`, `ContentFetcher.kt`, `PlayerController.kt`, `QueueManager.kt`, `PersonalizationRepositoryImpl.kt`, `PersonalizedModels.kt`, and `MusicViewModel.kt`.

Read existing docs `youtube_personalization_auth_audit.md`, `youtube_personalization_auth_diagnostics.md`, `same_song_loop_root_cause.md`, `same_song_loop_red_green.md`, `autoplay_repair_baseline.md`. Several cite prior DeepEye commit `baf23a...`; only current dirty source checks are used below for current-tree statements.

## Resolved questions

| Question | Resolution |
|---|---|
| SmartTube SHA | `ffa16426ea3c7c82077e0f00228818b85f373588`, repository HEAD recorded on research date. |
| Submodule access | Both direct submodules fetched at pinned SHAs; nested SharedModules resolved. No source unavailable for inspected paths. |
| Submodule licenses | No root LICENSE in submodules; nested SharedModules third-party components carry separate MIT, Apache-2.0, and EPL-1.0 notices. Original module code license remains UNKNOWN. |
| DeepEye auth classes | UI `YouTubeLoginScreen.kt`; token provider `YouTubeDeviceAuthManager.kt`; expiry-aware coordinator `InnerTubeAuthManager.kt`; token-consuming clients include `AuthenticatedYouTubeClient.kt`, `InnerTubeRemoteClient.kt`, `SmartTubeEngine.kt`. |
| AutoRadioCoordinator | `domain/autoplay/AutoRadioCoordinator.kt` is `NOT FOUND`. Actual coordination paths include `AutoplayRepository`, `PlayerController`, `QueueManager`, `SmartTubeEngine`, `ContentFetcher`. |
| HTTP 400 / same-song | Existing docs correlate prior HTTP 400 with synthetic `RDAMVM<videoId>` and single-item Repeat ALL wrap with repeated current item. Current dirty diff removes the playlist field from SmartTubeEngine and returns null for single-item Repeat ALL. Exact provider error reason remains `UNKNOWN`; H1/H2 probe and current tests were not run. |

## Findings summary

- SmartTube sign-in uses activation/device-code UI. Selected account service produces a bearer header from refresh-token-derived access token and optional branded page ID; refresh header cache is synchronized/time-limited. Full YouTubeAccountManager storage/encryption and account-switch cache behavior remain partial.
- Home and Music are distinct provider browse surfaces with shelf mapping and continuation. They call auth checks; a successful response is not proof of personalization.
- SmartTube parses separate `nextVideoRenderer` and `autoplayVideoRenderer`. Traced app automatic path consumes `nextVideoRenderer` via `MediaItemMetadata.getNextVideo()`. `SuggestionsController.getNext()` gives user queue priority, then applicable radio/playlist successor, local section candidate, then provider next. Repeat ONE restarts; ALL/SHUFFLE advance; LIST stops when no playlist/queue successor. Device success is unverified.
- SmartTube disposes some pending Rx metadata actions on video change and before new loads. Continuation has a single auth-disabled retry on null/empty. These are source-level mitigations, not proof of transport cancellation or all races.
- DeepEye current tree has an expiry-aware mutex/single-flight path in `InnerTubeAuthManager`, but `AuthenticatedYouTubeClient` reads stored token directly and has separate 401 refresh logic. Global cache purge exists; `PersonalizationRepositoryImpl.invalidateAccountCache(accountKey)` is a no-op. Account-specific cache and stale generation protection are not established.
- DeepEye `AutoplayRepository` merges provider candidate plus local related list, so only the provider candidate itself retains provider provenance; subsequent list order includes local results.
- Watch history pause/resume/clear is observed in SmartTube; event-by-event playback reporting and its recommendation effect remain unknown.

## Deliverables

`research_manifest.md`, `source_map.md`, `auth_flow.md`, `home_music_flow.md`, `up_next_autoplay_flow.md`, `history_and_privacy.md`, `license_review.md`, `deepeye_gap_matrix.md`, supplementary `source_pinning.md`.

## Phase A status and remaining items

**PARTIAL.** Required broad flows have source evidence, including repeat-mode branches, queue priority, next renderer consumer, continuation fallback, DeepEye auth split and account-cache no-op. Still incomplete: SmartTube submodule licensing, full YouTubeAccountManager persistence/switch invalidation, exact stale callback/network cancellation race, full DeepEye player transition/manual queue/pause-stop stale fencing, and runtime behavior. No tests/build/device were run. Phase A does not authorize Phase B/C implementation; stop here pending further authorization or review.

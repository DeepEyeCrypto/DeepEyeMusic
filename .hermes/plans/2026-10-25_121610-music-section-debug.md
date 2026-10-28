# Plan: DeepEyeMusicPro — Music Section Systematic Debug Pass

## Goal

One logcat-driven, device-verified debug pass across all Music-tab symptoms on the Realme RMX3945: shelf/feed loading & error/retry states, play/queue behavior, same-song autoplay loop, and duplicate `/next` prefetch — using the 3 in-flight uncommitted fixes already in the working tree.

## Current context / assumptions

- Working tree is dirty (uncommitted) with 3 in-flight fix groups that are the debug targets:
  1. **YT-MUSIC-OMEGA** — `MusicViewModel.kt` switched to `collectLatest` + `suspend` load + typed catches (`InnerTubeAuthException` / `InnerTubeMusicUnavailableException`); new `MusicShelfFailureReason` enum + `InnerTubeMusicUnavailableException` in `InnerTubeRemoteClient.kt`; new `failureReason` field in `PersonalizedModels.kt`.
  2. **FEL-400 / SAME-SONG-LOOP** — `SmartTubeEngine.getAlgorithmicNext(videoId)` now bare contract (playlistId param removed); `QueueManager.next()` yields `null` for single-item queue in `RepeatMode.ALL`; `AutoplayRepository` scrubs cached queue through current/history guard; `ContentFetcher` rethrows `CancellationException`.
  3. **PREFETCH-OMEGA** — `PlayerController` added `prefetchGuard = AtomicBoolean(false)` around `checkAndPrefetchNextTrack()` (100ms poller was firing duplicate `/next` fetches).
- Unit tests already exist for the OMEGA contract: `app/src/test/java/com/deepeye/musicpro/ui/music/MusicViewModelTest.kt` (6 tests: signed-out no-fetch, auth-failure state, network-failure state, honest empty, account-switch fences stale response, cancellation-not-error).
- Also present (untracked, debug-only, NOT part of debug targets — leave alone): `app/src/debug/.../debug/d7/D7StatusProbeReceiver.kt` + manifest receiver, `PersonalizationD5D6LogicTest.kt`, `authenticated client cancellation` + `FEmusicBrowseContractTest` + `InnerTubeMusicHomeContractTest` + `SmartTubeEngineCancellationTest`.
- Device `LZN7EERSZPS4VSUG` is attached (`adb devices`). Package installed is `com.deepeye.musicpro.debug`.
- Known baseline crash in `logcat_filtered.txt` (05-24): `MusicPlayerService` FATAL `ForegroundServiceStartNotAllowedException` at `onStartCommand:224` — Android 16 FGS-start restriction. This is the "playback crash" symptom and is one of the debug targets (see Task 8).
- Build: Gradle daemon lock contention is chronic → all builds run in background with log files, poll `pgrep -f GradleDaemon`.

## Architecture / approach

No new code unless a task proves the on-device log shows a real defect. The debug pass is: (a) get the tree to a clean compiling state, (b) run the OMEGA unit-test suite, (c) install the debug APK and run a scripted device scenario per subsystem while capturing `logcat` with tag filters, (d) assert pass/fail per symptom using the exact log lines each fix already emits (they are intentionally tagged: `MusicViewModel`, `InnerTubeAuth`, `SmartTubeEngine`, `PlayerController`, `AutoplayEngine`, `AUTOPLAY_DEBUG`). Where a log reveals a real defect, fix minimally in the named file and re-verify that one symptom before moving on.

Tag cheat-sheet for logcat grep (all pre-existing in the code):
- `MusicViewModel` → `event=music_home result=...` lines (`reauth_required`, `provider_unavailable`, `shelves=N rendered=M` success line)
- `InnerTubeAuth` → `[YT-MUSIC] FEmusic_home ...` lines (`blocked reason=no_account_token`, `unavailable reason=network|empty_browse_body|parse_failure`, `shelves=N authTokenPresent=true`)
- `SmartTubeEngine` → `Error posting to SmartTube InnerTube endpoint` / bare-contract `/next` calls
- `PlayerController` / `AUTOPLAY_DEBUG` → `event=prefetch_guard_blocked ...` and prefetch-fetched lines
- `AutoplayEngine` → `Queue empty on next(). Triggering personalized autoplay...`, `Emergency fallback generated N valid candidates`, `Autoplay pool completely exhausted. Halting.`

## Step-by-step tasks

### Task 1 — Stabilize the working tree (compile check)
- Run:
  ```bash
  cd /Users/enayat/Documents/DeepEyeMusicPro && ./gradlew compileDebugKotlin -x test > /tmp/gradle_compile.log 2>&1 &
  while pgrep -f GradleDaemon >/dev/null; do sleep 5; done
  grep -E "BUILD SUCCESSFUL|BUILD FAILED|error:" /tmp/gradle_compile.log | head
  ```
- Expected: `BUILD SUCCESSFUL` (or the errors listed). If it fails, read `compile_errors.txt` and fix the named file minimally; re-run until green.
- Do NOT commit anything yet.

### Task 2 — Run the MusicViewModel OMEGA test suite
- Run:
  ```bash
  cd /Users/enayat/Documents/DeepEyeMusicPro && ./gradlew :app:testDebugUnitTest --tests "com.deepeye.musicpro.ui.music.MusicViewModelTest" --no-daemon > /tmp/gradle_vmtest.log 2>&1 &
  ```
- Poll daemon same as Task 1, then:
  ```bash
  grep -E "MusicViewModelTest|tests completed|BUILD" /tmp/gradle_vmtest.log | head
  ```
- Expected: `BUILD SUCCESSFUL`, 6 tests, 0 failures.
- If any fails: that test names the exact contract (e.g. "auth failure shows reconnect state") — the ViewModel code at `loadMusicHomeShelvesSuspend()` catch-blocks is the fix location. Do not edit the test.

### Task 3 — Run the cancellation/contract test suite
- Run:
  ```bash
  cd /Users/enayat/Documents/DeepEyeMusicPro && ./gradlew :app:testDebugUnitTest --tests "com.deepeye.musicpro.data.source.remote.youtube.*" --no-daemon > /tmp/gradle_yttest.log 2>&1 &
  ```
- Expected: `BUILD SUCCESSFUL` (includes `AuthenticatedYouTubeClientCancellationTest`, `FEmusicBrowseContractTest`, `InnerTubeMusicHomeContractTest`, `SmartTubeEngineCancellationTest`, `SmartTubeEngineTest`).
- If `SmartTubeEngineCancellationTest` fails: `SmartTubeEngine.postInnerTube` now has `catch (CancellationException) { throw e }` before the generic `catch (Exception)` — verify the order is correct.

### Task 4 — Build + install the debug APK
- Run:
  ```bash
  cd /Users/enayat/Documents/DeepEyeMusicPro && ./gradlew assembleDebug > /tmp/gradle_apk.log 2>&1 &
  # poll daemon; then:
  adb -s LZN7EERSZPS4VSUG install -r app/build/outputs/apk/debug/app-debug.apk
  adb -s LZN7EERSZPS4VSUG shell am force-stop com.deepeye.musicpro.debug
  ```
- Expected: `BUILD SUCCESSFUL`, `Success` from `adb install`.

### Task 5 — Scenario A: shelf/feed load + error/retry states
Run one fresh logcat capture in the background:
```bash
adb -s LZN7EERSZPS4VSUG logcat -c
adb -s LZN7EERSZPS4VSUG logcat -v time | grep -E "MusicViewModel|InnerTubeAuth|YT-MUSIC|FEmusic" > /tmp/logcat_music.log 2>&1 &
```
Then on device (user, or `adb shell input`):
1. Launch app, go to Music tab with a signed-in account → expect success line:
   ```
   I/MusicViewModel: [YT-MUSIC] FEmusic_home shelves=N rendered=M
   I/InnerTubeAuth: [YT-MUSIC] FEmusic_home shelves=N authTokenPresent=true
   ```
   and `N >= 1`. Verify no `result=reauth_required` / `result=provider_unavailable`.
2. Airplane mode ON, pull-to-refresh → expect exactly:
   ```
   W/InnerTubeAuth: [YT-MUSIC] FEmusic_home unavailable reason=network
   W/MusicViewModel: event=music_home result=provider_unavailable
   ```
   UI shows "YouTube Music is unavailable right now. Pull to refresh." (not blank, not a generic filler).
3. Sign out, then back in (account switch) → expect the first in-flight `FEmusic_home` request cancelled (`collectLatest`), second one succeeds; verify no `FEmusic_home` line from the first account's generation lands (check `shelves=` lines are only from the newer generation).
4. Revoke/expire token path: if reachable, expect `W/MusicViewModel: event=music_home result=reauth_required` and UI "Sign in with your YouTube account...".
- Failure → fix location is `MusicViewModel.loadMusicHomeShelvesSuspend()` catch-blocks or `InnerTubeRemoteClient.fetchMusicHomeShelves()` throw-sites.

### Task 6 — Scenario B: play + queue + same-song loop (FEL)
Continue same logcat capture, add `AutoplayEngine|SmartTubeEngine` to the grep pattern.
On device:
1. Play a single track from a shelf (`playPersonalizedItem`), then let it complete naturally with Autoplay ON.
2. Expected (the SAME-SONG-LOOP fix works): `AutoplayEngine` logs `Queue empty on next(). Triggering personalized autoplay emergency fallback...` followed by `Emergency fallback generated N valid candidates` (N ≥ 1, all candidate ids ≠ current track id) — i.e. a *different* track plays. If instead the same audio silently replays: `QueueManager.next()` `RepeatMode.ALL` branch is not yielding null — check the `if (q.size <= 1) return null` line.
3. Verify no `SmartTubeEngine` `Error posting ... /next` error storm and no duplicate `/next` fetches for the same `videoId` within the same tick (would indicate the bare-contract change regressed).
- Failure → `QueueManager.next()` or `AutoplayRepository.generateNextQueue()` cache-scrub block.

### Task 7 — Scenario C: duplicate prefetch (PREFETCH-OMEGA)
Let playback run ~30s with autoplay on; add `AUTOPLAY_DEBUG` to the grep pattern.
Expected: `event=prefetch_guard_blocked` appears (100ms poller ticks are correctly blocked), and exactly ONE actual prefetch `/next` fetch per 15s window per track (not one per poller tick).
- If unbounded `/next` still appears: `PlayerController.checkAndPrefetchNextTrack()` `compareAndSet`/`set(false)` paths are missing — the guard must be reset at every return path (lines ~1151, 1160, 1175, 1267 in the current file).

### Task 8 — Scenario D: playback crash (FATAL ForegroundServiceStartNotAllowedException)
From `logcat_filtered.txt`, the crash is:
```
FATAL EXCEPTION in MusicPlayerService.onStartCommand (MusicPlayerService.kt:224)
android.app.ForegroundServiceStartNotAllowedException: Service.startForeground() not allowed due to mAllowStartForeground false
```
Reproduce: start playback, then trigger any path that starts `MusicPlayerService` from background (e.g. screen off, or `playMedia` while app backgrounded on Android 16).
Fix location: `app/src/main/java/com/deepeye/musicpro/player/service/MusicPlayerService.kt` — on Android 15+ an FGS must be **started AND its start type + notification declared** correctly, or started while the app is foreground/app-op eligible; the typical Android 16 fix is:
- ensure the `<service>` in `AndroidManifest.xml` declares `android:foregroundServiceType="mediaPlayback"` (verify) and the matching `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission,
- in `MusicPlayerService.kt`, replace the bare `startForeground(id, notification)` with the `ForegroundServiceOptions`/`startForeground(id, notif, foregroundServiceType)` overload OR guard the call so it is only issued when the start was granted `START_FOREGROUND_SERVICE` (check `intent` / allowlist).
- Re-verify: after fix, the same background start no longer throws; `adb logcat -s AndroidRuntime` shows no `FATAL EXCEPTION` during the trigger.

### Task 9 — Final regression + commit
- Re-run Tasks 2 and 3 commands (both green).
- Re-run Task 5 steps 1-3 (no regression on the shelf states).
- Commit the 3 in-flight fix groups + Task 8 fix in one commit (or split logically):
  ```bash
  cd /Users/enayat/Documents/DeepEyeMusicPro
  git add -A
  git commit -m "Music section debug pass: verify OMEGA/FEL/PREFETCH fixes, fix Android16 FGS crash in MusicPlayerService"
  ```
- Deliver to user: a one-line summary per symptom (A/B/C/D) with the exact log line that proves it, no fabricated success — if a symptom's log was not captured, say so explicitly.

## Tests / validation (TDD per task)

Tasks 2-3 ARE the failing→passing test cycle for the OMEGA + cancellation contracts:
- Failing state = any of the 6 `MusicViewModelTest` tests or the `*.youtube.*` tests red.
- Fix = minimal edit in the named source file only.
- Passing = that exact `./gradlew :app:testDebugUnitTest --tests ...` command `BUILD SUCCESSFUL`.
- Device log assertions in Tasks 5-8 are the integration-level "test"; the pass criterion is the exact log line quoted, not "it looks fine".

## Risks, tradeoffs, open questions

- **Working tree already dirty** — 3 fix groups uncommitted. Do not `git stash`; treat current files as the source of truth. If Task 1 compile fails, the tree is mid-edit; finish that edit, don't rewrite.
- **Daemon lock** — every `gradlew` invocation must be backgrounded + polled; never foreground-wait (known 3+ min builds).
- **Token state** — Scenario A step 4 (expired token) may not be reproducible without manually killing the token; if not reachable, mark that sub-check "not exercised" rather than inventing a log.
- **Android 16 FGS** — Task 8's fix is the most invasive; if the manifest already has `mediaPlayback` type, the remaining delta is the API-level start-permission guard. Confirm with `adb shell dumpsys` if unsure which path the system used to deny the start.
- **Open question** — is the `d7` debug receiver (`D7StatusProbeReceiver`) still needed, or can it be dropped before commit? Confirm with user before `git add -A` includes it.

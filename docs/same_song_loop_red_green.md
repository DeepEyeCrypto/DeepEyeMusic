# Same-Song Loop — RED -> GREEN Evidence

## Executed RED (pre-fix state, temporary revert proof — 2026-10-15 20:24)

Method: production code temporarily reverted to the pre-fix observable state
(line-anchored patch with content assertions, PATCH_OK), focused tests run,
originals restored verbatim from backups (RESTORE_EXIT=0), focused tests re-run.

Command (both runs):
`./gradlew :app:testDebugUnitTest --tests 'com.deepeye.musicpro.domain.autoplay.AutoplayRepositoryTest' --tests 'com.deepeye.musicpro.player.queue.QueueManagerRepeatTest' --console=plain`

RED result: RED_EXIT=1
```
AutoplayRepositoryTest > generateNextQueue sends bare videoId contract without fabricated playlistId FAILED
    java.lang.AssertionError at AutoplayRepositoryTest.kt:64
QueueManagerRepeatTest > single-item Repeat ALL must NOT silently replay current (must return null so autoplay resolves next) FAILED
    java.lang.AssertionError at QueueManagerRepeatTest.kt:54
9 tests completed, 2 failed
```
Relevance to real defect:
- AutoplayRepositoryTest.kt:64 — pre-fix code invoked /next with the fabricated
  `playlistId = RDAMVM<videoId>` (the field correlating with the device-observed
  HTTP 400), so coVerify on the bare videoId contract failed.
- QueueManagerRepeatTest.kt:54 — pre-fix `QueueManager.next()` with single-item
  Repeat ALL returned the SAME item via `(0 + 1) % 1 == 0`, i.e. the exact
  same-song replay observed on device.

GREEN result: GREEN_EXIT=0, BUILD SUCCESSFUL, 9/9 tests passed (20:28–20:32).
Restore fidelity: `git diff --stat` after restore identical to pre-proof state
(5 files, 61 insertions, 15 deletions); `git diff --check` exit 0.

## Request-contract correction (HTTP 400)
- Invalid field: fabricated `playlistId = RDAMVM<videoId>` on TVHTML5 `/next`
  (introduced by the earlier OMEGA change; reverted).
- Sanitized server evidence: device logcat `Autoplay /next returned HTTP 400`,
  followed by `autoplay_loop_guard merged=0 ... effective=0` and
  `AutoplayEngine: Prefetch returned empty candidates!`. No raw body captured;
  exact server-side reason string remains UNCERTAIN.
- Minimal correction: playlistId parameter removed from
  `SmartTubeEngine.fetchUpNext/getAlgorithmicNext`; bare provider contract
  (videoId only) at both `/next` call sites in AutoplayRepository and
  ContentFetcher.

## Prior gate history (infrastructure, not behavioral RED)
- First gate-3/4/5 failures: missing Song constructor params (test compile) —
  fixed in test fixture; JSONException from heredoc double-escaping of the
  parser fixture — rebuilt as Kotlin raw string; NPE from Uri.parse under
  plain JUnit — fixed with Robolectric runner. None of these were the defect
  under test.

## Gate results (final)
| Gate | Command | Exit |
|---|---|---|
| 1 KSP | `:app:kspDebugKotlin` | 0 PASS |
| 2 Kotlin | `:app:compileDebugKotlin` | 0 PASS |
| 3 Request-contract tests (4) | focused `--tests AutoplayRepositoryTest` | 0 PASS |
| 4 Repeat/no-replay tests (5) | focused `--tests QueueManagerRepeatTest` | 0 PASS |
| 5 Full suite | `:app:testDebugUnitTest` | 0 PASS — 277 discovered, 273 passed, 0 failures, 0 errors, 4 skipped (CryptoEngineTest 1, YoutubeExtractionTest 3 — pre-existing, files untouched) |
| 6 APK | `:app:assembleDebug` | 0 PASS |
| Post-restore focused re-run | both focused suites | 0 PASS (9/9) |


# RED -> GREEN Evidence — Account Music

## RED-1 (genuine behavioral, pre-fix production)
Command:
`./gradlew :app:testDebugUnitTest --tests
'com.deepeye.musicpro.data.source.remote.youtube.InnerTubeMusicHomeContractTest'`
Result: `blank account token blocks music-home request with no network call
FAILED — java.lang.AssertionError at InnerTubeMusicHomeContractTest.kt:35`
(3 tests completed, 2 failed — same run as RED-2 below).
Cause: production issued the tokenless request (no throw), so `blocked`
stayed false. Invalid-RED excluded: this is an assertion on production
behavior, not a compiler/fixture error (test-compile passed).

## RED-2 (genuine behavioral, pre-fix production)
Command: same run, `--tests 'com.deepeye.musicpro.ui.music.MusicViewModelTest'`
Result: `auth failure shows reconnect state instead of silent empty FAILED —
java.lang.AssertionError at MusicViewModelTest.kt:71`.
Cause: pre-fix catch mapped InnerTubeAuthException to the generic message
("Unable to load your YouTube Music home..."), which does not contain
"Sign in".

## Minimal corrections
- `InnerTubeRemoteClient.fetchMusicHomeShelves`: blank-token `InnerTubeAuthException`
  pre-network; `IOException`->NETWORK, blank body->INVALID_RESPONSE,
  parse failure->INVALID_RESPONSE, residual->PROVIDER_UNAVAILABLE;
  `CancellationException` rethrown everywhere on these paths.
- `MusicViewModel.loadMusicHomeShelves`: CancellationException rethrow first;
  auth->"Sign in..."; unavailable->"unavailable...".
- `PersonalizedSection.failureReason` added; repo caller returns error-only
  sections (`canRetry` by reason; `isAccountRequired=true`).
- No filler added anywhere; both callers compile unchanged.

## GREEN
- Focused suites post-fix: `BUILD SUCCESSFUL` (MusicViewModelTest 6/6,
  InnerTubeMusicHomeContractTest 4/4).
- New coverage (post-fix, reported as coverage not defect-proof):
  network->NETWORK reason; blank body->INVALID_RESPONSE; auth stays off the
  unavailable path; valid-empty->honest empty; account-switch stale discard;
  cancelled load->no fabricated error.

# DeepEye VVavy-Inspired Visualizer Handover

IMPLEMENTATION STATUS: PASS (stability) / NOT VERIFIED (60 fps)

The VVavy-inspired triangle visualizer was independently implemented using
DeepEyeMusicPro's existing VisualizerEngine and audio-session pipeline. No
VVavy source code, assets, branding, or scene code was copied.

Physical playback and visualizer rendering were verified on the connected
device (RMX3945, Android 16 / API 36, 720x1604, 320dpi, 60 Hz active mode) with
zero crashes and zero ANRs. Measured evidence is recorded in
`deepeye_triangle_visualizer_runtime_report.md`.

## Test suite status (verified, 2026-09-25)

The previous revision blamed "non-visualizer related unit test compilation
failures" in `DeepEyeVideoPlayerOverlayTest.kt`. That diagnosis was wrong on both
counts — it was not a compilation failure, and it was not a test bug.

**Root cause:** `app/build.gradle.kts` hardcoded `org.robolectric:robolectric:4.12`
while `gradle/libs.versions.toml` already declared `robolectric = "4.14.1"`. The
hardcoded 4.12 caps at `maxSdkVersion=34`, but the app sets `targetSdk = 35`, so
every `@RunWith(RobolectricTestRunner::class)` class died during initialization
with:

    java.lang.IllegalArgumentException: Package targetSdkVersion=35 > maxSdkVersion=34

**Fix:** switched to the version-catalog alias `libs.robolectric` (4.14.1), which
also removes the duplicated pin. The test class now runs instead of erroring out.

**Result:** 170 tests, 9 failures, 1 skipped.

| Failure set | Count | Status |
| --- | --- | --- |
| `SmartTubePlaybackFormatRepositoryTest` | 2 | Pre-existing, unrelated, unchanged by this fix |
| `DeepEyeVideoPlayerOverlaySemanticsTest` (NPE, `RobolectricIdlingStrategy.android.kt:32`) | 3 | Pre-existing, unrelated, unchanged by this fix |
| `DeepEyeVideoPlayerOverlayTest` (Compose `assertIsDisplayed`) | 4 | Newly *visible* — masked by the SDK error before |

The 4 newly-visible failures are **not introduced** by the Robolectric bump; they
were previously unreachable because the whole class aborted at initialization.
Their root cause is a stale test/source contract: the tests assert on
`Back` / `Search` / `HQ` / `Captions` content descriptions, but
`DeepEyeVideoPlayerOverlay.kt` only exposes `Brightness`, `Volume`, `Seek`, `Zoom`,
`Locked`, `Unlock`, `Close`. `"HQ"` exists solely in
`DeepEyeVideoPlayerOverlayPreview.kt`. Reconciling the tests with the real overlay
is a separate task and was deliberately left out of this change.

Final verdict: visualizer implementation PASS on stability. Performance is NOT
verified as 60 fps — see the runtime report (~48 fps UI-thread cadence, ~19 Hz FFT
feed). Physical D-pad/input verification remains outstanding; the
`gfxinfo` "High input latency" counter (1363) exceeds total frames (1222) and
warrants a dedicated touch-latency pass.

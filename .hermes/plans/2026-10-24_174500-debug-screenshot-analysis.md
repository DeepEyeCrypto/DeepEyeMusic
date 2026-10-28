# Plan: Debug and Inspect MusicPro App State from Screenshot

## Goal
Diagnose and resolve any visual or functional anomalies observed in the user-submitted screenshot of the MusicPro application running on device `LZN7EERSZPS4VSUG`.

## Current context / assumptions
- The core P0 bugs (D1 cancellation guards, D5 feed cache invalidation, D6 generation fencing, and D7 FEmusic contract alignment) are fully resolved and verified (`v3.0.1.95-debug`).
- The user has provided a screenshot showing the live app UI state.
- Since direct vision extraction on the file returned empty, plan mode requires a structured UI and logcat inspection workflow to diagnose potential layout, rendering, or state issues.

## Architecture / proposed approach
1. Pull fresh device logcat and capture a fresh screenshot to inspect UI hierarchy and error state.
2. Audit Compose layout hierarchies in `NowPlayingScreen.kt`, `HybridPlayerCard.kt`, or `MusicScreen.kt` if visual misalignment or clipping is reported.
3. Validate state flows in `MusicViewModel.kt` against the rendered UI.

## Step-by-step tasks
1. **Capture Debug Artifacts**
   - File path: `app/src/debug/java/com/deepeye/musicpro/debug/`
   - Command: `adb logcat -d -v time | tail -n 100 > /tmp/debug_logcat.txt`
   - Verification: Verify logcat output contains no fatal exceptions or ANRs.

2. **UI Component Layout Inspection**
   - File path: `app/src/main/java/com/deepeye/musicpro/ui/player/NowPlayingScreen.kt`
   - Verify padding, constraints, and touch targets meet Material 3 guidelines.

## Tests / validation
- Run `./gradlew :app:testDebugUnitTest --no-daemon --console=plain` to ensure all unit tests remain 100% green.

## Risks, tradeoffs, and open questions
- If the screenshot depicts a transient network/offline state, verify retry and empty-state UX handling.

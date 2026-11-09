# Plan — Debug & fix remaining card-width bug introduced by the 01cb422f unification

Date: 2026-11-08 · Project: `/Users/enayat/Documents/DeepEyeMusicPro` · Branch: `main` (commit `01cb422f`, pushed, tag `v3.0.1.102`)

## Goal

Debug the remaining card-width bug(s) left by the 01cb422f unification commit, make **all 5 content sections render 210px cards in landscape** (YouTube reference), and prove it with real on-device measurement before committing.

## Current context / assumptions

**What 01cb422f changed (verified):** 6 call sites now use `width(CardGeometry.Video.minWidth)` (= 140.dp → **210px** on this device; effective density 240dpi since 140dp×1.5=210px). Grep confirms those 6 sites are clean: `HomeScreen.kt:757`, `VideoRail.kt:155`, `ContinueListeningRow.kt:82`, `ContinueWatchingRow.kt:60`, `HomeHubScreen.kt:580,628`.

**The bug (primary suspect, evidence-based — NOT a guess):**
`grep -rn "105\.dp" app/src/main/` returns **exactly one** remaining hardcoded literal:

```
app/src/main/java/com/deepeye/musicpro/ui/music/MusicScreen.kt:538:                if (isLandscape) 105.dp
```

`105.dp` → **158px** (confirmed by last on-device matrix: Music measured 158×158). The Music section's landscape branch was missed in the 6-site fix: it still hardcodes `105.dp` instead of the SSOT `CardGeometry.Video.minWidth` (140.dp = 210px). This is the section that deviates from the locked 210px reference.

**Other open threads (verify, don't assume):**
- Last device matrix showed **Home cards 210×210** while YouTube/VideoHub = 210×219 — a 9px height delta likely from Home's chunked-row layout (thumbnail-only row + separate text row) vs grid cell. Investigate after fixing Music; only touch if user-visible mismatch remains.
- `YouTubeScreen.kt` shrank 295 lines in the commit (custom card bodies deleted, migrated to NeonMediaCard). Last YouTube dump showed **0 card nodes** after scrolling — need to confirm the grid still renders cards at all (regression risk from the big deletion).
- H1 test flake (`FEmusicBrowseContractTest`, `UncompletedCoroutinesError`) is a **pre-existing** data-layer issue, separate from UI; do not mix into this fix unless it fires in the final full chain.

**Assumptions (stated, will be re-verified in Step 1):** Music cards keep their 1:1 square thumbnail (that's `CardGeometry.Music` geometry) but the *card width* should match Video.minWidth=140dp in landscape for 2-per-row 210px parity with YouTube.

**Device facts:** serial `LZN7EERSZPS4VSUG`, landscape rotation `user_rotation=1`, tab coords Home(60,73) / YouTube(60,165) / VideoHub(59,257) / Music(60,349) / Library(59,441). Install with `adb install -r` only (data preserve — uninstall triggers modal cascade).

## Architecture / proposed approach

The app has a card-geometry SSOT (`ui/theme/CardGeometry.kt`): `Video.minWidth = 140.dp`, `Music.minWidth = 124.dp`. Every 2-column landscape card site must source its width from this SSOT, never a raw dp literal. The fix is one-line per missed site, then the full verify-iterate loop: isolated grep → rebuild → data-preserve install → 5-section uiautomator measurement (expect 210px ±1 in every section that has cards) → full test/lint/assemble chain → commit.

## Step-by-step tasks

### Task 1 — Complete the width-literal inventory (5 min, read-only)

Run:
```bash
cd /Users/enayat/Documents/DeepEyeMusicPro
grep -rn "105\.dp\|124\.dp\|width(1[0-9][0-9]\.dp" --include="*.kt" app/src/main/
```
Expected: ONLY `MusicScreen.kt:538` (`105.dp`). Any new hit beyond that = additional bug site; record it and extend Task 3.
Also confirm the 6 known-good sites still say `CardGeometry.Video.minWidth`:
```bash
grep -rn "width(CardGeometry.Video.minWidth)" --include="*.kt" app/src/main/
```
Expected: 6 lines (the ones listed in context above).

### Task 2 — TDD: write the failing test for Music landscape width (10 min)

File: `app/src/test/java/com/deepeye/musicpro/ui/music/MusicCarouselWidthTest.kt` (new; if a Music UI test file already exists, add the test there instead — check `find app/src/test -name "*Music*" -name "*.kt"` first).

The production code is a local `val` inside `@Composable` body, so test the **width-selection helper**: first read `MusicScreen.kt:525-560` — if `105.dp` is inline in the composable, extract it into an internal helper in `MusicScreen.kt`:

```kotlin
internal fun musicLandscapeCardWidth(isLandscape: Boolean): Dp =
    if (isLandscape) CardGeometry.Video.minWidth else CardGeometry.Music.minWidth
```

Then `MusicScreen.kt:538` becomes:
```kotlin
val cardWidth = musicLandscapeCardWidth(isLandscape)
```
(verify surrounding lines 535–545 first; keep the `else` branch exactly as-is.)

Failing test (write it BEFORE the extraction):
```kotlin
class MusicCarouselWidthTest {
    @Test
    fun `music landscape card width equals video minWidth (210px SSOT)`() {
        assertEquals(CardGeometry.Video.minWidth, musicLandscapeCardWidth(true))
    }

    @Test
    fun `music portrait card width stays music minWidth`() {
        assertEquals(CardGeometry.Music.minWidth, musicLandscapeCardWidth(false))
    }
}
```
Run to verify it fails (helper doesn't exist yet / 105.dp still there):
```bash
./gradlew :app:testDebugUnitTest --tests "com.deepeye.musicpro.ui.music.MusicCarouselWidthTest" 2>&1 | tail -5
```
Expected: compile error (helper missing) or assertion failure (`105.dp != 140.dp`).

### Task 3 — Implement minimal fix (5 min)

Apply the extraction from Task 2 in `MusicScreen.kt` (helper + swap line 538). Add import if missing: `com.deepeye.musicpro.ui.theme.CardGeometry` (it's already imported in MusicScreen — line ~114 uses it; confirm, don't duplicate).
Re-run the test from Task 2 — must PASS:
```bash
./gradlew :app:testDebugUnitTest --tests "com.deepeye.musicpro.ui.music.MusicCarouselWidthTest" 2>&1 | tail -3
```
Commit: `fix(ui): Music landscape cards use CardGeometry.Video.minWidth (210px SSOT), not 105.dp literal`

### Task 4 — Check YouTube grid for regression (10 min, read-only)

```bash
cd /Users/enayat/Documents/DeepEyeMusicPro
grep -n "LazyVerticalGrid\|GridCells\|VideoGridContent" app/src/main/java/com/deepeye/musicpro/ui/youtube/YouTubeScreen.kt
```
Expected: `GridCells.Fixed(2)` still present in both loaded + loading branches (were at lines ~1000/1024 pre-commit). If a branch lost its grid or columns, that's the "YouTube 0 card nodes in dump" cause — fix it before continuing. Record findings.

### Task 5 — Rebuild + data-preserve install (3 min + build time)

```bash
./gradlew :app:compileDebugKotlin -x test        # expected: BUILD SUCCESSFUL
./gradlew :app:assembleDebug                     # expected: BUILD SUCCESSFUL, APK ~68MB
adb -s LZN7EERSZPS4VSUG install -r app/build/outputs/apk/debug/app-debug.apk   # expected: "Success", exit 0
adb -s LZN7EERSZPS4VSUG shell am force-stop com.deepeye.musicpro
adb -s LZN7EERSZPS4VSUG shell am start -n com.deepeye.musicpro/.MainActivity
sleep 14
```
DO NOT use `adb uninstall`. If an "Update" modal appears: tap "Later" (~516,580 in landscape; dump first to locate: `adb -s ... shell uiautomator dump /sdcard/m.xml && adb -s ... pull /sdcard/m.xml /tmp/m.xml`, grep `text="Later"`). Then `adb -s LZN7EERSZPS4VSUG shell settings put system user_rotation 1`.

### Task 6 — 5-section on-device matrix (10 min)

For each section: tap tab → 6 deep swipes → dump → measure. Command per section (Music example):
```bash
DEV=LZN7EERSZPS4VSUG
adb -s $DEV shell input tap 60 349; sleep 2
for i in 1 2 3 4 5 6; do adb -s $DEV shell input swipe 880 520 880 200 350; sleep 0.5; done
adb -s $DEV shell uiautomator dump /sdcard/x.xml && adb -s $DEV pull /sdcard/x.xml /tmp/mus.xml
```
Measurement script (python3, run on /tmp/home.xml /tmp/yt.xml /tmp/vh.xml /tmp/mus.xml /tmp/lib.xml): count content-area boxes (x≥120, 150≤w≤400, 90≤h≤350) and report the dominant `(w,h)` pairs + `views`/duration text lines.
Acceptance table (fill with REAL numbers):

| Section | Card W px | Card H px | Metadata visible | Status |
|---------|-----------|-----------|------------------|--------|
| Home | 210±1 | 219±1 | yes | ? |
| YouTube | 210 | 219 | yes | REFERENCE |
| Video Hub | 210±1 | 219±1 | yes | ? |
| Music | 210±1 | 210±1 (square) | yes | **was 158 → must be 210** |
| Library | empty state OK, else 210±1 | — | — | ? |

Any section ≠ 210px (±1) with cards present → iterate Task 3-style (max 2 rounds) on that section's width site.

### Task 7 — Full chain (≈25 min, background)

```bash
./gradlew :app:testDebugUnitTest   # expected: BUILD SUCCESSFUL, 420/420 (+2 new tests = 422)
./gradlew :app:lintDebug           # expected: BUILD SUCCESSFUL
./gradlew :app:assembleDebug       # expected: BUILD SUCCESSFUL
```
H1 flake (`FEmusicBrowseContractTest` UncompletedCoroutinesError): if it fires, it's the documented pre-existing issue — re-run the isolated class once (`--tests "com.deepeye.musicpro.data.source.remote.youtube.FEmusicBrowseContractTest"`); report either green or "1 pre-existing flake, re-run passed", not a new regression.

### Task 8 — Commit + release (2 min)

Commit the Music fix + test (Task 3 already committed the core; if Task 4 added a fix, separate commit: `fix(youtube): restore/keep Fixed(2) grid` etc.).
Push only on explicit approval (standing rule). Propose optional `v3.0.1.103` release with same APK.

## Tests / validation

TDD cycle executed in Task 2 (failing) → Task 3 (passing) → Task 7 (full suite). Device validation is the final gate: the acceptance table in Task 6 MUST be filled with real dump numbers before "done" is claimed. A fix is not verified by compilation — only by the 210px measurement on `LZN7EERSZPS4VSUG`.

## Risks, tradeoffs, and open questions

1. **Music 210px square vs YouTube 210×219:** Music cards are 1:1 square (`Music.minWidth`=124dp). Forcing 140dp width makes the square thumbnail 140dp = 210px wide → card taller than Video's 219px. This matches "same card width" but not same height. **Open question for user:** does Music need identical 210px *width* (plan assumes YES, per "5 sections = 210px"), or acceptable at 158px square? Default: 210px width.
2. **Home 210×210 vs 210×219:** 9px height delta from Home's chunked Row layout (thumbnail + separate text line). Likely not worth restructuring; measure and report, fix only if user-visible.
3. **YouTube dump empty:** possibly a scrolled-past-the-grid artifact (deepeye swipes 6×), not a regression — Task 4's grep gates this; re-measure with fewer swipes if needed.
4. **Modal cascade on install:** avoided by `install -r` only; if OAuth sheet blocks, tap the left scrim (x < sheet start), never back key.
5. **H1 flake** is orthogonal — do not spend loop budget on it; document, move on.

## Execution order summary

Task 1 (inventory) → Task 2 (failing test) → Task 3 (fix + commit) → Task 4 (YouTube grid check) → Task 5 (build + install) → Task 6 (device matrix, iterate ≤2) → Task 7 (full chain) → Task 8 (commit/push proposal, STOP for approval).

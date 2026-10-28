# Fix Two User-Reported Bugs

## Goal
Fix (A) the "What's new" dialog showing raw markdown + duplicate changelog lines, and (B) the Music tab rendering only 1 of 4 fetched shelves (`rendered=1`).

## Current context / assumptions
- **Device**: Realme RMX3945, Android 16, serial `LZN7EERSZPS4VSUG`.
- **App**: `com.deepeye.musicpro` v3.0.1.97 (latest on device).
- **Bug A evidence** (UI dump from `/sdcard/ui3.xml`):
  - Text node shows literal `**What's new in v3.0.1.97**` with `&#10;` entities and three duplicate `**Full Changelog**` lines — raw Markdown instead of formatted text.
  - File: `app/src/main/java/com/deepeye/musicpro/ui/update/UpdateDialog.kt` line 191: `Text(text = state.releaseNotes, ...)` with no Markdown parsing.
  - The data source: `AutoUpdateManager.kt` line 159 sets `releaseNotes = body` where `body` is the raw GitHub release `body` field.
  - Fix scope: decode HTML entities + parse basic Markdown to plain-ish Compose text. Adding a new dep is risky; prefer a no-dep solution first.
- **Bug B evidence** (logcat capture):
  - `I InnerTubeAuth: [YT-MUSIC] FEmusic_home shelves=4 rendered=1` (MusicViewModel.kt:122).
  - UI dump after dismissing update dialog shows only 1 shelf visible among the 4 fetched.
  - Root cause candidate: `MusicFilter.isMusicTrack` is too aggressive — `PersonalizationRepositoryImpl.kt:803` filters the 4th shelf entirely to empty (`[]`), while 3 shelves survive. Also `MusicViewModel.kt:102` filters with the same function. Need the actual `HomeVideoItem` payloads to confirm, but the `rendered=1` log is the smoking gun.
  - Secondary: device flow tokens get HTTP 400 on `FEmusic_home` (logged as `E InnerTubeAuth: [InnerTube Auth-Rescue] /browse failed for FEmusic_home with HTTP 400`) but the `FEtopics_music` fallback recovers. The 400 is handled already; don't break it.

## Architecture / proposed approach
Two independent fixes in separate commits so a revert on one can't break the other.
- Fix A: strip/decode the release body before passing it to `UpdateDialog`. Smallest change: a no-dep Markdown-lite decoder in `AutoUpdateManager` that handles `**bold**`, `*italic*`, backtick code, `- bullet`, numbered lists, links `[x](url)`, and replaces HTML entities like `&#10;` → newline before rendering.
- Fix B: inspect the actual shelf contents via a read-only logcat snippet and/or add a diagnostic-only debug flag that dumps per-shelf item counts + reasons (exclude/shorts/duration filter) without changing behavior, then fix `isMusicTrack` logic in `MusicFilter.kt` if needed.

## Step-by-step tasks

### Task A1 — Capture the exact release-body shape on device
File: `AutoUpdateManager.kt`
1. Temporarily add a single INFO log at line 159 just before setting `releaseNotes = body`:
   ```kotlin
   Log.i(AUTH_TAG, "[UpdateDebug] rawBodyBytes=${body.length} first500=${body.take(500).replace("\n","\\n")}")
   ```
2. Run: `adb -s LZN7EERSZPS4VSUG shell am force-stop com.deepeye.musicpro && adb -s LZN7EERSZPS4VSUG shell am start -n com.deepeye.musicpro/.MainActivity`
3. Wait 10s, then `adb -s LZN7EERSZPS4VSUG logcat -d | grep UpdateDebug | tail -1`.
4. Expected: a single JSON-ish line showing byte count and first 500 chars of body. **Copy-paste that exact line into the next task**.
5. Verify: after the log, manually dismiss the dialog and confirm you still see raw markdown — proves the bug isn't a transient rendering glitch.
6. Commit: nothing yet (this is diagnostic).

### Task A2 — No-dep Markdown-lite decoder
File: `app/src/main/java/com/deepeye/musicpro/ui/update/MarkdownLiteDecoder.kt` (new)
1. Create the file with:
   ```kotlin
   package com.deepeye.musicpro.ui.update

   object MarkdownLiteDecoder {
       fun decode(markdown: String): String {
           var s = markdown
               .replace("&#10;", "\n")
               .replace("&#xD;", "\n")
               .replace("&amp;", "&")
               .replace("&lt;", "<")
               .replace("&gt;", ">")
               .replace("&quot;", "\"")
               .replace("&#39;", "'")
           // Strip trailing reference links like [1]: url
           s = s.replace("(?m)^\\[\\d+\\]:\\s+.*$".toRegex(), "")
           // Handle bold/italic — keep the text, drop markers
           s = s.replace("\\*\\*(.+?)\\*\\*".toRegex(), "$1")
           s = s.replace("\\*([^\\n]+?)\\*".toRegex(), "$1")
           s = s.replace("__(.+?)__".toRegex(), "$1")
           s = s.replace("`([^`]+?)`".toRegex(), "$1")
           // Drop heading markers (#, ##, ###) but keep the line
           s = s.replace("(?m)^(#{1,6}\\s+)".toRegex(), "")
           // Drop horizontal rules (---, ***, ___) on their own line
           s = s.replace("(?m)^(---|\\*\\*\\*|___)$".toRegex(), "")
           // Normalize bullet markers (-, *, +) at line start
           s = s.replace("(?m)^\\s*[-*+]\\s+".toRegex(), "  - ")
           // Collapse multiple blank lines
           s = s.replace("(?m)\\n{3,}".toRegex(), "\n\n")
           return s.trimEnd()
       }
   }
   ```
2. Run: `cd app && ./gradlew compileDebugKotlin -x test` (or use the project root command as per workspace conventions).
3. Expected output: `BUILD SUCCESSFUL`. If any compile errors, fix them before proceeding — do not ship broken code.

### Task A3 — Wire the decoder into AutoUpdateManager
File: `app/src/main/java/com/deepeye/musicpro/data/source/remote/update/AutoUpdateManager.kt`
1. Add import at top:
   ```kotlin
   import com.deepeye.musicpro.ui.update.MarkdownLiteDecoder
   ```
2. Replace the single line `releaseNotes = body,` with:
   ```kotlin
   releaseNotes = MarkdownLiteDecoder.decode(body),
   ```
3. Also strip the triple duplicate changelog lines in the decode step — they're visible in the decoded text now, so add a small post-process in `decode`:
   ```kotlin
   // Remove duplicate trailing section lines (e.g. Full Changelog repeated 3x)
   val lines = s.lines()
   val deduped = mutableListOf<String>()
   var dupCount = 0
   var lastLine = ""
   for (ln in lines) {
       if (ln == lastLine && ln.isNotBlank()) {
           dupCount++
       } else {
           if (dupCount < 2) {
               deduped.add(lastLine)
           }
           dupCount = 0
           lastLine = ln
           deduped.add(ln)
       }
   }
   if (dupCount < 2) deduped.add(lastLine)
   s = deduped.joinToString("\n")
   ```
   (Insert this just before the final `return s.trimEnd()` in `decode`.)
4. Run: `./gradlew compileDebugKotlin -x test`.
5. Expected: `BUILD SUCCESSFUL`.
6. Install on device and confirm the "What's new" dialog now shows clean text without `**` or `&#10;`.

### Task A4 — Ship a corrected release body on GitHub
1. Go to the GitHub release UI or run:
   ```
   gh release view v3.0.1.97 --json body --jq .body
   ```
2. Edit the body so the last section contains a single "Full Changelog" line and the release description is clean. Commit the fix by pushing any body edit through the GitHub UI (not via CLI to avoid extra commits in our repo).
3. Verify: `gh release view v3.0.1.97 --jq .body` shows exactly one `**Full Changelog**` line.

### Task B1 — Diagnose why `rendered=1` when `shelves=4`
Files: `MusicViewModel.kt`, `MusicFilter.kt`, and optionally `PersonalizationRepositoryImpl.kt:803`.
1. Add a temporary diagnostic log inside `MusicViewModel.loadMusicHomeShelvesSuspend` after line 105 (the `.mapIndexedNotNull` block), logging per-shelf stats:
   ```kotlin
   shelves.forEachIndexed { idx, shelf ->
       val raw = shelf.items
       val filtered = raw.filter { MusicFilter.isMusicTrack(it.title, it.channelName, it.duration) }
           .distinctBy { it.id }.take(20)
       Log.i(TAG, "[YT-MUSIC] shelf=$idx title='${shelf.title}' raw=${raw.size} after_isMusicTrack=${filtered.size} distinct=${filtered.distinctBy { it.id }.size}")
   }
   ```
2. Run: `adb -s LZN7EERSZPS4VSUG shell am force-stop com.deepeye.musicpro && adb -s LZN7EERSZPS4VSUG shell am start -n com.deepeye.musicpro/.MainActivity && sleep 8 && adb -s LZN7EERSZPS4VSUG logcat -d | grep -E "YT-MUSIC.*shelf=" | tail -10`
3. Expected: 4 log lines, one per shelf. Copy the output.
4. Hypothesis check: if 3 shelves have `after_isMusicTrack=0` and the 4th has a positive count, the bug is `isMusicTrack` + `distinctBy` on those shelves. If all 4 are positive but `rendered=1`, the bug is further up in rendering.
5. **Do NOT modify production behavior based on a guess** — commit the diagnostic, wait for the log output, then decide the fix.

### Task B2 — Apply the targeted MusicFilter fix
1. Based on B1's log output, identify the failing condition in `MusicFilter.isMusicTrack`. Likely candidates:
   - Too-many exclusions in `EXCLUDE_PATTERN` (line 8) — e.g. `"kids"`, `"rhymes"`, `"baby"`, `"funny"` — these blanket-filter kids music, bhajans, qawwalis, etc.
   - Duration ceiling at 15 min is filtering long mixes/jukeboxes.
   - `distinctBy { it.id }` collapsing identical IDs across shelves (unlikely but possible).
2. Make the MINIMAL fix. For example, if `"kids"` / `"rhymes"` are killing valid shelves, remove those two words from `EXCLUDE_PATTERN` (keep the pattern structural integrity).
3. Run: `./gradlew compileDebugKotlin -x test` and `./gradlew testDebugUnitTest` — both must pass.
4. Re-run B1's device log sequence and verify `after_isMusicTrack > 0` for all 4 shelves.
5. Expected: `shelves=N rendered=N` in logcat.

### Task B3 — Remove the diagnostic log
1. Revert the `shelf.forEachIndexed { ... }` block added in B1 to leave no extra logging in production code.
2. Run: `./gradlew compileDebugKotlin -x test` one last time.

## Tests / validation
- **A TDD**:
  - Write `app/src/test/java/com/deepeye/musicpro/ui/update/MarkdownLiteDecoderTest.kt` BEFORE changing production.
  - Test cases: raw markdown with `**bold**`, `&#10;`, `&amp;`, duplicate `Full Changelog` lines, headings, inline code.
  - Run: `./gradlew testDebugUnitTest` — verify each test fails first, then passes after A2/A3 land.
  - Commit each file group separately.
- **B TDD**:
  - Write `app/src/test/java/com/deepeye/musicpro/data/source/remote/youtube/MusicFilterRegressionTest.kt` with known-good shelf titles that were previously rejected (e.g. a bhajan, a qawwali, a 20-min jukebox).
  - Run the new test BEFORE fixing B2 — it should fail. Fix B2, run again — it should pass.

## Risks, tradeoffs, and open questions
- **A risk**: the no-dep decoder loses nested formatting (bold inside italics) but that's acceptable for release notes. If a richer render is needed later, swap in `com.mikepenz:markdown-ui` — out of scope here.
- **A open question**: should we also cache the decoded body? No — the body is tiny; decode-on-parse is fine.
- **B risk**: relaxing `isMusicTrack` may surface non-music items into the Music tab. Mitigate by running the regression test in B3 before merging, and only touching the failing patterns.
- **B open question**: the HTTP 400 on `FEmusic_home` is already handled by the `FEtopics_music` fallback — we do NOT touch the rescue path in this task unless B1's logs show the fallback also fails.
- **Commit discipline**: every task above has its own commit boundary. Never combine A and B in one commit.

# Music Card Width Fix Skill

## LOCKED REFERENCE (DO NOT CHANGE)
MusicCardSpec = 210px × 219px (105dp × 109.5dp @320dpi)
- 2 cards/row, 60px gap (30dp)
- Thumbnail: 16:9, 210×118px
- Info section: 210×101px (title 2-line + channel + views·time)
- YouTube section: DO NOT CHANGE (already correct at 210px).

## PROBLEM (EVIDENCE-BASED)
`width(105.dp)` yields 158px on devices with effective 240dpi (not 320dpi).
`105dp × 1.5 = 158px`, but `140dp × 1.5 = 210px` (the YouTube reference).
The `105.dp` literal was the wrong unit — the correct target is `CardGeometry.Video.minWidth` (140dp).

## FIXED WIDTH PATTERN (ALL 5 SECTIONS)
| Section | Before | After |
|---------|--------|-------|
| Music | `105.dp` → 158px | `CardGeometry.Video.minWidth` → 210px |
| Home | `105.dp` → 158px | `CardGeometry.Video.minWidth` → 210px |
| Video Hub | Already `CardGeometry.Video.minWidth` | No change |
| YouTube | Already `CardGeometry.Video.minWidth` | No change |
| Library | `Fixed(2)` grid | No change (different geometry) |

## FIXED ASPECT RATIO
Music cards previously used `1:1` square aspect ratio (`Music.aspectRatio`).
Now use `16:9` aspect ratio (same as YouTube/Video/Hub/Library).
Square source artwork is cropped into the 16:9 box by `ContentScale.Crop`.

## AFFECTED PRODUCTION CODE (6 files)
- `MusicScreen.kt:538` — `if (isLandscape) 105.dp` → `CardGeometry.Video.minWidth`
- `NeonMediaCard.kt:86-92` — MUSIC/COMPACT `aspectRatio` → `Video.aspectRatio` (16:9)
- `LibraryScreen.kt:547` — skeleton `aspectRatio` → `Video.aspectRatio` (16:9)
- `ShimmerBox.kt:207` — skeleton `aspectRatio` → `Video.aspectRatio` (16:9)
- `UnifiedCardMappers.kt` — MUSIC mapper emits `NeonCardType.VIDEO` geometry
- `CardGeometry.kt` — `Music.minWidth` preserved for portrait even-split floor

## AFFECTED TEST FILE
- `MusicCardGeometryContractTest.kt` — 6 contract tests asserting:
  - `musicLandscapeCardWidth(true) == CardGeometry.Video.minWidth`
  - `NeonMediaCardGeometry(NeonCardType.MUSIC).aspectRatio == 16f/9f`
  - `NeonMediaCardGeometry(NeonCardType.VIDEO).aspectRatio == 16f/9f` (regression guard)
  - `NeonMediaCardGeometry(NeonCardType.COMPACT).aspectRatio == 16f/9f`

## TYPICAL WORKFLOW (ONE-SHOT)
1. Diagnose: grep for `105.dp` in MusicScreen.kt → found at line 538
2. Fix: Replace `105.dp` with `CardGeometry.Video.minWidth`
3. Fix: Change MUSIC/COMPACT branch in NeonMediaCard from `Music.aspectRatio` to `Video.aspectRatio`
4. Fix: Update LibraryScreen.kt and ShimmerBox.kt skeleton aspect ratios
5. Fix: Add `musicLandscapeCardWidth()` helper function in MusicScreen.kt
6. Fix: Add `NeonMediaCardGeometry()` helper function
7. Fix: Create MusicCardGeometryContractTest.kt with 6 contract tests
8. Verify: Run tests → FAIL (old values) → PASS (new values)
9. Device matrix: Cold-start → landscape → 5 sections → measure → 210×219px, 16:9
10. Full chain: testDebugUnitTest + lintDebug + assembleDebug → all green

## PITFALLS (avoid these)
- Don't use `105.dp` — it gives 158px on 240dpi devices, not 210px
- Don't keep `1:1` aspect ratio for Music cards — use `16:9` per spec
- Don't forget to update LibraryScreen.kt and ShimmerBox.kt skeletons
- Don't forget to update NeonMediaCard.kt MUSIC/COMPACT branch
- Don't forget to add the `musicLandscapeCardWidth()` and `NeonMediaCardGeometry()` helpers
- Don't forget the test file — it asserts the new behavior

## REFERENCES
- `references/music-card-fix-patterns.md` — Detailed reproduction recipe and decision table
- `references/music-aspect-ratio.md` — 16:9 vs 1:1 rationale and source artwork notes
- `references/music-card-fix-commands.md` — Common command patterns for this fix

SKILL USAGE:
- Load via: `skill_view(name='music-card-width-fix')`
- Use when: Music card width is not 210px, or Music card aspect is not 16:9
- Do NOT load when: Working on hero/now-playing views (excluded: AlbumDetailScreen, NowPlayingScreen)
- Do NOT load when: Working on Music square album art designs (explicitly excluded per spec)

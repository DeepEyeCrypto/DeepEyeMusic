# Test Patterns Reference

## NeonMediaCardTest.kt Structure (18 tests)

### Mapper Blank-Data Rules (6 tests)
1. `homeVideoItem_zeroViewCount_mapsToNull` — viewCount 0 → null
2. `homeVideoItem_blankUploadDate_mapsToNull` — blank uploadDate → null
3. `homeVideoItem_dashUploadDate_mapsToNull` — `—` uploadDate → null
4. `homeVideoItem_zeroDuration_mapsToNull` — duration 0 → null
5. `homeVideoItem_neverDerivesQualityFromIdHash` — qualityLabel must be null
6. `libraryItem_zeroPlayCount_mapsToNull` — playCount 0 → null viewCount

### Composable Blank-Data Hiding (5 tests)
7. `card_withZeroViewsAndNoDuration_hidesBoth` — no "0 views"/"0:00" shown
8. `card_withRealData_showsViewsAndDuration` — real data renders correctly
9. `card_blankChannel_fallsBackToDeepEye` — channel → "DeepEye"
10. `card_avatarFallsBackToIdDerivedYtimg` — avatar contentDescription assertion
11. `card_liveBadge_takesPrecedenceOverQualityLabel` — LIVE suppresses quality
12. `card_noQualityLabel_hidesBadge` — no quality badge when null

### SmartTubeVideoCard Wrapper (1 test)
13. `smartTubeVideoCard_wrapsNeonMediaCard` — wrapper renders title/badge/duration

### Mapper Parsing (5 tests)
14. `videoItem_parsesDurationStringToMs` — "1:05" → 65000 ms
15. `videoItem_parsesHourDurationStringToMs` — "1:02:03" → 3723000 ms
16. `videoItem_blankDuration_mapsToNull` — empty duration → null
17. `playbackHistory_zeroTotalDuration_mapsToNull` — totalDurationMs 0 → null
18. `videoHistory_validDuration_mapsToMs` — durationMs 200000 → 200000

## Assertion Patterns
- **onNodeWithText(text, substring = true).assertExists()** — title/quality/badge presence
- **onNodeWithText(text, substring = true).assertDoesNotExist()** — "0 views", "0:00", quality labels when hidden
- Semantics nodes are asserted by text content; never by index position
- Each test sets its own `MaterialTheme { ... }` block; no shared setContent

---

**Run command**: `./gradlew testDebugUnitTest --tests 'com.deepeye.musicpro.ui.components.NeonMediaCardTest'

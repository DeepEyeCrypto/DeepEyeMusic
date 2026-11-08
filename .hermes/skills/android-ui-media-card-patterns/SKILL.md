---
name: android-ui-media-card-patterns
description: Android UI unified neon media card patterns - class-level skill for reusable card composable patterns
category: android-ui
tags: [android, neon, media-card, unified-card, kotlin]
---
# Android UI — Unified Neon Media Card Patterns

## Always-On Rules
1. Single source of truth: every card surface must feed into NeonMediaCard composable
2. Nullability contract: viewCount null/0 hides views; uploadDate null/blank/— hides date; durationMs null/0 hides badge; qualityLabel real data only
3. Geometry per card-type: VIDEO uses 16:9; MUSIC/COMPACT use 1:1 square with titleMaxLines=1
4. 2-column layout for primary scrollers; chunkedRow for rails
5. LIVE badge takes precedence over qualityLabel; never derive from id hash
6. Blank-data hiding: 0 views → hidden; blank date → hidden; 0 duration → hidden
7. Avatar fallback: ytimg thumbnail derived from video ID

## Procedure
1. Create NeonCardData.kt data model
2. Create NeonMediaCard.kt composable
3. Create UnifiedCardMappers.kt with 7 toNeonCardData() extensions
4. Rewrite card surfaces to delegate to NeonMediaCard
5. Add unit tests in NeonMediaCardTest.kt (18+ tests)
6. Run ./gradlew clean testDebugUnitTest lintDebug assembleDebug
7. Expect 420 tests (402 original + 18 new), 0 failures, 0 errors

## Pitfalls
- Re-using id-hash for quality badges violates real-data-only rule
- Omitting cardType check causes wrong geometry rendering
- Writing viewCount=0 instead of null renders "0 views" breaking hide rule
- Forgetting LIVE-before-quality check shows wrong badge

## User Preferences
- Concise directives: Next, fix kar, add kar
- Device verification on Realme RMX3945 (serial LZN7EERSZPS4VSUG) expected before reporting done
- Reject fabricated success claims — real logcat evidence required
- Git v3.0.1.25 APK is known-good baseline for regression diffing
- Hindi/English mix; terse feedback accepted
- Use background=true for long Gradle builds with /tmp/gradle_build.log polling

## References
See references/mapper-extensions.md for extension signatures
See references/blank-data-rules.md for truth table
See references/quality-badge-policy.md for LIVE-first policy
See references/test-patterns.md for test structure

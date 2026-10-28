# Plan: Music Sections Debug and Diagnostics

## Goal
Diagnose and verify music sections and shelf loading states in `MusicViewModel.kt` and `PersonalizationRepositoryImpl.kt`.

## Current context / assumptions
- `FEmusic_home` and `FEmusic_liked` contracts are aligned (`v3.0.1.95-debug`).
- We need a dedicated debugging plan for music sections to ensure robust logging and telemetry without exposing credentials.

## Architecture / proposed approach
1. Inspect section diagnostics in `PersonalizationRepositoryImpl.kt` and `SectionDiagnostics.kt`.
2. Add safe, redacted diagnostics logging for section parsing and item counts.
3. Verify via unit tests.

## Step-by-step tasks
1. **Inspect Section Diagnostics**
   - File path: `app/src/main/java/com/deepeye/musicpro/domain/personalization/SectionDiagnostics.kt`
   - Review diagnostic flags and logging output.

2. **Run Focused Unit Tests**
   - Command: `./gradlew :app:testDebugUnitTest --tests com.deepeye.musicpro.data.repository.PersonalizationD5D6LogicTest --no-daemon --console=plain`
   - Verification: Exit code 0.

## Tests / validation
- Execute unit tests for personalization logic and repository contracts.

## Risks, tradeoffs, and open questions
- None.

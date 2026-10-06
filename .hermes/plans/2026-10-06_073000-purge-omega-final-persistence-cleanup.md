# Plan: Purge-Omega Protocol (Final Cleanup of Local Persistence & Sync Bloat)

## Goal
Permanently delete all remaining on-device personalization data structures, synchronization workers, and repository layers to ensure 100% reliance on YouTube TVHTML5 native personalization.

## Current Context
- algorithmic recommendation engines (RecommendationEngine, ScoringEngine, DiversityRanker, AutoplayScorer, etc.) have successfully been deleted in previous steps (v3.0.1.72).
- Remaining local storage and sync infrastructure needs total excision: `TasteProfile`, `CloudSyncManager`, `AccountPersonalizationCache`, and `PersonalizedSectionDao`.

## Architecture / Proposed Approach
- Remove all remaining persistence DAOs, shared preferences/DataStore managers, and synchronization workers.
- Scrub the Hilt dependency injection modules of these obsolete definitions.
- Refactor top-level UI ViewModels and the `PlayerController` to remove dependencies on `TasteProfileRepository` and `CloudSyncManager`.

## Step-by-step Execution Plan

### Phase 1: Persistence Layer Excision
1. **Delete File Assets**:
   - `rm app/src/main/java/com/deepeye/musicpro/data/prefs/TasteProfileDataStore.kt`
   - `rm app/src/main/java/com/deepeye/musicpro/data/cache/AccountPersonalizationCache.kt`
   - `rm app/src/main/java/com/deepeye/musicpro/data/cache/dao/PersonalizedSectionDao.kt`
   - `rm app/src/main/java/com/deepeye/musicpro/domain/sync/CloudSyncManager.kt`
   - `rm app/src/main/java/com/deepeye/musicpro/domain/sync/CloudRestoreManager.kt`
   - `rm app/src/main/java/com/deepeye/musicpro/domain/repository/TasteProfileRepository.kt`
   - `rm app/src/main/java/com/deepeye/musicpro/data/repository/TasteProfileRepositoryImpl.kt`

2. **Clean Database Layer**:
   - `patch app/src/main/java/com/deepeye/musicpro/data/db/AppDatabase.kt` -> REMOVE `abstract fun personalizedSectionDao(): PersonalizedSectionDao`

### Phase 2: Dependency Injection & Architecture Cleanups
1. **Clean Modules**:
   - `patch app/src/main/java/com/deepeye/musicpro/di/DatabaseModule.kt` -> REMOVE `providePersonalizedSectionDao`.
   - `patch app/src/main/java/com/deepeye/musicpro/di/RepositoryModule.kt` -> REMOVE `bindTasteProfileRepository`.
   - `patch app/src/main/java/com/deepeye/musicpro/di/PersonalizationModule.kt` -> REMOVE `AccountPersonalizationCache` injection.

2. **Clean ViewModels**:
   - For `PlayerViewModel.kt`, `SettingsViewModel.kt`, `SearchViewModel.kt`, `HomeHubViewModel.kt`, `AIRadioEngine.kt`:
     - Remove `TasteProfileRepository` and `CloudSyncManager` constructor dependencies.
     - Remove all usages of `.recordFeedback`, `getTasteProfile`, `syncTasteProfile` calls.

### Phase 3: Verification & Build
1. **Verify Compilation**:
   - `./gradlew compileDebugKotlin` - identify remaining compilation errors.
2. **Standard Tests**:
   - `./gradlew testDebugUnitTest` - ensure no regression in core playback/InnerTube pipeline.

## Risks, tradeoffs, and open questions
- **Risks**: Immediate build failure is guaranteed until DI modules are clean. Risk of subtle crashes if `PlayerController.kt` attempts to call `tasteProfileRepository` before the file is finished being scrubbed.
- **Tradeoffs**: Total destruction of local taste-based fallback is permanent; the user will only see YouTube-native recommendations from now on.

## Validation Strategy
- Verification is done by clean compilation success after DI and reference removal.
- Runtime sanity check: Open the app, ensure Home hub loads correctly via SmartTube Engine, and check settings screen to ensure manual taste configuration is gone.

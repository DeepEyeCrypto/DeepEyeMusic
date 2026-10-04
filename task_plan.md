# DeepEyeMusicPro Task Plan

## Active Milestone: HERMES R8-Omega Production Protocol

### Phase 1: Network Interceptor & Token Injection Audit (Completed)
- [x] Audit `InnerTubeAuthManager.kt` — ensure proactive refresh on missing/expired tokens.
- [x] Audit `InnerTubeRemoteClient.kt` — inject `Authorization: Bearer ***` on all `/youtubei/v1/` endpoints.
- [x] Eliminate API key query param conflicts (`?key=...`) on authenticated `/browse` and `/next` requests.

### Phase 2: R8 Production Armor & ProGuard Rules (Completed)
- [x] Harden `app/proguard-rules.pro` with explicit keep rules for DSP processors, Auth models, InnerTube JSON parsers, and NewPipe extractors.
- [x] Configure `app/build.gradle.kts` release build with `isMinifyEnabled = true`, `isShrinkResources = true`, and graceful local signing fallback.

### Phase 3: Assemble Production Release APK (Completed)
- [x] Execute `./gradlew assembleRelease`.
- [x] Parse and resolve any R8 warnings or missing class references.
- [x] Record size reduction metrics (Debug APK vs. Release APK).

### Phase 4: Sideload & Physical Device Verification (Completed)
- [x] Sideload `app-release.apk` onto Realme RMX3945.
- [x] Verify clean boot, DSP operation, and YouTube Cinema playback without reflection crashes.
- [x] Commit and push to repository.

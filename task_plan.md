# DeepEyeMusicPro Task Plan

## Active Milestone: v3.0.1.42 (SmartTube-Omega Protocol & Cyberpunk UI)

### Phase 1: InnerTube & SmartTube Subsystem
- [x] Create `InnerTubeAuthManager.kt` (OAuth 2.0 Device Code Flow / RFC 8628).
- [x] Create `InnerTubeRemoteClient.kt` (`/youtubei/v1/browse` & `/youtubei/v1/next` AutoPlay algorithm).
- [x] Wire InnerTube AutoPlay prefetching into PlayerController for gapless ExoPlayer transitions.

### Phase 2: Cyberpunk Auth & UI Architecture
- [x] Redesign `LoginScreen.kt` with responsive landscape dual-pane layout (Hero branding + Glassmorphic Auth Card).
- [x] Eliminate viewport overflow and 1600px input stretching on landscape/automotive displays.
- [x] Purge obsolete onboarding gate (`OnboardingScreen`, `OnboardingGateViewModel`) from initial navigation graph.

### Phase 3: Stability & Security Audit (Mantis Reflect)
- [x] Validate Coroutine lifecycles in `AuthViewModel` (`@ApplicationScope` for background data restore).
- [x] Audit ViPER4Android DSP and JNI AudioSession ID lifecycle.
- [x] Verify Android 14+ foreground service and background worker execution bounds.

### Phase 4: Production Release & Device Verification
- [ ] Complete R8 ProGuard Release Build (`app-release.apk`).
- [ ] Push Git commit and tag `v3.0.1.42`.
- [ ] Create GitHub Release `v3.0.1.42`.

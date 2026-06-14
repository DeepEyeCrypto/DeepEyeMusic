# DeepEye Music Pro — Testing Strategy & Quality Gates (STAGE 14)

This document specifies the validation criteria, automated test commands, and manual verification sweeps to enforce production readiness in DeepEye Music Pro.

---

## 1. Automated Unit & Integration Testing

All core components (controllers, repositories, DAOs, resolvers) must pass automated gradle test suites:

### Running JUnit Local Tests:
```bash
./gradlew testDebugUnitTest
```

### Scope of Automated Tests:
* **PlayerControllerTest**: Validates state updates, track skipping, and autoplay streak progressions.
* **HistoryRepositoryTest**: Verifies playback events logs insertion and backup imports.
* **DownloadWorkerTest**: Verifies database download state changes under network constraints.

---

## 2. Regression Protection Gates

Before releasing any build to staging, the following QA verification suite must compile cleanly:

1. **Static Linting Checks**:
   ```bash
   ./gradlew lintDebug
   ```
2. **KSP & Annotation Generation Verification**:
   ```bash
   ./gradlew kspDebugKotlin
   ```
3. **Debug Build Compilation**:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 3. Manual Staging Verification Checklists

### Playback Operations Check:
* Play local tracks. Ensure continuous output.
* Play YouTube tracks. Verify streaming resolution transitions without stalls.
* Toggle player quality (1080p, 720p, 480p). Verify logs confirm resolution parameter updates.

### DSP & V4A Route Verification:
* Plug in headphones. Verify `AudioRouteReceiver` applies `Premium Headphone Bass` preset.
* Connect Bluetooth. Verify `AudioRouteReceiver` applies `Bluetooth Optimized` preset.
* Connect USB DAC. Verify `AudioRouteReceiver` applies `Audiophile USB DAC` preset.
* Verify `releaseSession` calls clean up hardware effect slots on failure states.
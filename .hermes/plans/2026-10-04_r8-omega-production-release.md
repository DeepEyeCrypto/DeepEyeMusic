# HERMES R8-Omega Production Release Protocol Implementation Plan

## Goal
Assemble a fully optimized, minified, and R8-shrunk Production Release APK for DeepEyeMusicPro (`app-release.apk`) with strict ProGuard shields for JNI, Media3, InnerTube models, and DSP processors.

## Current Context & Assumptions
- DeepEyeMusicPro is at version `3.0.1.44` (code `30054`).
- Stack: Jetpack Compose + Material 3 + Media3 (1.5.1) + Hilt + Room + Coil 3 + InnerTube / NewPipe extractors.
- `app/build.gradle.kts` has `isMinifyEnabled = true` and `isShrinkResources = true` configured for release builds.
- Local release builds require fallback to debug signing when `release-keystore.jks` is absent locally.

## Architecture & Proposed Approach
1. **R8 Armor & Model Shielding (`proguard-rules.pro`)**:
   - Shield native JNI methods, Room DAOs/Entities, Hilt dependency injection classes, Media3 decoders, Gson models, InnerTube JSON parsers, and DSP processors (`com.deepeye.musicpro.dsp.**`).
   - Prevent R8 obfuscation from renaming reflection-based data models or stripping ASM bytecode transformers.
2. **Resilient Release Signing (`app/build.gradle.kts`)**:
   - Configure release signing to gracefully fall back to debug signing if the production keystore file is not present locally, ensuring local `./gradlew assembleRelease` runs smoothly.
3. **Release Assembly & Verification**:
   - Run `./gradlew assembleRelease` with terminal logging filters to capture any R8 `-dontwarn` / missing class warnings.
   - Measure APK size reduction compared to debug build and verify on Realme RMX3945.

---

## Step-by-Step Implementation Tasks

### Task 1: Initialize Tracking Documents
- **Files**:
  - `task_plan.md`
  - `progress.md`
- **Action**: Create tracking files to document R8 shrinking phases, rule sets, and release metrics.

### Task 2: Harden ProGuard & R8 Configuration
- **File**: `app/proguard-rules.pro`
- **Action**: Add explicit keep rules for:
  - `com.deepeye.musicpro.dsp.**` (All audio processors and state classes)
  - `com.deepeye.musicpro.domain.auth.**` (OAuth token response and device auth models)
  - `com.deepeye.musicpro.data.source.remote.youtube.**` (InnerTube parser, models, payload constructors)
  - `org.schabi.newpipe.extractor.**` (NewPipe stream extractor classes)
  - `us.shandian.giga.**` (Download engine components)

### Task 3: Configure Local Signing Fallback in `app/build.gradle.kts`
- **File**: `app/build.gradle.kts`
- **Action**: Ensure `buildTypes.getByName("release")` signs with debug keystore if the release keystore file does not exist locally:
  ```kotlin
  val releaseSigning = signingConfigs.getByName("release")
  val hasReleaseKeystore = releaseSigning.storeFile?.exists() == true
  signingConfig = if (hasReleaseKeystore) releaseSigning else signingConfigs.getByName("debug")
  ```

### Task 4: Execute Autonomous `assembleRelease`
- **Command**:
  ```bash
  cd /Users/enayat/Documents/DeepEyeMusicPro && ./gradlew --no-daemon assembleRelease
  ```
- **Verification**:
  - Confirm `app/build/outputs/apk/release/app-release-unsigned.apk` or `app-release.apk` is generated.
  - Verify zero fatal R8 missing class errors.

### Task 5: Side-by-Side Size Comparison & On-Device Validation
- **Commands**:
  ```bash
  ls -lh app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/release/app-release.apk
  adb install -r app/build/outputs/apk/release/app-release.apk
  adb shell am start -n com.deepeye.musicpro/.MainActivity
  ```
- **Verification**:
  - App boots cleanly with no `ClassNotFoundException`, `NoSuchMethodError`, or JNI link failures.
  - Record final APK metrics in `progress.md`.

---

## Risks & Mitigations
- **Risk**: R8 may strip non-referenced reflective InnerTube / NewPipe classes.
  - **Mitigation**: `-keep class com.deepeye.musicpro.data.source.remote.youtube.** { *; }` and `-keep class org.schabi.newpipe.extractor.** { *; }` added to `proguard-rules.pro`.
- **Risk**: Resource shrinker (`isShrinkResources = true`) might drop dynamic drawables.
  - **Mitigation**: Verified project uses Vector `Icons.Default.*` and Compose icons; `keep.xml` present if needed.

## Deliverables
- Fully minified and R8-optimized `app-release.apk`.
- Updated `proguard-rules.pro`, `app/build.gradle.kts`, `task_plan.md`, `findings.md`, and `progress.md`.
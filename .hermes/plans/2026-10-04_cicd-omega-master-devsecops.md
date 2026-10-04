# HERMES CI/CD Omega-Master Protocol (DevSecOps) Implementation Plan

## Goal
Upgrade the DeepEyeMusicPro release automation to an enterprise-grade DevSecOps pipeline featuring secure Base64 keystore injection, dual artifact generation (APK + AAB), SHA-256 release checksum verification, and optimized Gradle build caching.

## Current Context & Assumptions
- GitHub Actions workflow exists at `.github/workflows/release.yml` triggered on tag pushes (`v*`).
- `app/build.gradle.kts` has dynamic signing with fallback to debug signing if keystore is absent locally.
- Runner uses Temurin JDK 21 and Gradle 8.9 via wrapper with AGP 8.5+.

## Architecture & Proposed Approach
1. **Universal Keystore Environment Bridge (`app/build.gradle.kts`)**:
   - Accept either `STORE_PASSWORD` or `KEYSTORE_PASSWORD`, and `KEY_PASSWORD` or `KEYSTORE_KEY_PASSWORD`.
   - Safely resolve relative/absolute `KEYSTORE_FILE` paths with debug signing fallback for offline/local builds.
2. **Hardened DevSecOps Workflow (`.github/workflows/release.yml`)**:
   - Safely decode `KEYSTORE_B64` / `KEYSTORE_BASE64` into a transient runner keystore file with strict cleanup.
   - Execute combined `./gradlew assembleRelease bundleRelease --no-daemon` utilizing `gradle/actions/setup-gradle@v4` caching.
   - Generate SHA-256 checksums (`checksums.sha256`) for security provenance.
   - Publish APK, AAB, and checksums to GitHub Releases via `softprops/action-gh-release@v2`.

---

## Step-by-Step Implementation Tasks

### Task 1: Refine Dynamic Signing in `app/build.gradle.kts`
- **File**: `app/build.gradle.kts`
- **Action**: Update `signingConfigs.release` to support flexible secret naming:
  ```kotlin
  signingConfigs {
      create("release") {
          val keystorePath = System.getenv("KEYSTORE_FILE")
              ?: (keystoreProps["storeFile"] as? String)
              ?: "keystore.jks"
          storeFile = if (keystorePath.startsWith("/")) {
              file(keystorePath)
          } else {
              rootProject.file(keystorePath)
          }
          storePassword = System.getenv("STORE_PASSWORD")
              ?: System.getenv("KEYSTORE_PASSWORD")
              ?: (keystoreProps["storePassword"] as? String)
              ?: ""
          keyAlias = System.getenv("KEY_ALIAS")
              ?: (keystoreProps["keyAlias"] as? String)
              ?: ""
          keyPassword = System.getenv("KEY_PASSWORD")
              ?: System.getenv("KEYSTORE_KEY_PASSWORD")
              ?: (keystoreProps["keyPassword"] as? String)
              ?: ""
      }
  }
  ```

### Task 2: Upgrade `.github/workflows/release.yml` for DevSecOps
- **File**: `.github/workflows/release.yml`
- **Action**:
  - Secure keystore decoder with validation & base64 padding tolerance.
  - Consolidated single-invocation Gradle task: `./gradlew assembleRelease bundleRelease`.
  - Checksum generator: `sha256sum app-release.apk app-release.aab > checksums.sha256`.
  - Publish all artifacts with release notes.

### Task 3: Local Verification & Gradle Validation
- **Command**:
  ```bash
  cd /Users/enayat/Documents/DeepEyeMusicPro && ./gradlew --no-daemon assembleRelease bundleRelease -x test
  ```
- **Verification**:
  - Both `app/build/outputs/apk/release/app-release.apk` and `app/build/outputs/bundle/release/app-release.aab` exist.
  - Zero crashes when environment variables are not set locally.

---

## Deliverables
- Hardened `.github/workflows/release.yml`.
- Resilient `app/build.gradle.kts`.
- Verified local and CI build parity.
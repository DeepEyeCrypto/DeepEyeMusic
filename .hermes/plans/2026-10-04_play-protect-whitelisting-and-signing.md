# Google Play Protect Elimination & Production Signing Protocol Plan

## Goal
Permanently eliminate the Google Play Protect *"App scan recommended / Play Protect hasn't seen this app before"* warning dialog when installing and updating DeepEyeMusicPro APKs by establishing a consistent production signing key, submitting the package for Google Play Protect reputation whitelisting, and optimizing manifest security declarations.

---

## Root Cause Analysis (Deep Research)
The screenshot displays the Android 14/15/16 Google Play Protect Unknown App Scanning prompt:
- **Dialog Details**: *"Google Play Protect: App scan recommended. Play Protect hasn't seen this app before. To protect your device and data, send this app to Google for a security scan."*
- **Technical Causes**:
  1. **Unregistered Signing Certificate (Zero Cloud Reputation)**:
     - When GitHub Actions builds release APKs without an established persistent `.jks` keystore, it falls back to the default debug key (`CN=Android Debug,O=Android,C=US`) or rotating signing keys.
     - Google Play Protect's cloud telemetry flags any APK signed with an unknown or debug signature certificate that has not been scanned across Google's global telemetry network.
  2. **Android 14+ Real-Time Unknown App Interception**:
     - Google Mobile Services (GMS) on Android 14, 15, and 16 automatically intercepts PackageInstaller when an APK's signature SHA-256 is not registered in Google's cloud database.
  3. **High-Privilege Manifest Permissions**:
     - Permissions such as `REQUEST_INSTALL_PACKAGES`, `RECORD_AUDIO`, `BLUETOOTH_ADMIN`, and `ACCESS_FINE_LOCATION` increase the heuristic risk score for unknown packages.

---

## Multi-Phase Resolution Plan

### Phase 1: Permanent Release Keystore & V2/V3/V4 Signing Configuration
- **Objective**: Ensure the APK is signed with a consistent, production-grade RSA 4096-bit certificate with full V1, V2, and V3 APK Signature Schemes enabled.
- **File**: `app/build.gradle.kts`
  ```kotlin
  signingConfigs {
      create("release") {
          val keystoreFile = System.getenv("KEYSTORE_FILE") ?: System.getenv("KEYSTORE_PATH")
          if (keystoreFile != null && file(keystoreFile).exists()) {
              storeFile = file(keystoreFile)
              storePassword = System.getenv("STORE_PASSWORD")
              keyAlias = System.getenv("KEY_ALIAS")
              keyPassword = System.getenv("KEY_PASSWORD")
              enableV1Signing = true
              enableV2Signing = true
              enableV3Signing = true
              enableV4Signing = true
          }
      }
  }
  ```
- **Action for User**:
  - Upload the production `.jks` file to GitHub Secrets as `KEYSTORE_B64`, `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`.

---

### Phase 2: Google Play Protect Official Developer Whitelisting Submission
- **Objective**: Register `com.deepeye.musicpro` and its signing certificate SHA-256 with Google's Play Protect security database.
- **Procedure**:
  1. Extract the release APK's SHA-256 certificate fingerprint:
     ```bash
     keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
     ```
  2. Submit the APK to Google's official **Play Protect Appeals & Whitelisting Portal**:
     - **URL**: `https://support.google.com/googleplay/android-developer/contact/protectappeals`
     - **Form Fields**:
       - Package Name: `com.deepeye.musicpro`
       - Developer Name: `DeepEye`
       - App Link / APK Download: `https://github.com/DeepEyeCrypto/DeepEyeMusic/releases/latest/download/app-release.apk`
       - Description: *"DeepEyeMusicPro is an open-source, non-commercial audio player and visualizer. The APK is clean of any malware, trojans, or spyware and is distributed directly via GitHub Releases."*
  3. Once Google's automated scanners verify the APK (typically 24–48 hours), the certificate hash is whitelisted across all Android devices globally.

---

### Phase 3: Instant Sideloading Resolution on Device
- **Immediate Resolution for Current Device**:
  1. **Option A (Recommended)**: On the dialog, tap **"Scan app"** once.
     - Google Play Protect uploads the APK header and hash to Google Cloud.
     - Within 1–2 minutes, Google verifies the binary is clean, completes the installation, and remembers the signature hash so future updates do not trigger the prompt.
  2. **Option B (Disable Unknown App Scanning in Device Settings)**:
     - Open **Google Play Store** -> Tap Profile Icon (Top Right) -> **Play Protect** -> Tap Gear Icon (Settings) -> Turn OFF **"Scan apps with Play Protect"** or **"Improve harmful app detection"** (only if preferred for local development).

---

### Phase 4: AndroidManifest Permission & Component Hygiene
- **File**: `app/src/main/AndroidManifest.xml`
  - Audit and narrow permission flags:
    - Ensure `REQUEST_INSTALL_PACKAGES` is properly documented for in-app updates.
    - Add `android:usesPermissionFlags="neverForLocation"` to Bluetooth/Wi-Fi permissions where applicable.
    - Set `android:debuggable="false"` explicitly in release builds (handled by `isMinifyEnabled = true`).

---

## Verification & Validation
1. **Certificate Verification Command**:
   ```bash
   apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
   ```
   - Verify that `Verified using v1 scheme: true`, `Verified using v2 scheme: true`, `Verified using v3 scheme: true` are all confirmed.
2. **On-Device Install Verification**:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

# Phase 6 — Pre-Build Checklist & Release Pipeline
## DeepEyeMusicPro · Android Studio Ladybug Setup

---

## CRITICAL: Before First Gradle Sync

### 1. Create local.properties
```bash
echo "sdk.dir=/Users/enayat/Library/Android/sdk" > local.properties
```

### 2. Verify gradle-wrapper.properties
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
```

### 3. KSP (NOT kapt) — Kotlin 2.0+ requires KSP
```kotlin
// app/build.gradle.kts
plugins {
    id("com.google.devtools.ksp")      // ADD this
    // id("kotlin-kapt")               // REMOVE if present
}
dependencies {
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    ksp("androidx.room:room-compiler:2.6.1")
}
```

### 4. AndroidManifest — MusicPlayerService
```xml
<service
    android:name=".player.service.MusicPlayerService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="true">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService"/>
    </intent-filter>
</service>
```

### 5. buildFeatures in app/build.gradle.kts
```kotlin
android {
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}
```

---

## Common Sync Errors + Fixes

| Error | Fix |
|---|---|
| `SDK location not found` | Create local.properties with sdk.dir path |
| `Unsupported class file major version` | Project Structure -> Gradle JDK -> JDK 17 |
| `Hilt MissingBinding` | Check @Module classes have @InstallIn annotation |
| `Room: cannot find symbol` | Change kapt to ksp in build.gradle.kts |
| `Unresolved BuildConfig` | Add buildFeatures { buildConfig = true } |
| `ExoPlayer AudioEffect needs RECORD_AUDIO` | Add permission in Manifest + runtime request |
| `Duplicate class kotlin.collections` | Add kotlin-bom to dependencies |

---

## Phase 6 Unit Test Plan

### Tests to Write (Critical Path)

```kotlin
// 1. GainBudgetCalculatorTest
class GainBudgetCalculatorTest {
    // Test: bass 10dB + EQ peak 6dB = MODERATE risk
    // Test: bass 12dB + EQ 9dB = HIGH risk (clipping)
    // Test: flat params = LOW risk
}

// 2. PresetRepositoryTest
class PresetRepositoryTest {
    // Test: seedBuiltIns() inserts exactly 7 presets
    // Test: save() + load() roundtrip preserves all DspParams fields
    // Test: delete() removes user preset, keeps built-in
}

// 3. QueueManagerTest
class QueueManagerTest {
    // Test: shuffle produces different order
    // Test: unshuffle restores original order
    // Test: REPEAT_ONE replays same index
    // Test: REPEAT_ALL wraps from last to first
}

// 4. SearchUseCaseTest
class SearchUseCaseTest {
    // Test: empty query returns all songs
    // Test: "arijit" matches title + artist fields
    // Test: special chars dont crash
}
```

Run all:
```bash
./gradlew test --continue
```

---

## Manual QA Checklist (Device)

### Audio Engine
- [ ] Song plays from MediaStore
- [ ] Notification shows with prev/play/next controls
- [ ] Audio continues after screen off
- [ ] Bluetooth headset buttons work (play/pause/next)
- [ ] Wired headset unplug pauses playback automatically
- [ ] V4A EQ slider changes audio in real-time
- [ ] Preset "Bollywood" loads all 14 modules correctly
- [ ] GainBudget shows RED when total gain > 12dB
- [ ] AudioVisualizer bars pulse with music beat

### UI / Compose
- [ ] HomeScreen GlowCards scroll at 60fps
- [ ] NowPlayingScreen album art blur renders
- [ ] MarqueeText scrolls for long titles
- [ ] ShimmerBox appears on scan, disappears after
- [ ] V4AScreen all 14 module cards functional
- [ ] Dark theme: no white-on-white text anywhere
- [ ] Rotation: NowPlaying keeps state (no restart)
- [ ] Back navigation works on all 7 screens

### Permissions
- [ ] First launch: READ_MEDIA_AUDIO dialog appears
- [ ] Permission denied: empty state + retry button shown
- [ ] POST_NOTIFICATIONS asked on Android 13+

### Edge Cases
- [ ] 0 songs: HomeScreen empty state renders correctly
- [ ] Song file deleted mid-play: graceful error, no crash
- [ ] Very long title (60+ chars): MarqueeText handles it
- [ ] App killed + relaunched: resumes last song position
- [ ] Low memory device: no OOM crash (Coil bitmap pool)

---

## Keystore Setup (One Time)

```bash
keytool -genkey -v \
  -keystore ~/keys/deepeye-release.jks \
  -alias deepeye-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Create keystore.properties (NEVER commit this):
```properties
storeFile=/Users/enayat/keys/deepeye-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=deepeye-key
keyPassword=YOUR_KEY_PASSWORD
```

Add to .gitignore:
```
*.jks
*.keystore
keystore.properties
local.properties
/app/build/
```

---

## Release Signing Config (app/build.gradle.kts)

```kotlin
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = java.util.Properties().apply {
    if (keystorePropsFile.exists()) load(keystorePropsFile.inputStream())
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps["storePassword"] as String
            keyAlias = keystoreProps["keyAlias"] as String
            keyPassword = keystoreProps["keyPassword"] as String
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

Build commands:
```bash
./gradlew assembleRelease   # -> app/build/outputs/apk/release/app-release.apk
./gradlew bundleRelease     # -> app/build/outputs/bundle/release/app-release.aab
```

---

## ProGuard Rules (proguard-rules.pro)

```proguard
# Gson - DspParams JSON serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.deepeye.musicpro.dsp.model.** { *; }
-keep class com.google.gson.** { *; }

# Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Media3
-keep class androidx.media3.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
```

---

## GitHub Actions release.yml

```yaml
name: Release Build
on:
  push:
    tags: ["v*"]

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: "17"
          distribution: "temurin"
      - uses: android-actions/setup-android@v3

      - name: Write keystore.properties
        run: |
          echo "storeFile=app/deepeye-release.jks" > keystore.properties
          echo "storePassword=${{ secrets.STORE_PASSWORD }}" >> keystore.properties
          echo "keyAlias=${{ secrets.KEY_ALIAS }}" >> keystore.properties
          echo "keyPassword=${{ secrets.KEY_PASSWORD }}" >> keystore.properties

      - name: Decode Keystore
        run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > app/deepeye-release.jks

      - name: Build
        run: |
          chmod +x gradlew
          ./gradlew assembleRelease bundleRelease

      - name: GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          generate_release_notes: true
          files: |
            app/build/outputs/apk/release/app-release.apk
            app/build/outputs/bundle/release/app-release.aab
```

GitHub Secrets needed:
```
KEYSTORE_BASE64  ->  base64 -i deepeye-release.jks | pbcopy
STORE_PASSWORD
KEY_ALIAS        ->  deepeye-key
KEY_PASSWORD
```

---

## Phase 6 Completion Criteria

- [ ] ./gradlew test — all unit tests PASS
- [ ] ./gradlew connectedAndroidTest — instrumented tests PASS
- [ ] Manual QA — all 20+ checklist items verified
- [ ] ./gradlew assembleRelease — signed APK generated
- [ ] APK installs on Android 8.0 (API 26)
- [ ] APK installs on Android 15 (API 35)
- [ ] LeakCanary — zero memory leaks
- [ ] Android Profiler — NowPlayingScreen 60fps steady
- [ ] /docs has final ARCHITECTURE.md + PLAN.md
- [ ] GitHub tag v1.0.0 created with APK artifact attached

---

*DeepEyeMusicPro v1.0.0 — Ship it. 🎧*

# Plan: Auto-Omega Protocol (Media3 MediaLibraryService, Android Auto & Ecosystem Architecture)

## Goal
Upgrade the playback service to `MediaLibraryService` (`MediaLibrarySession`), integrate complete Android Auto (`automotive_app_desc.xml`, `com.google.android.gms.car.application`), WearOS smartwatch controls, lockscreen 512x512 artwork caching via Coil, and Bluetooth steering wheel media button hooks.

## Current Context / Assumptions
- `MusicPlayerService` currently extends `MediaSessionService` with a `ForwardingPlayer` connected to `ExoPlayer` and `PlayerController`.
- Android Auto, WearOS, and automotive clients expect a `MediaLibraryService` with valid root browsing hierarchy (`onGetLibraryRoot`, `onGetChildren`, `onGetItem`) and `MediaLibrarySession`.
- `AndroidManifest.xml` needs `<action android:name="androidx.media3.session.MediaLibraryService" />` and `<meta-data android:name="com.google.android.gms.car.application" android:resource="@xml/automotive_app_desc" />`.
- Foreground service permission `FOREGROUND_SERVICE_MEDIA_PLAYBACK` is already configured for Android 14+.

## Proposed Architecture
1. **Automotive XML Configuration**:
   - Create `app/src/main/res/xml/automotive_app_desc.xml` with `<automotiveApp><uses name="media"/></automotiveApp>`.
   - Wire `com.google.android.gms.car.application` meta-data in `AndroidManifest.xml`.
2. **MediaLibraryService & MediaLibrarySession**:
   - Refactor `MusicPlayerService` to inherit `MediaLibraryService()`.
   - Build a `MediaLibrarySession` with a comprehensive `MediaLibrarySession.Callback` supporting:
     - `onGetLibraryRoot`: Returns library root node for Android Auto & WearOS.
     - `onGetChildren`: Exposes recently played, queue, and personalized mixes.
     - `onGetItem`: Fetches specific media items.
     - `onPlaybackResumption`: Instant resume from Android Auto dashboard.
     - `onMediaButtonEvent`: Hardware Bluetooth / steering wheel media controls.
3. **Lockscreen & Android Auto 512x512 Artwork Pipeline**:
   - When tracks change, fetch artwork via Coil `ImageLoader`, downscale to 512x512 JPEG/PNG bytes, and attach `artworkData` to `MediaMetadata`.
4. **Manifest Exposure**:
   - Update `<service android:name=".player.service.MusicPlayerService">` with `MediaLibraryService` intent filter.

## Step-by-Step Implementation Plan

### Step 1: Create `automotive_app_desc.xml`
- File: `app/src/main/res/xml/automotive_app_desc.xml`
- Content:
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <automotiveApp>
      <uses name="media" />
  </automotiveApp>
  ```

### Step 2: Update `AndroidManifest.xml`
- File: `app/src/main/AndroidManifest.xml`
- Add automotive meta-data under `<application>`:
  ```xml
  <meta-data
      android:name="com.google.android.gms.car.application"
      android:resource="@xml/automotive_app_desc" />
  ```
- Update `MusicPlayerService` intent-filter:
  ```xml
  <intent-filter>
      <action android:name="androidx.media3.session.MediaLibraryService" />
      <action android:name="androidx.media3.session.MediaSessionService" />
      <action android:name="android.media.browse.MediaBrowserService" />
  </intent-filter>
  ```

### Step 3: Upgrade `MusicPlayerService.kt` to `MediaLibraryService`
- File: `app/src/main/java/com/deepeye/musicpro/player/service/MusicPlayerService.kt`
- Inherit `MediaLibraryService`.
- Create `MediaLibrarySession` instance.
- Implement `MediaLibrarySession.Callback` with Auto browse hierarchy, Bluetooth steering wheel key handlers, and Coil 512x512 artwork builder.

### Step 4: Add Unit Tests for MediaLibraryService Architecture
- File: `app/src/test/java/com/deepeye/musicpro/player/service/MusicPlayerServiceTest.kt`
- Validate browse tree root, command handling, and intent actions.

### Step 5: Version Bump, Build, Device Test & Release
- Bump `versionCode = 30078`, `versionName = "3.0.1.68"`.
- Run `./gradlew --no-daemon testDebugUnitTest`.
- Run `./gradlew --no-daemon assembleDebug`.
- Sideload onto Realme RMX3945, verify lockscreen/notification controls, and capture screenshot.
- Commit, tag `v3.0.1.68`, and push to GitHub.

## Risks & Tradeoffs
- **Android 14 Foreground Service Policies**: Must ensure `foregroundServiceType="mediaPlayback"` is strictly respected during background start or head unit connection. Handled by standard Media3 `DefaultMediaNotificationProvider` lifecycle.

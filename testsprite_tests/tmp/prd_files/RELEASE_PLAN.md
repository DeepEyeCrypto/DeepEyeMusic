# DeepEye Music Pro — Release & Deployment Plan (STAGE 15)

This document specifies the staging steps, proguard configuration guidelines, and deployment checks for shipping DeepEye Music Pro to production.

---

## 1. Release Package Compilation

To compile the production-ready package with code shrinking enabled:

### Compile Release APK:
```bash
./gradlew assembleRelease
```

### Compile Android App Bundle (AAB):
```bash
./gradlew bundleRelease
```

---

## 2. Code Shrinking & Proguard Rules

To safeguard internal classes (especially Media3, Room, and Hilt reflection targets) from being stripped during R8 optimization, the following configurations are defined in `proguard-rules.pro`:

* **Room Persistence**: Keep all entities and DAO interfaces:
  ```proguard
  -keep class com.deepeye.musicpro.data.db.** { *; }
  ```
* **Media3 / ExoPlayer**: Preserve native rendering and audio effects bindings:
  ```proguard
  -keep class androidx.media3.** { *; }
  ```
* **Hilt Dependency Injection**: Preserve injection parameters:
  ```proguard
  -keep class * implements hilt.internal.GeneratedComponent { *; }
  ```

---

## 3. Shipping Staging Steps

1. **Verify Sandbox Cleanliness**: Confirm that all debug logs, diagnostics tools, and temporary test databases are disabled in production configurations.
2. **Execute Stage 14 Gates**: Pass linting, unit tests, and compile checks.
3. **Internal Alpha Staging**: Deploy the release APK to internal testers to verify DSP/V4A hardware compatibility across varied device manufacturer skins (Samsung, OnePlus, Pixel).
4. **Gradual Production Rollout**: Deploy the AAB via Google Play Console starting with a 10% rollout to monitor crash metrics.
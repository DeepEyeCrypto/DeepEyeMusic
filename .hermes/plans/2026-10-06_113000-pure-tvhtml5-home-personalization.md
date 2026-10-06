# 🚀 PLAN: 100% PURE TVHTML5 CLOUD PERSONALIZATION ENGINE (HOME & RAILS)

**Document ID:** `.hermes/plans/2026-10-06_113000-pure-tvhtml5-home-personalization.md`  
**Status:** READY TO EXECUTE  

---

## 1. Goal
Enforce 100% pure TVHTML5 cloud personalization across the Home screen, eradicating all dummy placeholders and hardcoded fallbacks, and binding authenticated InnerTube endpoints (`FEwhat_to_watch`, `FEhistory`, `FEsubscriptions`, `VLLL`, `FEmusic_home`) directly into live dynamic rails.

---

## 2. Current Context & Findings
1. **Authenticated Client Ready**: `AuthenticatedYouTubeClient.kt` has been hardened with the `7.20210614.03.00` TVHTML5 client fingerprint and `fetchSupermix()` support.
2. **Dummy Data Artifacts**: `HomeFeedRepository.kt` currently holds dummy fallback lists (`"dummy1"`, `"dummy2"`, `"dummy3"`, `"dummy4"`, `"dummy5"`) if queries return empty. These must be purged.
3. **1:1 Mirror Requirement**: The Home feed must reflect the user's real YouTube account content:
   - **🌟 For You**: InnerTube `FEwhat_to_watch`
   - **✨ My Supermix**: InnerTube `FEmusic_home` / `authClient.fetchSupermix()`
   - **❤️ Liked Music**: InnerTube `VLLL` (Playlist `LL`)
   - **📺 Continue Watching**: InnerTube `FEhistory` (Cloud History)
   - **🔔 Subscriptions**: InnerTube `FEsubscriptions`

---

## 3. Step-by-Step Implementation Tasks

### Task 1: Eradicate Dummy Data & Wire `fetchSupermix()` in `HomeFeedRepository.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/data/repository/HomeFeedRepository.kt`
- **Changes**:
  1. Delete all dummy/hardcoded fallback lists (`dummy1`, `dummy2`, `dummy3`, `dummy4`, `dummy5`).
  2. When authenticated (`hasAuth == true`), call `authClient.fetchSupermix()` to populate `supermix`.
  3. Ensure `emptyList()` is returned gracefully without crashing or fabricating fake tracks.

### Task 2: Reactive OAuth Auth State Observation in `HomeHubViewModel.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/homehub/HomeHubViewModel.kt`
- **Changes**:
  1. Observe `settingsDataStore.oauthAccessToken` and trigger automatic `refreshFeed()` when the user logs in or switches accounts.
  2. Expose `hasAuth` StateFlow for the UI.

### Task 3: Home Hub UI Cloud Indicators (`HomeHubScreen.kt`)
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/homehub/HomeHubScreen.kt`
- **Changes**:
  1. Render dynamic TVHTML5 badges on cloud-synced rails (*"☁️ YouTube Cloud"*, *"❤️ Liked on YouTube"*).
  2. Seamless empty state handling when loading personalized shelves.

### Task 4: Compilation, Deployment & Hardware Verification
- **Actions**:
  1. Bump version to `3.0.1.79` (`versionCode = 30089`) in `app/build.gradle.kts` and `AppChangelog.kt`.
  2. Run `./gradlew assembleDebug`.
  3. Deploy to Realme RMX3945 via ADB.
  4. Verify logcat for HTTP 200 on InnerTube TVHTML5 calls with zero crashes.
  5. Git commit and tag `v3.0.1.79`.

---

## 4. Verification Commands
```bash
./gradlew compileDebugKotlin -x test
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.deepeye.musicpro/com.deepeye.musicpro.MainActivity
```

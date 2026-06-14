# QA_AUTOMATION_PLAN

## Objective
Eliminate regressions via automated ADB scripting and Android UI Automator tests.

## Automated Validation Suite
1. **Audio Path Validation**:
   - `adb shell dumpsys audio` MUST return `USAGE_MEDIA` tied to `com.deepeye.musicpro`.
   - Script will assert that no concurrent AudioSessions exist.
2. **MediaSession Validation**:
   - `adb shell dumpsys media_session` MUST reflect accurate `metadata` matching the UI.
   - Script will verify `state=PLAYING` matches `PlaybackState` UI toggles.
3. **UI Navigation**:
   - UI Automator scripts will simulate 100 rapid clicks on "Next", verifying that the application does not ANR.
4. **Memory Validation**:
   - `adb shell dumpsys meminfo` will track memory consumption before and after 50 remote track loads to detect WebView memory leaks.

## Continuous Integration
Scripts will be compiled into a bash suite `qa_suite.sh` that must pass before any pull request is merged to `main`.

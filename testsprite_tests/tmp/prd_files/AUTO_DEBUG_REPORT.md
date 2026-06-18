# DeepEye Music Pro — Auto Debugging & Validation (STAGE 12)

This document specifies the validation suites, diagnostic log filters, and ADB test runners to verify player stability and DSP state in DeepEye Music Pro.

---

## 1. Regression Testing Framework

DeepEye Music Pro integrates standard diagnostic layers that monitor internal states to prevent regressions:

* **PlaybackPathEnforcer**: Registers the active player and enforces that only valid resolved stream URLs play on ExoPlayer, catching routing mismatches.
* **NowPlayingGuardian**: A lightweight checker looping every 4 seconds to confirm that the UI, ExoPlayer, and MediaSession metadata are synchronized, auto-repairing metadata states if a drift occurs.
* **AudioSessionGuardian**: Monitors if the Audio Session ID resets or drops, logging tracking parameters.
* **DSPHealthMonitor**: Checks if active audio effects are processing and detects initialization failures, returning the system to safe modes.

---

## 2. ADB Diagnostic Shell Verification

To analyze player, V4A, and audio focus transitions under load, execute these ADB terminal commands:

### Verify MediaSession & Notifications:
```bash
adb shell dumpsys media_session
```

### Stream Playback Logs:
```bash
adb logcat -c
adb logcat | grep -E "PlayerController|MediaSession|ExoPlayer|AudioSessionManager"
```

### Inspect Hardware Audio Session ID & Effect Bindings:
```bash
adb logcat | grep -i -E "AudioEffect|Equalizer|DSPEngine|V4A"
```

---

## 3. Playback Verification Checklist
1. **Cold Startup**: App launches and draws layouts within 2 seconds.
2. **Audio Track Transition**: Clicking a track advances the queue, updates the session ID, and attaches the DSP without playback stalls.
3. **Focus Loss**: Incoming calls pause the player, resuming playback automatically upon call completion.
4. **Disconnect Behavior**: Unplugging headphones or disconnecting Bluetooth pauses playback.

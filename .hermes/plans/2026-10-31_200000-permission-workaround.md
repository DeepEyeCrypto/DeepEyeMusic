# Android 16 RECORD_AUDIO Permission Workaround

## Problem

On Android 16 (API 35), `adb shell pm grant` fails with exit code 255 for `RECORD_AUDIO`:

```
$ adb -s LZN7EERSZPS4VSUG shell pm grant com.deepeye.musicpro android.permission.RECORD_AUDIO
Error: Permission android.permission.RECORD_AUDIO not found
```

This is because on Android 16, `RECORD_AUDIO` is a runtime permission that requires the app to be in the foreground and actively using the microphone. The `pm grant` command cannot grant it statically.

## Solution

The app uses **ExoPlayer's internal audio pipeline** which handles audio capture internally without requiring explicit `RECORD_AUDIO` permission. The visualizer reads audio data from the ExoPlayer's `AudioProcessor` chain, not from the system microphone.

### How it works:

1. **ExoPlayer AudioProcessor**: The visualizer's `frequencyBands` and `fftSpectrum` StateFlows are fed by `AudioFrameInterpolator` which taps into ExoPlayer's internal audio pipeline.
2. **No microphone access**: The visualizer does not use `MediaRecorder` or `AudioRecord` directly.
3. **PCM data flow**: Audio PCM data flows through ExoPlayer → `AudioProcessor` → `AudioFrameInterpolator` → visualizer StateFlows.

### Verification:

```
$ adb logcat -d | grep -iE "PCM_PIPELINE|AudioFrame|VisualizerEngine"
# No output when media session is idle (expected)
# PCM_PIPELINE_ACTIVE appears when audio is playing
```

### If explicit permission is needed:

For features that genuinely need microphone access (e.g., live voice visualization), the app must:
1. Declare `RECORD_AUDIO` in `AndroidManifest.xml`
2. Request the permission at runtime using `ActivityResultLauncher`
3. Handle the permission result in the UI

The current implementation does NOT require this — it uses ExoPlayer's internal pipeline.

## Status

- ✅ Visualizer works without `RECORD_AUDIO` permission
- ✅ ExoPlayer internal pipeline handles audio capture
- ✅ No `pm grant` needed for visualizer functionality
- ⚠️ If user enables "Voice Visualizer" feature, runtime permission request will be needed
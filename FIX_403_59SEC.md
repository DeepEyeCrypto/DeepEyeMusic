# Fix: HTTP 403 Error at 59-Second Mark During YouTube Music Playback

## Root Cause
YouTube stream URLs expire (~6 hours). DASH adaptive formats fail with HTTP 403 after ~59 seconds without BotGuard/SABR tokens. Progressive formats (itag 18) are primary but still expire.

## Solution Implemented
Enhanced `PlayerController.onPlayerError()` to detect and handle 403/410 errors:

1. **Detect 403/410**: Check `PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS` and error message
2. **Refresh URL**: Call `sourceResolverManager.resolveSource(videoId, preferVideo, forceRefresh=true)`
3. **Resume**: Pause → setMediaItem(newUrl) → seekTo(oldPos) → play
4. **Fallback**: Generic retry 2-3 times if refresh fails, then skip

## Files Changed
- `PlayerController.kt` lines 235-301: Enhanced `onPlayerError` listener

## Test Procedure
```bash
# Build
./gradlew clean assembleDebug -x test
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Test: Play YouTube Music for >2 minutes
# Expected: At ~59s, brief "Refreshing stream..." toast, then playback continues
# Logcat: "[403 Recovery] Resumed at XXXXX ms"
```

## Expected Result
- No 403 error shown to user
- Playback continues >2 minutes without interruption
- Position preserved during refresh

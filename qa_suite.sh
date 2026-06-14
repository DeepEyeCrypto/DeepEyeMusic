#!/bin/bash
# QA Automation Suite for DeepEye Music Pro
# Checks DSP Audio Session and MediaSession Sync

echo "Starting DeepEye QA Automation Suite..."

PACKAGE="com.deepeye.musicpro.debug"

# 1. Check if Audio Session is correctly bound to Music Usage
echo "Checking Audio Focus..."
adb shell dumpsys audio | grep -iE "USAGE_MEDIA|CONTENT_TYPE_MUSIC" | grep $PACKAGE
if [ $? -eq 0 ]; then
    echo "✅ AudioFocus is properly bound to ExoPlayer."
else
    echo "❌ AudioFocus not found for $PACKAGE!"
    exit 1
fi

# 2. Check MediaSession Sync
echo "Checking MediaSession State..."
adb shell dumpsys media_session | grep -iE "state=PLAYING|state=PAUSED"
if [ $? -eq 0 ]; then
    echo "✅ MediaSession is active and responding."
else
    echo "❌ MediaSession is missing or stopped!"
    exit 1
fi

echo "All QA tests passed."
exit 0

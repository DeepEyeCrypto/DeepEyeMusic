#!/usr/bin/env bash

# ==============================================================================
# 🛡️ DEEPEYE MUSIC PRO: ADB AUTO-DEBUGGING, TESTING, AND LAUNCH AUTOMATION ENGINE
# ==============================================================================
#
# This script automates unit testing, compilation, deployment, and live-crash monitoring.
# Author: Antigravity AI Engine
# Target Device: Connected ADB physical Android phone

# Styling & Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Clear terminal screen
clear

echo -e "${MAGENTA}${BOLD}======================================================================${NC}"
echo -e "${CYAN}${BOLD}   🛡️  DEEPEYE MUSIC PRO: AUTONOMOUS ADB DEBUGGING & AUTOMATION SENTINEL${NC}"
echo -e "${MAGENTA}${BOLD}======================================================================${NC}"

# Check ADB connections
echo -e "${BLUE}[1/6] Scanning for connected ADB devices...${NC}"
DEVICE_LIST=$(adb devices | tail -n +2 | grep -v -e '^$')

if [ -z "$DEVICE_LIST" ]; then
    echo -e "${RED}${BOLD}❌ ERROR: No connected ADB devices found!${NC}"
    echo -e "${YELLOW}Please connect your Motorola Edge 30 Pro or enable USB Debugging.${NC}"
    exit 1
fi

DEVICE_MODEL=$(adb shell getprop ro.product.model | tr -d '\r')
DEVICE_OS=$(adb shell getprop ro.build.version.release | tr -d '\r')

echo -e "${GREEN}✅ FOUND ACTIVE TARGET:${NC} ${BOLD}$DEVICE_MODEL${NC} (Android $DEVICE_OS)"
echo -e "${BLUE}----------------------------------------------------------------------${NC}"

# Check arguments
STREAM_ONLY=false
TEST_ONLY=false

for arg in "$@"; do
    if [ "$arg" == "--stream-only" ] || [ "$arg" == "-s" ]; then
        STREAM_ONLY=true
    elif [ "$arg" == "--test-only" ] || [ "$arg" == "-t" ]; then
        TEST_ONLY=true
    fi
done

if [ "$STREAM_ONLY" = true ]; then
    echo -e "${YELLOW}⏩ SKIPPING BUILD. Entering live log streaming mode...${NC}"
else
    # Run Unit Tests
    echo -e "${BLUE}[2/6] Executing Automated Unit Test Suite...${NC}"
    ./gradlew testDebugUnitTest
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}${BOLD}❌ UNIT TESTS FAILED! Aborting deployment to prevent corruption.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ ALL TESTS PASSED SUCCESSFULLY!${NC}"
    echo -e "${BLUE}----------------------------------------------------------------------${NC}"

    if [ "$TEST_ONLY" = true ]; then
        echo -e "${GREEN}${BOLD}🎉 Test-only pipeline complete. Goodbye!${NC}"
        exit 0
    fi

    # Compile and Build Debug APK
    echo -e "${BLUE}[3/6] Compiling, packaging, and installing Debug APK via OTA...${NC}"
    ./gradlew installDebug

    if [ $? -ne 0 ]; then
        echo -e "${RED}${BOLD}❌ COMPILATION OR OTA INSTALLATION FAILED!${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ APP DEPLOYED AND INSTALLED ON $DEVICE_MODEL!${NC}"
    echo -e "${BLUE}----------------------------------------------------------------------${NC}"
fi

# Launch App
echo -e "${BLUE}[4/6] Launching DeepEye Music Pro launcher activity...${NC}"
adb shell am start -n com.deepeye.musicpro.debug/com.deepeye.musicpro.MainActivity

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Failed to launch MainActivity.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ LAUNCHED COMPLETED SUCCESSFUL!${NC}"
echo -e "${BLUE}----------------------------------------------------------------------${NC}"

# Clear old logcat
echo -e "${BLUE}[5/6] Flushing historical logcat buffers...${NC}"
adb logcat -c
echo -e "${GREEN}✅ LOG BUFFER IS CLEAN!${NC}"
echo -e "${BLUE}----------------------------------------------------------------------${NC}"

# Background Sentinel live crash streaming
echo -e "${MAGENTA}${BOLD}[6/6] STARTING LIVE CRASH SENTINEL WATCHDOG...${NC}"
echo -e "${YELLOW}Listening in real-time for any FATAL exceptions, crashes, or runtime failures.${NC}"
echo -e "${YELLOW}Press Ctrl+C to terminate the watchdog logging session.${NC}"
echo -e "${CYAN}----------------------------------------------------------------------${NC}"

# Run adb logcat streaming in real-time, highlighting Fatal, exceptions, and deepeye events
# Using awk to dynamically format and colorize lines containing FATAL, AndroidRuntime or deepeye tags.
adb logcat | awk '
/Fatal|Exception|AndroidRuntime/ {
    print "\033[1;31m[CRASH WARNING]\033[0m " $0
    next
}
/com.deepeye.musicpro/ {
    print "\033[0;32m[DEEPEYE LOG]\033[0m " $0
    next
}
/ExoPlayer|HybridPlayerCard|YouTubeVideoPlayer/ {
    print "\033[0;36m[PLAYER LOG]\033[0m " $0
    next
}
'

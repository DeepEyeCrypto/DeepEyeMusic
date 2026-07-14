#!/bin/bash
set -e

echo "Stashing unrelated WIP files..."
git stash push -m "WIP NetMirror" -- app/src/main/java/com/deepeye/musicpro/ui/screens/NetMirrorScreen.kt app/src/main/java/com/deepeye/musicpro/ui/screens/NetMirrorViewModel.kt || echo "No NetMirror files to stash"

echo "Committing AEOS integration changes..."
git add app/proguard-rules.pro app/src/main/java/com/deepeye/musicpro/ui/navigation/NavGraph.kt app/src/main/java/com/deepeye/musicpro/ui/navigation/Routes.kt app/src/main/java/com/deepeye/musicpro/ui/settings/SettingsScreen.kt
git commit -m "feat: integrate AEOS Factory Dashboard into Settings" || echo "Nothing to commit"

echo "Building Release APK..."
./gradlew clean assembleRelease --no-daemon

echo "Pushing release branch to origin..."
git push origin release/v1.0.0-new

echo "Creating GitHub Release..."
/usr/local/bin/gh release create v3.0.1.20 app/build/outputs/apk/release/app-release.apk \
  --title "DeepEye Music Pro v3.0.1.20" \
  --notes "AEOS Day-0 Factory Dashboard integration. The Vibe-Coding sandbox is now accessible from the Developer section in Profile Settings."

echo "Restoring stash..."
git stash pop || echo "No stash to pop"

echo "Release complete!"

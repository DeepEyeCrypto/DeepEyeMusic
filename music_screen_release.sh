#!/bin/bash
set -e

# Commit changes
git add .
git commit -m "fix: resolve status bar overlap and improve premium glassmorphic overlay on MusicScreen"
git push origin release/v1.0.0-new

# Update main branch
git checkout main
git merge release/v1.0.0-new -m "Merge MusicScreen UI fixes into main" || echo "Merge failed or already up to date"
git push origin main
git checkout release/v1.0.0-new

# Build APK
echo "Building APK..."
./gradlew assembleRelease

# Create GitHub Release
echo "Creating GitHub Release..."
gh release create v3.0.1.0 app/build/outputs/apk/release/app-release.apk \
  --title "DeepEye Music Pro v3.0.1.0" \
  --notes "MusicScreen UI fixes: Fixed status bar overlap and implemented true edge-to-edge premium glassmorphic scroll overlays."

echo "Release complete!"

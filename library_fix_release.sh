#!/bin/bash
set -e

# Commit changes
git add .
git commit -m "fix: resolve library list scrolling overlap and clipping issue"
git push origin release/v1.0.0-new

# Update main branch
git checkout main
git merge release/v1.0.0-new -m "Merge library fix into main" || echo "Merge failed or already up to date"
git push origin main
git checkout release/v1.0.0-new

# Build APK
echo "Building APK..."
./gradlew assembleRelease

# Create GitHub Release
echo "Creating GitHub Release..."
gh release create v3.0.1.2 app/build/outputs/apk/release/app-release.apk \
  --title "DeepEye Music Pro v3.0.1.2" \
  --notes "Fixed LibraryScreen layout bug where songs, albums, and artist lists were not scrolling properly or getting cut off at the bottom due to unbound LazyList height constraints."

echo "Release complete!"

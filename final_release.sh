#!/bin/bash
set -e

# Commit changes
git add .
git commit -m "fix: correctly scope showPlaylistSheet"
git push origin release/v1.0.0-new

# Update main branch
git checkout main
git merge release/v1.0.0-new -m "Merge fix into main" || echo "Merge failed or already up to date"
git push origin main
git checkout release/v1.0.0-new

# Build APK
echo "Building APK..."
./gradlew assembleRelease

# Create GitHub Release
echo "Creating GitHub Release..."
gh release create v3.0.0.9 app/build/outputs/apk/release/app-release.apk \
  --title "DeepEye Music Pro v3.0.0.9" \
  --notes "UX improvements, Premium Search, and Playlist Flow fixes."

echo "Release complete!"

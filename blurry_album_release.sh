#!/bin/bash
set -e

# Commit changes
git add .
git commit -m "fix: resolve blurry album artwork by using HighResAlbumArtInterceptor for Coil"
git push origin release/v1.0.0-new

# Update main branch
git checkout main
git merge release/v1.0.0-new -m "Merge blurry album art fix into main" || echo "Merge failed or already up to date"
git push origin main
git checkout release/v1.0.0-new

# Build APK
echo "Building APK..."
./gradlew assembleRelease

# Create GitHub Release
echo "Creating GitHub Release..."
gh release create v3.0.1.1 app/build/outputs/apk/release/app-release.apk \
  --title "DeepEye Music Pro v3.0.1.1" \
  --notes "Fixed blurry album artwork across the app. Implemented a custom Coil interceptor that bypasses low-res MediaStore thumbnails and dynamically extracts high-quality (1024x1024) album art directly from the audio files using Android's native \`loadThumbnail\` API."

echo "Release complete!"

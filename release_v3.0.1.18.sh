#!/bin/bash
set -e

# Compile the APK
echo "Building Release APK..."
./gradlew clean assembleRelease --no-daemon

echo "Stashing WIP files..."
git stash push -m "WIP stash before release 18" -- app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/YoutubeRemoteDataSource.kt app/src/main/java/com/deepeye/musicpro/domain/autoplay/AutoplayRepository.kt app/src/main/java/com/deepeye/musicpro/domain/recommendation/ContentFetcher.kt app/src/main/java/com/deepeye/musicpro/domain/sync/CloudSyncManager.kt app/src/main/java/com/deepeye/musicpro/extractor/IExtractorBridge.kt app/src/main/java/com/deepeye/musicpro/extractor/NewPipeExtractorPlugin.kt app/src/main/java/com/deepeye/musicpro/player/controller/PlayerController.kt app/src/main/java/com/deepeye/musicpro/ui/components/GlassComponents.kt app/src/main/java/com/deepeye/musicpro/ui/screens/NetMirrorScreen.kt app/src/main/java/com/deepeye/musicpro/ui/screens/NetMirrorViewModel.kt window_dump* parsed_dump* scratch_* screen* crash* logcat*

echo "Committing version bump..."
git add app/build.gradle.kts app/src/main/java/com/deepeye/musicpro/updates/AppChangelog.kt
git commit -m "chore(release): bump version to v3.0.1.18 and add changelog"

echo "Pushing..."
git push origin release/v1.0.0-new

echo "Merging to main..."
git checkout main
git merge release/v1.0.0-new -m "Merge release v3.0.1.18" || echo "Merge failed or up to date"
git push origin main

echo "Switching back to release branch..."
git checkout release/v1.0.0-new

echo "Restoring stash..."
git stash pop || echo "No stash to pop"

echo "Creating GitHub Release..."
/usr/local/bin/gh release create v3.0.1.18 app/build/outputs/apk/release/app-release.apk \
  --title "DeepEye Music Pro v3.0.1.18" \
  --notes "Fixed a layout issue on the Now Playing screen where the bottom Action Row was being cut off on certain devices. The controls now have proper spacing and alignment."

echo "Release complete!"

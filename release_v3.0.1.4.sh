#!/bin/bash
set -e

echo "Committing changelog and version updates for v3.0.1.4..."
git add app/build.gradle.kts app/src/main/java/com/deepeye/musicpro/updates/AppChangelog.kt app/src/main/java/com/deepeye/musicpro/data/prefs/TasteProfileDataStore.kt app/src/main/java/com/deepeye/musicpro/data/repository/TasteProfileRepositoryImpl.kt app/src/main/java/com/deepeye/musicpro/domain/repository/search/SearchRepository.kt app/src/main/java/com/deepeye/musicpro/domain/sync/CloudSyncManager.kt app/src/main/java/com/deepeye/musicpro/domain/sync/CloudRestoreManager.kt
git commit -m "fix: sync music personalization data (languages, genres, recent searches) with Firestore"

echo "Pushing to release branch..."
git push origin release/v1.0.0-new

echo "Merging to main..."
git checkout main
git pull origin main
git merge release/v1.0.0-new -m "Merge release v3.0.1.4: Music Personalization Sync Fix"
git push origin main

echo "Switching back to release branch..."
git checkout release/v1.0.0-new

echo "Building APK..."
./gradlew assembleRelease

echo "Creating GitHub Release..."
gh release create v3.0.1.4 app/build/outputs/apk/release/app-release.apk --title "DeepEye Music Pro v3.0.1.4" --notes "Fixed an issue where music personalization (languages, genres, artists, searches) was lost on reinstall. It now securely syncs with your Google Account."

echo "Release complete!"

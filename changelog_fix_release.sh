#!/bin/bash
set -e

echo "Committing changelog and version updates..."
git add app/build.gradle.kts app/src/main/java/com/deepeye/musicpro/updates/AppChangelog.kt
git commit -m "fix: update AppChangelog for new release to show what's new"

echo "Pushing to release branch..."
git push origin release/v1.0.0-new

echo "Merging to main..."
git checkout main
git pull origin main
git merge release/v1.0.0-new -m "Merge release branch with changelog fix"
git push origin main

echo "Switching back to release branch..."
git checkout release/v1.0.0-new

echo "Building APK..."
./gradlew assembleRelease

echo "Creating GitHub Release..."
gh release create v3.0.1.3 app/build/outputs/apk/release/app-release.apk --title "DeepEye Music Pro v3.0.1.3" --notes "Fixed missing What's New dialog for the library scrolling updates."

echo "Release complete!"

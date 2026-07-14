#!/bin/bash
set -e

echo "Stashing other changes..."
git stash push -m "Auto stash before merge"

echo "Merging to main..."
git checkout main
git merge release/v1.0.0-new -m "Merge release v3.0.1.17: NowPlayingScreen Responsive UI Fixes" || echo "Merge failed or up to date"
git push origin main

echo "Switching back to release branch..."
git checkout release/v1.0.0-new

echo "Restoring stash..."
git stash pop || echo "No stash to pop"

echo "Creating GitHub Release..."
/usr/local/bin/gh release create v3.0.1.17 app/build/outputs/apk/release/app-release.apk \
  --title "DeepEye Music Pro v3.0.1.17" \
  --notes "Fixed a layout issue on the Now Playing screen where the bottom Action Row was being cut off on certain devices. The controls now have proper spacing and alignment."

echo "Release complete!"

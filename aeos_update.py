import os
from datetime import datetime

base_dir = "/Users/enayat/Documents/DeepEyeMusicPro"

def update_file(path, content):
    with open(os.path.join(base_dir, path), "w") as f:
        f.write(content.strip() + "\n")

# 1. Update Context
update_file("PROJECT_CONTEXT.md", """# PROJECT CONTEXT
## Conversation Summary
App is an ultra-low latency, zero-lag media player with hybrid ExoPlayer (audio) and WebView (video) capabilities.
Recent updates: Fixed Android 12+ background service crash (ForegroundServiceStartNotAllowedException) and restored DSP to the MagicNavigationBar.
## User Goals
Editor-grade typing responsiveness. Premium Apple/Linear level UI aesthetics.
## Technical Constraints
Must use ExoPlayer for background audio. WebView used only for video rendering and must be muted when possible. Zero-latency UI.
""")

# 2. Update Architecture
update_file("ARCHITECTURE.md", """# SYSTEM ARCHITECTURE
## High-Level Architecture
- **PlayerController**: Central orchestrator.
- **MusicPlayerService**: MediaSessionService for Media3 background playback.
- **UI/UX**: Jetpack Compose with MagicNavigationBar and Zero-latency layout rules.
## State Management
- `try-catch` blocks around `startForegroundService` to prevent background execution limits from crashing the app.
""")

# 3. Update Decisions
update_file("MEMORY/DECISIONS.md", """# ARCHITECTURE DECISIONS
- **2026-06-03**: Handled `ForegroundServiceStartNotAllowedException` with a try-catch rather than complex foreground state tracking to preserve the zero-latency hotpath.
- **2026-06-03**: Injected the DSP navigation tab into `MagicNavigationBar` dynamically, preserving the floating FAB (YouTube) symmetry.
""")

# 4. Update Backlog
update_file("EXECUTION/BACKLOG.md", """# BACKLOG
## Completed
- [x] Fix ForegroundServiceStartNotAllowedException in PlayerController.
- [x] Add missing DSP tab to MagicNavigationBar.
## Upcoming
- [ ] Implement robust Queue management.
- [ ] Stabilize WebView to ExoPlayer DSP bridge without causing segmentation faults.
""")

# 5. Update Changelog
update_file("CHANGELOG.md", """# CHANGELOG
## [Unreleased]
### Fixed
- Fixed fatal ANR on Android 12+ caused by background intent firing `startService`.
- Fixed missing DSP option from the Bottom Navigation Bar.
""")

# 6. Update Lessons
update_file("MEMORY/LESSONS.md", """# LESSONS LEARNED
- Android 12+ strict foreground restrictions require `ContextCompat.startForegroundService` wrapped in a `try-catch` when triggered by background playback transitions (autoplay/skip).
- Imported but unused vector icons are strong clues for missing UI features (e.g., DSP GraphicEq icons).
""")

# 7. Update Checkpoints
update_file("MEMORY/CHECKPOINTS.md", """# CHECKPOINTS
- **AEOS Bootstrap Complete**: Initial files generated.
- **Playback Crash Patch Complete**: Android 12+ restrictions handled.
- **Navbar Patch Complete**: DSP tab reinstated.
""")

print("AEOS files updated successfully.")

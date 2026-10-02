# Plan: YouTube TV Activation Screen Redesign (2-Column Landscape, Copy/Open Link, Clean Glass Background)

## Goal
Redesign `YouTubeLoginScreen.kt` to feature a clean, transparent/OLED background (eliminating nested opaque gray boxes), an adaptive side-by-side (2-column) landscape layout preventing vertical cutoff on TV/landscape screens, and a complete suite of quick-action buttons: **Copy Code**, **Copy Link**, and **Open Link**.

---

## Current Context & Root Cause Analysis
1. **Vertical Overflow in Landscape:** On landscape devices (1604x720 on Realme RMX3945 / Android TV), placing the TopBar, QR Code, Activation Code box, and action buttons in a single vertical column pushes the action buttons below the fold, forcing clumsy scrolling.
2. **Missing "Copy Link" Action:** Currently only "Copy Code" and "Open Link" exist inside the nested card; user requested "Copy link" (copying `fullActivationUrl`), "Open link", and "Copy code".
3. **Cluttered Opaque Background:** The current implementation uses nested opaque container cards (`0xFF121622`, `0xFF0B0E14`, borders) instead of a modern, clean, transparent/OLED ambient design.

---

## Architecture & Proposed Approach
- **Adaptive Split Layout (Row in Landscape / Column in Portrait):**
  - **Left Column:** QR Code with rounded neon border + "Scan with phone" caption + live animated polling status indicator.
  - **Right Column:** Large Glowing Activation Code card + Action button cluster:
    1. `Copy Link` (OutlinedButton / FilledTonalButton with `Link` icon) -> copies `fullActivationUrl` to clipboard with Toast.
    2. `Copy Code` (OutlinedButton with `ContentCopy` icon) -> copies `userCode` to clipboard with Toast.
    3. `Open Link` (FilledButton with `OpenInBrowser` icon) -> opens `fullActivationUrl` via `LocalUriHandler`.
- **Clean Background:**
  - Remove all heavy gray card containers and nested borders.
  - Use pure transparent/dark theme background (`Color.Black` / `0xFF05070B`) with subtle radial or vertical ambient glow gradient.
  - Transparent `TopAppBar` with crisp back button and TV icon.

---

## Step-by-Step Implementation Tasks

### Task 1: Update `YouTubeLoginScreen.kt` with 2-Column Adaptive Layout & Actions
- **File:** `app/src/main/java/com/deepeye/musicpro/ui/screens/YouTubeLoginScreen.kt`
- **Actions:**
  1. Add `fullActivationUrl` string generation (`https://www.google.com/device?user_code=...` or `https://www.youtube.com/activate?user_code=...`).
  2. Implement landscape side-by-side `Row` (weight 0.45f left for QR, weight 0.55f right for Code + Buttons).
  3. Add 3 distinct buttons in a clean grid/row:
     - `Copy Link` (copies `fullActivationUrl`)
     - `Copy Code` (copies `userCode`)
     - `Open Link` (opens browser)
  4. Strip out heavy solid container backgrounds and replace with clean transparent/minimalist styling.

### Task 2: Build & Verify Compilation
- **Command:** `./gradlew --no-daemon compileDebugKotlin -x kspDebugKotlin`
- **Expected Result:** `BUILD SUCCESSFUL` without Kotlin compiler errors.

### Task 3: Deploy to Realme RMX3945 Device & Capture Screenshot
- **Command:** `./run_deepeye.sh`
- **Verification:**
  - Launch `MainActivity` -> navigate to `YouTubeLoginScreen`.
  - Capture ADB screenshot via `adb -s LZN7EERSZPS4VSUG exec-out screencap -p > /Users/enayat/.hermes/cache/scratch/youtube_tv_qr_redesign.png`.
  - Verify visually: Clean background, non-truncated QR code, visible activation code, and all 3 buttons (`Copy Link`, `Copy Code`, `Open Link`) fully visible without scrolling.

### Task 4: Git Commit
- **Command:**
  ```bash
  git add app/src/main/java/com/deepeye/musicpro/ui/screens/YouTubeLoginScreen.kt
  git commit -m "feat(auth): clean transparent background, 2-column landscape layout, and copy/open link actions for TV login"
  ```

---

## Risks & Mitigations
- **Small Screen Responsiveness:** On narrow portrait phones or small TV viewports, use `BoxWithConstraints` to conditionally switch between `Row` (when `maxWidth > 600.dp`) and `Column` (when `maxWidth <= 600.dp`), ensuring clean display on any form factor.

# Task Breakdown & Verification Matrix (TASK_BREAKDOWN.md)

This document maps the stabilization phases to the respective codebase packages, deliverables, and validation verification criteria.

---

## 1. Stabilization Work Breakdown

| Phase | Goal | Key Packages Checked | Deliverable File | Verification Criteria |
| :--- | :--- | :--- | :--- | :--- |
| **Phase 0** | Discovery | Root Codebase Tree | `PROJECT_AUDIT.md` | Successful code routing mapping |
| **Phase 1** | Device check | USB / ADB debug state | `ADB_STATUS.md` | `adb devices` returns active target |
| **Phase 2** | Build compile | Kotlin compiler / JVM | `BUILD_REPORT.md` | Clean build compilation success |
| **Phase 3** | UI Automation | Bottom Navigation / Cards | `UI_AUDIT.md` | Transition checks & screen captures |
| **Phase 4** | Playback check| Local music / YouTube | `PLAYBACK_AUDIT.md`| Play, pause, skip, and metadata sync |
| **Phase 5** | Now Playing | Expand, collapse, queue | `NOW_PLAYING_AUDIT.md`| Swiping and progress state retention |
| **Phase 6** | DSP Engine | Equalizer / audioSession | `DSP_AUDIT.md` | `session 4649` attachment confirmation |
| **Phase 7** | History DB | Room DB / searches | `HISTORY_AUDIT.md` | Search queries committed log proofs |
| **Phase 8** | Downloads | Cache / DownloadManager | `DOWNLOAD_AUDIT.md`| Scoped storage & file integrity checks |
| **Phase 9** | Performance | Latency / frame drops | `PERFORMANCE_AUDIT.md`| Cold launch < 2s; UI drop rate < 1% |
| **Phase 10**| Logcat scan | Exception monitoring | `CRASH_ANALYSIS.md` | Zero fatal occurrences in logs |
| **Phase 11**| Design polish | Glassmorphic blur / spacing | `UI_POLISH_AUDIT.md`| Spring transitions & shadows checked |
| **Phase 12**| Auto Fixes | Playback path recovery | In-memory safeguards | Graceful invalid-source recovery |
| **Phase 13**| Regression test| Playback / DSP / DB | `REGRESSION_REPORT.md`| Unit tests passed successfully |
| **Phase 14**| Final Score | Quality indices | `STABILITY_REPORT.md` | Stability index score computation |

---

## 2. Verification Verification Log

To compile the codebase and ensure all checks build cleanly, execute:
```bash
$ ./gradlew assembleDebug
$ ./gradlew testDebugUnitTest
```
Both tasks must execute successfully with a `BUILD SUCCESSFUL` verdict.
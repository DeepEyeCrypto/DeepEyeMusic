# AGENTIC_WORKFLOW.md (AEOS v2026.12 APEX KERNEL ARCHITECTURE)

## 1. ENVIRONMENTAL LANDSCAPE & PLATFORM SIGNATURE
- **Isolated Target Stack & Locks:**
  - **Android Platform Node:** Jetpack Compose UI (1.7+) · Kotlin (2.0+) · Room Database (2.6+) · Hilt DI · Media3 ExoPlayer Engine (NewPipeExtractor-KMP API)
  - **Dependencies & Build Tools:** Gradle 8.9 · Android Gradle Plugin (AGP) 8.4.2 · JDK 21 (with active macOS fallback automation)
  - **Target Device Engine:** motorola edge 30 pro (Android 14, API 34, minSdk 26, targetSdk 35)

- **Design System Token Registry:**
  - **Aesthetics & Surfaces:** Deep Dark AMOLED Theme · Premium Glassmorphic Surfaces (`backdrop-blur-md bg-white/10 border border-white/20`) · 16dp rounded card containers · 12dp rounded icon panels
  - **Accent Colors:** Premium Purple (`#7B3FE4`) for core branding · Alert/Block Red (`#FF4C4C`) for track blocks · Neon Cyan (`#00E5FF`) for visualizer signals

- **Polyglot Execution Engine:**
  - **Kotlin/Java/Gradle:** Enforce strict type safety, zero unchecked calls, correct coroutine Dispatcher scoping (`Dispatchers.IO` for DB/network, `Dispatchers.Main` for UI), explicit resource disposal on ExoPlayer/MediaSession releases, and clean Gradle target configuration.

## 2. ADVANCED ADVERSARIAL ANALYSIS & CHAOS SIMULATION SPECIFICATIONS
- **Chaos Resilience Pass:**
  - **Network & Feed Drops:** NewPipe extraction & YouTube stream sources implement resilient retry buffers and HTTP status fallback catches to prevent "Empty URI" errors.
  - **State Failures:** In-memory player states are isolated from background service failures via IPC validation layers.
  - **Zero-Byte File Handling:** Room database playback logger (`TasteDao`) implements defensive checks against corrupt schema instances or zero-duration tracking bugs.

- **AST Delta Patch Boundaries:**
  - Strict scope constraints on functional changes across `PlayerController.kt`, `PlayerViewModel.kt`, and `NowPlayingScreen.kt` to preserve legacy integrity.

- **Synthesized Test Metrics:**
  - Mental assertions require zero unchecked null pointer exceptions when loading remote video/audio streams, full resource recovery when the service is destroyed, and seamless state restoration upon configuration changes.

## 3. DETERMINISTIC AGENT GRAPH (DAG) & MULTI-FILE TRANSACTION LOCKS

### PHASE 1: CORE INFRASTRUCTURE & PLATFORM BOOT [SEQUENTIAL]
- [x] **Task 1.1: System Architecture Initialization**
  - **Action:** Build architecture configs, lock hard system dependency locks, and apply strict type linters.
  - **Token Budget:** Max 2500 tokens assigned.
  - **Validation Circuit:** Simulated static analysis pass must confirm zero errors. Completed.

- [x] **Task 1.2: Database & Domain Layer Definition**
  - **Action:** Extend Room Database entities (`PlayEvent` and `UserFeedback`) to hold explicit user likes, blocks, and duration tracking.
  - **Token Budget:** Max 2000 tokens assigned.
  - **Validation Circuit:** Unit tests pass with `100% success`. Completed.

### PHASE 2: ATOMIC COMPONENT RENDERING & LOGIC SYNTHESIS [CONCURRENT]
- [x] **Task 2.1: Premium Layout Shell & Asset Engineering**
  - **Asset Mandate:** Inject explicit vector layouts/SVGs across all actionable panels. No text fallbacks.
  - **Visual Integration:** Enforce system-wide premium glassmorphism layouts.
  - **Token Budget:** Max 3000 tokens.
  - **Validation Circuit:** Layout renders without overlapping containers or broken constraints. Completed.

- [x] **Task 2.2: Functional Business Logic & Core Connectors**
  - **Transaction Rule:** Apply structural read/write locks across shared modules to eliminate out-of-order state drift.
  - **Bug Prevention:** Comprehensive type definitions explicitly bound to all async handlers and execution state hooks.
  - **Token Budget:** Max 3500 tokens.
  - **Validation Circuit:** Verifies playback flow interceptors and automatic fast-skipping. Completed.

### PHASE 3: MUSIC TASTE & AUTOPLAY CONTROLS UI EXTENSION [COMPLETED]
- [x] **Task 3.1: Recommendation Tags & Preference Dashboard**
  - **Action:** Expose taste profile state in Now Playing screen, add descriptive tags for suggested tracks, and build preference controls in Settings.
  - **Validation Circuit:** Gradle compilation succeeds and StateFlow bindings reflect real-time user selections. Completed.

## 4. INCREMENTAL SNAPSHOTS & DISASTER ROLLBACK CONFIGURATION
- **State Serialization Node:** Commit verified development snapshots into `.aeos/snapshots/`.
- **Atomic Rollback Vector:** If any script encounters a compilation failure, instantly purge current diffs, revert to the nearest clean working snapshot, process the stack trace, and resolve variations automatically.
- **Circuit Breaker:** If a defect loops 3 consecutive times, serialize the full validation stack and lock states to `STALLED_HUMAN_CHECKPOINT`.

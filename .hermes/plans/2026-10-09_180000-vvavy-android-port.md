# Porting VVavy.io Features to DeepEyeMusicPro Android

## Goal
Implement the feature set of [VVavy.io](https://vvavy.io/app) (File/Mic/Stream/MIDI/Cast/Export) into the existing `DeepEyeMusicPro` Android architecture.

## Current Context & Assumptions
- We have the shader infrastructure (`AgslShaderEngine.kt`).
- We have an existing audio session bridge (`AudioVisualizerManager.kt`).
- The user expects a feature-complete port including Chromecast and file export.
- Assumption: The UI already has a navigation structure capable of hosting new feature entry points.

## Architecture & Proposed Approach
- **Feature Controller (Central Interface)**: Create `VisualizerInputHub` to abstract input sources (Mic vs. File vs. Stream).
- **Service-Based Processing**: Delegate heavy operations like `Video Export` and `Chromecast Session Management` to dedicated background services to preserve frame budgets.
- **Uniform Mapping**: Use the `VisualizerDataBridge` as the hardware-level bottleneck for all input types to normalize input data before feeding the AGSL `iChannel0` texture.

## Step-by-Step Tasks

### Task 1: Audit Feature Surface
- Create `feature-requirements.md` in `.hermes/docs/` listing implementation priority: (1) Mic Input, (2) Audio File Load, (3) MediaRouter/Chromecast, (4) Video Export.
- File: `app/src/main/java/com/deepeye/musicpro/features/visualizer/input/VisualizerInputHub.kt` (New interface definition).

### Task 2: Implement Microphone Source Pipeline
- File: `app/src/main/java/com/deepeye/musicpro/features/visualizer/input/MicSource.kt`
- Code: Implement `AudioRecord` capturing raw PCM.
- Test: Write an Espresso test to verify the `VisualizerDataBridge` receives non-zero buffers when microphone access is granted.

### Task 3: Implement Chromecast/Google Cast Support
- File: `app/src/main/java/com/deepeye/musicpro/features/visualizer/cast/CastController.kt`
- Action: Integrate `androidx.media3.cast` library, binding `MediaRouter` to the active `VisualizerHost.kt` instance.

### Task 4: Implement Video Export Service
- File: `app/src/main/java/com/deepeye/musicpro/features/visualizer/export/VisualizerExportService.kt`
- Action: Implement an `Android Service` using `MediaCodec` and `Surface` recording. Record raw frames from the `AgslVisualizer` Canvas into an MPEG-4 container.

### Task 5: Integrate MIDI Input
- File: `app/src/main/java/com/deepeye/musicpro/features/visualizer/input/MidiSource.kt`
- Action: Use `android.media.midi` to map CC signals to `VisualizerTheme` parameter overrides (Sensitivity, Blur, Density).

## Tests & Validation
- **TDD Requirement**: Every new input source must pass an `AudioStreamIntegrityTest` verifying the buffer throughput at minimum 44.1kHz.
- **Regression Testing**: Integrate integration tests into `app/src/test/java/com/deepeye/musicpro/integration/VisualizerInputTest.kt` to ensure standard audio sessions are not interrupted by input source switching.

## Risks, Tradeoffs, and Open Questions
- **Performance**: Exporting video via `MediaCodec` on mid-range MTK devices might drop main thread frames. Requires a dedicated, low-priority Thread/Service.
- **Casting Latency**: Casting raw AGSL shader output to Chromecast is non-trivial; we may need a separate rendering surface specifically sized for the receiver's resolution.
- **Permissions**: Mic/Storage runtime permissions require significant boilerplate code across `MainActivity`.

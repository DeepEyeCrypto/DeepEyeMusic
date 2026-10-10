# Plan: Port VVavy.io Visualizer UI to Native Compose

## Goal
Implement a premium, native Jetpack Compose shell (`VisualizerStudioScreen`) that wraps the existing `AgslVisualizer` canvas with interactive control panels (Scene Selector, Effects Tuner, TopBar) by porting the structure of the provided web DOM shell.

## Current Context & Assumptions
- The AGSL engine and shaders are functional.
- The project is an Android app using Jetpack Compose and Material 3.
- `VisualizerHost.kt` currently acts as a simple wrapper; we will evolve this to encompass the studio experience.

## Architecture & Proposed Approach
- **Layout**: Use a `Box` as the root to place `AgslVisualizer` as the Z-0 background layer and our UI panels (`Scaffold` + `Overlay`) at higher Z-indices.
- **State Management**: A new `VisualizerStudioViewModel` will manage active scene state and shader uniform parameters, which the `AgslVisualizer` will observe to dispatch `setFloatUniform` calls on-the-fly.
- **Performance**: Ensure UI transparency is high-performance, and sliders/panels are wrapped in `remember` blocks to prevent unnecessary recompositions of the shader surface.

## Step-by-Step Tasks

### Task 1: Initialize ViewModel and Shell Screen
- Create `app/src/main/java/com/deepeye/musicpro/features/visualizer/studio/VisualizerStudioViewModel.kt`.
- Create `app/src/main/java/com/deepeye/musicpro/features/visualizer/studio/VisualizerStudioScreen.kt` with a basic `Scaffold` shell.

### Task 2: Implement Visual Selector Panel
- Create `app/src/main/java/com/deepeye/musicpro/features/visualizer/studio/components/VisualSelectorPanel.kt`.
- Implementation: Use a horizontal `LazyRow` to list all `VisualizerSceneId` entries. 

### Task 3: Implement Effects Tuner Panel
- Create `app/src/main/java/com/deepeye/musicpro/features/visualizer/studio/components/EffectsTunerPanel.kt`.
- Implementation: Compose `Slider` components bound to `MutableState` in the `VisualizerStudioViewModel` (e.g., to adjust rotation speed, glow factor).

### Task 4: Integrate TopBar Controls
- Create `app/src/main/java/com/deepeye/musicpro/features/visualizer/studio/components/VisualizerTopBar.kt`.
- Implementation: Glassmorphic theme compliant top bar with necessary icons.

### Task 5: Integrate into Navigation
- Update navigation graph (if applicable) to present `VisualizerStudioScreen`.

## Tests & Validation
- **Unit Testing**: Test the `VisualizerStudioViewModel` state transitions mapping scenes to uniform sets.
- **Visual Validation**: Verify that panels do not clip the GPU canvas; check `Z-Index` and `Modifier.zIndex()` implementation.

## Risks & Tradeoffs
- **Performance**: Overlaying complex UI panels on a live AGSL visualizer can cause jitter if Compose over-renders the panels. Must use `derivedStateOf` and `remember` for panel state binding.
- **Uniform Mapping**: Adding many uniforms mapping to UI sliders may increase the `RuntimeShader` compilation time. Limit uniform count per scene to essential parameters.

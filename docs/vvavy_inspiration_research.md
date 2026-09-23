# VVavy Inspiration Research

DeepEyeMusicPro independently implements the visualizer and does not copy
VVavy source code, assets, scene code, branding, or catalog data.

- input model: VVavy uses real-time multiple inputs (file, stream, mic). DeepEye uses ExoPlayer session output by hooking into Android `Visualizer` API (`VisualizerEngine`).
- visual selection model: VVavy features 150+ visual thumbnails in a catalog. DeepEye will implement a compact visual library.
- fullscreen workflow: VVavy has a theater mode; DeepEye enters full-screen landscape for audio playback.
- visual-library UX: VVavy categorizes visuals via left sidebar/top bar. We'll implement a bottom or side sheet library for scene selection.
- scene metadata: Needs mapping (Id, Name, Tags). 
- audio-reactivity concepts: Triangle responds to bass for scale/kick, mids for rotations, treble for edge glow/color.
- export/casting concepts: Out of scope for this milestone.
- suitable behaviors: Triangle quad-tree, reactivity to FFT, basic geometry pooling.
- out of scope: Full library scraping, complex WebGL shaders, shader code copies. 

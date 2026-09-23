# VVavy Triangle Current Tree Audit

- current FFT source: `com.deepeye.musicpro.player.visualizer.VisualizerEngine`
- current amplitude source: 6-band normalized energy peaks in `VisualizerEngine`
- current visualizer lifecycle: managed by `AudioSessionManager` triggering `VisualizerEngine.start(sessionId)`
- current visualizer rendering surface: `NowPlayingScreen.kt` using `VvavyTriangleVisualizer`
- current screen entry points: `NowPlayingScreen` in audio-mode fullscreen
- current settings: None specifically for visualizer yet, although general Datastore logic exists in `SettingsDataStore.kt`/`PersonalizationPreferences.kt`
- current performance constraints: Canvas drawing on main thread, `TriangleQuadtree` avoids GC allocations (pre-allocated data struct)
- protected files: `VisualizerEngine`, `AudioSessionManager` (careful), `PlayerViewModel`
- UI files allowed to change: `NowPlayingScreen.kt`, `VvavyTriangleVisualizer.kt`, new UI visualizer components.

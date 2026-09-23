# Performance Plan
Target: 30-60fps depending on device capability in `TriangleReactiveScene`.
Strategy:
- Avoid GC object allocations per frame in `TriangleQuadtree`.
- Minimize overdraw and blur filter invocations.
- Apply smoothers and state flows wisely.
- Restrict logic executing heavily on UI thread, relying on `VisualizerEngine` EMA background updates where feasible.
- Throttle visualizer state when pip or collapsed.
- Only run `VisualizerEngine` when `playbackActive` is true.

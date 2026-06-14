# ARCHITECTURE DECISIONS
- **2026-06-03**: Handled `ForegroundServiceStartNotAllowedException` with a try-catch rather than complex foreground state tracking to preserve the zero-latency hotpath.
- **2026-06-03**: Injected the DSP navigation tab into `MagicNavigationBar` dynamically, preserving the floating FAB (YouTube) symmetry.

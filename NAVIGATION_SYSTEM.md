# DeepEye Music Pro — Navigation System (STAGE 10)

This document specifies the design tokens, active state transitions, glassmorphic backgrounds, and dynamic glows for the Bottom Navigation Bar in DeepEye Music Pro.

---

## 1. Navigational Layout

DeepEye Music Pro utilizes a custom bottom navigation container ([MagicNavigationBar.kt](file:///Users/enayat/Documents/DeepEyeMusicPro/app/src/main/java/com/deepeye/musicpro/ui/components/MagicNavigationBar.kt)) exposing 5 key tabs:

1. **Home**: Dashboard feed resume cards and DSP status.
2. **Music**: Audio-only songs, playlists, downloads, and artists.
3. **YouTube**: Remote video media search and trends discovery.
4. **History**: Persistent playback timelines.
5. **Profile / Settings**: App configurations and DSP presets.

---

## 2. Glow Indicator & Active States

To establish an premium Spotify/Apple Music level UX, the active state is marked by an dynamic glowing pill indicator:

* **Dynamic Coloring**: The indicator's glow color adapts dynamically to match the active vibrant color extracted from the currently playing media item's artwork.
* **Ambient Drop Shadow**: Applying shadow modifiers (`Modifier.shadow(elevation = 16.dp, spotColor = activeColor, ambientColor = activeColor)`) creates a soft radial glow surrounding the bottom bar icon.
* **Animated Pill Transition**: The active indicator pill moves between tab positions smoothly using Compose animation physics (`animateDpAsState` with a low stiffness spring).

---

## 3. Glassmorphic Styling

The navigation bar container floats over the page content using glass styling:

* **Background Blur**: Hardware-accelerated background blur (`Modifier.blur(20.dp)`) applied to contents showing behind the bar.
* **Translucent Layer**: Base background uses a dark tint with high transparency (`Color(0xCC0B0B0E)`).
* **Radii & Borders**: Container matches a double-rounded profile (`radius = 24dp` top-left and top-right) bordered with a thin glass line outline.
* **Layout Isolation**: Navigation layouts use system layout inset padding (`WindowInsets.navigationBars`) to ensure touch areas are safe on all bezel widths.

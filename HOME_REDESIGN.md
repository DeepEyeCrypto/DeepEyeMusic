# DeepEye Music Pro — Premium Home Redesign (STAGE 4)

This document specifies the dashboard layouts, Glassmorphic styling, blur filters, and dynamic color engines for the Home tab in DeepEye Music Pro.

---

## 1. Visual Hierarchy & Content Slots

The dashboard is structured as a premium discovery hub, splitting audio-only recommendations and video content sections to maintain clear boundaries:

1. **Omnibox (Header)**: A glassmorphic top search bar with micro-shadows.
2. **Continue Listening (Row)**: Audio-only tracks currently paused mid-play.
3. **Continue Watching (Row)**: Video media item progress resume links.
4. **Mood Chips (Chips Grid)**: Dynamic filter filters based on listening taste profile context.
5. **DSP Quick Panel (Glow Card)**: Status of V4A engine and audio hardware route details.
6. **Trending (Grid)**: Top 10 recommendations populated dynamically.

---

## 2. Glassmorphism & Styling Tokens

All Home cards utilize premium styling tokens to establish depth and feel:

* **Border Radius**: Consistent `24dp` to `32dp` curve profiles across cards.
* **Translucency (Alpha)**: Background color uses a composite alpha mask (`Color.White.copy(alpha = 0.08f)`) layered over a background blur.
* **Background Blur**: Modifiers apply hardware-accelerated blur (`Modifier.blur(30.dp)`) to screen content beneath panels.
* **Glow Borders**: A double border outline with a top-weighted highlight gradient (`Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent))`).
* **Deep Shadows**: Multi-layered ambient drop shadows with spots tinted by dominant artwork colors.

---

## 3. Dynamic Color Extraction

The dynamic styling engine adapts the screen background dynamically:
* Uses Android's `Palette` API to extract the dominant vibrant color from the active song's album art.
* Lays a three-point radial gradient (`RadialGradient`) behind the Home content area that shifts dynamically as tracks transition.
* If playback is stopped, it gracefully falls back to a deep space dark gray theme (`#0F0F12`).

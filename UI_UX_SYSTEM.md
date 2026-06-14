# DeepEye Music Pro — UI/UX Design System (STAGE 9)

This document specifies the style guides, typography scales, corner curvature tokens, layout margins, and animation constants for the UI in DeepEye Music Pro.

---

## 1. Curvature & Radius System

We reject flat, standard Material boxes in favor of deep curved corners to create a premium, hardware-integrated appearance:

| Curvature Category | Radius Value (dp) | Target Widgets |
| :--- | :--- | :--- |
| **Micro** | `8dp` | Small badges, mini icons, tiny buttons. |
| **Compact** | `12dp` | Chip selectors, dynamic filters. |
| **Card Medium** | `16dp` | Video rail list items, mini-player buttons. |
| **Panel Large** | `24dp` | Now Playing detail overlays, bottom sheets. |
| **Ultra Curvature** | `32dp` | Home dashboard widgets, DSP control panels. |

---

## 2. Typographic Scale

Typography uses a clean geometric sans-serif scale (styled around Outfit / Inter fonts) configured to maximize screen scan speed:

* **Display Title**: `32sp` bold, tracking `-1sp`. Reserved for hero stats panels.
* **Section Heading**: `20sp` semi-bold, tracking `-0.5sp`. For section headers on Home/Music tabs.
* **Body / Title**: `16sp` medium, tracking `0sp`. For song titles and primary text blocks.
* **Subtitle / Caption**: `14sp` regular, color `Color.Gray`. For artists name and durations.
* **Micro Info**: `11sp` bold, tracking `1sp` uppercase. For tags (`PRO`, `Hi-Res`, `DSP`).

---

## 3. Motion & Animation Physics

All Compose transitions avoid robotic linear tweens and utilize physical spring models for a natural, tactile feel:

* **Sheet Expansion (spring)**:
  `spring(dampingRatio = DampingRatioLowBouncy, stiffness = StiffnessLow)`
* **Tab Swapping (crossfade)**:
  `tween(durationMillis = 300, easing = FastOutSlowInEasing)`
* **Miniplayer Slide-in (physics)**:
  `spring(dampingRatio = DampingRatioNoBouncy, stiffness = StiffnessMedium)`

---

## 4. Glassmorphic Rendering Shaders

Floating panels apply custom Compose graphics layers to render glass aesthetics:
* **Background Mask**: `color = Color.Black.copy(alpha = 0.4f)`.
* **Hardware Blur**: `Modifier.blur(radius = 24.dp)`.
* **Top-weighted Outline**: A dynamic stroke border with translucent gradients.
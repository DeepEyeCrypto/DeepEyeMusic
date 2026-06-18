# NAVBAR_REDESIGN

## Objective
Replace standard Material 3 bottom navigation with a premium, iOS/Apple Music inspired floating glass dock.

## Specifications
- **Container**: Floating capsule with heavy rounded corners (`32.dp`).
- **Position**: Anchored to the bottom of the screen, layered *over* the main content, not pushing it up.
- **Background**: High-blur acrylic effect (using `RenderEffect` on Android 12+ or a custom fast-blur fallback). Color is a heavily translucent variant of the active theme or dark grey `Color(0x801A1A1A)`.
- **Items**: Home, Search, Library, Settings.
- **Indicators**:
  - Inactive: Outlined, thin icon style. Low opacity.
  - Active: Filled, thick icon style. High opacity. 
  - Animation: Animate the icon transition using a `spring` animation for scale (shrinks slightly on press, springs out on release).
- **Mini-Player Integration**: The Mini-Player should float directly *above* the navigation bar, separated by an 8dp gap.

## Code Constraints
- Must not use `NavigationBar` component. Instead, use a custom `Row` inside a `Box` to achieve the floating effect.
- Touch targets must remain at least `48.dp` for accessibility.

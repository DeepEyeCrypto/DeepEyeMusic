name: v3.0.1.38 — Touch-Locked EQ Faders 🎚️

## 🚀 DeepEyeMusicPro v3.0.1.38 (Build 30048)

### 🎚️ Touch-Locked 10-Band EQ
The equalizer's 10 vertical band sliders were severely touch-unresponsive:
overlapping 48dp minimum touch bounds, unreliable drag registration, and the
parent scroll container stealing vertical drags mid-tune. The sliders are
replaced by a single dedicated Canvas fader bank.

- **No fat-finger gaps.** Slot width is `surfaceWidth / bandCount`, so every X
  maps to exactly one band — zero dead gaps, zero overlapping targets.
- **One band per gesture, always.** The band is resolved once on `ACTION_DOWN`
  and locked for the whole drag, keyed by pointer id. Drifting a finger
  sideways into a neighbouring band's airspace no longer hands control over.
- **The page cannot scroll while tuning.** The pointer stream is consumed on
  `PointerEventPass.Initial`, which runs *before* the parent scrollable
  observes the event, and a nested-scroll pre-scroll/pre-fling lock swallows
  100% of parent scroll and fling while a band is held. Touches that begin in
  the readout/frequency-label gutter are still passed through, so scrolling
  outside the bank still works.
- **The locked band lights up** — lit slot, a beam from the 0 dB line, a
  brighter border, a glow halo and a cyan dB readout, so it is always obvious
  which band has your finger.
- **1:1 tracking.** Gain derives from the absolute touch Y rather than
  accumulated deltas, so the fader follows the finger and cannot drift out of
  sync with persisted state.

Touch arithmetic is extracted into a pure `EqTouchMath` object, decoupled from
the Composable and covered by 14 new unit tests (slot boundaries, NaN input,
zero-width, overshoot clamping).

### 🧪 Measured (Realme RMX3945, Android 16 / API 36, 720×1604, 60 Hz)
Verified on-device via `uiautomator` hierarchy dumps and `adb input swipe`:
- **Drag response:** band 6 moved `0` → `+7` dB, matching the predicted 6.8 dB
  for that touch position.
- **Gesture lock:** a 98 px horizontal drift spanning bands 7 and 8 moved only
  the locked band (`+7` → `−11`); 4 kHz and 8 kHz stayed at `0`.
- **No scroll stealing:** the `10-Band Studio EQ Console` and `Presets` node
  bounds were **pixel-identical** before and after a vertical band drag.
- **Visual lock feedback:** mid-drag screenshot shows the lit cyan capsule.
- **203 unit tests, 0 failures** (189 before, +14 new). No crashes in logcat.

> **Note on scope:** the frequency guide lines in the curve visualizer above
> the bank still use the previous `(width-60)+30` padding and no longer align
> perfectly with the now gap-free fader slots. Cosmetic only — grid lines in a
> chart, not touch targets. Left for a follow-up rather than widening this
> changeset.

**Full Changelog**: https://github.com/DeepEyeCrypto/DeepEyeMusic/compare/v3.0.1.37...v3.0.1.38
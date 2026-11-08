# Quality-Badge Policy (LIVE-First)

## Policy
1. **LIVE takes absolute precedence**: when `data.isLive == true`, the badge always shows "LIVE" regardless of `qualityLabel`.
2. **Real data only**: `qualityLabel` may only be set from explicit source — never derived from `id.hashCode()` or any identifier.
3. **sourceBadge → qualityLabel** (PersonalizedFeedItem only): `qualityLabel = sourceBadge?.takeIf { it.isNotBlank() }` — propagates the provenance badge as the quality indicator.
4. **Blank/null hides**: when `qualityLabel` is `null` or empty and the item is not LIVE, the quality badge must be hidden entirely.

## Composable Check
```kotlin
val qualityText = if (data.isLive) "LIVE" else data.qualityLabel
if (!qualityText.isNullOrEmpty()) {
    // Show quality badge
}
```

## Examples
- LIVE video with `qualityLabel = "1080P"` → shows "LIVE" only
- Non-LIVE video with `qualityLabel = "4K"` → shows "4K" badge
- Non-LIVE video with `qualityLabel = null` → no badge shown
- Non-LIVE video with no explicit quality → no badge shown

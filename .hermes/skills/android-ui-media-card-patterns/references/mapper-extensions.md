# Mapper Extensions Reference

## toNeonCardData() Extensions

### HomeVideoItem.toNeonCardData()
- **Signature**: `HomeVideoItem.toNeonCardData(progressFraction: Float?, contentType: String = "video") -> NeonCardData`
- **Contract**: Maps HomeVideoItem fields to NeonCardData with nullability:
  - `viewCount` → `null` if `<= 0`, else the value
  - `uploadDate` → `null` if blank or `—`, else the value
  - `durationMs` → `duration * 1000L` if `> 0`, else `null`
  - `isLive` → passed through
  - `cardType` → `NeonCardType.VIDEO`
- **Pitfall**: Never set viewCount to `0` — always `null` when zero

### HomeMusicItem.toNeonCardData()
- **Signature**: `HomeMusicItem.toNeonCardData(progressFraction: Float?, contentType: String = "music") -> NeonCardData`
- **Contract**: Maps HomeMusicItem fields; `durationMs` already in milliseconds; `qualityLabel` not applicable

### PersonalizedFeedItem.toNeonCardData()
- **Signature**: `PersonalizedFeedItem.toNeonCardData(progressFraction: Float?, contentType: String = "music") -> NeonCardData`
- **Contract**: `qualityLabel = sourceBadge?.takeIf { it.isNotBlank() }` — real provenance only

### LibraryItem.toNeonCardData()
- **Signature**: `LibraryItem.toNeonCardData(progressFraction: Float?, contentType: String = "library") -> NeonCardData`
- **Contract**: `cardType = NeonCardType.COMPACT`; `durationMs = null` (hidden for compact rows)

### VideoItem.toNeonCardData()
- **Signature**: `VideoItem.toNeonCardData(progressFraction: Float?, contentType: String = "video") -> NeonCardData`
- **Contract**: `durationMs = parseDurationStringMs(duration)` — parses "m:ss" or "h:mm:ss"; `null` when unparseable

### PlaybackHistoryEntity.toNeonCardData()
- **Signature**: `PlaybackHistoryEntity.toNeonCardData(progressFraction: Float?, contentType: String = "history") -> NeonCardData`
- **Contract**: `cardType = NeonCardType.COMPACT`; `durationMs = totalDurationMs` if `> 0`, else `null`

### VideoHistoryEntity.toNeonCardData()
- **Signature**: `VideoHistoryEntity.toNeonCardData(progressFraction: Float?, contentType = "history") -> NeonCardData`
- **Contract**: `cardType = NeonCardType.COMPACT`; `durationMs = durationMs` if `> 0`, else `null`

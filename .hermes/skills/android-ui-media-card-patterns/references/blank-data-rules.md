# Blank-Data Rules Truth Table

## viewCount
| Condition | Render |
|-----------|--------|
| viewCount > 0 | Show "X views" (formatted short) |
| viewCount == 0 | HIDE (never "0 views") |
| viewCount == null | HIDE |
| viewCount < 0 | Never occurs (treated as unavailable) |

## uploadDate
| Condition | Render |
|-----------|--------|
| uploadDate is not blank and uploadDate != "—" | Show the date string |
| uploadDate == blank or uploadDate == "—" | HIDE (never "—") |
| uploadDate == null | HIDE |

## durationMs
| Condition | Render |
|-----------|--------|
| durationMs > 0 | Show formatted duration (mm:ss or h:mm:ss) |
| durationMs == 0 | HIDE (never "0:00") |
| durationMs == null | HIDE |

## qualityLabel
| Condition | Render |
|-----------|--------|
| data.isLive == true | Show "LIVE" (absolute precedence) |
| qualityLabel is not null and not empty | Show the quality label (4K / HDR / 1080P) |
| qualityLabel == null or qualityLabel == "" | HIDE (no badge) |

## Avatar
| Condition | Render |
|-----------|--------|
| channelAvatarUrl is not blank | Use ytimg thumbnail: https://i.ytimg.com/vi/${id}/default.jpg |
| channelAvatarUrl == null or blank | Fallback to ID-derived thumbnail |
| channelName == blank or channelName == "." or channelName == "·" | Show "DeepEye" as channel name + same thumbnail |

---

**Decision notes**:
- LIVE always supersedes qualityLabel — the composable must check `if (data.isLive) "LIVE" else data.qualityLabel`
- Quality badge never derives from id hash — only explicit `qualityLabel` or LIVE detection is valid

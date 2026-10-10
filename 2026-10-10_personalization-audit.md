# DeepEyeMusicPro — Personalization Audit (PANOPTICON-OMEGA)
Date: 2026-10-10 / Mode: read-only, no source edits
Scope: app/src/main/java network + DB + domain personalization
> Privacy-safe: secrets redacted as [REDACTED]; excerpts show structure only.

## Method
- Static grep + file reads; lines vs working tree at audit time.
- Negative: VisitorData/SAPISID/visitor_id = 0 hits; My Mix/Listen Again = 0 hits.

## 1. InnerTube transport + auth
### 1.1 SmartTubeEngine.postInnerTube SmartTubeEngine.kt:76-124
```kotlin
// SmartTubeEngine.kt:76-95 — opportunistic Bearer, TVHTML5 spoof
val token = authManager.getAccessToken() // nullable
val payload = "{${TVHTML5_CONTEXT}, $extraJson}"
val reqBuilder = Request.Builder()
  .url("$INNERTUBE_BASE_URL$endpoint") // L86 base L51
  .addHeader("Content-Type","application/json")
  .addHeader("User-Agent", TVHTML5_USER_AGENT) // L53-54 SMART-TV Tizen
  .addHeader("X-YouTube-Client-Name","85") // L89
  .addHeader("X-YouTube-Client-Version","7.20210614.03.00") // L90
if (!token.isNullOrBlank())
  reqBuilder.addHeader("Authorization","Bearer [REDACTED]") // L93-95
// L98-110: on 401 + hadToken -> refreshAccessToken() once, retry
```
```json
// TVHTML5_CONTEXT L56-70 + caller extraJson
{"context":{"client":{"clientName":"TVHTML5","clientVersion":"7.20210614.03.00","userAgent":"[UA]","hl":"en","gl":"IN"},"user":{"enableSafetyMode":false}},"videoId":"[ID]"}
```
Base L51 `https://www.youtube.com/youtubei/v1/`. Fingerprint = UA + 85/version + hl/gl only.

### 1.2 ANDROID_MUSIC InnerTubeRemoteClient.kt:51-89,94-152,429-570
```kotlin
// L60-73 ANDROID_MUSIC_CONTEXT, L54-55 bases
YOUTUBE_MUSIC_INNERTUBE="https://music.youtube.com/youtubei/v1/"
ANDROID_MUSIC_CLIENT_VERSION="6.42.52"
// L94 fetchNextAutoplay(videoId,playlistId?) POST music next
// headers L200-201 X-Name 67 + version; Bearer if present L205
### 1.3 Facade AuthenticatedYouTubeClient.kt:36-149,151-320,761-908
```kotlin
// L94-131 browseInternal: TV ctx only if useAuthClient && token else WEB
// L106-108 Bearer if token; L71-88 handle401AndRetry refresh once
// L133-149 FEmusic_* -> browseMusic else main/WEB
// L151-160 getHomeFeed: browseMain(FEwhat_to_watch) -> browseMusic(FEmusic_home) -> public browse
// L293 search POST https://youtubei.googleapis.com/youtubei/v1/search?key=[REDACTED] + Bearer L297-299
// L821-908 interaction status + like/dislike/removelike TVHTML5+Bearer
```
### 1.4 Token sources (structure only)
- `domain/auth/InnerTubeAuthManager.kt:58-109` get/refresh/logout + purgeAllCaches.
- `domain/auth/YouTubeDeviceAuthManager.kt:50-220` device/code + token + userinfo Bearer L167; DEBUG loopback L242-256.
- `data/OAuthConfig.kt:9-39` CLIENT_ID/SECRET [REDACTED], SCOPE, DEVICE/TOKEN/VERIFY URLs.
- `account/*` opaque accountKey only; log hash prefix `AccountSessionManager.kt:128-134`.
- Trigger: Bearer present = personalized; absent = same JSON anonymous regional.

## 2. JSON traversal
### 2.1 /next SmartTubeEngine.kt:245-452
```kotlin
// P1 singleColumnWatchNextResults.autoplay.autoplay.sets[]
autoplayVideoRenderer.autonavEndpointRenderer.endpoint.watchEndpoint.videoId
autoplayVideoRenderer.autoplayEndpointRenderer.endpoint.watchEndpoint.videoId
// skip replayIfSameVideo==true L266-271 + skip ==current
// preview nextVideoRenderer.maybeHistoryEndpointRenderer.item.previewButtonRenderer{title,byline,thumbnail} if match L278-284
// P2 twoColumnWatchNextResults same; P3 direct autoplayVideo L312-326
// P4 legacy L359-452: playerOverlay autoplay; music tabs[0].musicQueueRenderer.playlistPanelRenderer.contents[] playlistPanelVideoRenderer{videoId,title,short/longBylineText,thumbnail,lengthText}; secondaryResults.results[] compactVideoRenderer{same}
// unwrapper L337-357 autonav->autoplayEndpoint->endpoint->watchEndpoint->videoId
```
InnerTubeRemoteClient `parseAutoplayFromJson L336-424` -> single AutoplayTrack(videoId,title,artist,thumbnailUrl,durationSeconds) L26-32.

### 2.2 /browse
- `getPersonalizedHome(FEwhat_to_watch) L457+`: `contents.twoColumnBrowseResultsRenderer.tabs[].tabRenderer.content.sectionListRenderer.contents[].itemSectionRenderer.contents[0].shelfRenderer` title default `Recommended For You` L478 -> BASED_ON_LISTENING L519-521.
- `parseBrowseSections L593+` + `parseInnerTubeVideos AuthClient:323`: carousel/twoRow/video/compact/tile/responsive; helpers extractVideoId L670, thumbnail L680, runsText L694, duration L705.

// 401 -> refresh -> retry L115-129 else fallback TVHTML5 L157+
```
Browse aliases L550-570: `browseLikedMusic()=browseMusic("FEmusic_liked")`, `browseHistory()=browseMain("FEhistory")`, `browseSubscriptions()=browseMain("FEsubscriptions")`, liked fallback `browseMain("VLLL")`.
## 3. Local telemetry (Room v12 AppDatabase.kt:15-48)
```kotlin
// HistoryEntities.kt + HistoryDao.kt
search_history(query PK,timestamp,source,result_type) L10-21
playback_history(id auto,media_id,title,artist,album,artwork_uri,played_at,play_duration_ms,total_duration_ms,completion_percent,source) L23-48
video_history(video_id PK,title,thumbnail_uri,position_ms,duration_ms,watched_at,completion_percent) L50-67
download_history(download_id PK,media_id,title,status,timestamp) L69-82
queue_history(id=1,queue_json,current_index,timestamp) L84-95
// RecommendationEntities.kt + RecommendationDao.kt
listen_events(id auto,videoId,title,artist,channelId,genre,listenDurationMs,totalDurationMs,completionRatio,wasSkipped/Liked/Disliked/AddedToPlaylist/Replayed,seekCount,shareCount,timeOfDay,dayOfWeek,isHeadphonesOn,sessionId,timestamp) L6-32
// aggs: topSongs L13-31, topArtists L33-50, liked L52-67, favArtists L69-83, timeContext>0.7 L85-96, genres L98-107
// blacklist L109-117:
SELECT videoId FROM listen_events WHERE wasDisliked=1 GROUP BY videoId HAVING COUNT(*)>=2
artist_scores(channelId PK,totalScore,playCount,avgCompletion,like/skip,lastPlayed) L34-45
song_scores(videoId PK,preferenceScore,playCount,totalListenMs,isBlacklisted) L47-57
// LibraryEntities.kt: liked_tracks(videoId PK,likedAt), saved_tracks, playlists, playlist_tracks, downloads, recent_plays(playedAt idx)
// Cache: row +6h, autoplay TTL 1h (CacheDao:40-51) purge 3d, personalized accountKey+type +6h, hidden account-scoped
// Prefs: SettingsDataStore deepeye_settings yt_access/refresh/profile plain; PersonalizationPreferences 12 fields (blend 2, maxArtist 2, cooldown 24h); TokenStorage secure_tokens encrypted AES256 5-min buffer
```

## 4. Session dedup (Infinite Radio)
```kotlin
// AutoplayModels.kt:49-62
data class AutoplayState(..., history:List<String>, blacklist:Set<String>, sessionHistory:Set<String> /*played+queued strict*/) 
// AutoplayRepository.kt:56-66,129-132
val blacklist = dao.getBlacklistedVideoIds().toSet() + autoplayState.blacklist
val recentHistory = autoplayState.sessionHistory.toSet()
.filterNot { it.videoId in blacklist || it.videoId in recentHistory } // cache L58-60 + network L131
// rank L134-144 take20 reason Up Next vs Similar score 1.0-i*0.04
// PlayerController.kt:120,581-592,986,1004-1015,1154-1172
recentAutoplayTrackIds mutableList cap1000 trim500; prefetch addAll + history.takeLast(50)+sessionHistory; emergency filter !recent.contains && !=current L986; gapless peekNext else generate L1154
```
Queue CRUD `QueueManager.kt:46-276`.

## 5. Gaps
Dual token stores; DEBUG ingest; OAuth IDs public-client; confirm auditBrowseTelemetry; cap sessionHistory; region IN vs US.
*No code changed. Re-verify lines after edits.*



# DeepEyeMusicPro — Personalization Architecture Audit
Date: 2026-10-10 / Scope: read-only, no code modified
> Privacy-safe: secrets redacted as [REDACTED]. Structure only.

## 0. Summary
- Bearer-only OAuth device flow. No SAPISID cookie, no VisitorData in app/src/main/java (grep 0 hits).
- Remote: InnerTube next+browse via TVHTML5 client 85 and ANDROID_MUSIC client 67, opportunistic Bearer, anonymous fallback.
- Local: Room AppDatabase v12 + 2 DataStores + EncryptedSharedPreferences.
- No My Mix / Listen Again literals. Equivalents: Up Next on YouTube, Based on listening, Because you listened, Continue listening, Liked Music, Subscriptions, Trending.
- Dedup triple: recentAutoplayTrackIds + AutoplayState.sessionHistory + RecommendationDao.getBlacklistedVideoIds().

## 1.1 SmartTubeEngine POST
File: `data/source/remote/youtube/SmartTubeEngine.kt:76-124`
- Base `https://www.youtube.com/youtubei/v1/` L51. UA SMART-TV Tizen L53-54,88. X-Client-Name 85 L89, Version 7.20210614.03.00 L52,90. Content-Type json L87.
- Bearer added only if token non-blank L77,93-95. Body TVHTML5_CONTEXT L56-70 (TVHTML5, hl en, gl IN, safetyMode false) + extraJson videoId/browseId.
- 401-rescue L98-110: refresh once via InnerTubeAuthManager.refreshAccessToken, retry. Else null.

## 1.2 ANDROID_MUSIC next/browse/lyrics
File: `data/source/remote/youtube/InnerTubeRemoteClient.kt`
- Bases L54-55 music + main youtubei/v1. Context L60-73 ANDROID_MUSIC 6.42.52.
- fetchNextAutoplay L94-152 POST music next X-Name 67 Bearer if present 401 refresh retry else TVHTML5 fallback.
- Lyrics tabs scan MPLY or title Lyrics L229-247 then browse L249-279.
- browseMusic FEmusic_home L429, browseMain FEwhat_to_watch L492, aliases L550-570 liked/history/subs/VLLL.
- auditBrowseTelemetry L576 — verify no raw dump.

## 1.3 Authenticated facade
File: `data/source/remote/youtube/AuthenticatedYouTubeClient.kt`
- WEB ctx L39-48 public, TV ctx L51-64 personalized.
- browseInternal L94-131 TV only if useAuthClient+token else WEB, Bearer L106-108, 401 retry L71-88.
- browse/authBrowse routing L133-149 FEmusic to browseMusic.
- getHomeFeed L151-160 token? main->music->public. History L243-253 MusicFilter, token+empty -> [].
- search L270-320 TV if token else WEB, key [REDACTED] L293 + Bearer L297-299.
- interaction status L821-875 + like actions L877-908 TVHTML5+Bearer.
## 1.4 Auth send sites
- InnerTubeAuthManager.kt:58-109 getAccessToken/refresh/logout + purgeAllCaches. DeviceCode L35-53 delegates.
- YouTubeDeviceAuthManager.kt:50-220 device/code POST client_id [REDACTED], token POST, userinfo Bearer L167. Loopback ingest only DEBUG L242-256.
- OAuthConfig.kt:9-39 CLIENT_ID/SECRET [REDACTED], SCOPE, DEVICE/TOKEN/VERIFY URLs.
- Bearer sites: SmartTubeEngine 94,102; InnerTubeRemoteClient 111,119,171,205,263,444,452,507,515; AuthClient 78,107,298,773,847; DeviceManager 167; Extractor 261; NetworkModule 45-46 X-Client headers.
- Privacy boundary AccountDataSource.kt/AccountSession.kt/AccountSessionManager.kt opaque accountKey only, log hash prefix L128-134.
- Trigger: token present -> Bearer personalized; absent -> same JSON anonymous regional. Metered guard can suppress prefetch.

## 2.1 Up-Next paths
File SmartTubeEngine.kt:150-357,359-452 extractUpNextTrack L245-330 priority L231-243:
1. singleColumnWatchNextResults.autoplay.autoplay.sets[] autonav/autoplayEndpoint watchEndpoint.videoId, skip replayIfSameVideo L266-271 + skip current, preview metadata only if match L278-284.
2. twoColumnWatchNextResults same. 3. direct autoplayVideo L312-326. 4. legacy: playerOverlay autoplay; music playlistPanelRenderer playlistPanelVideoRenderer; secondaryResults compactVideoRenderer L425-449.
Unwrapper extractWatchEndpointVideoId L337-357. InnerTubeRemoteClient parseAutoplayFromJson L336-424 single AutoplayTrack L26-32, server rank opaque first-non-current wins.

## 2.2 Browse + local orchestration
- getPersonalizedHome FEwhat_to_watch L457+ -> twoColumnBrowseResultsRenderer tabs sectionList itemSection shelfRenderer title default Recommended For You L478 -> BASED_ON_LISTENING L519-521.
- parseBrowseSections L593+ / parseInnerTubeVideos AuthClient:323 handle carousel/twoRow/video/compact/tile/responsive. Helpers extractVideoId L670, extractThumbnail L680, runsText L694, parseDuration L705.
- AutoplayRepository.kt:31-145 generateNextQueue: metered guard L42-50, cache-first L53-63, parallel smartTube primary + related L68-132 distinct filterNot blacklist/history L129-131, take20 score 1.0-i*0.04 reason Up Next vs Similar L134-144.
- ContentFetcher.kt:25-151 algorithmic prepend + NewPipe fallback; trending fallback-only L133-150.
- PersonalizationRepositoryImpl.kt:72-733 sections sec_continue_listening, your_queue, recently_played, liked via browseLikedMusic, playlists, subscriptions via browseSubscriptions, based_on_listening + blend, trending region US default. Hidden filtered, switchAccount+buildFeed L74-80.

## 3.1 Room v12
File data/db/AppDatabase.kt:15-48. Tables: songs, local_playlists, playlist_song_cross_ref, play_events, user_feedback, onboarding_preferences, listen_events, artist_scores, song_scores, cached rows/tracks/autoplay/search, liked/saved/playlists/playlist_tracks/downloads/recent_plays, search/playback/video/download/queue_history, subscribed_channels, cached personalized sections/items, hidden_content.
- HistoryEntities 10-95 + HistoryDao 10-64: search_history(query PK,source), playback_history(media_id,completion), video_history(video_id PK,position), download_history(status), queue_history(id=1,queue_json).
- RecommendationEntities 6-84 + RecommendationDao 9-120: listen_events engagement+context+sessionId; aggs topSongs/Artists, liked, timeContext>0.7, genres, blacklist wasDisliked x2 L109-117 HAVING COUNT>=2, deleteOld.
- Entities 20-159 + TasteDao 13-65: play_events SUM(played_ms) GROUP BY song_id; user_feedback liked/skipped/dont_play; onboarding JSON.
- LibraryEntities 8-75: liked/saved/playlists/downloads/recent_plays.
- Cache: row expires +6h, autoplay TTL 1h read CacheDao:40-51 purge 3d CacheManager:198-204, search expires, personalized accountKey+type +6h, hidden account-scoped no tokens.

## 3.2 Prefs
- SettingsDataStore 21-177 deepeye_settings incl yt_access/refresh/name/avatar/email plain — harden; clear on empty L153-168; export excludes tokens L127-151.
- PersonalizationPreferencesDataStore 22-164 deepeye_personalization_prefs: enables, trending_region US, hideNonMusic true, blend 2 (0-8), maxArtist 2 (1-5), cooldown 24h (1-720), last_refresh. Interface PersonalizationPreferenceStore 12-16.
- PersonalizationPreferences 12-41 defaults true. TokenStorage 18-94 secure_tokens EncryptedSharedPreferences AES256, 5-min buffer.

## 4. Blacklist/session dedup
- AutoplayModels 48-62 AutoplayState history List blacklist Set sessionHistory Set strict dedup.
- AutoplayRepository 56-66,129-132 blacklist=dao.getBlacklisted+state.blacklist recentHistory=sessionHistory filterNot both (cache+network). SongScore.isBlacklisted parallel — confirm writer.
- PlayerController 120,581-592,986,1004-1015,1154-1172 recentAutoplayTrackIds cap1000 trim500 not persisted; prefetch generate->addItems->addAll->history takeLast50+sessionHistory; emergency second filter L985-986 score<0.4 discovery; gapless peekNext else generate L1153-1154 skip ==current/prefetched L1177. QueueManager 46-276 CRUD.

## 5. Hardening
1. Standardize tokens on encrypted store. 2. Gate debug logs. 3. OAuth IDs public-client treat as such. 4. Confirm auditBrowseTelemetry no dump. 5. Cap sessionHistory. 6. Align region IN vs US default.
Appendix: see file:line refs above. No code changed. Re-verify after edits.


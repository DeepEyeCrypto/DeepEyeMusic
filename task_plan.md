# DeepEyeMusicPro Task Plan

## Active Milestone: v3.0.1.43 (InnerTube Auth-Rescue Protocol & Personalized Feed Alignment)

### Phase 1: Network Interceptor & Token Injection Audit
- [x] Audit `InnerTubeAuthManager.kt` — ensure proactive refresh on missing/expired tokens.
- [x] Audit `InnerTubeRemoteClient.kt` — inject `Authorization: Bearer <TOKEN>` on all `/youtubei/v1/` endpoints.
- [x] Eliminate API key query param conflicts (`?key=...`) on authenticated `/browse` and `/next` requests.

### Phase 2: Payload & Client Spoofing Alignment
- [x] Spoof `ANDROID_MUSIC` (version `6.42.52`, Client-Name `67`) for `https://music.youtube.com/youtubei/v1/browse` (`FEmusic_home`, `FEmusic_liked`).
- [x] Spoof `TVHTML5` (version `7.20230412.08.00`, Client-Name `85`) for `https://www.youtube.com/youtubei/v1/browse` (`FEwhat_to_watch`, `FEhistory`, `FEsubscriptions`).
- [x] Context payload includes `{ "user": { "enableSafetyMode": false, "lockedSafetyMode": false } }`.

### Phase 3: Browse-ID Precision & Response Parser
- [x] Wire `browseMusic("FEmusic_home")` for personalized recommendations ("Mixed for you", "Listen again", "Quick picks").
- [x] Wire `browseLikedMusic()` (`FEmusic_liked`) for personal likes.
- [x] Implement multi-renderer recursive parsing for `musicCarouselShelfRenderer`, `musicShelfRenderer`, `musicResponsiveListItemRenderer`, and `musicTwoRowItemRenderer`.

### Phase 4: Telemetry Audit & Mantis Reflect
- [x] Add `[InnerTube Auth-Rescue]` logging with 500-char preview and personalized shelf detection.
- [x] Handle HTTP 401 Unauthorized with inline token refresh and single automatic retry.
- [x] Sideload and verify on physical Realme RMX3945 device.

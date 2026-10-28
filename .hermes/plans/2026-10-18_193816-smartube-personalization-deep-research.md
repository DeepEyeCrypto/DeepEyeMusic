# SmartTube Personalization Research + YouTube Account Wire Connect — Combined Plan

**Repo:** `/Users/enayat/Documents/DeepEyeMusicPro`  
**Package:** `com.deepeye.musicpro`  
**HEAD:** `6740b663` (main)  
**Plan ID:** `2026-10-18_193816`

---

## Goal (one sentence)
Produce a revision-pinned SmartTube personalization research deliverable set, map the YouTube account wire connection (auth → request → response → parser → UI), gap-analyze against current DeepEye code, and implement only evidenced gaps via TDD with on-device verification.

---

## Current Context / Assumptions

| Item | State |
|------|-------|
| Working tree | 10 modified files, 8 new test files, docs exist for auth/audit/loop |
| HEAD | `6740b663b87dc6a0d948dedfabcd3e9e7e91151a` |
| Build | `./gradlew` exists, JDK via wrapper, `local.properties` present |
| Key modified files | `AuthenticatedYouTubeClient.kt` (926L), `InnerTubeRemoteClient.kt` (933L), `SmartTubeEngine.kt` (832L), `AutoplayRepository.kt` (197L), `QueueManager.kt` (282L), `PlayerController.kt` (1716L), `ContentFetcher.kt` (167L), `PersonalizationRepositoryImpl.kt` (922L), `PersonalizedModels.kt` (92L), `MusicViewModel.kt` (264L) |
| Existing tests | 40+ unit tests incl. `QueueManagerRepeatTest`, `AutoplayRepositoryTest`, `SmartTubeEngineTest`, `YouTubeDeviceAuthManagerTest`, `FEmusicBrowseContractTest` |
| Existing docs | `youtube_personalization_auth_audit.md`, `oauth_expiry_fix_plan.md`, `same_song_loop_*.md`, `autoplay_repair_baseline.md`, `docs/account_music/` |
| Device | Realme RMX3945 (MT6835, Android 16) via ADB — **on-device verification mandatory** |
| Reference | SmartTube at `https://github.com/yuliskov/SmartTube` — inspect at pinned commit, do not build |

---

## Architecture / Proposed Approach (2–3 sentences)

**Phase A (Research):** Clone SmartTube read-only at a pinned commit, trace six systems (Auth, Home, Music, Up Next, Autoplay, Local) through code — not names — recording file:class:method:line evidence. Produce six markdown deliverables in `docs/smarttube_personalization/`.

**Phase B (Gap Analysis):** Compare each traced capability against DeepEye's current path (AuthManager, AuthenticatedYouTubeClient, InnerTubeRemoteClient, SmartTubeEngine, ContentFetcher, AutoplayRepository, AutoRadioCoordinator, PlayerController, QueueManager, ranking). Output `deepeye_gap_matrix.md` with P0/P1/P2 prioritization.

**Phase C (TDD Implementation):** For each P0/P1 gap, write a failing test (RED), implement minimal fix (GREEN), run `compileDebugKotlin`, `testDebugUnitTest`, `lint`, `assembleDebug` sequentially. Preserve pending fixes (400 fix, same-song loop, queue destruction).

**Phase D (Device Verification):** Install APK on Realme RMX3945, verify 3+ distinct automatic audio transitions, repeat compatibility, auth/account-switch cache isolation, provenance labels. No device = BLOCKED.

---

## Step-by-Step Tasks

### PHASE A — READ-ONLY RESEARCH (SmartTube)

| # | Task | Command / Action | Expected Output |
|---|------|------------------|-----------------|
| A1 | Record DeepEye baseline | `git -C /Users/enayat/Documents/DeepEyeMusicPro status --short`<br>`git -C /Users/enayat/Documents/DeepEyeMusicPro rev-parse HEAD`<br>`git -C /Users/enayat/Documents/DeepEyeMusicPro rev-parse --abbrev-ref HEAD` | `docs/smarttube_personalization/research_manifest.md` with baseline SHAs, pending fixes list |
| A2 | Clone SmartTube read-only | `git clone --depth 1 --branch master https://github.com/yuliskov/SmartTube /tmp/SmartTube-ref`<br>`cd /tmp/SmartTube-ref && git rev-parse HEAD > /tmp/smarttube_sha.txt` | Pinned commit SHA recorded; `.gitmodules` + submodule SHAs captured |
| A3 | License check | `cat /tmp/SmartTube-ref/LICENSE 2>/dev/null || cat /tmp/SmartTube-ref/LICENSE.md 2>/dev/null || echo "NO LICENSE FOUND"` | `license_review.md` with license type + reuse implications |
| A4 | Discover Auth system | `grep -r -n -E "(account|sign.in|sign.out|refreshToken|credential|OAuth|token)" /tmp/SmartTube-ref --include="*.kt" --include="*.java" | head -200` | `auth_flow.md` — entry points, credential provider, token acquisition, refresh, expiry, sign-out, account switch, guest |
| A5 | Discover Home/Music | `grep -r -n -E "(Home|Music|recommend|shelf|browse|feed|continuation)" /tmp/SmartTube-ref --include="*.kt" --include="*.java" | head -200` | `home_music_flow.md` — entry View/VM, service, request builder, parser, cache, continuation, fallback |
| A6 | Discover Up Next / Autoplay | `grep -r -n -E "(autoplay|suggest|nextVideo|getNext|playlist|radio|queue)" /tmp/SmartTube-ref --include="*.kt" --include="*.java" | head -200` | `up_next_autoplay_flow.md` — explicit candidate? parser? selection policy? repeat/shuffle/radio interaction? |
| A7 | Discover History/Privacy | `grep -r -n -E "(history|watch|track|playback|report|consent)" /tmp/SmartTube-ref --include="*.kt" --include="*.java" | head -100` | `history_and_privacy.md` — reporting mechanism, opt-in, lifecycle, sign-out behavior |
| A8 | Follow submodules | If `.gitmodules` exists, clone each submodule at its pinned SHA, repeat A4–A7 | Updated deliverables with submodule evidence |
| A9 | Create source_map.md | Consolidate A4–A8 into table: Capability \| Entry point \| Service \| Request builder \| Parser \| Selection policy \| Cache \| Source permalink \| Confidence | `source_map.md` with VERIFIED_IN_SOURCE / INFERRED / UNKNOWN labels |

### PHASE B — GAP ANALYSIS (DeepEye vs SmartTube)

| # | Task | Action | Output |
|---|------|--------|--------|
| B1 | Map DeepEye auth path | Read: `AuthenticatedYouTubeClient.kt`, `InnerTubeRemoteClient.kt`, `YouTubeDeviceAuthManager` (test exists), `AccountSessionManager` | Auth flow diagram in gap matrix |
| B2 | Map DeepEye Home/Music | Read: `PersonalizationRepositoryImpl.kt`, `ContentFetcher.kt`, `SmartTubeEngine.kt`, `MusicViewModel.kt` | Home/Music flow diagram |
| B3 | Map DeepEye Up Next/Autoplay | Read: `AutoplayRepository.kt`, `AutoRadioCoordinator.kt` (search), `QueueManager.kt`, `PlayerController.kt` | Autoplay flow diagram |
| B4 | Map DeepEye History/Queue | Read: `QueueManager.kt`, `QueueManagerRepeatTest.kt`, `PlayerController.kt` transition logic | Queue/repeat/history diagram |
| B5 | Produce gap matrix | For each capability in `source_map.md`: SmartTube evidence ↔ DeepEye path ↔ verified mismatch ↔ user impact ↔ minimal patch ↔ tests ↔ uncertainty ↔ priority (P0/P1/P2) | `deepeye_gap_matrix.md` |

### PHASE C — TDD IMPLEMENTATION (P0 → P1)

| # | Task | RED Test | GREEN Implementation | Verify |
|---|------|----------|---------------------|--------|
| C1 | HTTP 400 fix (P0) | Add test: `AuthenticatedYouTubeClientTest.shouldNotRetry400Unchanged` | Fix request builder — remove fabricated playlist prefix, correct field structure | `./gradlew testDebugUnitTest --no-daemon` passes new test |
| C2 | Same-song loop (P0) | Add test: `QueueManagerRepeatTest.repeatOneDoesNotReplayCurrent` | Fix `QueueManager.next()` — exclude current item when Repeat One | `./gradlew testDebugUnitTest` passes |
| C3 | User queue destruction (P0) | Add test: `AutoplayRepositoryTest.userQueuePreservedAfterRecommendationFailure` | Fix `AutoplayRepository` — append exactly once, discard stale on failure | Test passes |
| C4 | Invalid auth treated as personalized success (P0) | Add test: `PersonalizationRepositoryImplTest.guestProvenanceNotPersonalized` | Fix parser — distinguish guest vs signed-in response, label provenance | Test passes |
| C5 | Provider-selected Up Next lost (P1) | Add test: `AutoplayRepositoryTest.providerNextCandidatePreserved` | Preserve explicit candidate through parser → queue, label `PROVIDER_SELECTED` | Test passes |
| C6 | Missing continuation/context propagation (P1) | Add test: `ContentFetcherTest.continuationTokenPropagated` | Thread continuation through request builder → parser → cache | Test passes |
| C7 | Stale account cache (P1) | Add test: `PersonalizationRepositoryImplTest.cacheInvalidatedOnTokenRefresh` | Invalidate Home/Music cache on token expiry/refresh | Test passes |
| C8 | Expiry/refresh failure handling (P1) | Add test: `AuthenticatedYouTubeClientTest.refreshFailureSurfacesTypedError` | Typed error (not retry unchanged), 401 recovery path | Test passes |

**Each C-task cycle:**
```bash
# 1. Write failing test
# 2. Run to confirm RED
./gradlew testDebugUnitTest --no-daemon --console=plain -Dtest.single=<TestClass>

# 3. Implement minimal fix
# 4. Run to confirm GREEN
./gradlew testDebugUnitTest --no-daemon --console=plain -Dtest.single=<TestClass>

# 5. Full gate
./gradlew :app:compileDebugKotlin --no-daemon --console=plain
./gradlew testDebugUnitTest --no-daemon --console=plain
./gradlew lint --no-daemon --console=plain
```

### PHASE D — AUTHORIZED DEVICE VERIFICATION

| # | Task | Command | Pass Criteria |
|---|------|---------|---------------|
| D1 | Assemble Debug APK | `./gradlew assembleDebug --no-daemon --console=plain` | Exit 0, APK at `app/build/outputs/apk/debug/app-debug.apk` |
| D2 | Record APK metadata | `sha256sum app-debug.apk`<br>`ls -la app-debug.apk`<br>`aapt dump badging app-debug.apk \| grep version` | SHA-256, byte size, version recorded |
| D3 | Install on Realme RMX3945 | `adb -s <serial> install -r app-debug.apk` | `Success` (no data wipe) |
| D4 | Verify Home auth provenance | Launch app → Home tab → check logs for `PROVENANCE=SIGNED_IN` or `GUEST` | Signed-in shows personalized shelves; guest shows generic |
| D5 | Verify Music auth provenance | Navigate to Music tab → same provenance check | Same standard |
| D6 | Verify /next (autoplay) outcome | Play track → observe 3+ distinct automatic audio transitions | 3+ transitions, different audio identities |
| D7 | Verify candidate parsing/resolution | Logcat: `c2.mtk.avc.decoder`, `DSPEngine`, `discardFps=0`, zero HTTP 403 on GoogleVideo | Clean logs |
| D8 | Verify repeat settings | Toggle Repeat One / Repeat All / Off → verify queue behavior | Each mode works with autoplay ON/OFF |
| D9 | Verify pause/stop | Pause mid-track → resume → stop → verify queue preserved | Queue intact |
| D10 | Verify account switch + cache isolation | Switch account → verify Home/Music cache refreshes, no cross-leak | Clean isolation |

---

## Tests / Validation

- **Unit:** All new tests + existing suite pass (`testDebugUnitTest`).
- **Compile:** `:app:compileDebugKotlin` — zero errors.
- **Lint:** `lint` — no new warnings (existing suppressions documented).
- **APK:** `assembleDebug` — successful, SHA recorded.
- **Device:** 3+ verified automatic transitions on Realme RMX3945; provenance labels match source; repeat modes correct; account switch clean.
- **Gate:** No authorized device/account → **BLOCKED** (not PASS).

---

## Risks, Tradeoffs, Open Questions

| Risk | Mitigation |
|------|------------|
| SmartTube submodules inaccessible | Mark `UNAVAILABLE` in `source_map.md`; do not block Phase B/C on missing submodule code |
| SmartTube license restricts reuse | Prefer independent implementation via existing DeepEye abstractions; document in `license_review.md` |
| Gradle daemon lock contention | Use `background=true` with `/tmp/gradle_build.log`, poll `pgrep -f GradleDaemon` |
| HTTP 400 root cause unclear | Current modified `AuthenticatedYouTubeClient.kt` — inspect diff first before C1 |
| Same-song loop root cause unclear | Existing `same_song_loop_root_cause.md` — read before C2 |
| Device unavailable | Phase D = BLOCKED; document in final verdict |
| SmartTube uses undocumented InnerTube endpoints | Label `UNDOCUMENTED` in source map; DeepEye must not claim official contract |

---

## Open Questions (resolve before Phase C)

1. **Which exact commit of SmartTube to pin?** (Latest master at research start, or a tagged release?)
2. **Are there private SmartTube forks/submodules not in public repo?** (Check `.gitmodules` at A3)
3. **Which DeepEye auth class holds the token?** (`YouTubeDeviceAuthManager`? `AccountSessionManager`? — need to read)
4. **Does `AutoRadioCoordinator` exist in current tree?** (Search needed — not in modified files list)
5. **Pending 400 fix details:** Read `AuthenticatedYouTubeClient.kt` diff before planning C1.
6. **Pending same-song loop fix:** Read `same_song_loop_root_cause.md` and `QueueManager.kt` diff before planning C2.

---

## Files to Create

```
docs/smarttube_personalization/
├── research_manifest.md
├── source_map.md
├── auth_flow.md
├── home_music_flow.md
├── up_next_autoplay_flow.md
├── history_and_privacy.md
├── license_review.md
└── deepeye_gap_matrix.md
```

---

## Files to Modify (Phase C)

- `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/AuthenticatedYouTubeClient.kt`
- `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/InnerTubeRemoteClient.kt`
- `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/SmartTubeEngine.kt`
- `app/src/main/java/com/deepeye/musicpro/domain/autoplay/AutoplayRepository.kt`
- `app/src/main/java/com/deepeye/musicpro/domain/recommendation/ContentFetcher.kt`
- `app/src/main/java/com/deepeye/musicpro/player/queue/QueueManager.kt`
- `app/src/main/java/com/deepeye/musicpro/player/controller/PlayerController.kt`
- `app/src/main/java/com/deepeye/musicpro/data/repository/PersonalizationRepositoryImpl.kt`
- Test files under `app/src/test/...` (new + existing)

---

## Final Deliverable (single response at end)

| Section | Content |
|---------|---------|
| RESEARCH | SmartTube inspected SHA, submodules, files/methods traced, unavailable source, official vs undocumented |
| PERSONALIZATION | Home, Music, signed-in context, history role, account cache, local filtering |
| UP NEXT | Explicit provider autoplay candidate found?, parser/consumer evidence, playlist/continuation context, selection policy, repeat/queue interaction |
| DEEPEYE | Confirmed gaps, HTTP 400 finding, same-song loop finding, proposed/applied minimal patches |
| TESTS | RED/GREEN counts, total/passed/failed/errors/skipped, compile, lint, APK |
| DEVICE | Model/OS, real endpoint, provenance, 3+ transitions, repeat compat, auth/switch, blockers |
| LICENSE/PRIVACY | License findings, code reuse, credentials copied: NO, private data logged: NO |
| FILES | Created, modified |
| FINAL VERDICT | RESEARCH VERIFIED / IMPLEMENTATION PARTIAL / IMPLEMENTATION PASS |

---

**Next step (if authorized):** Execute Phase A1–A3 to establish baseline and SmartTube clone, then proceed sequentially. Do not edit production code until Phase B gap matrix is reviewed and Phase C implementation is authorized.

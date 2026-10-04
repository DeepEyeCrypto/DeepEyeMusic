# DeepEyeMusicPro Engineering Findings & Root-Cause Analysis

## Finding 1: Auth Coroutine Lifecycle Cancellation
- **Root Cause**: `gamificationEngine.restoreFromFirestore()` and `cloudRestoreManager.restoreAllData()` were called inside `viewModelScope` in `AuthViewModel`. When login completed and navigated away, the backstack pop cancelled `viewModelScope`, throwing `JobCancellationException`, caught and misreported as "Firebase Sign In Failed".
- **Fix**: Injected `@ApplicationScope CoroutineScope` into `AuthViewModel` to dispatch post-auth sync tasks independently of ViewModel lifecycle.

## Finding 2: Landscape Auth Screen Viewport Overflow
- **Root Cause**: `LoginScreen` stacked all elements vertically with `fillMaxWidth()`, resulting in 1460px wide input fields and pushing the Google Sign In, YouTube TV Login, and Guest buttons off-screen on 720p landscape screens.
- **Fix**: Redesigned `LoginScreen` into a Cyberpunk Two-Column Split Pane layout (Left Hero branding, Right Glassmorphic Card), fitting all controls within 550px vertical bounds without scrolling.

## Finding 3: InnerTube Algorithmic AutoPlay
- **Root Cause**: Legacy recommendation system used artificial client-side scoring rather than YouTube's native recommendation graph.
- **Fix**: Built `InnerTubeRemoteClient.kt` calling `/youtubei/v1/next` with `ANDROID_MUSIC` spoofing, extracting exact `autoplayEndpoint` / `musicQueueRenderer` candidates and prefetching into ExoPlayer gaplessly.

# Design Document: Premium Strategy Upgrade

## Overview

This design document describes the technical architecture for implementing the Premium Strategy Upgrade across DeepEye Music Pro's 8 pillars. The implementation builds on the existing Jetpack Compose glassmorphic UI, Media3/ExoPlayer player, V4A DSP Engine, and NewPipe-based YouTube streaming infrastructure.

## Architecture

### High-Level Component Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         UI Layer (Compose)                           │
├──────────┬──────────┬──────────┬──────────┬──────────┬─────────────┤
│ Color    │ Now      │ Video    │ Motion   │ Quality  │ Onboarding  │
│ Theming  │ Playing  │ Rail &   │ & Haptic │ States   │ & History   │
│ System   │ Screen   │ Player   │ System   │ (Empty/  │ Screens     │
│          │          │          │          │ Error)   │             │
├──────────┴──────────┴──────────┴──────────┴──────────┴─────────────┤
│                      ViewModel Layer (MVI)                           │
├──────────┬──────────┬──────────┬──────────┬─────────────────────────┤
│ Palette  │ Player   │ Video    │ Timer    │ Personalization         │
│ ViewModel│ ViewModel│ ViewModel│ ViewModel│ ViewModel               │
├──────────┴──────────┴──────────┴──────────┴─────────────────────────┤
│                      Domain Layer                                    │
├──────────┬──────────┬──────────┬──────────┬─────────────────────────┤
│ Palette  │ DSP      │ Crossfade│ Sleep    │ Personalization         │
│ Extractor│ Profile  │ Manager  │ Timer    │ Engine                  │
│          │ Manager  │          │ UseCase  │ (History, TopTracks)    │
├──────────┴──────────┴──────────┴──────────┴─────────────────────────┤
│                      Data Layer                                      │
├──────────┬──────────┬──────────┬──────────┬─────────────────────────┤
│ Palette  │ DSP      │ Audio    │ Play     │ Onboarding              │
│ Cache    │ Profile  │ Route    │ Event    │ Preferences             │
│ (Memory) │ DAO      │ Detector │ DAO      │ DataStore               │
├──────────┴──────────┴──────────┴──────────┴─────────────────────────┤
│                      Player / DSP Layer                              │
├──────────┬──────────┬──────────┬──────────┬─────────────────────────┤
│ DSP      │ Bass     │ Crossfade│ Hi-Res   │ Media Session           │
│ Engine   │ Processor│ Processor│ Path     │ & Notification          │
│ (V4A)    │          │          │ (AAudio) │ Manager                 │
└──────────┴──────────┴──────────┴──────────┴─────────────────────────┘
```

### Key Design Decisions

1. **Palette extraction uses AndroidX Palette** with in-memory LRU cache (keyed by artwork URI) to avoid re-extraction on recomposition
2. **DSP profile storage uses Room** with a composite key of (trackId, deviceId) for per-track and per-device profiles
3. **Crossfade is implemented at the ExoPlayer level** using dual MediaSource with volume ducking rather than mixing two ExoPlayer instances
4. **Bass processing extends the existing DSP chain** by splitting the bass boost module into sub-bass and mid-bass bands using DynamicsProcessing multi-band configuration
5. **Sleep timer uses WorkManager** for reliability across process death with a coroutine-based countdown in the ViewModel for UI updates
6. **Motion system uses shared animation specs** defined as top-level constants to ensure consistency across all screens
7. **Media notification uses Media3 MediaSession** which handles MediaStyle notification automatically with custom artwork sizing

---

## Components and Interfaces

### 1. Color Theming System

**Package:** `com.deepeye.musicpro.ui.theme.palette`

```kotlin
// PaletteExtractor.kt
class PaletteExtractor @Inject constructor() {
    private val cache = LruCache<String, PaletteResult>(20)
    
    suspend fun extract(artworkUri: Uri): PaletteResult
    fun getCached(artworkUri: Uri): PaletteResult?
}

data class PaletteResult(
    val dominant: Color,
    val vibrant: Color,
    val muted: Color,
    val darkVibrant: Color,
    val darkMuted: Color,
    val lightVibrant: Color
)

// PaletteViewModel.kt - provides animated palette state
class PaletteViewModel : ViewModel() {
    val animatedPalette: StateFlow<PaletteResult>
    fun onTrackChanged(artworkUri: Uri)
}
```

**Contrast enforcement:** A `PaletteConstrainer` utility clamps luminance values to ensure WCAG AA compliance in dark/AMOLED modes.

### 2. DSP Profile Manager

**Package:** `com.deepeye.musicpro.dsp.profile`

```kotlin
// DspProfileEntity.kt (Room)
@Entity(primaryKeys = ["trackId", "deviceId"])
data class DspProfileEntity(
    val trackId: String,        // "*" for global
    val deviceId: String,       // "*" for all devices
    val eqBands: String,        // JSON serialized float array
    val bassBoostStrength: Int,
    val subBassGain: Float,
    val midBassGain: Float,
    val virtualizerStrength: Int,
    val reverbPreset: Int,
    val dynamicsEnabled: Boolean,
    val limiterThreshold: Float,
    val updatedAt: Long
)

// DspProfileManager.kt
class DspProfileManager @Inject constructor(
    private val dao: DspProfileDao,
    private val audioRouteDetector: AudioRouteDetector,
    private val dspEngine: DSPEngine
) {
    suspend fun loadProfileForTrack(trackId: String): DspParams
    suspend fun saveProfileForTrack(trackId: String, params: DspParams)
    suspend fun deleteProfileForTrack(trackId: String)
    suspend fun loadProfileForDevice(deviceId: String): DspParams
    suspend fun saveProfileForDevice(deviceId: String, params: DspParams)
    fun onDeviceChanged(newDevice: AudioRoute)
}
```

**Resolution order:** per-track → per-device → global

### 3. Bass Processor Enhancement

**Package:** `com.deepeye.musicpro.dsp.bass`

```kotlin
// BassProcessor.kt
class BassProcessor @Inject constructor() {
    data class BassConfig(
        val subBassGain: Float,      // 20-60Hz, range -12..+12 dB
        val midBassGain: Float,      // 60-250Hz, range -12..+12 dB
        val harmonicSaturation: Float, // 0.0..1.0
        val limiterEnabled: Boolean,
        val limiterThreshold: Float,  // dBFS, typically -0.5 to 0
        val preset: BassPreset       // HEADPHONE, SPEAKER, CAR, CUSTOM
    )
    
    fun applyBassConfig(config: BassConfig, dynamicsProcessing: DynamicsProcessing)
    fun getDefaultPreset(route: AudioRoute): BassConfig
}

enum class BassPreset { HEADPHONE, SPEAKER, CAR, CUSTOM }
```

The bass limiter uses the existing `DynamicsProcessing` limiter stage with threshold set to prevent clipping.

### 4. Crossfade Manager

**Package:** `com.deepeye.musicpro.player.crossfade`

```kotlin
// CrossfadeManager.kt
class CrossfadeManager @Inject constructor(
    private val player: ExoPlayer
) {
    data class CrossfadeConfig(
        val enabled: Boolean,
        val durationMs: Long,  // 1000..12000
        val curve: CrossfadeCurve
    )
    
    enum class CrossfadeCurve { EQUAL_POWER, LINEAR }
    
    fun startCrossfade(nextItem: MediaItem, config: CrossfadeConfig)
    fun cancelCrossfade()
}
```

Equal-power crossfade uses `cos(t * π/2)` for fade-out and `sin(t * π/2)` for fade-in to maintain constant energy.

### 5. Sleep Timer

**Package:** `com.deepeye.musicpro.player.timer`

```kotlin
// SleepTimerUseCase.kt
class SleepTimerUseCase @Inject constructor(
    private val playerController: PlayerController
) {
    data class TimerState(
        val isActive: Boolean,
        val remainingMs: Long,
        val totalMs: Long,
        val isFading: Boolean
    )
    
    val state: StateFlow<TimerState>
    
    fun start(durationMs: Long)
    fun cancel()
    // Volume fade: linear from current to 0 over final 30 seconds
    private fun computeVolume(remainingMs: Long): Float
}
```

### 6. Listening History & Top Tracks

**Package:** `com.deepeye.musicpro.domain.history`

```kotlin
// PlayEventEntity.kt (Room - extends existing)
@Entity
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val playedMs: Long,
    val durationMs: Long,
    val completedFully: Boolean,
    val timestamp: Long
)

// HistoryRepository.kt
interface HistoryRepository {
    fun getRecentHistory(limit: Int): Flow<List<PlayEventEntity>>
    fun getTopTracks(period: TimePeriod, limit: Int): Flow<List<TopTrackResult>>
    suspend fun recordPlayEvent(event: PlayEventEntity)
}

data class TopTrackResult(
    val trackId: String,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val totalPlayTimeMs: Long,
    val playCount: Int
)

enum class TimePeriod { WEEK, MONTH, ALL_TIME }
```

### 7. Onboarding Flow

**Package:** `com.deepeye.musicpro.ui.onboarding`

```kotlin
// OnboardingViewModel.kt
class OnboardingViewModel @Inject constructor(
    private val preferencesDataStore: DataStore<Preferences>,
    private val tasteProfileRepository: TasteProfileRepository
) {
    data class OnboardingState(
        val currentStep: Int,  // 0=language, 1=genre, 2=artists
        val selectedLanguages: Set<String>,
        val selectedGenres: Set<String>,
        val selectedArtists: Set<String>,
        val isComplete: Boolean
    )
    
    fun selectLanguage(lang: String)
    fun selectGenre(genre: String)
    fun selectArtist(artist: String)
    fun skip()
    fun complete()
}
```

### 8. Motion & Haptic Constants

**Package:** `com.deepeye.musicpro.ui.motion`

```kotlin
// MotionTokens.kt
object MotionTokens {
    // Standard easing
    val StandardEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    // Spring profiles
    val ExpandCollapseSpring = spring<Float>(
        stiffness = 300f,
        dampingRatio = 0.7f
    )
    val ArtworkTransitionSpring = spring<Float>(
        stiffness = 400f,
        dampingRatio = 0.8f
    )
    
    // Durations
    const val PaletteTransitionMs = 600
    const val ArtworkCrossfadeMs = 400
    const val ControlFadeMs = 200
    const val TabBarScrollThreshold = 100 // dp
}

// HapticPatterns.kt
object HapticPatterns {
    fun transportControl(haptic: HapticFeedback) { ... }
    fun seekComplete(haptic: HapticFeedback) { ... }
    fun modeToggle(haptic: HapticFeedback) { ... }
}
```

### 9. Media Notification Manager

**Package:** `com.deepeye.musicpro.player.notification`

Leverages existing Media3 `MediaSession` with enhanced metadata:

```kotlin
// MediaNotificationManager.kt
class MediaNotificationManager @Inject constructor(
    private val context: Context,
    private val mediaSession: MediaSession
) {
    fun updateMetadata(item: MediaItem, artworkBitmap: Bitmap?)
    fun setActions(isPlaying: Boolean, isLiked: Boolean)
    // Ensures 512x512 artwork minimum
    private suspend fun loadArtwork(uri: Uri): Bitmap
}
```

### 10. Audio Quality Surface

**Package:** `com.deepeye.musicpro.ui.player.quality`

```kotlin
// AudioQualityInfo.kt
data class AudioQualityInfo(
    val sampleRate: Int,        // e.g., 44100, 48000, 96000
    val bitDepth: Int,          // e.g., 16, 24, 32
    val codec: String,          // e.g., "FLAC", "AAC", "OPUS"
    val isHiRes: Boolean,       // sampleRate > 44100 || bitDepth > 16
    val isDvcActive: Boolean
)

// AudioQualityBadge composable
@Composable
fun AudioQualityBadge(info: AudioQualityInfo, modifier: Modifier)
```

---

## Data Models

### Database Schema Changes

#### New Tables

```sql
-- Per-track and per-device DSP profiles
CREATE TABLE dsp_profiles (
    track_id TEXT NOT NULL,
    device_id TEXT NOT NULL,
    eq_bands TEXT NOT NULL,
    bass_boost_strength INTEGER NOT NULL,
    sub_bass_gain REAL NOT NULL,
    mid_bass_gain REAL NOT NULL,
    virtualizer_strength INTEGER NOT NULL,
    reverb_preset INTEGER NOT NULL,
    dynamics_enabled INTEGER NOT NULL,
    limiter_threshold REAL NOT NULL,
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (track_id, device_id)
);

-- Extended play events for history
CREATE TABLE play_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    track_id TEXT NOT NULL,
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    artwork_uri TEXT,
    played_ms INTEGER NOT NULL,
    duration_ms INTEGER NOT NULL,
    completed_fully INTEGER NOT NULL,
    timestamp INTEGER NOT NULL
);

-- Onboarding preferences
CREATE TABLE onboarding_preferences (
    id INTEGER PRIMARY KEY,
    languages TEXT NOT NULL,
    genres TEXT NOT NULL,
    artists TEXT NOT NULL,
    completed_at INTEGER NOT NULL
);
```

### Key Data Classes

- **PaletteResult**: Holds dominant, vibrant, muted, darkVibrant, darkMuted, lightVibrant colors extracted from artwork
- **DspProfileEntity**: Room entity with composite key (trackId, deviceId) storing full DSP parameter set
- **PlayEventEntity**: Room entity recording track play events with timestamp, duration, and completion status
- **AudioQualityInfo**: Data class holding sampleRate, bitDepth, codec, isHiRes, and isDvcActive
- **BassConfig**: Data class for sub-bass gain, mid-bass gain, harmonic saturation, limiter settings, and preset type
- **CrossfadeConfig**: Data class for crossfade enabled state, duration (1000–12000ms), and curve type
- **TimerState**: Data class for sleep timer active state, remaining time, total time, and fading status
- **TopTrackResult**: Aggregated result with trackId, title, artist, artwork, total play time, and play count
- **OnboardingState**: UI state for onboarding flow step, selected languages, genres, and artists

---

## Correctness Properties

### Property 1: Palette Contrast Compliance
For any extracted palette color applied as a text background in Dark or AMOLED mode, the computed contrast ratio between the text color and background color meets WCAG AA minimum (4.5:1 for body text, 3:1 for large text).

**Validates: Requirements 1.5, 21.1, 21.2, 21.3**

**Tested by:** Generating random palette colors, applying the PaletteConstrainer, and verifying contrast ratios.

### Property 2: Hi-Res Badge Correctness
For any AudioQualityInfo instance, the `isHiRes` field is true if and only if `sampleRate > 44100 OR bitDepth > 16`.

**Validates: Requirements 3.2**

**Tested by:** Generating arbitrary sample rate and bit depth values and verifying the badge logic.

### Property 3: DSP Chain Order Invariant
For any DSP parameter configuration applied to the engine, the processing modules are always invoked in the order: preamp → equalizer → bass boost → stereo widening → limiter → output.

**Validates: Requirements 3.3**

**Tested by:** Instrumenting the DSP chain and verifying call order for random parameter sets.

### Property 4: Per-Device EQ Profile Round-Trip
For any DspParams saved for a device identifier, loading the profile for that same device identifier returns parameters equal to the saved values.

**Validates: Requirements 3.5**

**Tested by:** Generating random DspParams, saving, loading, and asserting equality.

### Property 5: Crossfade Duration Validation
For any crossfade duration value, the system accepts values in [1000, 12000] ms and rejects values outside this range.

**Validates: Requirements 4.2**

**Tested by:** Generating arbitrary Long values and verifying acceptance/rejection.

### Property 6: Equal-Power Crossfade Energy Conservation
For any time point t in [0, 1] during a crossfade, the sum of squared fade-out gain and squared fade-in gain equals 1.0 (within floating-point tolerance of 0.001).

**Validates: Requirements 4.3**

**Tested by:** Generating random time values in [0,1] and verifying the energy equation.

### Property 7: Bass Limiter Clipping Prevention
For any bass configuration with any gain values applied to any input signal level, the output signal level reported by the limiter stage does not exceed 0dBFS.

**Validates: Requirements 5.3**

**Tested by:** Generating random bass gain configurations and verifying output bounds.

### Property 8: Autoplay Exclusion of Blocked Tracks
For any set of blocked track IDs and any generated autoplay recommendation list, no track ID in the recommendation list appears in the blocked set.

**Validates: Requirements 12.2**

**Tested by:** Generating random blocked sets and recommendation candidates, verifying exclusion.

### Property 9: Autoplay No-Repeat Window
For any sequence of autoplay selections, no track ID appears more than once within any sliding window of 10 consecutive selections.

**Validates: Requirements 12.3**

**Tested by:** Generating autoplay sequences and verifying the no-repeat invariant.

### Property 10: Per-Track DSP Profile Round-Trip
For any DspParams saved for a track identifier, loading the profile for that track returns parameters equal to the saved values.

**Validates: Requirements 13.1, 13.2**

**Tested by:** Generating random DspParams and track IDs, saving, loading, and asserting equality.

### Property 11: Sleep Timer Volume Fade Linearity
For any time t in [0, 30] seconds remaining, the computed volume equals t/30 of the original volume (within tolerance of 0.01).

**Validates: Requirements 14.2**

**Tested by:** Generating random remaining-time values in [0,30] and verifying the linear relationship.

### Property 12: Null Metadata Handling
For any combination of null/missing metadata fields (title, artist, artwork), the system produces valid placeholder display values without throwing exceptions.

**Validates: Requirements 15.4**

**Tested by:** Generating MediaItem instances with random null field combinations and verifying no exceptions.

### Property 13: Touch Target Minimum Size
For all interactive composable elements, the measured minimum dimension is at least 48dp.

**Validates: Requirements 19.1**

**Tested by:** Semantic tree traversal verifying clickable nodes have minimum bounds.

### Property 14: Content Description Completeness
For all icon button composable elements, the contentDescription property is non-null and non-empty.

**Validates: Requirements 19.2**

**Tested by:** Semantic tree traversal verifying all clickable icon nodes have descriptions.

### Property 15: Play Event Recording Round-Trip
For any play event recorded to the history database, querying recent history returns an entry with matching trackId, timestamp, and playedMs values.

**Validates: Requirements 20.1**

**Tested by:** Generating random play events, recording them, and verifying retrieval.

### Property 16: Top Tracks Sort Order
For any set of play events, the computed Top Tracks list is sorted in strictly descending order by total play time.

**Validates: Requirements 20.2**

**Tested by:** Generating random play event sets and verifying sort order of results.

### Property 17: Onboarding Preferences Persistence Round-Trip
For any set of selected languages, genres, and artists saved during onboarding, loading preferences returns the same sets.

**Validates: Requirements 11.3**

**Tested by:** Generating random preference sets, saving, loading, and asserting equality.

### Property 18: WCAG Text Contrast Compliance
For any text element rendered against its computed background color, the contrast ratio meets WCAG AA requirements (4.5:1 for body, 3:1 for large text).

**Validates: Requirements 19.5**

**Tested by:** Generating random background colors, applying theme constraints, and verifying contrast.

---

## Error Handling

### Playback Errors
- **Network failure / corrupt file / unsupported codec:** The DSP Engine logs the error, skips to the next track in the queue, and displays a non-intrusive error toast. Playback is never interrupted for the user.
- **Audio session loss:** If the system reclaims the audio session, the DSP Engine re-attaches to a new session within 500ms when playback resumes.
- **Crossfade buffer failure:** If the next track fails to buffer before the crossfade window begins, the system falls back to gapless playback without crossfade.

### DSP Profile Errors
- **Profile load failure:** If a per-track or per-device profile cannot be loaded (corrupt data, migration issue), the system falls back to the global profile and logs a warning.
- **Profile save failure:** If saving fails (disk full, database error), the user is notified via a toast and the current in-memory settings remain active.

### UI and Rendering Errors
- **Null/missing metadata:** The Quality Framework substitutes defined placeholder values (e.g., "Unknown Title", "Unknown Artist", default artwork) without throwing exceptions.
- **Palette extraction failure:** If artwork is unavailable or extraction fails, the Color Theming System falls back to a neutral dark palette without visual glitches.
- **Configuration changes (rotation, split-screen, fold):** The Now Playing Screen preserves playback state and UI state without visual discontinuity via ViewModel state retention.

### Media Notification Errors
- **Service killed by system:** When the playback service restarts, the Media Surface restores the notification with correct playback state and metadata.
- **Artwork loading failure:** If notification artwork cannot be loaded, a default placeholder bitmap is used to prevent notification layout issues.

### Sleep Timer Errors
- **Process death during countdown:** WorkManager ensures the timer persists across process death. On restart, the timer resumes from the last known remaining time.
- **Volume restoration failure:** If the user cancels the timer and volume restoration fails, the system resets volume to the system default level.

### Personalization Errors
- **Autoplay generation timeout (>500ms):** Playback pauses gracefully rather than playing silence or crashing.
- **History database write failure:** Play events are buffered in memory and retried on next opportunity; the user experience is not interrupted.

---

## Performance Budgets

| Operation | Budget | Fallback |
|-----------|--------|----------|
| Palette extraction | 150ms | Use cached/default palette |
| DSP profile load | 100ms | Use global profile |
| Device EQ switch | 200ms | Continue with previous profile |
| Autoplay generation | 500ms | Pause playback gracefully |
| Frame render (animations) | 16ms | Reduce blur/glass effects |
| Glass shader (GPU) | 4ms | Disable AGSL, use static overlay |
| Notification metadata update | 100ms | Update on next tick |
| FFT visualizer (degraded) | 33ms (30fps) | Reduce band count |

---

## Migration Strategy

1. **Database migration:** Add new tables via Room auto-migration (additive only, no breaking changes)
2. **DSP Engine:** Extend existing `DspParams` data class with new bass fields (backward compatible with defaults)
3. **UI:** New composables added alongside existing ones; existing `NowPlayingScreen` refactored incrementally
4. **Feature flags:** Each pillar can be enabled/disabled independently via DataStore preferences for staged rollout

---

## Testing Strategy

- **Unit tests:** DSP profile manager, palette constrainer, crossfade math, sleep timer logic, history repository
- **Property-based tests:** All 18 correctness properties above using kotlin-test with Kotest property testing
- **UI tests:** Compose UI tests for Now Playing layout, onboarding flow, accessibility compliance
- **Integration tests:** Media notification on real devices, OEM skin compatibility, audio session recovery

# DeepEye Music Pro — Full Project Audit Report (PROJECT_AUDIT.md)

This document represents the full architectural, system, and quality audit of the DeepEye Music Pro codebase. It maps out subsystems, dependency relationships, playback routes, risk matrices, and crash maps.

---

## 1. System Architecture Map

The project conforms to a Clean Architecture style with Hilt-driven dependency injection:

```mermaid
graph TD
    UI[Compose UI Layer] --> VM[ViewModels]
    VM --> Domain[Domain Layer / Use Cases / Models]
    Domain --> Data[Data Layer / Repositories]
    Data --> DB[(Room SQLite Database)]
    Data --> Network[YouTube / SponsorBlock Remote APIs]
    Data --> Player[Media3 ExoPlayer Engine]
    Player --> DSP[DSP hardware AudioEffects Engine]
```

---

## 2. Dependency Graph (Modules & Libraries)

```
app (com.deepeye.musicpro)
  ├── androidx.compose (UI Layer)
  ├── androidx.media3 (ExoPlayer, MediaSession, Notification Engine)
  ├── dagger.hilt (Dependency Injection Framework)
  ├── androidx.room (Persistence Layer)
  ├── kotlinx.coroutines & Flow (Concurrency & State streaming)
  └── gson (JSON serialization layer)
```

---

## 3. Playback State & Runtime Graph

This diagram logs the lifecycle transitions of playback states inside `PlayerController` and the ExoPlayer engine:

```mermaid
stateDiagram-v2
    [*] --> IDLE
    IDLE --> RESOLVING : playMedia(item)
    RESOLVING --> BUFFERING : Source URL resolved / prepare()
    BUFFERING --> PLAYING : onPlaybackStateChanged(READY) / play()
    PLAYING --> PAUSED : togglePlayPause()
    PAUSED --> PLAYING : togglePlayPause()
    PLAYING --> ENDED : onPlaybackStateChanged(ENDED)
    ENDED --> RESOLVING : next()
    RESOLVING --> ERROR : Extraction Failed / onPlayerError
    ERROR --> RESOLVING : Toast notification -> next() after 500ms
```

---

## 4. UI Navigation Graph

This diagram charts tab switching and screen expansions:

```mermaid
graph LR
    Home[Home Screen] --> Music[Music Tab]
    Home --> YouTube[YouTube Tab]
    Home --> Library[Library Tab]
    Home --> Profile[Profile Tab]
    Home --> DSP[DSP screen / Equalizer Card]
    Home --> NowPlaying[Now Playing Card / Player]
    NowPlaying --> Queue[Queue Drawer Sheet]
```

---

## 5. Risk Map & Crash Resolution Log

| Subsystem | Risk Description | Severity | Mitigation / Resolution |
| :--- | :--- | :---: | :--- |
| **Playback** | Network drops or source extraction failures could cause crashes. | `High` | Added safety try-catch block inside `PlayerController.onPlayerError` that toast-notifies the user and skips to next track. |
| **DSP Engine** | Leaking Android hardware audio effects slot allocations on transition errors. | `High` | `DSPEngine.kt` implements an outer catch block calling `releaseSession()` to release partially allocated effects slots. |
| **State Sync**| UI progress and control keys desyncing from lockscreen MediaSession notifications. | `Medium`| Unified all controllers under native Media3 `MediaSession` connected directly to the singleton player instance. |
| **History DB**| SQLite lockups and thread blocks during queue snapshot serialization. | `Medium`| Implemented a 1-second debounce (`queueManager.queue.debounce(1000L)`) in `PlayerController` for queue snapshotting. |

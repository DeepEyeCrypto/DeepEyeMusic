# DeepEyeMusicPro — Architecture Overview

## Architecture Pattern

**Clean Architecture + MVI (Model-View-Intent)**

```
┌─────────────────────────────────────────────┐
│                UI Layer                      │
│  Compose Screens → ViewModels → UiState      │
├─────────────────────────────────────────────┤
│              Domain Layer                    │
│  Use Cases → Repository Interfaces           │
│  Models (Song, Album, Artist, Playlist)      │
├─────────────────────────────────────────────┤
│               Data Layer                     │
│  Room DB ← MediaStore Scanner                │
│  DataStore Preferences                       │
│  Repository Implementations                 │
├─────────────────────────────────────────────┤
│              DSP Layer                       │
│  V4AEngine (AudioEffect chain)               │
│  AudioSessionManager ↔ ExoPlayer             │
│  DspDatabase (presets)                       │
├─────────────────────────────────────────────┤
│             Player Layer                     │
│  MusicPlayerService (Media3)                 │
│  PlayerController                            │
│  QueueManager                                │
│  VisualizerEngine                            │
└─────────────────────────────────────────────┘
```

## Dependency Flow

- **UI → Domain**: ViewModels inject Use Cases
- **Domain → Data**: Use Cases call Repository interfaces
- **Data implements Domain**: Repository implementations inject DAOs and scanners
- **Player → DSP**: MusicPlayerService wires AudioSessionManager → V4AEngine
- **No reverse dependencies**: Domain has zero Android imports

## Dependency Injection

All DI is managed via **Hilt (Dagger)**:

| Module            | Scope       | Provides                                |
|-------------------|-------------|-----------------------------------------|
| `AppModule`       | Singleton   | Application Context                     |
| `DatabaseModule`  | Singleton   | AppDatabase, SongDao, PlaylistDao       |
| `PlayerModule`    | Singleton   | ExoPlayer, AudioAttributes              |
| `NetworkModule`   | Singleton   | Gson                                    |
| `V4AModule`       | Singleton   | DspDatabase, DspPresetDao               |
| `RepositoryModule`| Singleton   | MusicRepository, PlaylistRepository     |

## Key Design Decisions

1. **Room as single source of truth** — MediaStore is scanned and results cached in Room. UI always reads from Room.
2. **V4AEngine as Singleton** — only one AudioEffect chain per app. Symmetric attach/detach prevents leaks.
3. **QueueManager decoupled from ExoPlayer** — manages queue logic independently, PlayerController bridges to ExoPlayer.
4. **DataStore for settings** — lightweight, type-safe, reactive preferences.
5. **GainBudget calculator** — real-time headroom analysis prevents audio clipping.

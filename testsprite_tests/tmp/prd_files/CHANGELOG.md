## v1.0.0 (2026-06-15)

### 🎉 Features
- **Onboarding**: Language/Singer/Genre/Mood selection
- **Autoplay**: 4-Layer Recommendation Engine (InnerTube + History + Scoring)
- **Gamification**: Daily streaks, 8 badges, reward points system
- **AI Radio**: Voice prompt bar + time-aware mood curation
- **Premium UI**: Liquid Glass + Material 3 Expressive design

### 🔥 Addiction Triggers
- Instant autoplay (no pause = no thinking)
- Daily streak progress bar + flame icon
- Achievement popups with Instagram share
- Time-aware mood auto-curation (morning=energy, night=chill)
- AI voice: "play romantic Hindi songs for rain"

### 🛠 Technical
- Minified + obfuscated (R8)
- Hilt + Jetpack Compose optimized
- DataStore persistence
- Media3 playback stack

## v1.0.1 (2026-06-16)

### 🚀 Performance & Architecture
- **Statically Linked Extractor**: Merged NewPipe extractor directly into the core app, eliminating dynamic DexClassLoader overhead.
- **Zero Reflection Overhead**: Faster boot times and instantaneous search loading.
- **Improved Stability**: Removed complex module bridge architecture to prevent class mismatch errors.

# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK proguard file.

# Keep DSP engine classes (reflection-based AudioEffect instantiation)
-keep class com.deepeye.musicpro.dsp.engine.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }

# Media3
-keep class androidx.media3.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.deepeye.musicpro.domain.model.** { *; }
-keep class com.deepeye.musicpro.dsp.model.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# NewPipe Extractor
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**
-keep class com.deepeye.musicpro.data.source.remote.youtube.** { *; }

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# ── Phase 9: Release Hardening Additions ──

# Haze (glassmorphism blur library)
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**

# DataStore Preferences
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Paging 3
-keep class androidx.paging.** { *; }

# Compose Stability Annotations
-keep @androidx.compose.runtime.Immutable class * { *; }
-keep @androidx.compose.runtime.Stable class * { *; }

# Project cache & library entities (Room reflection)
-keep class com.deepeye.musicpro.data.cache.entities.** { *; }
-keep class com.deepeye.musicpro.data.library.entities.** { *; }
-keep class com.deepeye.musicpro.data.db.** { *; }

# Recommendation models (Gson serialization via CacheManager)
-keep class com.deepeye.musicpro.domain.recommendation.** { *; }

# Palette (album art color extraction)
-keep class androidx.palette.** { *; }


-dontwarn com.google.re2j.**
-dontwarn java.beans.**
-dontwarn javax.script.**


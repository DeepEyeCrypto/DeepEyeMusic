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

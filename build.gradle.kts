// Top-level build file — DeepEyeMusicPro
// R8-OMEGA NOTE (2026-10-11): Overriding the bundled R8 (classpath "com.android.tools:r8:8.13.19")
// was trialed and REVERTED. Although R8 8.13.19 parsed all Kotlin 2.2/2.3 metadata with zero
// metadata warnings, the AGP 8.7.3 ↔ R8 8.13.19 pairing made minifyReleaseWithR8 pathologically
// slow (>34 min CPU on the single minify task vs. ~42 min for the ENTIRE clean build on the
// bundled R8). The bundled R8 produces a fully green assembleRelease; the ~986 metadata warnings
// are benign (R8 skips unparseable metadata and proceeds). To kill the warnings legitimately,
// bump AGP+Kotlin together per https://developer.android.com/studio/build/kotlin-d8-r8-versions.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
    // google-services plugin removed (Firebase dependencies removed)
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

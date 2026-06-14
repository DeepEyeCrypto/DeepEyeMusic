// Top-level build file — DeepEyeMusicPro
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

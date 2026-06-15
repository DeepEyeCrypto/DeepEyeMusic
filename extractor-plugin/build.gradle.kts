plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.deepeye.musicpro.extractor.plugin"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.deepeye.musicpro.extractor.plugin"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
dependencies {
    implementation(project(":extractor-bridge"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(libs.newpipe.extractor.kmp)
    implementation("io.github.ajaydhattarwal:youtube-extractor-android:1.0.2")
    implementation(libs.okhttp)
}

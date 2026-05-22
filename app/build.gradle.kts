import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.deepeye.musicpro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.deepeye.musicpro"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export for migration verification
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties()
    if (keystorePropsFile.exists()) {
        keystoreProps.load(FileInputStream(keystorePropsFile))
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_FILE")
                ?: (keystoreProps["storeFile"] as? String)
                ?: "keystore.jks"
            storeFile = if (keystorePath.startsWith("/")) {
                file(keystorePath)
            } else {
                rootProject.file(keystorePath)
            }
            storePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: (keystoreProps["storePassword"] as? String)
                ?: ""
            keyAlias = System.getenv("KEY_ALIAS")
                ?: (keystoreProps["keyAlias"] as? String)
                ?: ""
            keyPassword = System.getenv("KEY_PASSWORD")
                ?: (keystoreProps["keyPassword"] as? String)
                ?: ""
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Ensure Kotlin uses JDK 21 toolchain to avoid Java version parsing issues
    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // AndroidX Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Adaptive Layouts
    implementation(libs.compose.m3.adaptive)
    implementation(libs.compose.adaptive.navigation.suite)
    implementation(libs.androidx.window)
    implementation(libs.compose.material3.window.size)


    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Media3 / ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation("androidx.media:media:1.7.0")

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // DataStore Preferences
    implementation(libs.datastore.prefs)

    // Gson
    implementation(libs.gson)

    // Coil (image loading for Compose)
    implementation(libs.coil.compose)

    // LeakCanary (debug only)
    // debugImplementation(libs.leakcanary)

    // Paging
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // NewPipe Extractor & Network
    implementation(libs.newpipe.extractor.kmp)
    implementation(libs.okhttp)

    // Palette & Splash
    implementation(libs.palette)
    implementation(libs.splashscreen)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

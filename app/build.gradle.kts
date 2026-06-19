import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("io.gitlab.arturbosch.detekt")
    id("com.google.gms.google-services")
    id("jacoco")
}

android {
    namespace = "com.deepeye.musicpro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.deepeye.musicpro"
        minSdk = 24
        targetSdk = 35
        versionCode = 306
        versionName = "3.0.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Build config for YouTube API
        val youtubeApiKey = project.findProperty("youtubeApiKey") as? String ?: ""
        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"")

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
            val keystorePath =
                System.getenv("KEYSTORE_FILE")
                    ?: (keystoreProps["storeFile"] as? String)
                    ?: "keystore.jks"
            storeFile =
                if (keystorePath.startsWith("/")) {
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
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    // Ensure Kotlin uses JDK 17 toolchain to match compile options
    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    detekt {
        config.setFrom(file("../config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        allRules = false
        autoCorrect = true
        ignoreFailures = true
    }
}

dependencies {
    // Firebase BOM & Analytics & Auth & Firestore & Messaging
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")

    // Credential Manager for Google Sign-In
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

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

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")

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
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Media3 / ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.3.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.3.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.0")
    implementation("androidx.media3:media3-session:1.3.0")
    implementation("androidx.media3:media3-ui:1.3.0")
    implementation("androidx.media:media:1.7.0")

    // Coroutines & Collections
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.kotlinx.collections.immutable)

    // DataStore Preferences
    implementation(libs.datastore.prefs)

    // Gson
    implementation(libs.gson)

    // Coil (image loading for Compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.coil.video)
    // Haze (glassmorphism blur)
    implementation(libs.haze.compose)
    implementation(libs.liquidglass)



    // LeakCanary (debug only)
    // debugImplementation(libs.leakcanary)

    // Paging
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // NewPipe Extractor (primary) and YouTube Extractor (fallback)
    implementation(libs.newpipe.extractor.kmp)
    implementation(libs.okhttp)
    implementation("io.github.ajaydhattarwal:youtube-extractor-android:1.0.2")

    // Palette & Splash
    implementation(libs.palette)
    implementation(libs.splashscreen)
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.mockk)
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.robolectric:robolectric:4.12")
    testImplementation("org.json:json:20231013")
    
    // Instrumented tests
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
    androidTestImplementation("org.mockito:mockito-android:5.7.0")
    androidTestImplementation("com.google.android.apps.mousewheel:mousewheel:1.0.0")
    
    // Compose Testing
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}

android {
    testOptions {
        unitTests {
            all {
                it.jvmArgs("-noverify")
            }
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

configurations.all {
    exclude(group = "com.google.protobuf", module = "protobuf-lite")
    resolutionStrategy {
        force("com.google.protobuf:protobuf-javalite:3.22.3")
    }
}

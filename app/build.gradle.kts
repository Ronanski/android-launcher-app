import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Single source of truth for the app version. Bump VERSION_NAME for every release.
val versionProps = Properties().apply { rootProject.file("version.properties").inputStream().use { load(it) } }
val appVersionName: String = versionProps.getProperty("VERSION_NAME")
val (vMajor, vMinor, vPatch) = appVersionName.split(".").map { it.toInt() }

android {
    namespace = "com.ronanski.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ronanski.launcher"
        minSdk = 26
        targetSdk = 34
        versionCode = vMajor * 10000 + vMinor * 100 + vPatch
        versionName = appVersionName
        buildConfigField("String", "GITHUB_REPO", "\"Ronanski/android-launcher-app\"")
    }

    // Fixed debug keystore so every CI build has the same signature
    // (you can install updates over the old version without losing settings).
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}

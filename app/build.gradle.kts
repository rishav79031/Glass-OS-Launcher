import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Private release key (keystore/ and keystore.properties are git-ignored). Back them up: updates must use the same key.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.glasslauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.glasslauncher"
        // Android 12+: needed for RenderEffect blur. The Tab S9 FE+ ships with Android 14.
        minSdk = 31
        // Google Play requires new apps to target a recent Android release.
        targetSdk = 36
        versionCode = 65
        versionName = "1.65"
        resourceConfigurations += listOf("en", "hi", "es", "fr", "de", "pt", "ja", "ru", "ar", "zh")
    }

    signingConfigs {
        create("release") {
            if (keystoreProperties.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    // full: every feature, for installing from the APK. play: the Google Play edition, the same without the
    // Accessibility top-edge helper (Play only allows Accessibility for apps that help people with disabilities).
    // lite: no notification access, which Play Protect blocks for sideloaded apps in some regions.
    flavorDimensions += "features"
    productFlavors {
        create("full") {
            dimension = "features"
            buildConfigField("boolean", "NOTIFICATIONS", "true")
            buildConfigField("boolean", "PANEL_GUARD", "true")
            buildConfigField("boolean", "SCREEN_RECORDING", "true")
        }
        create("play") {
            dimension = "features"
            buildConfigField("boolean", "NOTIFICATIONS", "true")
            buildConfigField("boolean", "PANEL_GUARD", "false")
            buildConfigField("boolean", "SCREEN_RECORDING", "true")
        }
        create("lite") {
            dimension = "features"
            buildConfigField("boolean", "NOTIFICATIONS", "false")
            buildConfigField("boolean", "PANEL_GUARD", "false")
            buildConfigField("boolean", "SCREEN_RECORDING", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (keystoreProperties.isNotEmpty()) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Shizuku API for privileged ADB actions (Airplane mode, Wi-Fi, Mobile Data, Bluetooth toggles)
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    testImplementation("junit:junit:4.13.2")
}

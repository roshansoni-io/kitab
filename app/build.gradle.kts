import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val releaseKeystoreProperties =
    Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }

android {
    namespace = "io.github.roshansoni_io.kitab"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.roshansoni_io.kitab"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.maxHeapSize = "256m"
                it.maxParallelForks = 1
            }
        }
    }

    signingConfigs {
        create("release") {
            val configuredStoreFile = releaseKeystoreProperties.getProperty("storeFile")
            if (configuredStoreFile != null) {
                storeFile = rootProject.file(configuredStoreFile)
                storePassword = releaseKeystoreProperties.getProperty("storePassword")
                keyAlias = releaseKeystoreProperties.getProperty("keyAlias")
                keyPassword = releaseKeystoreProperties.getProperty("keyPassword")
            } else {
                // Keep release builds installable on machines without a release key.
                storeFile = File(System.getProperty("user.home"), ".android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            ndk {
                abiFilters += listOf("arm64-v8a")
            }
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Dependency versions are intentionally pinned: newer AGP/BOM/activity releases
        // require compileSdk 37 / minSdk 23, which the SDK setup used here does not provide.
        disable += listOf("AndroidGradlePluginVersion", "GradleDependency")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

val sources = listOf("src/main/**/*.kt")

val ktlintCheck by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Runs ktlint and reports style violations."
    classpath = ktlint
    mainClass = "com.pinterest.ktlint.Main"
    args(sources)
}

val ktlintFormat by tasks.registering(JavaExec::class) {
    group = "formatting"
    description = "Fixes ktlint style violations in place."
    classpath = ktlint
    mainClass = "com.pinterest.ktlint.Main"
    args(listOf("-F") + sources)
}

tasks.named("check") {
    dependsOn(ktlintCheck)
}

val ktlint by configurations.creating {
    isCanBeConsumed = false
    isVisible = false
}

dependencies {
    ktlint(libs.ktlint.cli)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.artifex.mupdf.fitz)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
}

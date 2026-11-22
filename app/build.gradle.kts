import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val releaseKeystoreProperties =
    Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }

val releaseStoreFile = releaseKeystoreProperties.getProperty("storeFile")?.let { rootProject.file(it) }
val releaseStorePassword = releaseKeystoreProperties.getProperty("storePassword")
val releaseKeyAlias = releaseKeystoreProperties.getProperty("keyAlias")
val releaseKeyPassword = releaseKeystoreProperties.getProperty("keyPassword")
val hasReleaseSigningConfig =
    releaseStoreFile?.isFile == true &&
        !releaseStorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "io.github.roshansoni_io.kitab"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.roshansoni_io.kitab"
        minSdk = 24
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
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
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
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Dependency versions are intentionally pinned: newer BOM releases
        // require compileSdk 37 / minSdk 24 / AGP 9.1.1, all configured here.
        disable += listOf("AndroidGradlePluginVersion", "GradleDependency")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.configureEach {
    if (name == "packageRelease" || name == "bundleRelease") {
        doFirst {
            check(hasReleaseSigningConfig) {
                "Configure release signing in keystore.properties before building a release APK or bundle."
            }
        }
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
}

dependencies {
    ktlint(libs.ktlint.cli)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.tracing)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.artifex.mupdf.fitz)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.material.kolor)
    implementation(libs.androidx.compose.material.icons.core)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
}

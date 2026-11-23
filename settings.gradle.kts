pluginManagement {
    plugins {
        id("com.android.settings") version "9.1.1" apply false
    }
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.android.settings")
}

android {
    execution {
        profiles {
            create("local") {
                r8 {
                    runInSeparateProcess = true
                    jvmOptions += listOf("-Xmx2g", "-XX:+UseParallelGC")
                }
            }
            create("ci") {
                r8 {
                    runInSeparateProcess = true
                    jvmOptions +=
                        listOf(
                            "-Xms1g",
                            "-Xmx4g",
                            "-XX:+UseParallelGC",
                            "-XX:+HeapDumpOnOutOfMemoryError",
                        )
                }
            }
            defaultProfile = "local"
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("$rootDir/third_party/maven")
            content {
                includeGroup("com.artifex.mupdf")
            }
        }
        maven {
            url = uri("https://maven.ghostscript.com")
            content {
                includeGroup("com.artifex.mupdf")
            }
        }
    }
}

rootProject.name = "kitab"
include(":app")

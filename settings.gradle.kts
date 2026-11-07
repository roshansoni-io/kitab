pluginManagement {
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

# kitab

Minimal Android app template (Vanilla Kotlin, no Jetpack Compose/dependencies) designed for building on-device in Termux.

## Requirements (Termux)

- `pkg install openjdk-17 gradle android-sdk aapt2` plus the `termuxvoid` `android-sdk` package
- Licenses accepted in `$ANDROID_HOME/licenses/`

## Build

```sh
./gradlew assembleDebug          # output: app/build/outputs/apk/debug/
```

## Structure

```
app/src/main/java/.../MainActivity.kt
app/src/main/res/
app/src/main/AndroidManifest.xml
gradle/libs.versions.toml         # AGP / Kotlin plugin versions
```

## Renaming the package

Replace all `io.github.roshansoni_io.kitab` occurrences in:

- `app/build.gradle.kts` (`namespace`, `applicationId`)
- `app/src/main/java/...`
- `settings.gradle.kts` (`rootProject.name`)

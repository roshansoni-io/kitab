# Kitab

<p align="center">
  <strong>An open-source ebook reader for Android.</strong>
  <br />
  Built with Kotlin, Jetpack Compose, and Material 3.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?logo=android" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Design-Material%203-757575" alt="Material 3" />
  <img src="https://img.shields.io/badge/Status-Beta-orange" alt="Beta" />
  <img src="https://img.shields.io/badge/License-AGPL--3.0-blue" alt="AGPL 3.0" />
</p>

## About

Kitab is an open-source ebook reader for Android. It currently supports PDF documents, with additional ebook formats planned for the future.

## Features

- **PDF reading:** Read PDF documents using MuPDF.
- **Book library:** Organize and manage your books.
- **Reading progress:** Keep track of your reading progress.
- **Offline reading:** Read your documents without an internet connection.
- **Modern interface:** Built with Material 3 and Jetpack Compose.

## Planned

- EPUB support.
- Additional ebook formats.
- More reading and customization features.

## Technology stack

| Technology | Purpose |
| --- | --- |
| Kotlin | Primary programming language |
| Jetpack Compose | UI framework |
| Material 3 | Design system |
| Material 3 Expressive | UI components and design |
| MuPDF | PDF rendering engine |

## Requirements

- Android 7.0 (API 24) or higher.
- An Android device or emulator.

## Installation

### Download

Download the latest available APK from [GitHub Releases](https://github.com/roshansoni-io/kitab/releases).

### Build from source

1. Clone the repository:

   ```sh
   git clone https://github.com/roshansoni-io/kitab.git
   cd kitab
   ./gradlew assembleDebug
   ```

2. Open the project in Android Studio, allow Gradle to sync, then run the app on a device or emulator.

## Development status

Kitab is in beta and under active development. Features and interfaces may change as the project evolves. Contributions, suggestions, and bug reports are welcome.

## Contributing

Contributions are welcome. For major changes, open an issue first to discuss the proposal. For smaller changes:

1. Fork the repository.
2. Create a branch for your changes.
3. Make and commit your changes.
4. Submit a pull request.

## License

Kitab is licensed under the GNU Affero General Public License v3.0 (AGPL-3.0). See the [LICENSE](LICENSE) file for details.

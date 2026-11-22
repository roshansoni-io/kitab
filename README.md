# Kitab

Kitab is a lightweight Android PDF reader with a personal library and a focused reading experience. It is built with Kotlin and Jetpack Compose, and uses MuPDF to open and render PDF documents.

## Features

- Open PDF files from the device or through Android's open-with flow.
- Keep PDFs in the Kitab library and find recently opened books.
- Search and filter the recent and library collections, and star books for quick access.
- Read with horizontal or vertical page navigation, a page slider, and pinch-to-zoom up to 10×.
- Select and copy or share text, and confirm before opening links in a PDF.
- Choose light, dark, or system theme, with Android dynamic colors or a custom Material 3 color.
- Adjust page tone, reading direction, resume behavior, selection color, and reader controls.

## Requirements

- Android 7.0 (API 24) or newer.
- Android Studio with JDK 17 and Android SDK Platform 37 to build from source.

## Build

Open this project in Android Studio and run the `app` configuration, or build a debug APK from the project root:

```sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

## Release signing

Release APKs and bundles must be signed with a configured release key. Put `storeFile`, `storePassword`, `keyAlias`, and `keyPassword` in the ignored local `keystore.properties` file. The release build fails if the keystore file or any signing property is missing; it never falls back to the Android debug key. Keep the keystore and its passwords out of source control.

## Usage

Use **Add Book** to choose a PDF on your device. Added PDFs are stored in Kitab’s private app storage and appear in the library; opening a PDF also places it in Recent. Imported files stay private to Kitab, and the Android document picker grants access only to the PDF you select. Tap a book to read it. In the reader, swipe or use the page controls to move between pages, pinch to zoom, and tap a link to review it before opening it outside Kitab.

Kitab currently supports PDF documents. Password-protected PDFs are not supported.

## Bug reports and feature requests

Found a bug or have an idea for Kitab? [Open an issue](https://github.com/roshansoni-io/kitab/issues) with steps to reproduce the problem or details about the feature you would like.

If Kitab is useful to you, please [star the repository](https://github.com/roshansoni-io/kitab) to support the project.

## License

Kitab is licensed under the GNU Affero General Public License, version 3. See [LICENSE](LICENSE) for the complete terms. MuPDF is a separate third-party component and is also distributed under its own license terms; see the in-app **Open-source licenses** information and the [MuPDF project](https://mupdf.com/).

## Acknowledgements

- [MuPDF](https://mupdf.com/) provides PDF document handling and rendering.
- The project structure and feature overview follow familiar patterns used by open-source readers such as [KOReader](https://github.com/koreader/koreader) and [Librera Reader](https://github.com/foobnix/LibreraReader). Kitab is an independent project.

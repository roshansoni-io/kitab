# Dependency wiring

`AppContainer` is the app's manual composition root. It creates the application-scoped `BookRepository`, `SettingsRepository`, and `StorageAccess` implementations and exposes them to `MainActivity`, navigation, and feature routes.

The container currently wires `MuPdfCoverRenderer` from `feature/reader/document` into `LocalBookRepository` through the `PdfCoverRenderer` interface. Keep this concrete cross-package dependency in the composition root; repositories and ViewModels should continue to depend on contracts.

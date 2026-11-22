# Dependency wiring

`AppContainer` is the app's manual composition root. It creates the application-scoped `BookRepository` and `SettingsRepository` implementations and exposes them to `MainActivity`, navigation, and feature routes.

The container wires `BookCoverRenderer` from `core/platform` into `LocalBookRepository`; keep this concrete dependency in the composition root.

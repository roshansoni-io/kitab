# Kitab app package

This package is the Android application's composition root. `MainActivity` configures the window and app theme, handles incoming PDF `ACTION_VIEW` intents, creates the app container, and hosts `KitabRoot`.

## Package map

- [`core`](core/README.md) contains shared models, repository contracts and implementations, platform adapters, dependency wiring, and shared UI.
- [`navigation`](navigation/README.md) owns the destination graph, tab navigation, and app-shell workflows.
- [`feature`](feature/README.md) contains the Recent, Library, Settings, and Reader destinations.

The usual flow is `MainActivity` → `KitabRoot` → a feature route. Routes connect feature UI state to callbacks and repositories; repositories are the shared source of persisted book and settings data. Navigation state remains with Navigation Compose.

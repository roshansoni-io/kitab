# Features

Feature packages own destination-specific UI and state. Routes connect a screen to its ViewModel and app-level callbacks; screens render the provided state and forward user actions. Shared book/settings data remains in [`core/data`](../core/data/README.md).

- [`recent`](recent/README.md) presents recently opened books.
- [`library`](library/README.md) presents the full collection with search, filters, and list/grid choice.
- [`settings`](settings/README.md) edits appearance preferences and displays app information.
- [`reader`](reader/README.md) opens a PDF and provides page reading, zoom, links, and text selection.

Recent, Library, and Settings are bottom-bar tabs. Reader is a separate destination launched from a book action.

Recent and Library expose immutable `StateFlow` UI state from their ViewModels and collect it lifecycle-aware in routes. Reader currently keeps transient reading gestures and presentation state in Compose state; it reports durable reading progress through the app shell to `BookRepository`.

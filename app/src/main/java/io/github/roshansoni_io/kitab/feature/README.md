# Features

Feature packages own destination-specific UI and state. Routes connect a screen to its ViewModel and app-level callbacks; screens render the provided state and forward user actions. Shared book/settings data remains in [`core/data`](../core/data/README.md).

- [`collection`](collection/README.md) presents recent and library books with search and the library list/grid choice.
- [`settings`](settings/README.md) edits appearance preferences and displays app information.
- [`reader`](reader/README.md) opens a PDF and provides page reading, zoom, links, and text selection.

Recent, Library, and Settings are bottom-bar tabs. Reader is a separate destination launched from a book action.

Collection and Settings expose immutable `StateFlow` UI state from their ViewModels and collect it lifecycle-aware in routes. Reader currently keeps transient reading gestures and presentation state in Compose state; it reports durable reading progress through the app shell to `BookRepository`.

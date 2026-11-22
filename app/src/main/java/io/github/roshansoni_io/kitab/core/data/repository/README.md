# Repository contracts and implementations

`BookRepository` and `SettingsRepository` are the shared interfaces used by feature ViewModels. Their `StateFlow` properties provide observable state; operations update the repository source of truth. Settings persistence covers appearance, reader behavior, selection, zoom, and library layout.

- `LocalBookRepository` stores copied PDFs in app-private `files/books/`, stores generated cover thumbnails in `files/thumbnails/`, and persists book metadata and reading progress. It generates a thumbnail during import or on the first cover request, then loads that saved image for Recent and Library.
- `SharedPreferencesSettingsRepository` persists theme and dynamic-color choices.
- `BookCoverRenderer` uses Android `PdfRenderer` to create and load saved cover thumbnails.

Keep storage and Android-specific behavior in repository implementations. Add persistence behavior to the interface only when a feature needs it.

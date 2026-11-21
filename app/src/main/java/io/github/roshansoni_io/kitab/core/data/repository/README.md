# Repository contracts and implementations

`BookRepository` and `SettingsRepository` are the shared interfaces used by feature ViewModels. Their `StateFlow` properties provide observable state; operations update the repository source of truth. Settings persistence covers appearance, reader behavior, selection, zoom, and library layout.

- `LocalBookRepository` stores the book list and manages copied PDFs and cached cover images.
- `SharedPreferencesSettingsRepository` persists theme and dynamic-color choices.
- `PdfCoverRenderer` is the narrow PDF-rendering port used to create a cover without coupling the book repository to MuPDF.

Keep storage and Android-specific behavior in repository implementations. Add persistence behavior to the interface only when a feature needs it.
